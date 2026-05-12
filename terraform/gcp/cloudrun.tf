resource "google_cloud_run_v2_service" "backend" {
  name     = "klex-backend"
  location = var.region

  template {
    containers {
      image = var.backend_image

      env {
        name  = "DATABASE_HOST"
        value = google_sql_database_instance.postgres.public_ip_address
      }

      env {
        name  = "DATABASE_NAME"
        value = google_sql_database.app.name
      }

      env {
        name  = "DATABASE_USER"
        value = google_sql_user.postgres.name
      }

      env {
        name  = "DATABASE_PASSWORD"
        value = var.db_password
      }
    }
  }
}

resource "google_cloud_run_service_iam_member" "backend_public" {
  service  = google_cloud_run_v2_service.backend.name
  location = google_cloud_run_v2_service.backend.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}

resource "google_cloud_run_v2_service" "frontend" {
  name     = "klex-frontend"
  location = var.region

  template {
    containers {
      image = var.frontend_image
    }
  }
}

resource "google_cloud_run_service_iam_member" "frontend_public" {
  service  = google_cloud_run_v2_service.frontend.name
  location = google_cloud_run_v2_service.frontend.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}
