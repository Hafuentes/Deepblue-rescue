package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "rescue_cases")
public class RescueCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_code", nullable = false, unique = true, length = 50)
    private String caseCode;

    @Column(name = "rescue_date", nullable = false)
    private LocalDate rescueDate;

    @Column(name = "rescue_location", nullable = false)
    private String rescueLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RescueStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_center_id", nullable = false)
    private RescueCenter rescueCenter;

    @OneToOne(mappedBy = "rescueCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Animal animal;

    protected RescueCase() {
    }

    public RescueCase(String caseCode, LocalDate rescueDate, String rescueLocation, RescueStatus status) {
        this.caseCode = caseCode;
        this.rescueDate = rescueDate;
        this.rescueLocation = rescueLocation;
        this.status = status;
    }

    public void setRescueCenter(RescueCenter rescueCenter) {
        this.rescueCenter = rescueCenter;
    }

    public void assignAnimal(Animal animal) {
        this.animal = animal;
        animal.setRescueCase(this);
    }

    public Long getId() {
        return id;
    }

    public String getCaseCode() {
        return caseCode;
    }

    public LocalDate getRescueDate() {
        return rescueDate;
    }

    public String getRescueLocation() {
        return rescueLocation;
    }

    public RescueStatus getStatus() {
        return status;
    }

    public RescueCenter getRescueCenter() {
        return rescueCenter;
    }

    public Animal getAnimal() {
        return animal;
    }
}