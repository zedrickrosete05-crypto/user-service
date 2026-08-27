package com.zed.user_service.user;

import com.zed.user_service.user.dto.CreateUserRequest;
import com.zed.user_service.user.dto.UpdateUserRequest;
import com.zed.user_service.user.dto.UserResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
		UserResponse response = userService.createUser(request);
		return ResponseEntity
				.created(URI.create("/users/" + response.id()))
				.body(response);
	}

	@GetMapping
	public List<UserResponse> getUsers(Authentication authentication) {
		return userService.getUsers(isAdmin(authentication));
	}

	@GetMapping("/{id}")
	public UserResponse getUser(@PathVariable Long id, Authentication authentication) {
		return userService.getUser(id, authentication.getName(), isAdmin(authentication));
	}

	@PutMapping("/{id}")
	public UserResponse updateUser(
			@PathVariable Long id,
			@Valid @RequestBody UpdateUserRequest request,
			Authentication authentication) {
		return userService.updateUser(id, request, authentication.getName(), isAdmin(authentication));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
		userService.deleteUser(id, authentication.getName(), isAdmin(authentication));
		return ResponseEntity.noContent().build();
	}

	private boolean isAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
	}
}
