variable "ghcr_username" {
  type = string
}

variable "ghcr_token" {
  type = string
}

variable "image_name" {
  type = string
}

variable "new_relic_license_key" {
  type      = string
  sensitive = true
}
