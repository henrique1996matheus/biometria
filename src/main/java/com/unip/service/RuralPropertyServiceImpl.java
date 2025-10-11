package com.unip.service;

import java.time.LocalDate;
import java.util.List;

import com.unip.model.RuralProperty;
import com.unip.repository.RuralPropertyRepository;

public class RuralPropertyServiceImpl implements RuralPropertyService {

    private final RuralPropertyRepository repository;

    public RuralPropertyServiceImpl(RuralPropertyRepository repository) {
        this.repository = repository;
    }

    @Override
    public RuralProperty cadastrarNovaPropriedade(RuralProperty property) {
        return repository.save(property);
    }

    @Override
    public List<RuralProperty> listarTodasPropriedades() {
        return repository.findAll();
    }

    @Override
    public void deletarPropriedade(int id) {
        repository.deleteById(id);
    }

    @Override
    public void updateDate(int id, LocalDate date) {
        repository.updateInspectionDate(id, date);
    }

}
