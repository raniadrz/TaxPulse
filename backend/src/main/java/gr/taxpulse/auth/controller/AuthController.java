package gr.taxpulse.auth.controller;

import gr.taxpulse.auth.dto.AuthResponse;
import gr.taxpulse.auth.dto.ChangePasswordRequest;
import gr.taxpulse.auth.dto.LoginRequest;
import gr.taxpulse.auth.service.AuthService;
import gr.taxpulse.security.CurrentUser;
import gr.taxpulse.user.dto.UserResponse;
import gr.taxpulse.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /** Exchanges e-mail/password for a JWT access token. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Changes the caller's own password (staff and client portal accounts alike). */
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.require().id(), request);
    }

    /** Returns the profile of the authenticated user (fresh from the DB, not from the token). */
    @GetMapping("/me")
    public UserResponse me() {
        return userService.findById(CurrentUser.require().id());
    }
}
