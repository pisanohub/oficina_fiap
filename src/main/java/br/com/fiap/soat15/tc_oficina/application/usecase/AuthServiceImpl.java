package br.com.fiap.soat15.tc_oficina.application.usecase;

import br.com.fiap.soat15.tc_oficina.application.dto.LoginRequest;
import br.com.fiap.soat15.tc_oficina.application.dto.LoginResponse;
import br.com.fiap.soat15.tc_oficina.config.security.JwtService;
import br.com.fiap.soat15.tc_oficina.domain.entity.Usuario;
import br.com.fiap.soat15.tc_oficina.domain.service.AuthService;
import br.com.fiap.soat15.tc_oficina.adapter.out.persistence.repository.UsuarioRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));

        String cpfNormalizado = normalizeCpf(request.getCpf());
        if (usuario.getCpf() == null || !usuario.getCpf().equals(cpfNormalizado)) {
            throw new BadCredentialsException("CPF inválido");
        }

        String token = jwtService.gerarToken(request.getUsername(), cpfNormalizado);
        return new LoginResponse(token);
    }

    @Override
    public void registro(LoginRequest request) {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Usuário já existe: " + request.getUsername());
        }
        usuarioRepository.save(Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .cpf(normalizeCpf(request.getCpf()))
                .build());
    }

    private String normalizeCpf(String cpf) {
        if (cpf == null || cpf.replaceAll("\\D", "").isEmpty()) {
            throw new BadCredentialsException("CPF obrigatorio");
        }
        return cpf.replaceAll("\\D", "");
    }
}
