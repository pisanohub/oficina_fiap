package br.com.fiap.soat15.tc_oficina.config.observability;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=health,metrics,prometheus",
        "management.endpoint.health.probes.enabled=true"
})
class ObservabilityEndpointsIntegrationTest {

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Autowired
    private PrometheusScrapeEndpoint prometheusScrapeEndpoint;

    @Autowired
    private PrometheusMeterRegistry prometheusMeterRegistry;

    @Test
    void deveCriarEndpointsDeHealthEMetricasPrometheus() {
        assertThat(healthEndpoint).isNotNull();
        assertThat(prometheusScrapeEndpoint).isNotNull();
        prometheusMeterRegistry.counter("oficina.observabilidade.teste").increment();
        assertThat(prometheusMeterRegistry.scrape())
                .contains("oficina_observabilidade_teste_total");
    }
}
