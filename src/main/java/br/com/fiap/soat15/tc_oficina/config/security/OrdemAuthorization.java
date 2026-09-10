package br.com.fiap.soat15.tc_oficina.config.security;

import br.com.fiap.soat15.tc_oficina.adapter.out.persistence.repository.OrdemDeServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class OrdemAuthorization {
    private final OrdemDeServicoRepository ordens;

    public AuthorizationDecision autorizar(Supplier<? extends Authentication> supplier, RequestAuthorizationContext context) {
        Authentication auth = supplier.get();
        if (auth == null || !auth.isAuthenticated()) return new AuthorizationDecision(false);
        if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return new AuthorizationDecision(true);
        }
        if (!(auth.getPrincipal() instanceof ClientePrincipal cliente)) return new AuthorizationDecision(false);
        try {
            String clienteId = context.getVariables().get("clienteId");
            if (clienteId != null) {
                return new AuthorizationDecision(cliente.clienteId().equals(Long.valueOf(clienteId)));
            }
            String ordemId = context.getVariables().get("id");
            return new AuthorizationDecision(ordemId != null
                    && ordens.existsByIdAndVeiculoClienteId(Long.valueOf(ordemId), cliente.clienteId()));
        } catch (NumberFormatException exception) {
            return new AuthorizationDecision(false);
        }
    }
}
