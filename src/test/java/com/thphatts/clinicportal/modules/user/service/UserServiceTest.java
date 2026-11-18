package com.thphatts.clinicportal.modules.user.service;

import com.thphatts.clinicportal.modules.person.entity.Person;
import com.thphatts.clinicportal.modules.person.repository.PersonRepository;
import com.thphatts.clinicportal.modules.user.dto.UserRequest;
import com.thphatts.clinicportal.modules.user.entity.User;
import com.thphatts.clinicportal.modules.user.entity.enums.Role;
import com.thphatts.clinicportal.modules.user.mapper.UserMapper;
import com.thphatts.clinicportal.modules.user.repository.UserRepository;
import com.thphatts.clinicportal.modules.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class UserServiceTest {

    private static final String VALID_CITIZEN_ID = "079123456789";

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final PersonRepository personRepository = Mockito.mock(PersonRepository.class);
    private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    private final UserServiceImpl userService = new UserServiceImpl(
            userRepository, new UserMapper(), passwordEncoder, personRepository);

    @Test
    void createRejectsMissingBlankAndMalformedCitizenIdBeforeRepositoryCalls() {
        for (String citizenId : new String[]{null, "", "   ", "123", "07912345678A"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> userService.create(request(citizenId, null)));
        }

        verifyNoInteractions(userRepository, personRepository, passwordEncoder);
    }

    @Test
    void createReusesPersonAndFillsMissingCitizenIdFromRequest() {
        Person existingPerson = personWithoutCitizenId();
        prepareAccountCreation(existingPerson);

        userService.create(request(VALID_CITIZEN_ID, "0901234567"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertSame(existingPerson, savedUser.getValue().getPerson());
        assertEquals(VALID_CITIZEN_ID, existingPerson.getCitizenId());
        assertEquals(VALID_CITIZEN_ID, savedUser.getValue().getCitizenId());
    }

    @Test
    void createReusesPersonWhenCitizenIdAlreadyMatches() {
        Person existingPerson = personWithoutCitizenId();
        existingPerson.setCitizenId(VALID_CITIZEN_ID);
        prepareAccountCreation(existingPerson);

        userService.create(request(VALID_CITIZEN_ID, "0901234567"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertSame(existingPerson, savedUser.getValue().getPerson());
        assertEquals(VALID_CITIZEN_ID, savedUser.getValue().getCitizenId());
    }

    @Test
    void createRejectsCitizenIdMismatchForExistingPersonWithoutSavingUser() {
        Person existingPerson = personWithoutCitizenId();
        existingPerson.setCitizenId("079111111111");
        prepareAccountCreation(existingPerson);

        assertThrows(IllegalArgumentException.class,
                () -> userService.create(request(VALID_CITIZEN_ID, "0901234567")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createReusesPersonFoundByCitizenIdWhenPhoneIsOmitted() {
        Person existingPerson = personWithoutCitizenId();
        existingPerson.setCitizenId(VALID_CITIZEN_ID);
        prepareAccountCreationWithoutPhone(existingPerson);

        userService.create(request(VALID_CITIZEN_ID, null));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertSame(existingPerson, savedUser.getValue().getPerson());
        assertEquals(VALID_CITIZEN_ID, savedUser.getValue().getCitizenId());
        verify(personRepository, never()).findByPhoneNumber(any());
    }

    @Test
    void createRejectsWhenPhoneAndCitizenIdResolveToDifferentPeople() {
        Person byPhone = personWithoutCitizenId();
        byPhone.setCitizenId("079111111111");
        Person byCitizenId = personWithoutCitizenId();
        byCitizenId.setId(43L);
        byCitizenId.setCitizenId(VALID_CITIZEN_ID);
        prepareAccountCreation(byPhone);
        when(personRepository.findByCitizenId(VALID_CITIZEN_ID)).thenReturn(Optional.of(byCitizenId));

        assertThrows(IllegalArgumentException.class,
                () -> userService.create(request(VALID_CITIZEN_ID, "0901234567")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createRejectsCitizenIdPersonAlreadyLinkedToAnAccount() {
        Person existingPerson = personWithoutCitizenId();
        existingPerson.setCitizenId(VALID_CITIZEN_ID);
        when(userRepository.existsByUsername("new.user")).thenReturn(false);
        when(userRepository.existsByEmail("new.user@example.test")).thenReturn(false);
        when(personRepository.findByCitizenId(VALID_CITIZEN_ID)).thenReturn(Optional.of(existingPerson));
        when(userRepository.existsByPerson_Id(existingPerson.getId())).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userService.create(request(VALID_CITIZEN_ID, null)));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateRejectsMissingOrMalformedCitizenIdBeforeRepositoryCalls() {
        for (String citizenId : new String[]{null, "", "123", "07912345678A"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> userService.update("user-id", request(citizenId, null)));
        }

        verifyNoInteractions(userRepository, personRepository, passwordEncoder);
    }

    @Test
    void updateRejectsCitizenIdAlreadyAssignedToAnotherPerson() {
        Person accountPerson = personWithoutCitizenId();
        accountPerson.setId(41L);
        accountPerson.setCitizenId("079111111111");
        User existingUser = User.builder().id("user-id").person(accountPerson)
                .username("old.user").email("old.user@example.test").password("old-password")
                .role(Role.ROLE_DOCTOR).build();
        Person otherPerson = personWithoutCitizenId();
        otherPerson.setId(42L);
        otherPerson.setCitizenId(VALID_CITIZEN_ID);
        when(userRepository.findById("user-id")).thenReturn(Optional.of(existingUser));
        when(personRepository.findByCitizenId(VALID_CITIZEN_ID)).thenReturn(Optional.of(otherPerson));

        assertThrows(IllegalArgumentException.class,
                () -> userService.update("user-id", request(VALID_CITIZEN_ID, null)));

        verify(personRepository, never()).save(any(Person.class));
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    private void prepareAccountCreation(Person existingPerson) {
        when(userRepository.existsByUsername("new.user")).thenReturn(false);
        when(userRepository.existsByEmail("new.user@example.test")).thenReturn(false);
        when(userRepository.existsByPerson_Id(existingPerson.getId())).thenReturn(false);
        when(personRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(existingPerson));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
    }

    private void prepareAccountCreationWithoutPhone(Person existingPerson) {
        when(userRepository.existsByUsername("new.user")).thenReturn(false);
        when(userRepository.existsByEmail("new.user@example.test")).thenReturn(false);
        when(personRepository.findByCitizenId(VALID_CITIZEN_ID)).thenReturn(Optional.of(existingPerson));
        when(userRepository.existsByPerson_Id(existingPerson.getId())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
    }

    private Person personWithoutCitizenId() {
        Person person = new Person();
        person.setId(42L);
        person.setFullName("Existing Person");
        person.setPhoneNumber("0901234567");
        return person;
    }

    private UserRequest request(String citizenId, String phone) {
        return new UserRequest(
                "New User", null, phone, "new.user@example.test", "new.user",
                "password123", citizenId, Role.ROLE_DOCTOR);
    }
}
