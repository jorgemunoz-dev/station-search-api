package com.petrolprice.station_search_api.station.infrastructure.postgres.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class StationEntityMappingTest {
    @Test
    void mapsCanonicalGeographicIds() throws Exception {
        assertColumn("adminArea1Id", "admin_area_1_id");
        assertColumn("adminArea2Id", "admin_area_2_id");
        assertColumn("adminArea3Id", "admin_area_3_id");
        assertColumn("localityId", "locality_id");
    }

    private void assertColumn(String field, String expected) throws Exception {
        Field mapped = StationEntity.class.getDeclaredField(field);
        assertThat(mapped.getAnnotation(Column.class).name()).isEqualTo(expected);
    }
}
