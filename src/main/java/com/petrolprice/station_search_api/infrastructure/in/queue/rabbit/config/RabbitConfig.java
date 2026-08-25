package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.config;

import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.properties.RabbitMessagingProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMessagingProperties.class)
public class RabbitConfig {
    @Bean
    public Declarables rabbitDeclarables(RabbitMessagingProperties properties) {
        List<Declarable> declarables = new ArrayList<>();

        TopicExchange mainExchange = ExchangeBuilder.topicExchange(
                        properties.exchange().name())
                .durable(properties.exchange().durable())
                .build();

        DirectExchange dlx = ExchangeBuilder.directExchange(properties.dlx().name())
                .durable(true)
                .build();

        declarables.add(mainExchange);
        declarables.add(dlx);

        for (RabbitMessagingProperties.QueueProperties queueConfig : properties.queues()) {
            String dlqRoutingKey = queueConfig.dlq();

            Queue queue = QueueBuilder.durable(queueConfig.name())
                    .deadLetterExchange(properties.dlx().name())
                    .deadLetterRoutingKey(dlqRoutingKey)
                    .build();

            Queue dlq = QueueBuilder.durable(queueConfig.dlq()).build();

            declarables.add(queue);
            declarables.add(dlq);

            for (String bindingKey : queueConfig.bindings()) {
                declarables.add(BindingBuilder.bind(queue).to(mainExchange).with(bindingKey));
            }

            declarables.add(BindingBuilder.bind(dlq).to(dlx).with(dlqRoutingKey));
        }

        return new Declarables(declarables);
    }
}
