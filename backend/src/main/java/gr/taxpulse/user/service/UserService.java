package gr.taxpulse.user.service;

import gr.taxpulse.common.exception.BusinessRuleException;
import gr.taxpulse.common.exception.ConflictException;
import gr.taxpulse.common.exception.ResourceNotFoundException;
import gr.taxpulse.user.dto.CreateUserRequest;
import gr.taxpulse.user.dto.UpdateUserRequest;
import gr.taxpulse.user.dto.UserResponse;
import gr.taxpulse.security.CurrentUser;
import gr.taxpulse.user.entity.Role;
import gr.taxpulse.user.entity.User;
import gr.taxpulse.user.mapper.UserMapper;
import gr.taxpulse.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** User management use-cases (office staff accounts). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("fullName")).stream().map(userMapper::toResponse).toList();
    }

    public UserResponse findById(UUID id) {
        return userMapper.toResponse(getEntity(id));
    }

    /** Loads the entity or fails with 404. Exposed for other services needing a user reference. */
    public User getEntity(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with e-mail %s already exists".formatted(email));
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = getEntity(id);
        // An administrator must never lock themselves out (and possibly the whole office) by accident.
        boolean self = CurrentUser.get().map(p -> p.id().equals(id)).orElse(false);
        if (self && (request.role() != Role.ADMIN || !request.active())) {
            throw new BusinessRuleException("Δεν μπορείτε να αφαιρέσετε τον ρόλο διαχειριστή ή να απενεργοποιήσετε τον δικό σας λογαριασμό");
        }
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setActive(request.active());
        return userMapper.toResponse(user);
    }
}
