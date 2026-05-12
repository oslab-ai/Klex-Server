resource "google_sql_database_instance" "postgres" {
  name             = "klex-postgres"
  region           = var.region
  database_version = "POSTGRES_15"

  settings {
    tier = "db-f1-micro"
  }

  deletion_protection = false
}

resource "google_sql_user" "postgres" {
  name     = "postgres"
  instance = google_sql_database_instance.postgres.name
  password = var.db_password
}

resource "google_sql_database" "app" {
  name     = "klex"
  instance = google_sql_database_instance.postgres.name
}
