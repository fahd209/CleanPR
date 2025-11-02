package com.fahd.cleanPR.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.fahd.cleanPR.CleanPrConstants.AUTH_ROUTE;

@RestController
@CrossOrigin
@RequestMapping(AUTH_ROUTE)
public class AuthController {

    /*
    * Since the clients cookie is httpOnly, we cannot delete it from the client side
    * So we will delete it from the server side by calling this endpoint and
    * setting its maxAge to 0 and its value to null
    * */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        try {
            Cookie cookie = new Cookie("jwt", null);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(0); // Delete the cookie
            response.addCookie(cookie);

            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
