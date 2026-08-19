package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.config;

import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.RabbitMessageRecoverer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitRetryProperties.class)
public class RabbitConsumerConfig {

    /**
     * Configures the JSON message converter used by Rabbit listeners.
     *
     * Type precendence is set to INFERRED so Spring uses the listener
     * method parameter type as the target deserialization type.
     */
    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {

        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();

        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();

        typeMapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);

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
        SimpleRabbitListenerContainerFactoryConfigurer configurer,
        ConnectionFactory connectionFactory,
        JacksonJsonMessageConverter converter,
        RabbitMessageRecoverer recoverer,
        RabbitRetryProperties retryProperties
    ) {
        SimpleRabbitListenerContainerFactory factory =
            new SimpleRabbitListenerContainerFactory();

        // Apply spring.rabbitmq.listener.simple.*
        configurer.configure(factory, connectionFactory);

        factory.setMessageConverter(converter);

        factory.setAdviceChain(
            RetryInterceptorBuilder.stateless()
                .maxRetries(retryProperties.maxRetries())
                .recoverer(recoverer)
                .build()
        );

        return factory;
    }
}
