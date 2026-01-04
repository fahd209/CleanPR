package com.fahd.cleanPR.handler;

import com.fahd.cleanPR.model.*;
import com.fahd.cleanPR.repository.InstallationRepository;
import com.fahd.cleanPR.repository.PullRequestRepository;
import com.fahd.cleanPR.repository.RepoRepository;
import com.fahd.cleanPR.util.GitHubServiceCaller;
import com.fahd.cleanPR.util.OpenAiCaller;
import com.fahd.cleanPR.util.TokenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PullRequestHandler extends BaseEventHandler{

    private final PullRequestRepository pullRequestRepository;

    private final TokenService tokenService;

    private final InstallationRepository  installationRepository;

    private final RepoRepository repoRepository;

    private final GitHubServiceCaller gitHubServiceCaller;

    private final OpenAiCaller openAiCaller;

    public PullRequestHandler(final PullRequestRepository pullRequestRepository,
                              final TokenService tokenService,
                              final InstallationRepository installationRepository,
                              final RepoRepository repoRepository,
                              final GitHubServiceCaller gitHubServiceCaller,
                              final OpenAiCaller openAiCaller) {

        this.pullRequestRepository = pullRequestRepository;
        this.tokenService = tokenService;
        this.installationRepository = installationRepository;
        this.repoRepository = repoRepository;
        this.gitHubServiceCaller = gitHubServiceCaller;
        this.openAiCaller = openAiCaller;
    }

    @Override
    public void triggerEvent(Map<String, Object> webHookPayload, String action) {
        logInfo(String.format("event triggered for action={ %s }", action));

        try {
            switch (action) {
                case "opened":
                    handleOpenedPullRequest(webHookPayload);
                    break;
                case "closed":
                    handleClosedPullRequest(webHookPayload);
                    break;
                default:
                    logInfo(String.format("no handler for this action={ %s }", action));
            }
        } catch (Exception e) {
            logError(String.format("error while handling action={ %s }, error={ %s } ",  action, e.getMessage()));
        }
    }

    /**
     *  when a pull request is closed
     *  this function will be triggered
     *  to change the status of the PR
     * */
    private void handleClosedPullRequest(Map<String, Object> webHookPayload) {
        Map<String, Object> pullRequestInfo =  (Map<String, Object>) webHookPayload.get("pull_request");
        Optional<PullRequest> pullRequest = pullRequestRepository.findById((long) pullRequestInfo.get("id"));
        String mergedAt = (String) pullRequestInfo.get("merged_at");

        // if the merge time stamp is null then pr was not merged it was just closed
        if(pullRequest.isPresent()) {
            if (mergedAt == null) {
                pullRequest.get().setClosedAt(OffsetDateTime.now());
                pullRequest.get().setStatus(Status.CLOSED);
            } else {
                pullRequest.get().setClosedAt(OffsetDateTime.parse(mergedAt));
                pullRequest.get().setStatus(Status.MERGED);
            }
            pullRequestRepository.save(pullRequest.get());
            return;
        }
        throw new EntityNotFoundException("Pull request entity not found");
    }

    private void handleOpenedPullRequest(Map<String, Object> webHookPayload) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // 1) create pull request model
        PullRequest pullRequest = (PullRequest) createPullRequest(webHookPayload);

        // 2) insert to db - so it can be fetch by the UI
        pullRequestRepository.save(pullRequest);

        // the installation and repo are needed for installation and repo specific information
        // like the access token url or the repo full name
        Optional<Installation> installation = Optional.of(installationRepository.getReferenceById(pullRequest.getInstallationId()));
        Optional<Repo> repo = repoRepository.findById(pullRequest.getRepoId());

        if (installation.isEmpty()) {
            logError(String.format("installation not found for pullRequestId={ %s }, installationI={ %s }", pullRequest.getId(), pullRequest.getInstallationId()));
            throw new EntityNotFoundException("Installation not found");
        }

        if (repo.isEmpty()) {
            logError(String.format("installation not found for pullRequestId={ %s }, installationI={ %s }", pullRequest.getId(), pullRequest.getInstallationId()));
            throw new EntityNotFoundException("Repo not found");
        }

        // 3) get the github api access token
        String accessToken = tokenService.getAccessToken(installation.get().getAccessTokenUrl());

        // post welcome message and let the user know they're code is being review
        int pullRequestNumber = (int) webHookPayload.get("number");
        String pullRequestCommentUrl = String.format("/repos/%s/pulls/%d/reviews", repo.get().getRepoName(), pullRequestNumber);
        gitHubServiceCaller.postWelcomeMessage(pullRequestCommentUrl, accessToken);

        // 4) fetch the pr file paths
        String url = String.format("/repos/%s/pulls/%d/files", repo.get().getRepoName(), pullRequestNumber);
        JsonNode prFileInfo = gitHubServiceCaller.fetchFilePaths(accessToken, url);

        /**
         * The file pathObjects will only contain the code patches
         * but not all the pr files so we have to extract
         * the content urls and download the pr file
         * */
        List<Map<String, Object>> fileInfoObject = convertFilePathJsonToListOfMap(prFileInfo);
        List<String> contentUrls = getContentUrl(fileInfoObject);

        // 5) get all the files in pr and the code diff
        List<String> prFiles = gitHubServiceCaller.fetchFileContent(contentUrls, accessToken);
        List<String> codePatches = getCodePatches(fileInfoObject);


        // 6) prompt chatgpt for a code summary and code comments
        String pullRequestSummary = openAiCaller.reviewCode(codePatches, prFiles, "summary");
        String codeCommentsJson = openAiCaller.reviewCode(codePatches, prFiles, "comments");
        List<CommentDTO> codeCommentsList = convertJsonCommentsToMap(codeCommentsJson);

        // 7) post chat gpts response in the pr

        ResponseEntity<String> githubCodeReviewResponse = gitHubServiceCaller.postReview(pullRequestCommentUrl, pullRequestSummary, codeCommentsList, accessToken);

        // 8) change the status of pr to reviewed
        Optional<PullRequest> reviewedPR = pullRequestRepository.findById(pullRequest.getId());
        if (!reviewedPR.isEmpty()) {
            reviewedPR.get().setStatus(Status.REVIEWED);
            reviewedPR.get().setReviewedAt(OffsetDateTime.now());
            pullRequestRepository.save(reviewedPR.get());
        } else {
            throw new EntityNotFoundException("PullRequest not found");
        }

    }

    private List<CommentDTO> convertJsonCommentsToMap(String codeCommentsJson) throws JsonProcessingException {
        String parsedJson = codeCommentsJson.strip().replace("```json", "").replace("```", "");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(parsedJson);
        List<CommentDTO> commentList = new ArrayList<>();

        for (JsonNode commentNode : jsonNode) {
            Map<String, Object> comment = mapper.convertValue(commentNode, new TypeReference<Map<String, Object>>() {});

            CommentDTO commentDTO = CommentDTO.builder()
                    .line((int) comment.get("line"))
                    .side((String) comment.get("side"))
                    .body((String) comment.get("body"))
                    .path((String) comment.get("path"))
                    .build();

            commentList.add(commentDTO);
        }

        return commentList;
    }

    // adding all the code patches to a list
    private List<String> getCodePatches(List<Map<String, Object>> filePathObjects) {
        List<String> codePaths = new ArrayList<>();

        for (Map<String, Object> filePath : filePathObjects) {
            codePaths.add("Filename: " + filePath.get("filename") + "\n " + "code patch: " + filePath.get("patch"));
        }

        return codePaths;
    }

    private List<Map<String, Object>> convertFilePathJsonToListOfMap(JsonNode filePaths) {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> filePathObjects = mapper.convertValue(
                filePaths,
                new TypeReference<List<Map<String, Object>>>() {}
        );
        return filePathObjects;
    }


    /**
     * extracting the contentUrl from the list of object
     * converting the list of objects to a list<String>
     * */
    private List<String> getContentUrl(List<Map<String, Object>> filePaths) {
        return filePaths.stream()
                .map(obj -> {
                   String url = (String) obj.get("contents_url");

                   // decoding the url if its encoded
                   if (url.contains("%2F")) {
                       String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
                       return decodedUrl;
                   }
                   return url;
                } )
                .toList();
    }

    private PullRequest createPullRequest(Map<String, Object> webHookPayload) {
        Map<String, Object> pullRequest = (Map<String, Object>) webHookPayload.get("pull_request");
        Map<String, Object> author = (Map<String, Object>) pullRequest.get("user");
        Map<String, Object> pullRequestHead = (Map<String, Object>) pullRequest.get("head");
        Map<String, Object> repo = (Map<String, Object>) pullRequestHead.get("repo");
        Map<String, Object> owner = (Map<String, Object>) repo.get("owner");
        Map<String, Object> installation = (Map<String, Object>) webHookPayload.get("installation");

        // 1) build pr model
        long pullRequestId = (long) pullRequest.get("id");
        String title = (String) pullRequest.get("title");
        int repoOwnerId = (int) owner.get("id");
        int authorId = (int) author.get("id");
        String authorName = (String) author.get("login");
        int repoId = (int) repo.get("id");
        int installationId = (int) installation.get("id");
        String url = (String) pullRequest.get("html_url");
        String openedAt = (String) pullRequest.get("created_at");

        return PullRequest.builder()
                .Id(pullRequestId)
                .title(title)
                .repoOwnerId(repoOwnerId)
                .authorId(authorId)
                .authorName(authorName)
                .repoId(repoId)
                .installationId(installationId)
                .status(Status.OPEN)
                .openedAt(OffsetDateTime.parse(openedAt))
                .url(url)
                .build();

    }
}
