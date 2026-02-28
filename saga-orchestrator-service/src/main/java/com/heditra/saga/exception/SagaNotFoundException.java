package com.heditra.saga.exception;

public class SagaNotFoundException extends BusinessException {

    public SagaNotFoundException(String sagaId) {
        super("SAGA_NOT_FOUND", "Saga instance not found for id: " + sagaId);
    }
}

