package com.unip.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity
public class RuralProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String owner;
    private LocalDate inspectionDate;

    public RuralProperty() {
    }

    public RuralProperty(String owner, LocalDate inspectionDate) {
        this.owner = owner;
        this.inspectionDate = inspectionDate;
    }

    public RuralProperty(Long id, String owner, LocalDate inspectionDate) {
        this.id = id;
        this.owner = owner;
        this.inspectionDate = inspectionDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public LocalDate getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(LocalDate inspectionDate) {
        this.inspectionDate = inspectionDate;
    }
}
