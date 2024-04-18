package kz.home.RelaySmartSystems.filters;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class JwtAuthorizationFilter extends OncePerRequestFilter {
    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt8Er1XsuNHhpnenX5VlTk0rY+nrx8dGl1jVFH9zTSdjm4x1GbA/JCyQB+fSzuEojj61lt8ywbRje/Ur0SlHeIFQHtenvIaMuNqC3vrFurmMcMHXWSpgeDvuzvyKE3caQhUZWsrW3LC4xgvYSi1d+DbCMZpdJdMexhXpFma+932Ftg6FbZ8D27fR/vRHCFnY72FxSLup93cG9jPGKKmpWamfTkdX+uZC/my0AdGE6WWMJPIfNRunKAiucqTKeqSJLLQ+FF+yZ25mWkDtMufO4mqvdgKDBt2mYAQn+dj1DpX7tzFlFvgcCbl/cD0e+Jvh35Kh+UQxWtauhdKKbqGPu1wIDAQAB";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = extractJwtFromHeader(request);
        //logger.info(String.format("token %s", token));
        request.setAttribute("token", token);
        if (token != null) {
            try {
                //Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
                Claims claims = Jwts.parser().verifyWith(generateJwtKeyDecryption(PUBLIC_KEY))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String username = //claims.getSubject(); // Имя пользователя preferred_username
                claims.get("preferred_username", String.class);

                // Здесь можно выполнить дополнительные проверки, например, проверка истечения срока действия токена

                // Передача имени пользователя в контекст для последующего использования в контроллерах
                request.setAttribute("username", username);
                logger.info(String.format("Filter. set username %s", username));
            } catch (SignatureException e) {
                // Подпись недействительна
                logger.error("--- SignatureException");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (NoSuchAlgorithmException e) {
                logger.error("--- NoSuchAlgorithmException");
                throw new RuntimeException(e);
            } catch (InvalidKeySpecException e) {
                logger.error("--- InvalidKeySpecException");
                throw new RuntimeException(e);
            } catch (Exception e) {
                logger.error("--- Exception " + e.getLocalizedMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromHeader(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
//        logger.info(String.format("authorizationHeader %s", authorizationHeader));
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    private PublicKey generateJwtKeyDecryption(String jwtPublicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] keyBytes = Base64.decodeBase64(jwtPublicKey);
        X509EncodedKeySpec x509EncodedKeySpec=new X509EncodedKeySpec(keyBytes);
        return keyFactory.generatePublic(x509EncodedKeySpec);
    }
}

/*

import io.jsonwebtoken.*;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;


public class JwtAuthorizationFilter extends OncePerRequestFilter {
    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt8Er1XsuNHhpnenX5VlTk0rY+nrx8dGl1jVFH9zTSdjm4x1GbA/JCyQB+fSzuEojj61lt8ywbRje/Ur0SlHeIFQHtenvIaMuNqC3vrFurmMcMHXWSpgeDvuzvyKE3caQhUZWsrW3LC4xgvYSi1d+DbCMZpdJdMexhXpFma+932Ftg6FbZ8D27fR/vRHCFnY72FxSLup93cG9jPGKKmpWamfTkdX+uZC/my0AdGE6WWMJPIfNRunKAiucqTKeqSJLLQ+FF+yZ25mWkDtMufO4mqvdgKDBt2mYAQn+dj1DpX7tzFlFvgcCbl/cD0e+Jvh35Kh+UQxWtauhdKKbqGPu1wIDAQAB";
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        //String token = extractJwtFromHeader(request);
        // Проверка и валидация токена JWT
        //String username = getUserNameFromJwtToken(token);
        //request.setAttribute("username", username);
        // Сохранение имени пользователя в контексте приложения
        logger.info("Hello from filter");
        try {
            String jwt = parseJwt(request);
            if (jwt != null && validateJwtToken(jwt)) {
                String username = getUserNameFromJwtToken(jwt);
                logger.info(String.format("Username %s", username));
                //UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//                UsernamePasswordAuthenticationToken authentication =
//                        new UsernamePasswordAuthenticationToken( userDetails, null, userDetails.getAuthorities());
//                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//
//                SecurityContextHolder.getContext()
//                        .setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7, headerAuth.length());
        }

        return null;
    }

    public String getUserNameFromJwtToken(String token)  {
        try {
            return Jwts.parser()
                    .verifyWith(generateJwtKeyDecryption(PUBLIC_KEY))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        //get("preferred_username", String.class);

    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(generateJwtKeyDecryption(PUBLIC_KEY))
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: " + e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("JWT claims string is empty: " + e.getMessage());
        }

        return false;
    }


    private String extractJwtFromHeader(HttpServletRequest request) {
        // Извлечение токена из заголовка запроса
        String authorizationHeader = request.getHeader("Authorization");

        // Проверяем наличие заголовка и формата Bearer <token>
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // Извлекаем и возвращаем только сам токен без префикса "Bearer"
            String token = authorizationHeader.substring(7);
            logger.info(String.format("Token %s", token));
            try {
                // Проверка валидности токена с использованием секретного ключа
                //Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
                Jws<Claims> claims = Jwts.parser()
                        .verifyWith(generateJwtKeyDecryption(PUBLIC_KEY))
                        .build()
                        .parseSignedClaims(token);
                //claims.getPayload().get("preferred_username", String.class);
                // Если проверка прошла успешно, возвращаем токен
                return token;
            } catch (SignatureException e) {
                // Подпись не верна, токен недействителен
                return null;
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
        }

        // Если заголовок пустой или не соответствует формату, возвращаем null
        return null;
    }



    private String extractUsernameFromToken(String token) {
        // Извлечение имени пользователя из токена
        return "test";
    }
}

*/