package com.ignacio.legacyanalyzer.application.usecase;

import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.domain.ports.TableDependencyRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteDatabaseUseCase {

    private final TableDependencyRepositoryPort repository;

    public void execute() {

        repository.deleteAll();
    }
}