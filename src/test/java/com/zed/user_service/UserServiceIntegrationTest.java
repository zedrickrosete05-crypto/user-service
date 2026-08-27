package com.zed.user_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf("isIntegrationEnvironmentAvailable")
class UserServiceIntegrationTest {

	static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("user_service")
			.withUsername("user_service")
			.withPassword("user_service_password");

	@Autowired
	private MockMvc mockMvc;

	@DynamicPropertySource
	static void configureDatasource(DynamicPropertyRegistry registry) {
		if (!usesCiMySql()) {
			mysql.start();
		}
		registry.add("spring.datasource.url", () -> usesCiMySql()
				? environmentValue("DB_URL")
				: mysql.getJdbcUrl());
		registry.add("spring.datasource.username", () -> usesCiMySql()
				? environmentValue("DB_USERNAME")
				: mysql.getUsername());
		registry.add("spring.datasource.password", () -> usesCiMySql()
				? environmentValue("DB_PASSWORD")
				: mysql.getPassword());
	}

	@AfterAll
	static void stopLocalMySqlContainer() {
		if (!usesCiMySql() && mysql.isRunning()) {
			mysql.stop();
		}
	}

	private static boolean usesCiMySql() {
		return Boolean.parseBoolean(System.getenv("CI_MYSQL"));
	}

	static boolean isIntegrationEnvironmentAvailable() {
		return usesCiMySql() || DockerClientFactory.instance().isDockerAvailable();
	}

	private static String environmentValue(String name) {
		String value = System.getenv(name);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(name + " must be set when CI_MYSQL is enabled");
		}
		return value;
	}

	@Test
	void userCrudWorksAgainstMySqlContainer() throws Exception {
		String createRequest = """
				{"email":"integration@example.com","name":"Integration User","password":"password123"}
				""";
		String updateRequest = """
				{"email":"updated@example.com","name":"Updated User","password":"password456"}
				""";

		mockMvc.perform(post("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("integration@example.com"))
				.andExpect(jsonPath("$.name").value("Integration User"));

		mockMvc.perform(get("/users/1")
				.with(httpBasic("integration@example.com", "password123")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("integration@example.com"));

		mockMvc.perform(put("/users/1")
				.with(httpBasic("integration@example.com", "password123"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("updated@example.com"));

		mockMvc.perform(get("/users/1")
				.with(httpBasic("updated@example.com", "password456")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated User"));

		mockMvc.perform(delete("/users/1")
				.with(httpBasic("updated@example.com", "password456")))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/users/1")
				.with(httpBasic("updated@example.com", "password456")))
				.andExpect(status().isNotFound());
	}

	@Test
	void duplicateEmailReturnsConflict() throws Exception {
		String request = """
				{"email":"duplicate@example.com","name":"First User","password":"password123"}
				""";

		mockMvc.perform(post("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}
}
