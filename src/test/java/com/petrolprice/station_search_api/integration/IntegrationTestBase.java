package com.petrolprice.station_search_api.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    private static final DockerImageName POSTGIS_IMAGE =
        DockerImageName
            .parse("postgis/postgis:16-3.4")
            .asCompatibleSubstituteFor("postgres");

    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(POSTGIS_IMAGE)
            .withDatabaseName("station_search_test")
            .withUsername("test")
            .withPassword("test");

    static final RabbitMQContainer rabbit =
        new RabbitMQContainer("rabbitmq:3.13-management")
            .withUser("guest", "guest");

    static {
        postgres.start();
        rabbit.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.liquibase.enabled", () -> "true");
    }
}