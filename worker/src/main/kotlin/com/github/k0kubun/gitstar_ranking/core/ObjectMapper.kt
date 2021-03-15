package com.github.k0kubun.gitstar_ranking.core

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.KotlinModule

val objectMapper: ObjectMapper = ObjectMapper()
    .registerModule(KotlinModule())
    .registerModule(JavaTimeModule())
    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
