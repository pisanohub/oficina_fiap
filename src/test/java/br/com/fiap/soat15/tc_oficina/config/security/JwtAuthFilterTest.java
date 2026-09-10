package br.com.fiap.soat15.tc_oficina.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UsuarioDetailsService usuarioDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve passar adiante quando Authorization header está ausente")
    void devePassarQuandoHeaderAusente() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Deve passar adiante quando Authorization header não começa com Bearer")
    void devePassarQuandoHeaderSemBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Deve autenticar quando token é válido")
    void deveAutenticarComTokenValido() throws Exception {
        String token = "token.valido.aqui";
        String cpf = "88417554076";
        String username = "admin@oficina.com";
        UserDetails userDetails = User.withUsername(username)
                .password("senha")
                .roles("ADMIN")
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extrairUsername(token)).thenReturn(username);
        when(jwtService.extrairCpf(token)).thenReturn(cpf);
        when(usuarioDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.isTokenValido(token, username, cpf)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(username);
    }

    @Test
    @DisplayName("Deve não autenticar quando token é inválido")
    void deveNaoAutenticarComTokenInvalido() throws Exception {
        String token = "token.invalido";
        String cpf = "88417554076";
        String username = "admin@oficina.com";
        UserDetails userDetails = User.withUsername(username)
                .password("senha")
                .roles("ADMIN")
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extrairUsername(token)).thenReturn(username);
        when(jwtService.extrairCpf(token)).thenReturn(cpf);
        when(usuarioDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.isTokenValido(token, username, cpf)).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Deve não sobrescrever autenticação já existente no contexto")
    void deveNaoSobrescreverAutenticacaoExistente() throws Exception {
        String token = "token.valido.aqui";
        String username = "admin@oficina.com";
        UserDetails userDetails = User.withUsername(username)
                .password("senha")
                .roles("ADMIN")
                .build();

        // Pré-popula o contexto com autenticação
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extrairUsername(token)).thenReturn(username);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Não deve tentar carregar o usuário de novo
        verify(usuarioDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }
}
