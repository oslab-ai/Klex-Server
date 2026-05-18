variable "resource_group_name" {
  type    = string
  default = "klex-rg"
}

variable "location" {
  type    = string
  default = "Central India"
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "django_secret_key" {
  type      = string
  sensitive = true
}

variable "backend_image" {
  type = string
}

variable "frontend_image" {
  type = string
}
