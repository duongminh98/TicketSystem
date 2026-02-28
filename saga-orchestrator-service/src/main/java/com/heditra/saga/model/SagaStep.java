package com.heditra.saga.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "saga_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saga_id", nullable = false)
    private SagaInstance saga;

    @Column(nullable = false)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status;

    private String compensationAction;

    private LocalDateTime executedAt;

    private LocalDateTime compensatedAt;

    private String errorMessage;
}

