package br.com.fiap.soat15.tc_oficina.config.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "test-secret-key-for-unit-tests-only-32chars";
    private static final long EXPIRATION = 86400000L; // 24h

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);
    }

    @Test
    @DisplayName("Deve gerar token não nulo para um username válido")
    void deveGerarTokenNaoNulo() {
        String cpf = "88417554076";
        String token = jwtService.gerarToken("admin@oficina.com", cpf);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Deve extrair o username corretamente do token gerado")
    void deveExtrairUsernameDoToken() {
        String username = "admin@oficina.com";
        String cpf = "88417554076";
        String token = jwtService.gerarToken(username, cpf);

        assertThat(jwtService.extrairUsername(token)).isEqualTo(username);
    }

    @Test
    @DisplayName("Deve extrair o username corretamente do token gerado")
    void deveExtrairCpfDoToken() {
        String username = "admin@oficina.com";
        String cpf = "88417554076";
        String token = jwtService.gerarToken(username, cpf);

        assertThat(jwtService.extrairCpf(token)).isEqualTo(cpf);
    }

    @Test
    @DisplayName("Deve validar token com username correto")
    void deveValidarTokenComUsernameCorreto() {
        String username = "admin@oficina.com";
        String cpf = "88417554076";
        String token = jwtService.gerarToken(username, cpf);

        assertThat(jwtService.isTokenValido(token, username, cpf)).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false para token com username diferente")
    void deveRetornarFalseParaUsernameErrado() {
        String cpf = "88417554076";
        String token = jwtService.gerarToken("admin@oficina.com", cpf);

        assertThat(jwtService.isTokenValido(token, "outro@oficina.com", cpf)).isFalse();
    }

    @Test
    @DisplayName("Deve lançar ExpiredJwtException ao tentar usar token expirado")
    void deveLancarExcecaoParaTokenExpirado() {
        // gera token com expiração de -1ms (já expirado)
        ReflectionTestUtils.setField(jwtService, "expiration", -1L);
        String cpf = "88417554076";
        String token = jwtService.gerarToken("admin@oficina.com", cpf);

        // JJWT lança ExpiredJwtException ao parsear token expirado
        assertThatThrownBy(() -> jwtService.isTokenValido(token, "admin@oficina.com", cpf))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Deve gerar tokens distintos para usernames diferentes")
    void deveGerarTokensDistintosParaUsernamesDiferentes() {
        String cpf = "88417554076";
        String token1 = jwtService.gerarToken("user1@oficina.com", cpf);
        String token2 = jwtService.gerarToken("user2@oficina.com", cpf);

        assertThat(token1).isNotEqualTo(token2);
    }
}
