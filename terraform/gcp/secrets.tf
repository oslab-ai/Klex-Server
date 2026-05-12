resource "google_secret_manager_secret" "django_secret" {
  secret_id = "django-secret-key"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "django_secret_version" {
  secret      = google_secret_manager_secret.django_secret.id
  secret_data = var.django_secret_key
}
