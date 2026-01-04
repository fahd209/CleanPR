package com.fahd.cleanPR.util;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fahd.cleanPR.AiSystemMessages.COMMENT_ACTION_SYSTEM_MESSAGE;
import static com.fahd.cleanPR.AiSystemMessages.SUMMARY_ACTION_SYSTEM_MESSAGE;

@Service
public class OpenAiCaller {

    private final ChatClient chatClient;

    public OpenAiCaller(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }

    /**
     * Generates a pull request summary
     * based on the user changes in the
     * pull request
     * */
    public String reviewCode(List<String> codePatches, List<String> prFiles, String action) {
        String systemMessage = getSystemMessage(action);

        String userMessage = """
                """;

        // adding code patches to the user prompt
        userMessage += "--------------------------------------------------------";
        userMessage += "Code patches:\n \n";
        for(String codePatch : codePatches) {
            userMessage += codePatch;
            userMessage += "\n ";
        }
        userMessage += "--------------------------------------------------------";


        userMessage += "\n Pull Request files: ";
        userMessage += "\n the code below is for context only";

        // adding pr files content to the user message
        for(String prFile : prFiles) {
            userMessage += prFile;
            userMessage += "\n ";
        }

        System.out.println(userMessage);

        return chatClient.prompt()
                .system(systemMessage)
                .user(userMessage)
                .call()
                .content();
    }


    private String getSystemMessage(String action) {
        Map<String, String> systemMessages = new HashMap<>();

        // code summary system message
        systemMessages.put(
                "summary",
                SUMMARY_ACTION_SYSTEM_MESSAGE
        );

        systemMessages.put(
                "comments",
                COMMENT_ACTION_SYSTEM_MESSAGE
        );

        return systemMessages.get(action);
    }

}
