package br.com.fiap.soat15.tc_oficina.application.usecase;

import br.com.fiap.soat15.tc_oficina.application.usecase.AuthServiceImpl;
import br.com.fiap.soat15.tc_oficina.application.dto.LoginRequest;
import br.com.fiap.soat15.tc_oficina.application.dto.LoginResponse;
import br.com.fiap.soat15.tc_oficina.domain.entity.Usuario;
import br.com.fiap.soat15.tc_oficina.adapter.out.persistence.repository.UsuarioRepository;
import br.com.fiap.soat15.tc_oficina.config.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequest loginRequest;

    @Test
    void deveRejeitarLoginSemCpfSemNullPointer() {
        loginRequest.setCpf(null);
        when(usuarioRepository.findByUsername(loginRequest.getUsername()))
                .thenReturn(Optional.of(Usuario.builder().cpf("88417554076").build()));
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void deveRejeitarUsuarioLegadoSemCpfSemNullPointer() {
        when(usuarioRepository.findByUsername(loginRequest.getUsername()))
                .thenReturn(Optional.of(Usuario.builder().build()));
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setUsername("admin@oficina.com");
        loginRequest.setCpf("88417554076");
        loginRequest.setPassword("123456");
    }

    @Test
    @DisplayName("Deve realizar login com sucesso e retornar token JWT")
    void deveRealizarLoginComSucesso() {
        String cpf = "88417554076";
        Usuario usuario = Usuario.builder()
                .id(1L)
                .username("admin@oficina.com")
                .cpf(cpf)
                .password("hash")
                .build();

        when(usuarioRepository.findByUsername("admin@oficina.com")).thenReturn(Optional.of(usuario));
        when(jwtService.gerarToken("admin@oficina.com", cpf)).thenReturn("jwt.token.gerado");

        LoginResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt.token.gerado");
        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).gerarToken("admin@oficina.com", cpf);
    }

    @Test
    @DisplayName("Deve lançar exceção quando credenciais são inválidas no login")
    void deveLancarExcecaoComCredenciaisInvalidas() {
        doThrow(new BadCredentialsException("Credenciais inválidas"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).gerarToken(any(), any());
    }

    @Test
    @DisplayName("Deve registrar novo usuário com sucesso")
    void deveRegistrarNovoUsuarioComSucesso() {
        when(usuarioRepository.findByUsername("admin@oficina.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("hash-da-senha");

        authService.registro(loginRequest);

        verify(usuarioRepository).save(argThat(u ->
                u.getUsername().equals("admin@oficina.com") &&
                u.getPassword().equals("hash-da-senha")));
    }

    @Test
    @DisplayName("Deve lançar exceção ao registrar usuário já existente")
    void deveLancarExcecaoAoRegistrarUsuarioExistente() {
        Usuario usuarioExistente = Usuario.builder()
                .id(1L)
                .username("admin@oficina.com")
                .password("hash")
                .build();

        when(usuarioRepository.findByUsername("admin@oficina.com"))
                .thenReturn(Optional.of(usuarioExistente));

        assertThatThrownBy(() -> authService.registro(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("admin@oficina.com");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve codificar a senha antes de salvar no registro")
    void deveCodificarSenhaAntesDeRegistrar() {
        when(usuarioRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("$2a$10$hashBcrypt");

        authService.registro(loginRequest);

        verify(passwordEncoder).encode("123456");
        verify(usuarioRepository).save(argThat(u -> u.getPassword().equals("$2a$10$hashBcrypt")));
    }
}
