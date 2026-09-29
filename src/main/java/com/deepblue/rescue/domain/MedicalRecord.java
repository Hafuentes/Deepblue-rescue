package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false, unique = true)
    private Animal animal;

    @Column(name = "initial_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal initialWeight;

    @Column(name = "initial_condition", nullable = false, length = 100)
    private String initialCondition;

    @Column(columnDefinition = "TEXT")
    private String injuries;

    @Column(columnDefinition = "TEXT")
    private String observations;

    protected MedicalRecord() {
    }

    public MedicalRecord(BigDecimal initialWeight, String initialCondition, String injuries, String observations) {
        this.initialWeight = initialWeight;
        this.initialCondition = initialCondition;
        this.injuries = injuries;
        this.observations = observations;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }

    public Long getId() {
        return id;
    }

    public Animal getAnimal() {
        return animal;
    }

    public BigDecimal getInitialWeight() {
        return initialWeight;
    }

    public String getInitialCondition() {
        return initialCondition;
    }

    public String getInjuries() {
        return injuries;
    }

    public String getObservations() {
        return observations;
    }
}
