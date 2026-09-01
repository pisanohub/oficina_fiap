package br.com.fiap.soat15.tc_oficina.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String gerarToken(String username, String cpf) {
        return Jwts.builder()
                .subject(username)
                .claim("cpf", cpf)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getChave())
                .compact();
    }

    public String extrairCpf(String token) {
        return getClaims(token).get("cpf", String.class);
    }

    public String extrairUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenValido(String token, String username, String cpf) {
        return extrairUsername(token).equals(username)
                && cpf.equals(extrairCpf(token))
                && !isTokenExpirado(token);
    }

    private boolean isTokenExpirado(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getChave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getChave() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
