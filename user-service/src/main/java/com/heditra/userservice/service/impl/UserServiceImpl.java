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
import com.heditra.userservice.model.User;
import com.heditra.userservice.model.UserRole;
import com.heditra.userservice.repository.UserRepository;
import com.heditra.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        validateUserUniqueness(request);

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

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

        publishUserDeletedEvent(user);

        log.info("User deleted: id={}", id);
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
