package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import com.deepblue.rescue.domain.RescueStatus;

public interface AnimalRepository extends JpaRepository<Animal, Long>{

    // A. Buscar por código del Animal
    Optional<Animal> findByAnimalCode(String animalCode);

    // B. Buscar por nombre común
    List<Animal> findByCommonNameContainingIgnoreCase(String name);

    // C. Buscar Animales cuyo caso tenga determinado estado
    List<Animal> findByRescueCaseStatus(RescueStatus status);

    // D. Buscar Animales pertenecientes a un centro mediante su código
    List<Animal> findByRescueCaseRescueCenterCode(String centerCode);
    

    @Query("""
    select distinct a
    from Animal a
    join a.rescueCase rc
    join a.treatments t
    join t.specialist s
    join s.expertiseAreas e
    where rc.status = :status
      and lower(e.name) = lower(:expertiseName)
    """)
    List<Animal> findAnimalsInRehabilitationByExpertise(
        @Param("status") RescueStatus status,
        @Param("expertiseName") String expertiseName    
    );
}