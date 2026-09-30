package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AddressMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapAddressFieldsPublishedByStationDataIngestion() throws Exception {
        String json = """
                {
                  "street": "CALLE ISLA DE MURANO, S/N",
                  "postalCode": "50020",
                  "locality": "ZARAGOZA",
                  "municipality": "Zaragoza",
                  "province": "ZARAGOZA"
                }
                """;

        AddressMessage address = objectMapper.readValue(json, AddressMessage.class);

        assertThat(address.localityName()).isEqualTo("ZARAGOZA");
        assertThat(address.normalizedLocalityName()).isEqualTo("zaragoza");
        assertThat(address.adminArea2Name()).isEqualTo("ZARAGOZA");
        assertThat(address.adminArea3Name()).isEqualTo("Zaragoza");
        assertThat(address.adminArea1Name()).isNull();
    }
}
