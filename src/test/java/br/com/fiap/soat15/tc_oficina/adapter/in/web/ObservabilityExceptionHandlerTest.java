package br.com.fiap.soat15.tc_oficina.adapter.in.web;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityExceptionHandlerTest {

    @Test
    void deveContabilizarFalhaNaoTratadaDeOrdemDeServico() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        handler.setMeterRegistry(meterRegistry);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/ordens");

        handler.handleGlobalException(new IllegalStateException("falha simulada"), new ServletWebRequest(request));

        assertThat(meterRegistry.counter("oficina.http.erros", "http.status", "500").count())
                .isEqualTo(1);
        assertThat(meterRegistry.counter(
                "oficina.ordens.processamento.falhas",
                "tipo", "IllegalStateException"
        ).count()).isEqualTo(1);
    }
}
