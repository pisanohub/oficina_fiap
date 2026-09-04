package br.com.fiap.soat15.tc_oficina.config.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void deveGerarRequestIdQuandoHeaderNaoFoiInformado() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(MDC.get("request.id")).isNull();
    }

    @Test
    void devePropagarRequestIdRecebido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ordens/1");
        request.addHeader("X-Request-Id", "teste-correlacao-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("teste-correlacao-123");
        assertThat(MDC.get("request.id")).isNull();
    }
}
