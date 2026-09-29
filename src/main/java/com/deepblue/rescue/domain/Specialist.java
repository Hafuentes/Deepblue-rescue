package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "specialists")
public class Specialist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professional_code", nullable = false, unique = true, length = 50)
    private String professionalCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToMany
    @JoinTable(
        name = "specialist_expertise",
        joinColumns = @JoinColumn(name = "specialist_id"),
        inverseJoinColumns = @JoinColumn(name = "expertise_id")
    )
    private Set<Expertise> expertiseAreas = new HashSet<>();

    @OneToMany(mappedBy = "specialist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Treatment> treatments = new ArrayList<>();

    protected Specialist() {
    }

    public Specialist(String professionalCode, String firstName, String lastName, String email, Boolean active) {
        this.professionalCode = professionalCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        if (active != null) {
            this.active = active;
        }
    }

    public void addExpertise(Expertise expertise) {
        this.expertiseAreas.add(expertise);
        expertise.getSpecialists().add(this);
    }

    public void addTreatment(Treatment treatment) {
        this.treatments.add(treatment);
        treatment.setSpecialist(this);
    }

    public Long getId() {
        return id;
    }

    public String getProfessionalCode() {
        return professionalCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getActive() {
        return active;
    }

    public Set<Expertise> getExpertiseAreas() {
        return expertiseAreas;
    }

    public List<Treatment> getTreatments() {
        return treatments;
    }
}