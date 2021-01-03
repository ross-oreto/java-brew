package io.oreto.brew.web.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.oreto.brew.security.Anonymous;
import io.oreto.brew.security.UserDetails;
import io.oreto.brew.serialize.json.JSON;
import io.oreto.brew.str.Str;
import io.oreto.brew.web.http.HttpContext;
import io.oreto.brew.web.page.constants.C;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class Jwt {
    private static final String defaultKeyFileName = "key";

    public static SecretKey getKey(String keyFile) {
        try {
            if (Files.exists(Paths.get(keyFile))) {
                byte[] decodedKey = Base64.getDecoder().decode(Files.readAllBytes(Paths.get(keyFile)));
                return new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
            } else {
                SecretKey newKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
                Files.write(Paths.get(keyFile), Base64.getEncoder().encodeToString(newKey.getEncoded()).getBytes());
                return newKey;
            }
        } catch (Exception e) {
            System.out.println("Error getting key file " + e.getMessage());
        }
        return null;
    }

    public static String createJwt(SecretKey key, UserDetails user, String issuer) {
        Date now = new Date();
        Date exp = new Date(System.currentTimeMillis() + (24000*3600));

        String userClaim;
        try {
            userClaim = JSON.asString(user);
        } catch (JsonProcessingException e) {
            userClaim = null;
        }
        return Jwts.builder()
                .setId(UUID.randomUUID().toString().replace("-", ""))
                .setNotBefore(now)
                .setExpiration(exp)
                .setIssuedAt(now)
                .setIssuer(issuer)
                .setSubject(user.getUsername())
                .claim(C.user, userClaim)
                .signWith(key)
                .compact();
    }

    public static Claims readJwt(SecretKey key, String jwt) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();
    }

    @SuppressWarnings("unchecked")
    public static <T extends UserDetails> T readUser(SecretKey key
            , String jwt
            , Class<T> userClass) {
        if (Str.isBlank(jwt))
            return (T) new Anonymous();
        try {
            Claims claims = readJwt(key, jwt);
            return JSON.mapper.readValue(claims.get(C.user).toString(), userClass);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return (T) new Anonymous();
    }

    public static <T extends UserDetails> T readUser(SecretKey key
            , String jwt
            , Map<String, String> headers
            , Class<T> userClass) {
        return readUser(key
                , jwt == null ? HttpContext.getAuthorizationToken(headers).orElse(Str.EMPTY) : jwt
                , userClass);
    }

    public static <T extends UserDetails> T readUser(SecretKey key
            , Map<String, String> headers
            , Class<T> userClass) {
        return readUser(key, null, headers, userClass);
    }

    private String cookieName;
    private String keyFile;

    public Jwt() {}

    public Jwt(String cookieName, String keyFile) {
        this.cookieName = cookieName;
        this.keyFile = keyFile;
    }

    public Jwt(String cookieName) {
        this(cookieName, defaultKeyFileName);
    }

    public SecretKey getKey()  {
        return getKey(keyFile);
    }

    public String createJwt(UserDetails user, String issuer) {
        return createJwt(getKey(), user, issuer);
    }

    public Claims readJwt(Map<String, String> valueMap) {
        return readJwt(getKey()
                , cookieName == null
                        ? HttpContext.getAuthorizationToken(valueMap).orElse(Str.EMPTY)
                        : valueMap.get(cookieName));
    }

    public Claims readJwt(String jwt) {
        return readJwt(getKey(), jwt);
    }

    public <T extends UserDetails> T readUser(Map<String, String> cookies
            , Map<String, String> headers
            , Class<T> userClass) {
        return readUser(getKey(), cookieName == null ? null : cookies.get(cookieName), headers, userClass);
    }

    public <T extends UserDetails> T readUserFromCookie(Map<String, String> cookies
            , Class<T> userClass) {
        return readUser(getKey(), cookieName == null ? null : cookies.get(cookieName), null, userClass);
    }

    public <T extends UserDetails> T readUserFromHeader(Map<String, String> headers
            , Class<T> userClass) {
        return readUser(getKey(), null, headers, userClass);
    }

    public <T extends UserDetails> T readUser(String jwt, Class<T> userClass) {
        return readUser(getKey(), jwt, userClass);
    }

    public String getCookieName() {
        return cookieName;
    }
}
