package com.thphatts.clinicportal.config;

import com.thphatts.clinicportal.modules.patient.repository.PatientRepository;
import com.thphatts.clinicportal.modules.person.entity.Person;
import com.thphatts.clinicportal.modules.person.repository.PersonRepository;
import com.thphatts.clinicportal.modules.user.entity.User;
import com.thphatts.clinicportal.modules.user.entity.enums.Role;
import com.thphatts.clinicportal.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {
    @Mock private UserRepository users;
    @Mock private PatientRepository patients;
    @Mock private PersonRepository people;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DataInitializer(users, patients, people, encoder);
        when(patients.count()).thenReturn(1L);
    }

    @Test
    void existingAdminDoesNotPreventCreatingOtherAccounts() throws Exception {
        User admin = User.builder().username("admin").password("existing-hash").build();
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(people.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initializer.run();

        ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
        verify(users, times(3)).save(created.capture());
        assertEquals(java.util.List.of("doctor", "receptionist", "patient"),
                created.getAllValues().stream().map(User::getUsername).toList());
        created.getAllValues().forEach(user -> assertTrue(encoder.matches("password123", user.getPassword())));
        assertEquals("existing-hash", admin.getPassword());
    }

    @Test
    void existingAccountsKeepPasswordsByDefault() throws Exception {
        User existing = User.builder().password("existing-hash").build();
        when(users.findByUsername(any())).thenReturn(Optional.of(existing));

        initializer.run();

        verify(users, never()).save(any());
        verifyNoInteractions(people);
        assertEquals("existing-hash", existing.getPassword());
    }

    @Test
    void explicitResetOnlyChangesMatchingDemoAccount() throws Exception {
        User admin = User.builder().username("admin").email("admin@clinic.com")
                .role(Role.ROLE_ADMIN).password("old-hash").build();
        User other = User.builder().email("real@example.com").password("keep-me").build();
        when(users.findByUsername(any())).thenReturn(Optional.of(other));
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        ReflectionTestUtils.setField(initializer, "resetDemoPasswords", true);

        initializer.run();

        verify(users).save(admin);
        verify(users, times(1)).save(any());
        assertTrue(encoder.matches("password123", admin.getPassword()));
        assertEquals("keep-me", other.getPassword());
        verifyNoInteractions(people);
    }

    @Test
    void conflictingEmailsDoNotCreateOrOverwriteUsers() throws Exception {
        when(users.existsByEmail(any())).thenReturn(true);

        initializer.run();

        verify(users, never()).save(any());
        verifyNoInteractions(people);
    }
}
