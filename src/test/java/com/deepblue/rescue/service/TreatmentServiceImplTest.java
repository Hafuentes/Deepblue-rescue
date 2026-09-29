package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    private static final LocalDate RESCUE_DATE = LocalDate.of(2026, 8, 20);

    private Animal animalWithCaseStatus(RescueStatus status) {
        RescueCase rescueCase = new RescueCase("RES-2026-100", RESCUE_DATE, "Santa Marta", status);
        Animal animal = new Animal("AN-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        animal.setRescueCase(rescueCase);
        return animal;
    }

    private Specialist specialist(boolean active) {
        return new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", active);
    }

    private CreateTreatmentRequest request(LocalDateTime performedAt, TreatmentType type) {
        return new CreateTreatmentRequest("AN-001", "SPEC-001", performedAt, type,
                "Cleaning of left front flipper injury.");
    }

    // TEST 5
    @Test
    void shouldRegisterTreatment() {
        Animal animal = animalWithCaseStatus(RescueStatus.IN_REHABILITATION);
        Specialist specialist = specialist(true);
        LocalDateTime when = LocalDateTime.of(2026, 8, 21, 9, 0);
        TreatmentResponse response = new TreatmentResponse(1L, "AN-001", "SPEC-001",
                when, TreatmentType.WOUND_CARE, "Cleaning of left front flipper injury.");

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Treatment.class))).thenReturn(response);

        TreatmentResponse result = service.register(request(when, TreatmentType.WOUND_CARE));

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Treatment> captor = ArgumentCaptor.forClass(Treatment.class);
        verify(treatmentRepository).save(captor.capture());
        Treatment saved = captor.getValue();
        assertThat(saved.getAnimal()).isSameAs(animal);
        assertThat(saved.getSpecialist()).isSameAs(specialist);
        assertThat(saved.getPerformedAt()).isEqualTo(when);
        assertThat(saved.getType()).isEqualTo(TreatmentType.WOUND_CARE);
    }

    @Test
    void shouldThrowWhenAnimalDoesNotExist() {
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(
                request(LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.WOUND_CARE)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-001");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSpecialistDoesNotExist() {
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animalWithCaseStatus(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(
                request(LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.WOUND_CARE)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SPEC-001");

        verify(treatmentRepository, never()).save(any());
    }

    // TEST 6
    @Test
    void shouldRejectInactiveSpecialistAndNeverSave() {
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animalWithCaseStatus(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist(false)));

        assertThatThrownBy(() -> service.register(
                request(LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.WOUND_CARE)))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    // TEST 7
    @Test
    void shouldRejectTreatmentWhenCaseIsReleased() {
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animalWithCaseStatus(RescueStatus.RELEASED)));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(
                request(LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.OBSERVATION)))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTreatmentWhenCaseIsClosed() {
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animalWithCaseStatus(RescueStatus.CLOSED)));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(
                request(LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.OBSERVATION)))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTreatmentBeforeRescueDate() {
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animalWithCaseStatus(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(
                request(LocalDateTime.of(2026, 8, 15, 9, 0), TreatmentType.WOUND_CARE)))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldFindTreatmentsByAnimalCode() {
        Animal animal = animalWithCaseStatus(RescueStatus.IN_REHABILITATION);
        Treatment treatment = new Treatment(LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.HYDRATION, "IV fluids");
        treatment.setAnimal(animal);
        TreatmentResponse response = new TreatmentResponse(1L, "AN-001", "SPEC-001",
                treatment.getPerformedAt(), TreatmentType.HYDRATION, "IV fluids");

        when(treatmentRepository.findByAnimalAnimalCodeOrderByPerformedAtAsc("AN-001"))
                .thenReturn(List.of(treatment));
        when(mapper.toResponse(treatment)).thenReturn(response);

        assertThat(service.findByAnimalCode("AN-001")).containsExactly(response);
    }
}
