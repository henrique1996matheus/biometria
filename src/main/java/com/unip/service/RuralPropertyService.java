package com.unip.service;

import java.time.LocalDate;
import java.util.List;

import com.unip.model.RuralProperty;

public interface RuralPropertyService {
    RuralProperty cadastrarNovaPropriedade(RuralProperty propriedade);

    List<RuralProperty> listarTodasPropriedades();

    void deletarPropriedade(int id);

    void updateDate(int id, LocalDate data);
}
