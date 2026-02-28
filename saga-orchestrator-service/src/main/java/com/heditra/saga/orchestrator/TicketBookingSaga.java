package com.heditra.saga.orchestrator;

import com.heditra.events.ticket.TicketCreatedEvent;
import com.heditra.saga.compensation.CompensationHandler;
import com.heditra.saga.exception.BusinessException;
import com.heditra.saga.model.SagaInstance;
import com.heditra.saga.model.SagaStatus;
import com.heditra.saga.model.SagaStep;
import com.heditra.saga.model.StepStatus;
import com.heditra.saga.repository.SagaInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class TicketBookingSaga {

    private static final Logger log = LoggerFactory.getLogger(TicketBookingSaga.class);

    private final SagaInstanceRepository sagaInstanceRepository;
    private final CompensationHandler compensationHandler;

    public TicketBookingSaga(SagaInstanceRepository sagaInstanceRepository,
                             CompensationHandler compensationHandler) {
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.compensationHandler = compensationHandler;
    }

    @KafkaListener(topics = "ticket-created", groupId = "saga-orchestrator-group")
    @Transactional
    public void onTicketCreated(TicketCreatedEvent event) {
        if (event == null || event.getTicketId() == null) {
            log.warn("Received invalid TicketCreatedEvent: {}", event);
            return;
        }

        log.info("Starting ticket booking saga for ticket: {}", event.getTicketId());

        SagaInstance saga = SagaInstance.builder()
                .sagaId(UUID.randomUUID().toString())
                .ticketId(event.getTicketId())
                .status(SagaStatus.STARTED)
                .startedAt(LocalDateTime.now())
                .build();

        saga = sagaInstanceRepository.save(saga);

        try {
            executeStep(saga, "inventory-reservation", () -> true);
            executeStep(saga, "payment-initiation", () -> true);
            executeStep(saga, "notification-sending", () -> true);

            saga.setStatus(SagaStatus.IN_PROGRESS);
            sagaInstanceRepository.save(saga);

            log.info("Saga in progress for ticket: {}", event.getTicketId());
        } catch (Exception ex) {
            log.error("Saga execution failed for ticket {}: {}", event.getTicketId(), ex.getMessage(), ex);
            compensate(saga, ex.getMessage());
        }
    }

    private void executeStep(SagaInstance saga, String stepName, StepExecutor executor) {
        SagaStep step = SagaStep.builder()
                .stepName(stepName)
                .status(StepStatus.IN_PROGRESS)
                .executedAt(LocalDateTime.now())
                .build();

        saga.addStep(step);

        boolean success = executor.execute();
        if (success) {
            step.setStatus(StepStatus.COMPLETED);
        } else {
            step.setStatus(StepStatus.FAILED);
            throw new BusinessException("SAGA_STEP_FAILED", "Saga step failed: " + stepName);
        }
    }

    private void compensate(SagaInstance saga, String reason) {
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setCompensationReason(reason);
        sagaInstanceRepository.save(saga);

        try {
            compensationHandler.compensate(saga);
            saga.setStatus(SagaStatus.COMPENSATED);
        } catch (Exception ex) {
            log.error("Saga compensation failed for saga {}: {}", saga.getSagaId(), ex.getMessage(), ex);
            saga.setStatus(SagaStatus.FAILED);
        }

        saga.setCompletedAt(LocalDateTime.now());
        sagaInstanceRepository.save(saga);
    }

    @FunctionalInterface
    private interface StepExecutor {
        boolean execute();
    }
}

