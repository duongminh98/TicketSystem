package com.heditra.saga.repository;

import com.heditra.saga.model.SagaInstance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaInstanceRepository extends JpaRepository<SagaInstance, String> {
}

