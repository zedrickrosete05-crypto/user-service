package com.zed.user_service.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zed.user_service.common.GlobalExceptionHandler;
import com.zed.user_service.config.PasswordConfig;
import com.zed.user_service.config.SecurityConfig;
import com.zed.user_service.user.dto.CreateUserRequest;
import com.zed.user_service.user.dto.UpdateUserRequest;
import com.zed.user_service.user.dto.UserResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@org.springframework.context.annotation.Import({
		GlobalExceptionHandler.class,
		PasswordConfig.class,
		SecurityConfig.class
})
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@org.junit.jupiter.api.BeforeEach
	void setUpAuthentication() {
		when(userDetailsService.loadUserByUsername("user@example.com"))
				.thenReturn(User.withUsername("user@example.com")
						.password(new BCryptPasswordEncoder().encode("password123"))
						.roles("USER")
						.build());
	}

	@Test
	void createUserReturnsCreatedResponse() throws Exception {
		UserResponse response = response(1L);
		when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

		mockMvc.perform(post("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(
						new CreateUserRequest("user@example.com", "Jane", "password123"))))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/users/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.email").value("user1@example.com"));
	}

	@Test
	void createUserRejectsInvalidRequest() throws Exception {
		mockMvc.perform(post("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(
						new CreateUserRequest("invalid", "", "short"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void getUsersReturnsUsers() throws Exception {
		when(userService.getUsers()).thenReturn(List.of(response(1L)));

		mockMvc.perform(get("/users")
				.with(httpBasic("user@example.com", "password123")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1));
	}

	@Test
	void getUserReturnsNotFoundError() throws Exception {
		when(userService.getUser(1L)).thenThrow(new UserNotFoundException(1L));

		mockMvc.perform(get("/users/1")
				.with(httpBasic("user@example.com", "password123")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.path").value("/users/1"));
	}

	@Test
	void updateUserReturnsUpdatedResponse() throws Exception {
		UserResponse response = response(1L);
		when(userService.updateUser(eq(1L), any(UpdateUserRequest.class))).thenReturn(response);

		mockMvc.perform(put("/users/1")
				.with(httpBasic("user@example.com", "password123"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(
						new UpdateUserRequest("user@example.com", "Jane", "password123"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void deleteUserReturnsNoContent() throws Exception {
		doNothing().when(userService).deleteUser(1L);

		mockMvc.perform(delete("/users/1")
				.with(httpBasic("user@example.com", "password123")))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(userService).deleteUser(1L);
	}

	@Test
	void deleteUserReturnsNotFoundError() throws Exception {
		doThrow(new UserNotFoundException(1L)).when(userService).deleteUser(1L);

		mockMvc.perform(delete("/users/1")
				.with(httpBasic("user@example.com", "password123")))
				.andExpect(status().isNotFound());
	}

	private UserResponse response(Long id) {
		Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
		return new UserResponse(id, "user" + id + "@example.com", "User " + id, timestamp, timestamp);
	}
}
