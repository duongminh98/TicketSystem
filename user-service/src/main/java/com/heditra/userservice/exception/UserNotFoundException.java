package com.heditra.userservice.exception;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long id) {
        super("USER_NOT_FOUND", "User not found with id: " + id);
    }

    public UserNotFoundException(String field, String value) {
        super("USER_NOT_FOUND", "User not found with " + field + ": " + value);
    }
}
