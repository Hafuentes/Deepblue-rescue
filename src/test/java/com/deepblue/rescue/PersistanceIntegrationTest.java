package com.deepblue.rescue;

import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.deepblue.rescue.domain.*;
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldStartContainerAndApplyFlyway() {
        assertThat(postgres.isRunning()).isTrue();

        // Comprobamos que Flyway ejecutó las migraciones consultando la historia
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history", Integer.class
        );
        
        assertThat(migrationCount).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldTestInheritedMethodsAnd1toN() {
        // Paso 48: Test de métodos heredados
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        assertThat(savedCenter.getId()).isNotNull();
        assertThat(rescueCenterRepository.findById(savedCenter.getId())).isPresent();
        assertThat(rescueCenterRepository.existsById(savedCenter.getId())).isTrue();
        assertThat(rescueCenterRepository.count()).isGreaterThan(0);

        // Paso 49: Test relación 1:N (RescueCenter 1 -> N RescueCase)
        RescueCase case1 = new RescueCase("RES-001", java.time.LocalDate.now(), "Playa Norte", RescueStatus.IN_REHABILITATION);
        RescueCase case2 = new RescueCase("RES-002", java.time.LocalDate.now(), "Playa Sur", RescueStatus.READY_FOR_RELEASE);

        savedCenter.addCase(case1);
        savedCenter.addCase(case2);

        rescueCaseRepository.saveAll(java.util.List.of(case1, case2));

        java.util.List<RescueCase> cases = rescueCaseRepository.findByRescueCenterCode("DB-CAR");
        assertThat(cases).hasSize(2);
        assertThat(cases).allMatch(c -> c.getRescueCenter().getCode().equals("DB-CAR"));
    }

    @Test
    void shouldTestOneToOneRelationships() {
        // Paso 50: Test RescueCase 1:1 Animal
        RescueCenter center = new RescueCenter("DB-PAC", "DeepBlue Pacific", "Tumaco");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase("RES-2026-001", java.time.LocalDate.now(), "Playa Central", RescueStatus.ADMITTED);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal("AN-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        assertThat(rescueCase.getAnimal()).isNotNull();
        assertThat(animal.getRescueCase()).isNotNull();
        assertThat(animal.getRescueCase().getCaseCode()).isEqualTo("RES-2026-001");

        // Paso 51: Test Animal 1:1 MedicalRecord con Cascade
        MedicalRecord record = new MedicalRecord(
                new java.math.BigDecimal("28.40"),
                "STABLE",
                "Left front flipper injury",
                "Possible plastic ingestion"
        );
        animal.assignMedicalRecord(record);
        animalRepository.save(animal);

        assertThat(animal.getMedicalRecord()).isNotNull();
        assertThat(animal.getMedicalRecord().getId()).isNotNull();
    }

    @Test
    void shouldTestManyToManySpecialistExpertise() {
        // Paso 52: Test N:M entre Specialist y Expertise (cargando desde V2)
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma")
                .orElseThrow(() -> new IllegalStateException("Expertise not found"));
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation")
                .orElseThrow(() -> new IllegalStateException("Expertise not found"));

        Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
        specialist.addExpertise(trauma);
        specialist.addExpertise(rehabilitation);

        Specialist savedSpecialist = specialistRepository.save(specialist);

        assertThat(savedSpecialist.getExpertiseAreas()).hasSize(2);
        
        // Verificamos la consulta JPQL creada en el SpecialistRepository
        java.util.List<Specialist> activeTraumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");
        assertThat(activeTraumaSpecialists).extracting(Specialist::getFirstName).contains("Elena");
    }

    @Test
    void shouldTestAdvancedTreatmentQueries() {
        // Paso 53: Probar consultas JPQL y navegación entre múltiples entidades en TreatmentRepository
        RescueCenter center = new RescueCenter("DB-NOR", "DeepBlue North", "Cartagena");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase("RES-2026-002", java.time.LocalDate.now(), "Playa Blanca", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal("AN-2026-002", "Manatee", "Trichechus manatus", AnimalSex.MALE);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        Expertise surgery = new Expertise("Surgery");
        expertiseRepository.save(surgery);

        Specialist specialist = new Specialist("SPEC-002", "Carlos", "Mendoza", "carlos@deepblue.org", true);
        specialist.addExpertise(surgery);
        specialistRepository.save(specialist);

        Treatment treatment = new Treatment(java.time.LocalDateTime.now(), TreatmentType.SURGERY, "Wound suture");
        animal.addTreatment(treatment);
        specialist.addTreatment(treatment);
        treatmentRepository.save(treatment);

        // Validar búsqueda por código de centro de rescate navegando 3 entidades
        java.util.List<Treatment> treatmentsByCenter = treatmentRepository.findByRescueCenterCode("DB-NOR");
        assertThat(treatmentsByCenter).hasSize(1);
        assertThat(treatmentsByCenter.get(0).getDescription()).isEqualTo("Wound suture");

        // Validar búsqueda por nombre de experticia del especialista (relación N:M)
        java.util.List<Treatment> treatmentsByExpertise = treatmentRepository.findBySpecialistExpertiseName("Surgery");
        assertThat(treatmentsByExpertise).hasSize(1);
    }

    @Test
        void shouldTestRescueCaseAnimalOneToOne() {
            // 1. Crear y guardar el centro de rescate
            RescueCenter center = new RescueCenter("DB-PAC", "DeepBlue Pacific Center", "Tumaco");
            rescueCenterRepository.save(center);

            // 2. Crear y asociar el caso de rescate
            RescueCase rescueCase = new RescueCase(
                "RES-2026-001", 
                java.time.LocalDate.now(), 
                "Playa Central", 
                RescueStatus.ADMITTED
            );
            center.addCase(rescueCase);
            rescueCaseRepository.save(rescueCase);

            // 3. Crear y asignar el animal utilizando el método de sincronización bidireccional
            Animal animal = new Animal(
                "AN-2026-001", 
                "Green Sea Turtle", 
                "Chelonia mydas", 
                AnimalSex.FEMALE
            );
            rescueCase.assignAnimal(animal);
            animalRepository.save(animal);

            // 4. Comprobaciones de la relación 1:1 en ambos sentidos
            RescueCase foundCase = rescueCaseRepository.findByCaseCode("RES-2026-001").orElseThrow();
            
            assertThat(foundCase.getAnimal()).isNotNull();
            assertThat(foundCase.getAnimal().getAnimalCode()).isEqualTo("AN-2026-001");
            assertThat(animal.getRescueCase()).isNotNull();
            assertThat(animal.getRescueCase().getCaseCode()).isEqualTo("RES-2026-001");
        }
    
    @Test
        void shouldTestAnimalMedicalRecordOneToOne() {
            RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
            rescueCenterRepository.save(center);

            RescueCase rescueCase = new RescueCase("RES-2026-002", java.time.LocalDate.now(), "Bahía Concha", RescueStatus.IN_REHABILITATION);
            center.addCase(rescueCase);
            rescueCaseRepository.save(rescueCase);

            Animal animal = new Animal("AN-2026-002", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
            rescueCase.assignAnimal(animal);

            MedicalRecord medicalRecord = new MedicalRecord(
                new java.math.BigDecimal("28.40"),
                "STABLE",
                "Left front flipper injury",
                "Possible plastic ingestion"
            );
            animal.assignMedicalRecord(medicalRecord);

            // Al tener CascadeType.ALL en Animal -> MedicalRecord, guardar el animal persiste el expediente automáticamente
            animalRepository.save(animal);

            assertThat(animal.getId()).isNotNull();
            assertThat(animal.getMedicalRecord()).isNotNull();
            assertThat(animal.getMedicalRecord().getId()).isNotNull();
    }

    @Test
        void shouldTestSpecialistExpertiseManyToMany() {
            // 1. Recuperar áreas de experiencia del catálogo insertado por Flyway en V2
            Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma")
                    .orElseThrow(() -> new IllegalStateException("Expertise not found"));
            Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation")
                    .orElseThrow(() -> new IllegalStateException("Expertise not found"));

            // 2. Crear especialista y asociar ambas especialidades
            Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
            specialist.addExpertise(trauma);
            specialist.addExpertise(rehabilitation);

            // 3. Persistir
            Specialist savedSpecialist = specialistRepository.save(specialist);

            // 4. Comprobar que Elena tiene 2 expertiseAreas asociadas indirectamente mediante la tabla intermedia
            assertThat(savedSpecialist.getExpertiseAreas()).hasSize(2);
            assertThat(savedSpecialist.getExpertiseAreas())
                    .extracting(Expertise::getName)
                    .containsExactlyInAnyOrder("Trauma", "Rehabilitation");
    }

    @Test
        void shouldTestSimpleQueryMethod() {
            // 1. Crear un centro de rescate auxiliar para la prueba
            RescueCenter center = new RescueCenter("DB-TST", "Test Center", "Bogota");
            rescueCenterRepository.save(center);

            // 2. Crear varios casos con diferentes estados
            RescueCase case1 = new RescueCase("RES-001", java.time.LocalDate.now(), "Playa 1", RescueStatus.IN_REHABILITATION);
            RescueCase case2 = new RescueCase("RES-002", java.time.LocalDate.now(), "Playa 2", RescueStatus.READY_FOR_RELEASE);
            RescueCase case3 = new RescueCase("RES-003", java.time.LocalDate.now(), "Playa 3", RescueStatus.IN_REHABILITATION);

            center.addCase(case1);
            center.addCase(case2);
            center.addCase(case3);

            rescueCaseRepository.saveAll(java.util.List.of(case1, case2, case3));

            // 3. Ejecutar el Query Method definido en RescueCaseRepository para filtrar por status
            java.util.List<RescueCase> rehabilitationCases = 
                rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

            // 4. Verificar que el resultado retorne exactamente los 2 casos esperados
            assertThat(rehabilitationCases).hasSize(2);
            assertThat(rehabilitationCases)
                    .extracting(RescueCase::getCaseCode)
                    .containsExactlyInAnyOrder("RES-001", "RES-003");
    }

    @Test
    void shouldTestQueryMethodNavigatingRelationships() {
        // 1. Crear dos centros: DB-CAR y DB-PAC
        RescueCenter centerCar = new RescueCenter("DB-CAR", "Caribbean Center", "Santa Marta");
        RescueCenter centerPac = new RescueCenter("DB-PAC", "Pacific Center", "Tumaco");
        rescueCenterRepository.saveAll(java.util.List.of(centerCar, centerPac));

        // 2. Registrar caso y animal en el centro DB-CAR
        RescueCase caseCar = new RescueCase("RES-CAR-01", java.time.LocalDate.now(), "Playa Car", RescueStatus.IN_REHABILITATION);
        centerCar.addCase(caseCar);
        rescueCaseRepository.save(caseCar);

        Animal animalCar = new Animal("AN-CAR-01", "Turtle Car", "Chelonia mydas", AnimalSex.FEMALE);
        caseCar.assignAnimal(animalCar);
        animalRepository.save(animalCar);

        // 3. Registrar caso y animal en el centro DB-PAC
        RescueCase casePac = new RescueCase("RES-PAC-01", java.time.LocalDate.now(), "Playa Pac", RescueStatus.IN_REHABILITATION);
        centerPac.addCase(casePac);
        rescueCaseRepository.save(casePac);

        Animal animalPac = new Animal("AN-PAC-01", "Turtle Pac", "Chelonia mydas", AnimalSex.MALE);
        casePac.assignAnimal(animalPac);
        animalRepository.save(animalPac);

        // 4. Ejecutar el Query Method navegando las asociaciones (Animal -> RescueCase -> RescueCenter -> Code)
        java.util.List<Animal> animalsInCar = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");

        // 5. Comprobar que solo retorna el animal de DB-CAR y excluye el de DB-PAC
        assertThat(animalsInCar).hasSize(1);
        assertThat(animalsInCar.get(0).getAnimalCode()).isEqualTo("AN-CAR-01");
    }

    @Test
    void shouldTestSpecialistJpqlQuery() {
        // 1. Recuperar áreas de experiencia del catálogo insertado por Flyway en V2
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma")
                .orElseThrow(() -> new IllegalStateException("Expertise not found"));
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation")
                .orElseThrow(() -> new IllegalStateException("Expertise not found"));
        Expertise marineMammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals")
                .orElseThrow(() -> new IllegalStateException("Expertise not found"));
        Expertise marineBirds = expertiseRepository.findByNameIgnoreCase("Marine Birds")
                .orElseThrow(() -> new IllegalStateException("Expertise not found"));

        // 2. Crear especialistas y asociar sus respectivas experticias
        Specialist elena = new Specialist("SPEC-101", "Elena", "Vargas", "elena.v@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist mateo = new Specialist("SPEC-102", "Mateo", "Perez", "mateo.p@deepblue.org", true);
        mateo.addExpertise(marineMammals);
        mateo.addExpertise(rehabilitation);

        Specialist sofia = new Specialist("SPEC-103", "Sofia", "Gomez", "sofia.g@deepblue.org", true);
        sofia.addExpertise(marineBirds);
        sofia.addExpertise(trauma);

        specialistRepository.saveAll(java.util.List.of(elena, mateo, sofia));

        // 3. Consultar utilizando el Query JPQL definido en SpecialistRepository
        java.util.List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");

        // 4. Verificar que solo retorne a Elena y Sofia (quienes poseen Trauma), excluyendo a Mateo
        assertThat(traumaSpecialists)
                .extracting(Specialist::getFirstName)
                .containsExactlyInAnyOrder("Elena", "Sofia");
    }

    @Test
    void shouldTestTreatmentCreationAndChronologicalQuery() {
        // 1. Crear centro, caso, animal y especialista necesarios
        RescueCenter center = new RescueCenter("DB-TRT", "Treatment Center", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase("RES-TRT-01", java.time.LocalDate.now(), "Playa", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal("AN-TRT-01", "Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        Specialist elena = new Specialist("SPEC-ELENA", "Elena", "Vargas", "elena.v@deepblue.org", true);
        Specialist mateo = new Specialist("SPEC-MATEO", "Mateo", "Perez", "mateo.p@deepblue.org", true);
        specialistRepository.saveAll(java.util.List.of(elena, mateo));

        // 2. Paso 56: Registrar tratamientos en orden cronológico
        java.time.LocalDateTime baseTime = java.time.LocalDateTime.of(2026, 8, 1, 10, 0);
        
        Treatment treatment1 = new Treatment(baseTime, TreatmentType.WOUND_CARE, "Cleaning wound");
        Treatment treatment2 = new Treatment(baseTime.plusDays(2), TreatmentType.HYDRATION, "Fluid therapy");
        Treatment treatment3 = new Treatment(baseTime.plusDays(5), TreatmentType.OBSERVATION, "General checkup");

        animal.addTreatment(treatment1);
        elena.addTreatment(treatment1);

        animal.addTreatment(treatment2);
        elena.addTreatment(treatment2);

        animal.addTreatment(treatment3);
        mateo.addTreatment(treatment3);

        treatmentRepository.saveAll(java.util.List.of(treatment1, treatment2, treatment3));

        // 3. Paso 57: Ejecutar el Query Method de tratamientos ordenados cronológicamente
        java.util.List<Treatment> chronologicalTreatments = 
            treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());

        // 4. Verificar el orden y los tipos de tratamiento
        assertThat(chronologicalTreatments).hasSize(3);
        assertThat(chronologicalTreatments)
                .extracting(Treatment::getType)
                .containsExactly(
                    TreatmentType.WOUND_CARE, 
                    TreatmentType.HYDRATION, 
                    TreatmentType.OBSERVATION
                );
    }

    @Test
        void shouldTestTreatmentQueryMethod() {
            RescueCenter center = new RescueCenter("DB-TRT2", "Treatment Center 2", "Cartagena");
            rescueCenterRepository.save(center);

            RescueCase rescueCase = new RescueCase("RES-TRT-02", java.time.LocalDate.now(), "Playa Norte", RescueStatus.IN_REHABILITATION);
            center.addCase(rescueCase);
            rescueCaseRepository.save(rescueCase);

            Animal animal = new Animal("AN-TRT-02", "Loggerhead", "Caretta caretta", AnimalSex.MALE);
            rescueCase.assignAnimal(animal);
            animalRepository.save(animal);

            Specialist specialist = new Specialist("SPEC-TRT", "Ana", "Gomez", "ana.g@deepblue.org", true);
            specialistRepository.save(specialist);

            java.time.LocalDateTime baseTime = java.time.LocalDateTime.of(2026, 8, 1, 10, 0);
            Treatment t1 = new Treatment(baseTime, TreatmentType.WOUND_CARE, "First treatment");
            Treatment t2 = new Treatment(baseTime.plusDays(1), TreatmentType.HYDRATION, "Second treatment");
            
            animal.addTreatment(t1);
            specialist.addTreatment(t1);
            animal.addTreatment(t2);
            specialist.addTreatment(t2);

            treatmentRepository.saveAll(java.util.List.of(t1, t2));

            java.util.List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());

            assertThat(treatments).hasSize(2);
            assertThat(treatments).extracting(Treatment::getType)
                    .containsExactly(TreatmentType.WOUND_CARE, TreatmentType.HYDRATION);
    }

    @Test
        void shouldTestTreatmentsBetweenDatesJpql() {
            // 1. Crear centro, caso, animal y especialista necesarios
            RescueCenter center = new RescueCenter("DB-INT", "Interval Center", "Santa Marta");
            rescueCenterRepository.save(center);

            RescueCase rescueCase = new RescueCase("RES-INT-01", java.time.LocalDate.now(), "Playa Interval", RescueStatus.IN_REHABILITATION);
            center.addCase(rescueCase);
            rescueCaseRepository.save(rescueCase);

            Animal animal = new Animal("AN-INT-01", "Interval Turtle", "Chelonia mydas", AnimalSex.FEMALE);
            rescueCase.assignAnimal(animal);
            animalRepository.save(animal);

            Specialist specialist = new Specialist("SPEC-INT", "Ana", "Ruiz", "ana.r@deepblue.org", true);
            specialistRepository.save(specialist);

            // 2. Crear tratamientos en fechas específicas (2026-08-01, 2026-08-10 y 2026-08-20)
            java.time.LocalDateTime t1Date = java.time.LocalDateTime.of(2026, 8, 1, 10, 0);
            java.time.LocalDateTime t2Date = java.time.LocalDateTime.of(2026, 8, 10, 10, 0);
            java.time.LocalDateTime t3Date = java.time.LocalDateTime.of(2026, 8, 20, 10, 0);

            Treatment t1 = new Treatment(t1Date, TreatmentType.WOUND_CARE, "Desc 1");
            Treatment t2 = new Treatment(t2Date, TreatmentType.HYDRATION, "Desc 2");
            Treatment t3 = new Treatment(t3Date, TreatmentType.OBSERVATION, "Desc 3");

            animal.addTreatment(t1);
            specialist.addTreatment(t1);
            animal.addTreatment(t2);
            specialist.addTreatment(t2);
            animal.addTreatment(t3);
            specialist.addTreatment(t3);

            treatmentRepository.saveAll(java.util.List.of(t1, t2, t3));

            // 3. Consultar entre 2026-08-05 y 2026-08-15 usando el método JPQL definido en TreatmentRepository
            java.time.LocalDateTime start = java.time.LocalDateTime.of(2026, 8, 5, 0, 0);
            java.time.LocalDateTime end = java.time.LocalDateTime.of(2026, 8, 15, 23, 59);

            java.util.List<Treatment> treatments = treatmentRepository.findByTreatmentsBetweenDates(start, end);

            // 4. Verificar que regrese únicamente el tratamiento del 2026-08-10
            assertThat(treatments).hasSize(1);
            assertThat(treatments.get(0).getPerformedAt()).isEqualTo(t2Date);
            assertThat(treatments.get(0).getType()).isEqualTo(TreatmentType.HYDRATION);
    }

    @Test
        void shouldThrowExceptionOnUniqueConstraintViolation() {
            RescueCenter center = new RescueCenter("DB-UNI", "Unique Center", "Santa Marta");
            rescueCenterRepository.save(center);

            RescueCase rescueCase1 = new RescueCase("RES-UNI-01", java.time.LocalDate.now(), "Playa", RescueStatus.ADMITTED);
            center.addCase(rescueCase1);
            rescueCaseRepository.save(rescueCase1);

            Animal animal1 = new Animal("AN-DUP-001", "Turtle 1", "Chelonia mydas", AnimalSex.FEMALE);
            rescueCase1.assignAnimal(animal1);
            animalRepository.saveAndFlush(animal1);

            // Intentar guardar otro animal con exactamente el mismo código utilizando un nuevo caso
            RescueCase rescueCase2 = new RescueCase("RES-UNI-02", java.time.LocalDate.now(), "Playa 2", RescueStatus.ADMITTED);
            center.addCase(rescueCase2);
            rescueCaseRepository.save(rescueCase2);

            Animal animal2 = new Animal("AN-DUP-001", "Turtle 2", "Chelonia mydas", AnimalSex.MALE);
            rescueCase2.assignAnimal(animal2);

            org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> animalRepository.saveAndFlush(animal2)
            );
    }

    @Test
        void shouldVerifyIntegerChallenge() {
            // 1. Crear y guardar el Centro de Rescate (DeepBlue Caribbean)
            RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
            rescueCenterRepository.save(center);

            // 2. Crear y asociar el Caso de Rescate (RES-2026-100)
            RescueCase rescueCase = new RescueCase(
                "RES-2026-100", 
                java.time.LocalDate.of(2026, 8, 18), 
                "Bahía Concha", 
                RescueStatus.IN_REHABILITATION
            );
            center.addCase(rescueCase);
            rescueCaseRepository.save(rescueCase);

            // 3. Crear y asignar el Animal (Tortuga marina verde - AN-2026-100)
            Animal animal = new Animal(
                "AN-2026-100", 
                "Green Sea Turtle", 
                "Chelonia mydas", 
                AnimalSex.FEMALE
            );
            rescueCase.assignAnimal(animal);

            // 4. Crear y asignar el Expediente Médico
            MedicalRecord medicalRecord = new MedicalRecord(
                new java.math.BigDecimal("27.80"),
                "STABLE",
                "Injury caused by fishing net",
                "Possible plastic ingestion"
            );
            animal.assignMedicalRecord(medicalRecord);
            
            // Persistir animal (guardará en cascada su expediente gracias a CascadeType.ALL)
            animalRepository.save(animal);

            // 5. Recuperar especialidades del catálogo Flyway (V2) y registrar al Especialista (Elena Vargas)
            com.deepblue.rescue.domain.Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
            com.deepblue.rescue.domain.Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
            com.deepblue.rescue.domain.Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

            Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
            specialist.addExpertise(marineReptiles);
            specialist.addExpertise(trauma);
            specialist.addExpertise(rehabilitation);
            specialistRepository.save(specialist);

            // 6. Registrar Tratamientos realizados al animal por la especialista
            java.time.LocalDateTime treatmentTime = java.time.LocalDateTime.of(2026, 8, 18, 11, 0);
            
            Treatment treatment1 = new Treatment(treatmentTime, TreatmentType.WOUND_CARE, "Cleaning of left front flipper");
            Treatment treatment2 = new Treatment(treatmentTime.plusHours(2), TreatmentType.HYDRATION, "Subcutaneous fluid therapy");

            animal.addTreatment(treatment1);
            specialist.addTreatment(treatment1);

            animal.addTreatment(treatment2);
            specialist.addTreatment(treatment2);

            treatmentRepository.saveAll(java.util.List.of(treatment1, treatment2));

            // Consulta 1: ¿Existe el caso RES-2026-100?
            boolean caseExists = rescueCaseRepository.findByCaseCode("RES-2026-100").isPresent();
            assertThat(caseExists).isTrue();

            // Consulta 2: Obtener todos los casos IN_REHABILITATION
            java.util.List<RescueCase> rehabCases = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
            assertThat(rehabCases).isNotEmpty();

            // Consulta 3: Obtener animales pertenecientes a DB-CAR
            java.util.List<Animal> centerAnimals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");
            assertThat(centerAnimals).isNotEmpty();
            assertThat(centerAnimals.get(0).getAnimalCode()).isEqualTo("AN-2026-100");

            // Consulta 4: Buscar animales cuyo nombre común contenga "turtle" ignorando mayúsculas
            java.util.List<Animal> turtles = animalRepository.findByCommonNameContainingIgnoreCase("turtle");
            assertThat(turtles).isNotEmpty();

            // Consulta 5: Obtener especialistas con experiencia "Trauma"
            java.util.List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");
            assertThat(traumaSpecialists).isNotEmpty();
            assertThat(traumaSpecialists).extracting(Specialist::getFirstName).contains("Elena");

            // Consulta 6: Obtener todos los tratamientos de AN-2026-100 ordenados cronológicamente
            java.util.List<Treatment> animalTreatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
            assertThat(animalTreatments).hasSize(2);
            assertThat(animalTreatments.get(0).getType()).isEqualTo(TreatmentType.WOUND_CARE);

            // Consulta 7: Obtener tratamientos realizados por especialistas con experiencia "Rehabilitation"
            java.util.List<Treatment> rehabTreatments = treatmentRepository.findBySpecialistExpertiseName("Rehabilitation");
            assertThat(rehabTreatments).isNotEmpty();

            // Consulta 8: Obtener tratamientos realizados entre dos fechas
            java.time.LocalDateTime start = java.time.LocalDateTime.of(2026, 8, 18, 0, 0);
            java.time.LocalDateTime end = java.time.LocalDateTime.of(2026, 8, 18, 23, 59);
            java.util.List<Treatment> dateTreatments = treatmentRepository.findByTreatmentsBetweenDates(start, end);
            assertThat(dateTreatments).hasSize(2);
    }

    @Test
        void shouldTestRetoSinGuiaAnimalQuery() {
            RescueCenter center = new RescueCenter("DB-RET", "Reto Center", "Santa Marta");
            rescueCenterRepository.save(center);

            RescueCase rescueCase = new RescueCase("RES-RET-01", LocalDate.now(), "Playa", RescueStatus.IN_REHABILITATION);
            center.addCase(rescueCase);
            rescueCaseRepository.save(rescueCase);

            Animal animal = new Animal("AN-RET-01", "Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
            rescueCase.assignAnimal(animal);
            animalRepository.save(animal);

            Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
            Specialist specialist = new Specialist("SPEC-RET", "Dr. House", "Gregory", "house@deepblue.org", true);
            specialist.addExpertise(trauma);
            specialistRepository.save(specialist);

            Treatment treatment = new Treatment(LocalDateTime.now(), TreatmentType.SURGERY, "Trauma care");
            animal.addTreatment(treatment);
            specialist.addTreatment(treatment);
            treatmentRepository.save(treatment);

            List<Animal> animals = animalRepository.findAnimalsInRehabilitationByExpertise(RescueStatus.IN_REHABILITATION, "Trauma");

            assertThat(animals).hasSize(1);
            assertThat(animals.get(0).getAnimalCode()).isEqualTo("AN-RET-01");
    }


}