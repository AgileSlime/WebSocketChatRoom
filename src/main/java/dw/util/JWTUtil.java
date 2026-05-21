package dw.util;

import io.jsonwebtoken.*;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;

public class JWTUtil {
    private static final String SECRET = "chatroom-secret-key-2026-websocket-project";
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000; // 7 days
    private static final Key KEY = new SecretKeySpec(SECRET.getBytes(), SignatureAlgorithm.HS256.getJcaName());

    public static String generateToken(String username) {
        return generateToken(username, 0);
    }

    public static String generateToken(String username, int userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION);
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(KEY)
                .compact();
    }

    public static int getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(token).getBody();
            Object uid = claims.get("userId");
            if (uid instanceof Integer) {
                return (Integer) uid;
            } else if (uid != null) {
                return Integer.parseInt(uid.toString());
            }
            return 0;
        } catch (JwtException e) {
            return 0;
        }
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public static String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(token).getBody();
            return claims.getSubject();
        } catch (JwtException e) {
            return null;
        }
    }
}
