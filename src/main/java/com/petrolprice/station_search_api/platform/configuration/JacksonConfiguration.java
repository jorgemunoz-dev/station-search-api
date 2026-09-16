package com.petrolprice.station_search_api.platform.configuration;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfiguration {

    @Bean
    JsonNullableModule jsonNullableModule() {
        return new JsonNullableModule();
    }
}
