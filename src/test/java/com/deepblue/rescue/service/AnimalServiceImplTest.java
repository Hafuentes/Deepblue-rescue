package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository repository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    private Animal animalWithStatus(RescueStatus status) {
        RescueCase rescueCase = new RescueCase("RES-001", LocalDate.of(2026, 8, 20), "Santa Marta", status);
        Animal animal = new Animal("AN-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        animal.setRescueCase(rescueCase);
        return animal;
    }

    @Test
    void shouldFindAnimalByCode() {
        Animal animal = animalWithStatus(RescueStatus.IN_REHABILITATION);
        AnimalResponse response = new AnimalResponse(1L, "AN-001", "Green Sea Turtle",
                "Chelonia mydas", AnimalSex.FEMALE, "RES-001", RescueStatus.IN_REHABILITATION);

        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(mapper.toResponse(animal)).thenReturn(response);

        assertThat(service.findByCode("AN-001")).isEqualTo(response);
    }

    @Test
    void shouldThrowWhenAnimalNotFound() {
        when(repository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldListAnimalsInRehabilitation() {
        Animal animal = animalWithStatus(RescueStatus.IN_REHABILITATION);
        AnimalResponse response = new AnimalResponse(1L, "AN-001", "Green Sea Turtle",
                "Chelonia mydas", AnimalSex.FEMALE, "RES-001", RescueStatus.IN_REHABILITATION);

        when(repository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(animal));
        when(mapper.toResponse(animal)).thenReturn(response);

        assertThat(service.findAnimalsInRehabilitation()).containsExactly(response);
        verify(repository).findByRescueCaseStatus(RescueStatus.IN_REHABILITATION);
    }

    @Test
    void shouldAllowTreatmentWhenUnderEvaluationOrInRehabilitation() {
        when(repository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animalWithStatus(RescueStatus.UNDER_EVALUATION)))
                .thenReturn(Optional.of(animalWithStatus(RescueStatus.IN_REHABILITATION)));

        assertThat(service.canReceiveTreatment("AN-001")).isTrue();
        assertThat(service.canReceiveTreatment("AN-001")).isTrue();
    }

    @Test
    void shouldNotAllowTreatmentInOtherStatuses() {
        when(repository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animalWithStatus(RescueStatus.ADMITTED)))
                .thenReturn(Optional.of(animalWithStatus(RescueStatus.READY_FOR_RELEASE)))
                .thenReturn(Optional.of(animalWithStatus(RescueStatus.RELEASED)))
                .thenReturn(Optional.of(animalWithStatus(RescueStatus.CLOSED)));

        assertThat(service.canReceiveTreatment("AN-001")).isFalse();
        assertThat(service.canReceiveTreatment("AN-001")).isFalse();
        assertThat(service.canReceiveTreatment("AN-001")).isFalse();
        assertThat(service.canReceiveTreatment("AN-001")).isFalse();
    }
}
