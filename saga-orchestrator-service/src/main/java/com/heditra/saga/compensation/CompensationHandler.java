package com.heditra.saga.compensation;

import com.heditra.saga.model.SagaInstance;
import com.heditra.saga.model.SagaStep;
import com.heditra.saga.model.StepStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;

@Component
public class CompensationHandler {

    private static final Logger log = LoggerFactory.getLogger(CompensationHandler.class);

    public void compensate(SagaInstance saga) {
        saga.getSteps().stream()
                .filter(step -> step.getStatus() == StepStatus.COMPLETED)
                .sorted(Comparator.comparing(SagaStep::getExecutedAt).reversed())
                .forEach(this::executeCompensation);
    }

    private void executeCompensation(SagaStep step) {
        try {
            switch (step.getStepName()) {
                case "inventory-reservation" -> log.info("Compensating inventory reservation for saga step {}", step.getId());
                case "payment-initiation" -> log.info("Compensating payment initiation for saga step {}", step.getId());
                case "notification-sending" -> log.info("No compensation required for notification step {}", step.getId());
                default -> log.info("Unknown step '{}', no compensation configured", step.getStepName());
            }
            step.setStatus(StepStatus.COMPENSATED);
            step.setCompensatedAt(LocalDateTime.now());
        } catch (Exception ex) {
            log.error("Compensation failed for step {}: {}", step.getId(), ex.getMessage(), ex);
            step.setErrorMessage("Compensation failed: " + ex.getMessage());
        }
    }
}

