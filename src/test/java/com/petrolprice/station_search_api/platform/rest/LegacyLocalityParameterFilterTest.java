package com.petrolprice.station_search_api.platform.rest;
import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
import jakarta.servlet.FilterChain; import org.junit.jupiter.api.Test; import org.springframework.mock.web.*;
class LegacyLocalityParameterFilterTest {
 @Test void rejectsRemovedLocalityFilter() throws Exception {var request=new MockHttpServletRequest("GET","/statistics/fuel-prices/current");request.addParameter("locality","Ardales");var response=new MockHttpServletResponse();new LegacyLocalityParameterFilter().doFilter(request,response,mock(FilterChain.class));assertThat(response.getStatus()).isEqualTo(400);}
 @Test void allowsAdministrativeHierarchy() throws Exception {var request=new MockHttpServletRequest("GET","/stations");request.addParameter("adminArea1","Andalucia");var response=new MockHttpServletResponse();var chain=mock(FilterChain.class);new LegacyLocalityParameterFilter().doFilter(request,response,chain);verify(chain).doFilter(request,response);}
}
