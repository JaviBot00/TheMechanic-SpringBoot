package com.hotguy.workshopmanagement.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3 configuration for Spring Boot 4.
 * In Jackson 3, dates are serialized as ISO-8601 by default.
 * Configuration is done via JsonMapper builder (immutable pattern).
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    public JsonMapper jacksonJsonMapper() {
        return JsonMapper.builder()
                .changeDefaultPropertyInclusion(include -> include.withValueInclusion(JsonInclude.Include.NON_NULL))
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .findAndAddModules()
                .build();
    }
}
