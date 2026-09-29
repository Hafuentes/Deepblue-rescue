package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.TreatmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TreatmentServiceImpl implements TreatmentService {

    private final AnimalRepository animalRepository;

    private final SpecialistRepository specialistRepository;

    private final TreatmentRepository treatmentRepository;

    private final TreatmentMapper mapper;

    public TreatmentServiceImpl(
            AnimalRepository animalRepository,
            SpecialistRepository specialistRepository,
            TreatmentRepository treatmentRepository,
            TreatmentMapper mapper) {

        this.animalRepository = animalRepository;
        this.specialistRepository = specialistRepository;
        this.treatmentRepository = treatmentRepository;
        this.mapper = mapper;
    }

    @Override
    public List<TreatmentResponse> findByAnimalCode(String animalCode) {
        return treatmentRepository
                .findByAnimalAnimalCodeOrderByPerformedAtAsc(animalCode)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TreatmentResponse register(CreateTreatmentRequest request) {

        // 1. Buscar Animal
        Animal animal = animalRepository
                .findByAnimalCode(request.animalCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Animal not found: " + request.animalCode()));

        // 2. Buscar Specialist
        Specialist specialist = specialistRepository
                .findByProfessionalCode(request.specialistCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Specialist not found: " + request.specialistCode()));

        // 3. Validar specialist.active
        if (!Boolean.TRUE.equals(specialist.getActive())) {
            throw new BusinessRuleException(
                    "Cannot register treatment because the specialist "
                            + request.specialistCode() + " is not active.");
        }

        // 4. Obtener RescueCase del Animal
        RescueCase rescueCase = animal.getRescueCase();

        // 5. Validar status
        RescueStatus status = rescueCase.getStatus();
        if (status == RescueStatus.RELEASED || status == RescueStatus.CLOSED) {
            throw new BusinessRuleException(
                    "Cannot register treatment because the case "
                            + rescueCase.getCaseCode() + " is " + status + ".");
        }

        // 6. Validar performedAt
        if (request.performedAt().toLocalDate().isBefore(rescueCase.getRescueDate())) {
            throw new BusinessRuleException(
                    "Treatment date " + request.performedAt().toLocalDate()
                            + " cannot be before the rescue date "
                            + rescueCase.getRescueDate() + ".");
        }

        // 7. Crear Treatment
        // (el Treatment del proyecto no recibe animal/specialist en el constructor:
        //  se asignan mediante los setters del dominio)
        Treatment treatment = new Treatment(
                request.performedAt(),
                request.type(),
                request.description());
        treatment.setAnimal(animal);
        treatment.setSpecialist(specialist);

        // 8. Guardar Treatment
        Treatment saved = treatmentRepository.save(treatment);

        // 9. Mapear TreatmentResponse
        return mapper.toResponse(saved);
    }
}
