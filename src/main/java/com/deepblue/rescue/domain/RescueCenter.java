package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rescue_centers")
public class RescueCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @OneToMany(mappedBy = "rescueCenter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RescueCase> rescueCases = new ArrayList<>();

    protected RescueCenter() {
    }

    public RescueCenter(String code, String name, String city) {
        this.code = code;
        this.name = name;
        this.city = city;
    }

    public void addCase(RescueCase rescueCase) {
        rescueCases.add(rescueCase);
        rescueCase.setRescueCenter(this);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public List<RescueCase> getRescueCases() {
        return rescueCases;
    }
}