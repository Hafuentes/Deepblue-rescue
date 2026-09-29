package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TreatmentRepository extends JpaRepository<Treatment, Long>{

    // Obtener tratamientos de un animal ordenados cronologicamente
    List<Treatment> findByAnimalIdOrderByPerformedAtAsc(Long animalId);

    @Query("""
            select t
            from Treatment t
            where t.performedAt between :start and :end
            order by t.performedAt asc
            """)
    List<Treatment> findByTreatmentsBetweenDates(
        @Param("start") LocalDateTime start,
        @Param ("end") LocalDateTime end
    );

    // JPQL navegando tres entidades
    @Query("""
        select t
        from Treatment t
        join t.animal a
        join a.rescueCase rc
        join rc.rescueCenter rcent
        where rcent.code = :centerCode
        """)
    List<Treatment> findByRescueCenterCode(@Param("centerCode") String centerCode);

    //JPQL con N:M
    @Query("""
        select distinct t
        from Treatment t
        join t.specialist s
        join s.expertiseAreas e
        where lower(e.name) = lower(:expertiseName)
        """)
    List<Treatment> findBySpecialistExpertiseName(@Param("expertiseName") String expertiseName);
}