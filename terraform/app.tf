resource "kubernetes_deployment" "spring_api" {

  metadata {

    name      = "spring-api"
    namespace = kubernetes_namespace.of_fiap.metadata[0].name

  }

  spec {

    replicas = 2

    selector {

      match_labels = {
        app = "spring-api"
      }

    }

    template {

      metadata {

        labels = {
          app = "spring-api"
        }

      }

      spec {

        image_pull_secrets {

          name = kubernetes_secret.ghcr.metadata[0].name

        }

        container {

          name  = "spring-api"
          image = var.image_name

          port {

            container_port = 8080
            name           = "http"

          }

          port {

            container_port = 8081
            name           = "management"

          }

          env {

            name  = "SPRING_DATASOURCE_URL"
            value = "jdbc:postgresql://postgres:5432/oficina"

          }

          env {

            name  = "SPRING_DATASOURCE_USERNAME"
            value = "postgres"

          }

          env {

            name  = "SPRING_DATASOURCE_PASSWORD"
            value = "postgres"

          }

          env {

            name  = "SPRING_PROFILES_ACTIVE"
            value = "newrelic"

          }

          env {

            name  = "MANAGEMENT_SERVER_PORT"
            value = "8081"

          }

          env {

            name  = "OTEL_SERVICE_NAME"
            value = "tc-oficina"

          }

          env {

            name  = "DEPLOYMENT_ENVIRONMENT"
            value = "production"

          }

          env {

            name  = "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT"
            value = "https://otlp.nr-data.net/v1/traces"

          }

          env {

            name = "NEW_RELIC_LICENSE_KEY"

            value_from {
              secret_key_ref {
                name = kubernetes_secret.new_relic.metadata[0].name
                key  = "license-key"
              }
            }

          }

          resources {
            requests = {
              cpu    = "200m"
              memory = "256Mi"
            }
            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
          }

          readiness_probe {

            http_get {

              path = "/actuator/health/readiness"
              port = 8081

            }

            initial_delay_seconds = 240
            period_seconds = 30

          }

          liveness_probe {

            http_get {

              path = "/actuator/health/liveness"
              port = 8081

            }

            initial_delay_seconds = 300
            period_seconds = 30

          }

        }

      }

    }

  }

}

resource "kubernetes_service" "spring_api" {

  metadata {

    name      = "spring-api"
    namespace = kubernetes_namespace.of_fiap.metadata[0].name

  }

  spec {

    selector = {
      app = "spring-api"
    }

    port {

      port        = 8080
      target_port = 8080

    }

    type = "NodePort"

  }

}

resource "kubernetes_secret" "new_relic" {

  metadata {

    name      = "newrelic-license"
    namespace = kubernetes_namespace.of_fiap.metadata[0].name

  }

  type = "Opaque"

  data = {
    "license-key" = var.new_relic_license_key
  }

}

resource "kubernetes_service" "spring_api_metrics" {

  metadata {

    name      = "spring-api-metrics"
    namespace = kubernetes_namespace.of_fiap.metadata[0].name

    annotations = {
      "prometheus.io/scrape" = "true"
      "prometheus.io/path"   = "/actuator/prometheus"
      "prometheus.io/port"   = "8081"
    }

  }

  spec {

    selector = {
      app = "spring-api"
    }

    port {

      name        = "management"
      port        = 8081
      target_port = 8081

    }

    type = "ClusterIP"

  }

}
