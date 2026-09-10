package br.com.fiap.soat15.tc_oficina.config.security;

import br.com.fiap.soat15.tc_oficina.config.SecurityConfig;
import br.com.fiap.soat15.tc_oficina.adapter.out.persistence.repository.ClienteRepository;
import br.com.fiap.soat15.tc_oficina.adapter.out.persistence.repository.OrdemDeServicoRepository;
import br.com.fiap.soat15.tc_oficina.domain.entity.Cliente;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Cadeia real de seguranca e JWT real; somente persistencia e endpoints sao dublês. */
class ClienteJwtIntegrationTest {
    private static final String SECRET = "chave-de-teste-compartilhada-apenas-para-integracao-123456789012345678";
    private static final String CPF = "52998224725";
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private ClienteRepository clientes;

    @TestConfiguration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class Config {
        @Bean ClienteRepository clientes() { return mock(ClienteRepository.class); }
        @Bean OrdemDeServicoRepository ordens() { return mock(OrdemDeServicoRepository.class); }
        @Bean UsuarioDetailsService usuarios() { return mock(UsuarioDetailsService.class); }
        @Bean JwtService jwt() {
            var service = new JwtService();
            ReflectionTestUtils.setField(service, "secret", SECRET);
            ReflectionTestUtils.setField(service, "expiration", 3600000L);
            return service;
        }
        @Bean JwtAuthFilter filtro(JwtService jwt, UsuarioDetailsService usuarios, ClienteRepository clientes) {
            return new JwtAuthFilter(jwt, usuarios, clientes);
        }
        @Bean OrdemAuthorization acesso(OrdemDeServicoRepository ordens) { return new OrdemAuthorization(ordens); }
        @Bean Endpoints endpoints() { return new Endpoints(); }
    }

    @RestController
    static class Endpoints {
        @GetMapping({"/api/v1/ordens/cliente/{clienteId}", "/api/v1/ordens/{id}", "/api/v1/ordens/{id}/status"})
        String consulta() { return SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString(); }
        @PostMapping("/api/v1/ordens") String criar() { return "criada"; }
        @GetMapping("/api/v1/cliente") String administrativo() { return "admin"; }
    }

    @BeforeEach
    void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
                "jwt-test", java.util.Map.of("jwt.secret", SECRET, "jwt.expiration", "3600000")));
        context.register(Config.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(FilterChainProxy.class)).build();
        clientes = context.getBean(ClienteRepository.class);
        when(clientes.findById(7L)).thenReturn(Optional.of(Cliente.builder()
                .id(7L).cpfCnpj(CPF).ativo(true).build()));
    }

    @AfterEach void cleanup() { context.close(); SecurityContextHolder.clearContext(); }

    // Mesmo contrato e assinatura utilizados por JwtTokenService da Lambda.
    private String token(String secret, String cpf, Object id, boolean ativo, Instant expiration) {
        var builder = Jwts.builder().subject(cpf).claim("clienteId", id)
                .claim("tipo", "CLIENTE").claim("ativo", ativo).issuedAt(new Date());
        if (expiration != null) builder.expiration(Date.from(expiration));
        return builder.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private String valido() { return token(SECRET, CPF, 7L, true, Instant.now().plusSeconds(3600)); }

    @Test void aceitaTokenLambdaSemUsuarioAdministrativo() throws Exception {
        mvc.perform(get("/api/v1/ordens/cliente/7").header("Authorization", "Bearer " + valido()))
                .andExpect(status().isOk()).andExpect(content().string("[ROLE_CLIENTE]"));
        verifyNoInteractions(context.getBean(UsuarioDetailsService.class));
    }

    @Test void bloqueiaOutroClienteEOperacoesAdministrativas() throws Exception {
        String token = valido();
        mvc.perform(get("/api/v1/ordens/cliente/8").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/ordens").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/cliente").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test void validaProprietarioDaOrdemEStatus() throws Exception {
        var ordens = context.getBean(OrdemDeServicoRepository.class);
        when(ordens.existsByIdAndVeiculoClienteId(10L, 7L)).thenReturn(true);
        String token = valido();
        mvc.perform(get("/api/v1/ordens/10").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/ordens/10/status").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/ordens/11").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test void rejeitaAssinaturaExpiracaoEClaimsInvalidas() throws Exception {
        for (String token : new String[] {
                "adulterado",
                token(SECRET + "outra", CPF, 7L, true, Instant.now().plusSeconds(3600)),
                token(SECRET, CPF, 7L, true, Instant.now().minusSeconds(60)),
                token(SECRET, CPF, 7L, true, null),
                token(SECRET, CPF, 7L, false, Instant.now().plusSeconds(3600)),
                token(SECRET, CPF, 7.5, true, Instant.now().plusSeconds(3600)),
                token(SECRET, "11144477735", 7L, true, Instant.now().plusSeconds(3600)) }) {
            mvc.perform(get("/api/v1/ordens/cliente/7").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
    }

    @Test void rejeitaClienteInativoOuRemovido() throws Exception {
        String token = valido();
        when(clientes.findById(7L)).thenReturn(Optional.of(Cliente.builder().id(7L).cpfCnpj(CPF).ativo(false).build()));
        mvc.perform(get("/api/v1/ordens/cliente/7").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        when(clientes.findById(7L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/ordens/cliente/7").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test void consultaNaoEhMaisPublica() throws Exception {
        mvc.perform(get("/api/v1/ordens/cliente/7")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/ordens/10")).andExpect(status().is4xxClientError());
    }

    @Test void preservaTokenAdministrativoDaBranchJwt() throws Exception {
        when(context.getBean(UsuarioDetailsService.class).loadUserByUsername("operador"))
                .thenReturn(User.withUsername("operador").password("irrelevante").roles("ADMIN").build());
        String token = context.getBean(JwtService.class).gerarToken("operador", CPF);
        mvc.perform(post("/api/v1/ordens").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }
}
