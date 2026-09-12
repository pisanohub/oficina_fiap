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

    public boolean isTokenCliente(String token) {
        Claims claims = getClaims(token);
        // Claims de cliente nunca devem cair no fluxo administrativo.
        return claims.containsKey("tipo") || claims.containsKey("clienteId");
    }

    public ClientePrincipal extrairCliente(String token) {
        Claims claims = getClaims(token);
        Object id = claims.get("clienteId");
        String cpf = claims.getSubject();
        if (!"CLIENTE".equals(claims.get("tipo")) || !Boolean.TRUE.equals(claims.get("ativo"))
                || claims.getExpiration() == null || cpf == null || !cpf.matches("[0-9]{11}")
                || !(id instanceof Number) || !id.toString().matches("[1-9][0-9]*")) {
            throw new IllegalArgumentException("Claims de cliente invalidas");
        }
        return new ClientePrincipal(Long.valueOf(id.toString()), cpf);
    }

    public String extrairUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenValido(String token, String username, String cpf) {
        return extrairUsername(token).equals(username)
                && cpf != null && cpf.equals(extrairCpf(token))
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
