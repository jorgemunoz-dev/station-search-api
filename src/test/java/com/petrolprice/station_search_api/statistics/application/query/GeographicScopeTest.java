package com.petrolprice.station_search_api.statistics.application.query;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
class GeographicScopeTest {
 @Test void acceptsOrderedHierarchy(){assertThat(GeographicScope.administrativeHierarchy(" Andalucía ","Málaga","Ardales").adminArea1Name()).isEqualTo("Andalucía");}
 @Test void acceptsCountry(){assertThat(GeographicScope.country().adminArea1Name()).isNull();}
 @Test void rejectsArea2Gap(){assertThatThrownBy(()->new GeographicScope(null,"Málaga",null)).hasMessage("adminArea2 requires adminArea1");}
 @Test void rejectsArea3Gap(){assertThatThrownBy(()->new GeographicScope("Andalucía",null,"Ardales")).hasMessage("adminArea3 requires adminArea1 and adminArea2");}
}
