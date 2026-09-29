#  DeepBlue Rescue

## Descripcion del Proyecto

 <strong>DeepBlue Rescue </strong> es una plataforma a la persistencia de datos para organizaciones dedicadas al rescate, atención médica y rehabilitación de fauna marina. Este módulo implementa de forma robusta la capa de datos utilizando Java 21, Spring Boot 4, Spring Data JPA, Hibernate, PostgreSQL, Flyway y Testcontainers.

 ## Modelo de Datos y Entidades

 El sistema modela el dominio operativo a tráves de las siguientes entidades principales:

 * <strong>RescueCenter</strong> : Centros de rescate distribuidos por ciudades.

 * <strong> RescueCase</strong>: Casos de rescate registrados con códigos únicos y estados definidos(```ADMITTED ```, ```UNDER_EVALUATION```, ```IN_REHABILITATION```, ```READY_FOR_RELEASE```, ```RELEASED```, ```CLOSED```).

 * <strong> Animal</strong>: Especies marinas rescatadas.

 * <strong> MedicalRecord</strong>: Expediente médico inicial con información de peso, condición e injurias.

 * <strong> Specialist</strong>: Profesionales encargados de la atención.

 * <strong> Expertise</strong>: Catálogo de áreas de experiencia profesional.

 * <strong> Treatment</strong>: Registro detallado de tratamientos aplicados a los animales.

 ## Relaciones del Modelo

 * <strong> RescueCenter 1 : N RescueCase </strong> (Un centro gestiona múltiples casos de rescate).

 * <strong> RescueCase 1 : 1 Animal </strong> (Cada caso involucra un animal; asegurado mediante restricción ```UNIQUE``` en la llave foránea).
 
 * <strong> Animal 1 : 1 MedicalRecord </strong> (Cada animal posee un expediente médico asociado).

 * <strong> Specialist N : M Expertise </strong> (Manejado mediante la tabla asociativa ```specialist_expertise```).

 * <strong> Animal 1 : N Treatment y Specialist 1 : N Treatment </strong> (Relaciones hacia los tratamientos realizados).

 ## Instrucciones de ejecucion

 Para verificar y compilar el proyecto mediante Maven:

 ```text
    mvn clean compile
 ```

 ## Ejecución de Pruebas

 Las pruebas de integración se ejecutan contra una base de datos PostgreSQL real gestionada por Testcontainers

 ```text
    mvn clean test
 ```

 ## Mecanismo de Migracion de Flyway

 Flyway es el único componente responsable de crear y evolucionar el esquema de la base de datos a tráves de migraciones versionadas ubicadas en
 ``` src/main/resources/db/migration```  (```V1, V2, V3```). Hibernate opera estrictamente bajo la regla ```ddl-auto: validate```.

 ## Uso de Testcontainers

 Testcontainers levanta contenedores ligeros de PostgreSQL durante la ejecución de las pruebas de integración (```PersistenceIntegrationTest```), garantizando un entorno idéntico a producción sin requerir instalaciones locales de bases de datos.

 ## Query Methods Implementados

 * <strong> RescueCenterRepository.findByCode(String code) </strong>
 
 * <strong> RescueCaseRepository.findByCaseCode(String caseCode) </strong>

 * <strong> RescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus status) </strong> 

 * <strong> RescueCaseRepository.findByRescueCenterCode(String code) </strong>

 * <strong> AnimalRepository.findByAnimalCode(String animalCode) </strong>

 * <strong> AnimalRepository.findByCommonNameContainingIgnoreCase(String name) </strong>

 * <strong> AnimalRepository.findByRescueCaseRescueCenterCode(String centerCode) </strong>

 * <strong> TreatmentRepository.findByAnimalIdOrderByPerformedAtAsc(Long animalId) </strong>

 * <strong> TreatmentRepository.findByNameIgnoreCase(String name) </strong>

 ## Consultas JPQL Implementadas

 * <strong> SpecialistRepository.findActiveByExpertise(@Param("expertiseName") String expertiseName)</strong>: Filtra especialistas activos con una experiencia especifica mediante un ``` JOIN ``` a la tabla asociativa N:M.

 * <strong> TreatmentRepository.findByTreatmentsBetweenDates(@Param("start") LocalDateTime start, @Param ("end") LocalDateTime end)</strong>: Consulta tratamientos ejecutados en un intervalo temporal utilizando parámetros nombrados.

 * <strong> TreatmentRepository.findByRescueCenterCode(@Param("centerCode") String centerCode)</strong>: Navega multiples asociaciones (```Treatment -> Animal -> RescueCase -> RescueCenter```).

 * <strong> TreatmentRepository.findBySpecialistExpertiseName(@Param("expertiseName") String expertiseName)</strong>: Filtra tratamientos según el área de experiencia del especialista.

 * <strong> AnimalRepository.indAnimalsInRehabilitationByExpertise(@Param("status") RescueStatus status, @Param("expertiseName") String expertiseName)</strong>: Consulta avanzada que cruza estatus de caso, tratamientos, especialistas y experticias utilizando ```DISTINCT```.