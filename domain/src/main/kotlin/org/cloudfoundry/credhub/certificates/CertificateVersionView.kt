package org.cloudfoundry.credhub.certificates

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

data class CertificateVersionView(
    val id: UUID,
    @JsonProperty("expiry_date")
    val expiryDate: Instant,
    val transitional: Boolean
)
