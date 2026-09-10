package br.com.fiap.soat15.tc_oficina.config.security;

import java.security.Principal;

public record ClientePrincipal(Long clienteId, String cpf) implements Principal {
    @Override
    public String getName() {
        return cpf;
    }
}
