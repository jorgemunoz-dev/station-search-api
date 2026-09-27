package com.petrolprice.station_search_api.station.infrastructure.postgres.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StationEntityMappingTest {

    @Test
    void shouldMapGenericAdministrativeFieldsToLiquibaseColumnNames() {
        Map<String, String> mappings = Map.of(
                "localityName", "locality_name",
                "normalizedLocalityName", "normalized_locality_name",
                "adminArea1Name", "admin_area_1_name",
                "adminArea2Name", "admin_area_2_name",
                "adminArea3Name", "admin_area_3_name");

        mappings.forEach((fieldName, columnName) -> assertThat(columnName(fieldName)).isEqualTo(columnName));
    }

    private String columnName(String fieldName) {
        try {
            return StationEntity.class.getDeclaredField(fieldName).getAnnotation(Column.class).name();
        } catch (NoSuchFieldException exception) {
            throw new AssertionError("Missing StationEntity field " + fieldName, exception);
        }
    }
}
