package com.company.assistant.hr;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyDocumentServiceTest {

    @Mock
    private PolicyDocumentRepository repository;

    @InjectMocks
    private PolicyDocumentService service;

    // FR-58: yeni versiyon eklenirken once eski current temizlenir, sonra current=true eklenir.
    @Test
    void yeniVersiyon_onceCurrentTemizlenirSonraCurrentEklenir() {
        when(repository.existsActive(5)).thenReturn(true);
        when(repository.nextVersionNo(5)).thenReturn(3);
        when(repository.insertVersion(anyInt(), anyInt(), any(), any(), any(), eq(true), any()))
                .thenReturn(42);
        when(repository.findVersionById(42)).thenReturn(Optional.of(ornekVersiyon()));

        var req = new VersionCreateRequest("guncel icerik", List.of(), LocalDate.of(2026, 2, 1));
        service.addVersion(5, req, null);

        InOrder ordered = inOrder(repository);
        ordered.verify(repository).clearCurrent(5);
        ordered.verify(repository).insertVersion(eq(5), eq(3), any(), any(), any(), eq(true), any());
    }

    @Test
    void gecersizProcedure_400VeDokumanEklenmez() {
        when(repository.procedureExists(99)).thenReturn(false);

        var req = new DocumentCreateRequest(99, "Baslik", null, null, LocalDate.of(2026, 1, 1));
        assertThatThrownBy(() -> service.createDocument(req, null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).insertDocument(anyInt(), any());
    }

    @Test
    void olmayanDokumanSilme_404() {
        when(repository.softDelete(7)).thenReturn(0);

        assertThatThrownBy(() -> service.delete(7))
                .isInstanceOf(PolicyDocumentNotFoundException.class);
    }

    private PolicyVersionResponse ornekVersiyon() {
        return new PolicyVersionResponse(42, 5, 3, "guncel icerik", List.of(),
                LocalDate.of(2026, 2, 1), true, Instant.now(), null);
    }
}
