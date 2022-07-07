package uk.gov.justice.digital.hmpps.courthearingeventreceiver.config.web

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component


@Component
@ConfigurationProperties(prefix = "observe")
data class ObserveFields(var fields: Map<String, String>? = mapOf())


