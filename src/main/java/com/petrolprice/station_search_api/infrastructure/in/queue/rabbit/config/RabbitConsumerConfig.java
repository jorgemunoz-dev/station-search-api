package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConsumerConfig {

    /**
     * Configures the JSON message converter used by Rabbit listeners.
     *
     * Type precendence is set to INFERRED so Spring uses the listener
     * method parameter type as the target deserialization type.
     */
    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {

        JacksonJsonMessageConverter converter =
            new JacksonJsonMessageConverter();

        DefaultJacksonJavaTypeMapper typeMapper =
            new DefaultJacksonJavaTypeMapper();

        typeMapper.setTypePrecedence(
            JacksonJavaTypeMapper.TypePrecedence.INFERRED
        );

        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    /**
     * Rabbit listener container factory used by @RabbitListener consumers.
     *
     * Reqisters:
     * - Rabbit connection factory
     * - Jackson message converter
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        JacksonJsonMessageConverter converter
    ) {

        SimpleRabbitListenerContainerFactory factory =
            new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);

        return factory;
    }
}
