package com.fahd.cleanPR.security;

import com.fahd.cleanPR.model.Account;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)); // 32 bytes

    // Helper method to inject the secret into JwtService using reflection
    private void injectSecret(JwtService service) throws Exception {
        Field secretField = JwtService.class.getDeclaredField("JWT_SECRET");
        secretField.setAccessible(true);
        secretField.set(service, SECRET);
    }

    /**
     * 1. Injecting the JWT_SECRET in memory
     * 2. Creating a mock Account abject
     * 3. When account.getEmail() is called, the object will return "user@example.com"
     * 4. Generating a token using the mock account with the subject "user@example.com"
     * 5. Asserting that the generated token is not null or empty
     * 6. Extracting the subject from the generated token and asserting it matches "user@example.com"
     * */
    @Test
    void generateToken_extractsSubjectAndExpiration() throws Exception {
        JwtService jwtService = new JwtService();
        injectSecret(jwtService);

        Account account = mock(Account.class);
        when(account.getEmail()).thenReturn("user@example.com");

        String token = jwtService.generateToken(account);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        String subject = jwtService.extractSubject(token);
        assertEquals("user@example.com", subject);

        Date expiration = jwtService.extractExpirationDate(token);
        assertTrue(expiration.after(new Date(System.currentTimeMillis())));
    }

    /**
     * 1. creating a valid user token with a mock account
     * 2. asserting if the validation function of the jwt service will return True
     *  for the correct subject and unexpired token date.
     * */
    @Test
    void isTokenValid_returnTrueIfTheTokenIsValid() throws Exception {
        JwtService jwtService = new JwtService();
        injectSecret(jwtService);

        Account account = mock(Account.class);
        when(account.getEmail()).thenReturn("user2@example.com");

        String token = jwtService.generateToken(account);
        assertTrue(jwtService.isTokenValid(token,"user2@example.com"));
    }

    /**
     * 1. creates jwt service class and injects secret
     * 2. creates a mock account object
     * 3. when account.getEmail() is called, it will return "user3@example.com"
     * 4. asserting if the validation function of the jwt service will return False if we
     *    provide a subject that doesn't match the one in the jwt.
     * */
    @Test
    void isTokenValid_returnsFalseForWrongUser() throws Exception {
        JwtService jwtService = new JwtService();
        injectSecret(jwtService);

        Account account = mock(Account.class);
        when(account.getEmail()).thenReturn("user3@example.com");

        String token = jwtService.generateToken(account);
        assertFalse(jwtService.isTokenValid(token, "other@example.com"));
    }

    /**
     * 1. creating a token with a expired date
     * 2. asserting if the validation function of the jwt service will return False
     * */
   @Test
   void isTokenValid_returnsFalseForExpiredToken() throws Exception {
       JwtService jwtService = new JwtService();
       injectSecret(jwtService);

       String expiredToken = Jwts.builder()
               .signWith(jwtService.getSignInKey(), SignatureAlgorithm.HS256)
               .setIssuedAt(new Date(System.currentTimeMillis()))
               .setExpiration(new Date(System.currentTimeMillis() - 1000)) // expired 1 second ago
               .setSubject("user4@example.com")
               .compact();

         assertFalse(jwtService.isTokenValid(expiredToken, "user4@example.com"));
    }

    @Test
    void getSignInKey_decodesBase64Secret() throws Exception {
        JwtService jwtService = new JwtService();
        injectSecret(jwtService);

        assertNotNull(jwtService.getSignInKey());
    }
}
