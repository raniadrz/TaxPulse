package gr.taxpulse.portal.service;

import gr.taxpulse.client.entity.Client;
import gr.taxpulse.client.repository.ClientRepository;
import gr.taxpulse.client.service.ClientService;
import gr.taxpulse.common.exception.ConflictException;
import gr.taxpulse.common.exception.ResourceNotFoundException;
import gr.taxpulse.portal.dto.CreatePortalAccountRequest;
import gr.taxpulse.portal.dto.PortalAccountSummary;
import gr.taxpulse.portal.dto.UpdatePortalAccountRequest;
import gr.taxpulse.security.UserAccessCache;
import gr.taxpulse.user.dto.UserResponse;
import gr.taxpulse.user.entity.Role;
import gr.taxpulse.user.entity.User;
import gr.taxpulse.user.mapper.UserMapper;
import gr.taxpulse.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Staff-side management of a client's portal logins. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ClientService clientService;
    private final ClientRepository clientRepository;
    private final UserAccessCache accessCache;

    /** Every portal login of the office, with the client it belongs to. */
    public List<PortalAccountSummary> listAll() {
        List<User> accounts = userRepository.findByRole(Role.CLIENT, Sort.by("fullName"));
        Map<UUID, Client> clients = clientRepository.findAllById(accounts.stream().map(User::getClientId).distinct().toList())
                .stream().collect(Collectors.toMap(Client::getId, Function.identity()));
        return accounts.stream().map(u -> {
            Client c = clients.get(u.getClientId());
            return new PortalAccountSummary(u.getId(), u.getEmail(), u.getFullName(), u.isActive(), u.getLastLoginAt(),
                    u.getCreatedAt(), u.getClientId(), c == null ? null : c.getName(), c != null && c.isActive());
        }).toList();
    }

    public List<UserResponse> list(UUID clientId) {
        clientService.getEntity(clientId); // 404 for unknown client
        return userRepository.findByClientId(clientId, Sort.by("fullName")).stream().map(userMapper::toResponse).toList();
    }

    @Transactional
    public UserResponse create(UUID clientId, CreatePortalAccountRequest request) {
        clientService.getEntity(clientId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with e-mail %s already exists".formatted(email));
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setRole(Role.CLIENT);
        user.setClientId(clientId);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(UUID clientId, UUID userId, UpdatePortalAccountRequest request) {
        User user = userRepository.findById(userId)
                .filter(u -> clientId.equals(u.getClientId()))
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setFullName(request.fullName().trim());
        user.setActive(request.active());
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        accessCache.evictAfterCommit(userId); // deactivation applies to the next request
        return userMapper.toResponse(user);
    }
}
