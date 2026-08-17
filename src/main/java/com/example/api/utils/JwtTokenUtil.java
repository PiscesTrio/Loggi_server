package com.example.api.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public final class JwtTokenUtil {
    //name of the request header carrying the token
    public final static String TOKEN_HEADER = "Authorization";

    //expires in one week
    public final static long REMEMBER_EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 7;

    //expires in one day
    public final static long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    // application secret (injected from configuration)
    private static String APP_SECRET;

    @Value("${jwt.secret}")
    private String appSecretValue;

    @PostConstruct
    public void init() {
        APP_SECRET = appSecretValue;
    }

    private static final String PREFIX = "logistics:";

    // role claim key
    private static final String ROLE_CLAIMS = "roles";

    //check whether the token is valid
    public static boolean checkToken(String token) {
        if ("null".equals(token) || token == null || "".equals(token)){
            System.out.println("token为空");
            return false;
        }
        return token.startsWith(PREFIX);
    }

    /**
     * Create a token.
     */
    public static String createToken(String username, String[] roles, long expiration) {
        System.out.println("---------------------------");
        System.out.println("username:"+username);
        System.out.println("-----------------------");
        Map<String, Object> map = new HashMap<>();
        map.put(ROLE_CLAIMS, roles);
        return PREFIX + Jwts.builder()
                .setClaims(map)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, APP_SECRET)
                .setSubject(username)
                .compact();
    }

    /**
     * Get the token body.
     */
    private static Claims getTokenClaims(String token) {
        token = token.substring(PREFIX.length());
        Claims claims = null;
        try {
            claims = Jwts.parser()
                    .setSigningKey(APP_SECRET)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            e.printStackTrace();
        }
        return claims;
    }

    /** Extract the username from the token. */
    public static String getUsername(String token) {
        System.out.println("----gettoken----");
        System.out.println(getTokenClaims(token));
        System.out.println("-------------");
        System.out.println(getTokenClaims(token).getSubject());
        System.out.println("-------------");
        return getTokenClaims(token).getSubject();
    }

    /**
     * Extract the user roles from the token.
     */
    public static List<String> getTokenRoles(String token) {
        List<String> roles = new ArrayList<>();
        Object object = getTokenClaims(token).get(ROLE_CLAIMS);
        if (object instanceof ArrayList<?>) {
            for (Object o : (List<?>) object) {
                roles.add((String) o);
            }
        }
        for (String role : roles) {
            System.out.println(role);
        }
        return roles;
    }

    /**
     * Check whether the token has expired.
     */
    public static boolean isExpiration(String token) {
        return getTokenClaims(token).getExpiration().before(new Date());
    }

}

