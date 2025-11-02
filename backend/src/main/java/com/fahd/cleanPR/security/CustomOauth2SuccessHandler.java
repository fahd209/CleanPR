package com.fahd.cleanPR.security;

import com.fahd.cleanPR.model.Account;
import com.fahd.cleanPR.model.Profile;
import com.fahd.cleanPR.repository.AccountRepository;
import com.fahd.cleanPR.service.AccountService;
import com.fahd.cleanPR.service.ProfileService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@AllArgsConstructor

public class CustomOauth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomOauth2SuccessHandler.class);

    private final AccountRepository accountRepository;

    private final AccountService accountService;

    private final ProfileService profileService;

    private final JwtService jwtService;

    @Value("${client.url}")
    String CLIENT_URL;

    @Autowired
    public CustomOauth2SuccessHandler(AccountRepository accountRepository, AccountService accountService, ProfileService profileService, JwtService jwtService) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.profileService = profileService;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {

            // 1) extracting oauth user and getting repo urls
            LOGGER.info("Authentication successful");
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();


            // 2) Building account and profile models
            Account account = createAccount(oauth2User);
            Profile profile = createProfile(oauth2User);

            // 3) saving user data
            Account savedAccount = accountService.saveAccount(account);
            Profile saveProfile = profileService.saveProfile(profile);
            LOGGER.info("Saved user account for userId={}", savedAccount.getUserId());


            // 4) generating jwt for client API authentication
            String jwt = jwtService.generateToken(account);
            LOGGER.info("Token generated for userId={}", account.getUserId());

            // 5) create a http cookie use the jwt and redirect the user to the dash board

            // same=None and secure=true means the cookie will be sent in cross-site requests only if they are sent over HTTPS
            // this won't work on localhost
            ResponseCookie responseCookie = ResponseCookie.from("jwt", jwt)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(24 * 60 * 60) // 1 day
                    .sameSite("None")
                    .build();

            response.addHeader("Set-Cookie", responseCookie.toString());

            // remove JSESSIONID because we'll use the jwt as a cookie
            Cookie jsessionCookie = new Cookie("JSESSIONID", null);
            jsessionCookie.setPath("/");
            jsessionCookie.setMaxAge(0);
            response.addCookie(jsessionCookie);

            response.sendRedirect(CLIENT_URL + "/call-back");
        } catch (Exception e) {
            LOGGER.error("Error after authentication error={}", e.getMessage());
            // TODO: redirect back to the login page

            // in case of an error we'll set max age of the cookie so it doesn't stay in the browser
            Cookie cookie = new Cookie("jwt", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
            response.sendRedirect(CLIENT_URL);
        }
    }

    public Profile createProfile(OAuth2User oauth2User) {
        return Profile.builder()
                .userId(oauth2User.getAttribute("id"))
                .login(oauth2User.getAttribute("login"))
                .avatarUrl(oauth2User.getAttribute("avatar_url"))
                .build();
    }

    public Account createAccount(OAuth2User oauth2User) {
        return Account.builder()
                .userId(oauth2User.getAttribute("id"))
                .userLogin(oauth2User.getAttribute("login"))
                .email(oauth2User.getAttribute("email"))
                .build();
    }

}
