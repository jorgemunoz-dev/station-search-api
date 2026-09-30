package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.config;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
class RabbitConsumerPropertiesTest {
 @Test void rejectsNonPositiveValues(){assertThatThrownBy(()->new RabbitConsumerProperties.Snapshot(0,1,1)).isInstanceOf(IllegalArgumentException.class);}
 @Test void rejectsMaxBelowConcurrency(){assertThatThrownBy(()->new RabbitConsumerProperties.Snapshot(8,4,100)).hasMessageContaining("maxConcurrency");}
 @Test void acceptsRecommendedValues(){assertThat(new RabbitConsumerProperties(new RabbitConsumerProperties.Snapshot(8,16,100),new RabbitConsumerProperties.Completion(1,1)).snapshot().prefetch()).isEqualTo(100);}
}
