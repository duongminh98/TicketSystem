package com.heditra.userservice.service.impl;

import com.heditra.events.core.EventPublisher;
import com.heditra.events.user.UserCreatedEvent;
import com.heditra.events.user.UserDeletedEvent;
import com.heditra.events.user.UserUpdatedEvent;
import com.heditra.userservice.dto.request.CreateUserRequest;
import com.heditra.userservice.dto.request.UpdateUserRequest;
import com.heditra.userservice.dto.response.UserResponse;
import com.heditra.userservice.exception.UserAlreadyExistsException;
import com.heditra.userservice.exception.UserNotFoundException;
import com.heditra.userservice.mapper.UserMapper;
import com.heditra.userservice.document.UserDocument;
import com.heditra.userservice.model.User;
import com.heditra.userservice.model.UserRole;
import com.heditra.userservice.repository.UserRepository;
import com.heditra.userservice.repository.UserSearchRepository;
import com.heditra.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserSearchRepository userSearchRepository;
    private final UserMapper userMapper;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        validateUserUniqueness(request);

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        indexUserToElasticsearch(savedUser);
        publishUserCreatedEvent(savedUser);

        log.info("User created: id={}, username={}", savedUser.getId(), savedUser.getUsername());
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toResponse(user);
    }

    @Override
    @Cacheable(value = "users", key = "'username:' + #username")
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("username", username));
        return userMapper.toResponse(user);
    }

    @Override
    @Cacheable(value = "users", key = "'email:' + #email")
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("email", email));
        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userMapper.toResponseList(userRepository.findAll());
    }

    @Override
    public List<UserResponse> getUsersByRole(String role) {
        UserRole userRole = UserRole.valueOf(role.toUpperCase());
        return userMapper.toResponseList(userRepository.findByRole(userRole));
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#id")
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        userMapper.updateEntityFromRequest(user, request);
        User savedUser = userRepository.save(user);

        indexUserToElasticsearch(savedUser);
        publishUserUpdatedEvent(savedUser);

        log.info("User updated: id={}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        userRepository.delete(user);

        try {
            userSearchRepository.deleteById(String.valueOf(id));
        } catch (Exception e) {
            log.warn("Failed to delete user from Elasticsearch: {}", e.getMessage());
        }
        publishUserDeletedEvent(user);

        log.info("User deleted: id={}", id);
    }

    @Override
    public List<UserResponse> searchUsers(String keyword) {
        List<UserDocument> results = userSearchRepository
                .findByFirstNameContainingOrLastNameContaining(keyword, keyword);

        if (results.isEmpty()) {
            results = userSearchRepository.findByUsernameContaining(keyword);
        }
        if (results.isEmpty()) {
            results = userSearchRepository.findByEmailContaining(keyword);
        }

        List<Long> ids = results.stream()
                .map(doc -> Long.parseLong(doc.getId()))
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            return List.of();
        }

        return userMapper.toResponseList(userRepository.findAllById(ids));
    }

    private void indexUserToElasticsearch(User user) {
        try {
            UserDocument doc = UserDocument.builder()
                    .id(String.valueOf(user.getId()))
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole().name())
                    .build();
            userSearchRepository.save(doc);
        } catch (Exception e) {
            log.warn("Failed to index user to Elasticsearch: {}", e.getMessage());
        }
    }

    private void validateUserUniqueness(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }
    }

    private void publishUserCreatedEvent(User user) {
        UserCreatedEvent event = new UserCreatedEvent(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
        eventPublisher.publish("user-created", event);
    }

    private void publishUserUpdatedEvent(User user) {
        UserUpdatedEvent event = new UserUpdatedEvent(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
        eventPublisher.publish("user-updated", event);
    }

    private void publishUserDeletedEvent(User user) {
        UserDeletedEvent event = new UserDeletedEvent(user.getId());
        eventPublisher.publish("user-deleted", event);
    }
}
