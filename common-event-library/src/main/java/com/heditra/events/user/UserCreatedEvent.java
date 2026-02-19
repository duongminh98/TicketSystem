package com.heditra.events.user;

import com.heditra.events.core.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserCreatedEvent extends DomainEvent {

    private Long userId;
    private String username;
    private String email;
    private String role;

    public UserCreatedEvent(Long userId, String username, String email, String role) {
        super("UserCreated", String.valueOf(userId));
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
    }
}
