package com.heditra.events.user;

import com.heditra.events.core.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserDeletedEvent extends DomainEvent {

    private Long userId;

    public UserDeletedEvent(Long userId) {
        super("UserDeleted", String.valueOf(userId));
        this.userId = userId;
    }
}
