package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.audit.AuditLogger;
import lv.bootcamp.shelter.audit.RejectionReason;
import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.model.*;
import lv.bootcamp.shelter.repository.AdopterRepository;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Write tests for AdoptionEligibilityService.
 * The class and mocks are set up — the rest is yours.
 */
@ExtendWith(MockitoExtension.class)
class AdoptionEligibilityServiceTest {

    private static final Long ADOPTER_ID = 1L;
    private static final Long ANIMAL_ID = 2L;

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AdoptionEligibilityService service;

    // Write your tests here
    // ---- Baseline - should pass every eligibility check ----
    private Adopter validAdopter() {
        Adopter adopter = new Adopter();
        adopter.setId(ADOPTER_ID);
        adopter.setName("Faustas Storm");
        adopter.setEmail("faustas@example.com");
        adopter.setAge(30);
        adopter.setCurrentPetCount(0);
        adopter.setPreviousAdoptions(0);
        adopter.setLargeProperty(false);
        adopter.setExoticPermit(false);
        return adopter;
    }

    private Animal validAnimal() {
        Animal animal = new Animal();
        animal.setId(ANIMAL_ID);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setBreed("Labrador");
        animal.setAge(3);
        animal.setDescription("Friendly dog");
        animal.setStatus(AnimalStatus.AVAILABLE);
        return animal;
    }

    // ---- helper methods to stub repository responses ----
    private void stubAdopter(Adopter adopter) {
        when(adopterRepository.findById(ADOPTER_ID)).thenReturn(Optional.ofNullable(adopter));
    }

    private void stubAnimal(Animal animal) {
        when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.ofNullable(animal));
    }

    // ---- availability checks ----
    @Test
    void rejectsWhenAdopterNotFound() {
        when(adopterRepository.findById(ADOPTER_ID)).thenReturn(Optional.empty());

        AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertFalse(result.approved());
        assertEquals(RejectionReasons.ADOPTER_NOT_FOUND, result.reason());
        verifyNoInteractions(animalRepository, notificationClient, auditLogger);
    }

    @Test
    void rejectsWhenAnimalNotFound() {
        stubAdopter(validAdopter());
        when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.empty());

        AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertFalse(result.approved());
        assertEquals(RejectionReasons.ANIMAL_NOT_FOUND, result.reason());
        verifyNoInteractions(notificationClient, auditLogger);
    }

    @Test
    void rejectsWhenAnimalIsNotAvailable() {
        stubAdopter(validAdopter());
        Animal animal = validAnimal();
        animal.setStatus(AnimalStatus.ADOPTED);
        stubAnimal(animal);

        AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertFalse(result.approved());
        assertEquals(RejectionReasons.ANIMAL_NOT_AVAILABLE, result.reason());
        verifyNoInteractions(notificationClient, auditLogger);
    }


    // ---- age check ----
    @Test
    void rejectsUnderageAdopter() {
        Adopter adopter = validAdopter();
        adopter.setAge(17);
        stubAdopter(adopter);
        stubAnimal(validAnimal());

        AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertFalse(result.approved());
        assertEquals(RejectionReasons.UNDERAGE, result.reason());
        verify(auditLogger).logRejection(ADOPTER_ID, ANIMAL_ID, RejectionReason.UNDERAGE);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void allowsAdopterExactlyAtMinimumAge() {
        Adopter adopter = validAdopter();
        adopter.setAge(18);
        stubAdopter(adopter);
        stubAnimal(validAnimal());

        AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertTrue(result.approved());
        verify(auditLogger, never()).logRejection(anyLong(), anyLong(), eq(RejectionReason.UNDERAGE));
    }


    // --- limit check ----
    @Test
    void rejectsWhenPetLimitReachedForRegularProperty() {
        Adopter adopter = validAdopter();
        adopter.setCurrentPetCount(3); // limit for regular property is 3
        stubAdopter(adopter);
        stubAnimal(validAnimal());

        AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertFalse(result.approved());
        assertEquals(RejectionReasons.PET_LIMIT_REACHED, result.reason());
        verify(auditLogger).logRejection(ADOPTER_ID, ANIMAL_ID, RejectionReason.PET_LIMIT_REACHED);
    }

    @Test
    void largePropertyOwnersGetAHigherPetLimit() {
        Adopter adopter = validAdopter();
        adopter.setLargeProperty(true);
        adopter.setCurrentPetCount(4); // would fail regular limit (3), allowed for large property (5)
        stubAdopter(adopter);
        stubAnimal(validAnimal());

        AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertTrue(result.approved());
    }

    @Test
    void rejectsExoticAnimalAdoptionWithoutPermit() {
        Adopter adopter = validAdopter();
        adopter.setExoticPermit(false);
        stubAdopter(adopter);
        Animal animal = validAnimal();
        animal.setType(AnimalType.BIRD);
        stubAnimal(animal);

        AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertFalse(result.approved());
        assertEquals(RejectionReasons.EXOTIC_PERMIT_REQUIRED, result.reason());
        verify(auditLogger).logRejection(ADOPTER_ID, ANIMAL_ID, RejectionReason.NO_EXOTIC_PERMIT);
    }

    @Test
    void allowsExoticAnimalAdoptionWithPermit() {
        Adopter adopter = validAdopter();
        adopter.setExoticPermit(true);
        stubAdopter(adopter);
        Animal animal = validAnimal();
        animal.setType(AnimalType.RABBIT);
        stubAnimal(animal);

        AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertTrue(result.approved());
    }

    @Test
    void approvesEligibleAdopterAndTriggersNotificationAndAudit() {
        stubAdopter(validAdopter());
        stubAnimal(validAnimal());

        AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertTrue(result.approved());
        verify(notificationClient).sendApprovalNotification("faustas@example.com", "Rex");
        verify(auditLogger).logApproval(eq(ADOPTER_ID), eq(ANIMAL_ID), anyInt());
    }

    // ---- score test ----
    @Test
    void calculatesPriorityScoreFromAdopterAndAnimalAttributes() {
        Adopter adopter = validAdopter();
        adopter.setLargeProperty(true);  // +15
        adopter.setPreviousAdoptions(4); // +10 +5
        adopter.setCurrentPetCount(1);   // -2
        Animal animal = validAnimal();
        animal.setAge(10); // senior bonus +20

        int score = service.calculatePriorityScore(adopter, animal);

        assertEquals(48, score); // 10 + 5 + 15 + 20 - 2
    }

    @Test
    void scoreHandlesNullAnimalAgeWithoutError() {
        Adopter adopter = validAdopter();
        Animal animal = validAnimal();
        animal.setAge(null);

        int score = service.calculatePriorityScore(adopter, animal);

        assertEquals(0, score);
    }
}
