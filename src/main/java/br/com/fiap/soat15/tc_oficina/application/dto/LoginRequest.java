package br.com.fiap.soat15.tc_oficina.application.dto;

import lombok.*;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String cpf;
}
