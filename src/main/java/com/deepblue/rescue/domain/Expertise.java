package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "expertise")
public class Expertise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @ManyToMany(mappedBy = "expertiseAreas")
    private Set<Specialist> specialists = new HashSet<>();

    protected Expertise() {
    }

    public Expertise(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Specialist> getSpecialists() {
        return specialists;
    }
}
