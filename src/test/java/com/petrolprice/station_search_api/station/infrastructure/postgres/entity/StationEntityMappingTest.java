package com.petrolprice.station_search_api.station.infrastructure.postgres.entity;
import static org.assertj.core.api.Assertions.*;
import jakarta.persistence.Column; import java.lang.reflect.Field; import org.junit.jupiter.api.Test;
class StationEntityMappingTest {
 @Test void mapsNumberedAdminColumnsExactly() throws Exception {assertColumn("adminArea1Name","admin_area_1_name");assertColumn("adminArea2Name","admin_area_2_name");assertColumn("adminArea3Name","admin_area_3_name");}
 private void assertColumn(String field,String expected)throws Exception{Field f=StationEntity.class.getDeclaredField(field);assertThat(f.getAnnotation(Column.class).name()).isEqualTo(expected);}
}
