package com.thphatts.clinicportal.modules.appointment.repository;

import com.thphatts.clinicportal.modules.appointment.entity.Appointment;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentRepositoryFilterTest {
    @Mock private AppointmentRepository repository;
    @Mock private Root<Appointment> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder builder;
    @Mock private Path<LocalDate> datePath;
    private final PageRequest pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        doCallRealMethod().when(repository).filterAppointments(any(), any(), any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    private void evaluateFilter(LocalDate fromDate, LocalDate toDate) {
        repository.filterAppointments(null, null, null, fromDate, toDate, pageable);
        ArgumentCaptor<Specification<Appointment>> specification = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(specification.capture(), eq(pageable));
        specification.getValue().toPredicate(root, query, builder);
    }

    @Test
    void omittedFiltersDoNotProduceNullParameterPredicates() {
        evaluateFilter(null, null);

        verifyNoInteractions(root);
        verify(builder).and(new jakarta.persistence.criteria.Predicate[0]);
        verifyNoMoreInteractions(builder);
    }

    @Test
    void fromDateOnlyUsesTypedInclusiveComparison() {
        LocalDate start = LocalDate.of(2026, 11, 15);
        when(root.<LocalDate>get("appointmentDate")).thenReturn(datePath);

        evaluateFilter(start, null);

        verify(builder).greaterThanOrEqualTo(datePath, start);
        verify(builder, never()).lessThanOrEqualTo(any(jakarta.persistence.criteria.Expression.class), any(LocalDate.class));
        verify(builder, never()).isNull(any());
    }

    @Test
    void toDateOnlyUsesTypedInclusiveComparison() {
        LocalDate end = LocalDate.of(2026, 11, 22);
        when(root.<LocalDate>get("appointmentDate")).thenReturn(datePath);

        evaluateFilter(null, end);

        verify(builder).lessThanOrEqualTo(datePath, end);
        verify(builder, never()).greaterThanOrEqualTo(any(jakarta.persistence.criteria.Expression.class), any(LocalDate.class));
        verify(builder, never()).isNull(any());
    }

    @Test
    void dateRangeKeepsBothInclusiveBounds() {
        LocalDate start = LocalDate.of(2026, 11, 15);
        LocalDate end = start.plusDays(7);
        when(root.<LocalDate>get("appointmentDate")).thenReturn(datePath);

        evaluateFilter(start, end);

        verify(builder).greaterThanOrEqualTo(datePath, start);
        verify(builder).lessThanOrEqualTo(datePath, end);
        verify(builder, never()).isNull(any());
    }
}
