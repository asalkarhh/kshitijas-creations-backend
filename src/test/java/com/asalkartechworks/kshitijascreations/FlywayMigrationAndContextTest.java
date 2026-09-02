package com.asalkartechworks.kshitijascreations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywayMigrationAndContextTest extends IntegrationTestBase {

    private static final List<String> EXPECTED_TABLES = List.of(
            "admin_users",
            "categories",
            "products",
            "product_media",
            "banners",
            "orders",
            "order_items"
    );

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void applicationContextStartsAndFlywayCreatesAllDomainTables() {
        assertThat(applicationContext).isNotNull();

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
        );
        assertThat(tables).containsAll(EXPECTED_TABLES);

        Integer successfulMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = TRUE",
                Integer.class
        );
        assertThat(successfulMigrations).isEqualTo(1);
    }
}
