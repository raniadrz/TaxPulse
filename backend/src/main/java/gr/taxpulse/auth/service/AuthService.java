package gr.taxpulse.auth.service;

import gr.taxpulse.auth.dto.AuthResponse;
import gr.taxpulse.auth.dto.LoginRequest;
import gr.taxpulse.security.JwtService;
import gr.taxpulse.security.UserPrincipal;
import gr.taxpulse.user.entity.User;
import gr.taxpulse.user.mapper.UserMapper;
import gr.taxpulse.user.service.UserService;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
}
