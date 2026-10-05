package gr.taxpulse.user.mapper;

import gr.taxpulse.user.dto.UserResponse;
import gr.taxpulse.user.entity.User;
import org.springframework.stereotype.Component;

/** Entity -> DTO mapping for users. Kept explicit (no reflection) for readability and compile-time safety. */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getClientId());
    }
}
