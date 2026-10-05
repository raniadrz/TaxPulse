package gr.taxpulse.auth.service;

import gr.taxpulse.auth.dto.AuthResponse;
import gr.taxpulse.auth.dto.ChangePasswordRequest;
import gr.taxpulse.auth.dto.LoginRequest;
import gr.taxpulse.common.exception.BusinessRuleException;
import gr.taxpulse.security.JwtService;
import gr.taxpulse.security.UserPrincipal;
import gr.taxpulse.user.entity.User;
import gr.taxpulse.user.mapper.UserMapper;
import gr.taxpulse.user.service.UserService;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Credential verification and token issuance. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserMapper userMapper;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException / DisabledException -> mapped to 401 by the exception handler.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userService.getEntity(principal.id());
        user.setLastLoginAt(clock.instant());

        JwtService.IssuedToken token = jwtService.issue(principal);
        return AuthResponse.bearer(token.token(), token.expiresAt(), userMapper.toResponse(user));
    }

    /** Lets any signed-in user replace their password, e.g. the initial one handed out by the office. */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userService.getEntity(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Ο τρέχων κωδικός δεν είναι σωστός");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessRuleException("Ο νέος κωδικός πρέπει να διαφέρει από τον τρέχοντα");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }
}
