package com.three.recipingadsservicebe.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("데이터베이스 테스트")
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("✅ 데이터소스 정상 생성")
    void dataSourceExists() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    @DisplayName("✅ H2 데이터베이스 연결 확인")
    void h2ConnectionWorks() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(1)).isTrue();

            // H2 데이터베이스 확인
            String url = connection.getMetaData().getURL();
            assertThat(url).contains("h2");
        }
    }
}
