package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RescueCaseRepository extends JpaRepository<RescueCase, Long>{

    // A. Buscar un caso por su código
    Optional<RescueCase> findByCaseCode(String caseCode);

    // B. Buscar casos por status ordenados por fecha de rescate ascendente
    List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);

    // C. Buscar casos pertenecientes a un centro determinado mediante su código (Navegando relaciones)
    List<RescueCase> findByRescueCenterCode(String code);

    // Casos ordenados posteriores a determinada fecha ordenados del más reciente al más antiguo
    List<RescueCase> findByRescueDateAfterOrderByRescueDateDesc(LocalDate date);

}