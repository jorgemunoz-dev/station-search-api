package com.petrolprice.station_search_api.station.search.application.searcharea;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
class LocalitySearchAreaTest {
 @Test void normalizesCountryAndHierarchy(){var a=new LocalitySearchArea("es","Andalucía","MÁLAGA","Ardales");assertThat(a).isEqualTo(new LocalitySearchArea("ES","andalucia","malaga","ardales"));}
 @Test void requiresAllLevels(){assertThatThrownBy(()->new LocalitySearchArea("ES","Andalucía",null,"Ardales")).hasMessage("adminArea2 is required");}
 @Test void validatesCountry(){assertThatThrownBy(()->new LocalitySearchArea("ESP","A","B","C")).hasMessageContaining("two-letter");}
}
