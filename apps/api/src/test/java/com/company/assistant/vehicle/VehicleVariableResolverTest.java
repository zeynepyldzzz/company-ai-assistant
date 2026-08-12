package com.company.assistant.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

/**
 * A-41 (#213): arac musaitligi ve kullanicinin kendi rezervasyonlari. Odak, issue'nun is
 * kurallari — bakimdaki arac musait sayilmamali ve rezervasyon sorusu birinci sahis olmali.
 */
@ExtendWith(MockitoExtension.class)
class VehicleVariableResolverTest {

    private static final String VARIABLE = "arac_bilgisi";
    private static final int EMPLOYEE_ID = 7;

    @Mock
    private VehicleService vehicleService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private Authentication authentication;

    private VehicleVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new VehicleVariableResolver(vehicleService, reservationService);
        lenient().when(authentication.getName()).thenReturn(String.valueOf(EMPLOYEE_ID));
    }

    @Test
    void aracDisiIntentBosDoner() {
        assertThat(resolver.resolve("yemek_menusu", "hangi araçlar müsait", authentication))
                .isEmpty();

        verifyNoInteractions(vehicleService, reservationService);
    }

    /**
     * KRITIK: musaitlik sorgusu {@code listVehicles(true)} cagirir; o metot
     * {@link MaintenanceStatus#AVAILABLE} filtresi uyguluyor (FR-38/41). {@code false} ya da
     * {@code null} gecilseydi BAKIMDAKI arac musait diye listelenirdi — dogru formatta yanlis
     * bilgi, bu projede en pahali hata turu.
     */
    @Test
    void musaitlikSorgusuBakimdakileriDisarida_birakir() {
        // Mock'lar DISARIDA kuruluyor: when(...)/thenReturn(...) argumani icinde mock
        // stub'lamak Mockito'da UnfinishedStubbing verir (ayni uyari
        // OfficeStatusVariableResolverTest'te de var).
        VehicleResponse ford = vehicle("34 ABC 123", "Ford Transit");
        when(vehicleService.listVehicles(true)).thenReturn(List.of(ford));

        String reply = resolver.resolve("arac_rezervasyon", "hangi araçlar müsait", authentication)
                .get(VARIABLE);

        assertThat(reply).contains("34 ABC 123").contains("Ford Transit");
        verify(vehicleService).listVehicles(true);
    }

    @Test
    void musaitAracYoksaAcikMesajDoner() {
        when(vehicleService.listVehicles(true)).thenReturn(List.of());

        assertThat(resolver.resolve("arac_rezervasyon", "müsait araç var mı", authentication)
                .get(VARIABLE)).contains("müsait araç görünmüyor");
    }

    @Test
    void aracSayisiSorulursaFiloToplamiDoner() {
        VehicleResponse ford = vehicle("34 ABC 123", "Ford");
        VehicleResponse fiat = vehicle("35 XYZ 9", "Fiat");
        when(vehicleService.listVehicles(false)).thenReturn(List.of(ford, fiat));

        assertThat(resolver.resolve("arac_rezervasyon", "kaç araç var", authentication)
                .get(VARIABLE)).isEqualTo("Filoda toplam 2 araç var.");
    }

    // Sayim + musaitlik birlikte gecerse musait olanlarin sayisi doner.
    @Test
    void musaitAracSayisiSorulabilir() {
        VehicleResponse ford = vehicle("34 ABC 123", "Ford");
        when(vehicleService.listVehicles(true)).thenReturn(List.of(ford));

        assertThat(resolver.resolve("arac_rezervasyon", "kaç araç müsait", authentication)
                .get(VARIABLE)).isEqualTo("Şu anda 1 araç müsait.");
    }

    /**
     * Rezervasyon dali iki filtre uygular: IPTAL edilenler ve GECMIS kayitlar disarida kalir.
     * {@code listMyReservations} ikisini de donuyor; filtrelenmezse "rezervasyonun var"
     * cevabi yanlis olur.
     */
    @Test
    void rezervasyonSorgusuIptalVeGecmisKayitlariEler() {
        LocalDateTime now = LocalDateTime.now();
        ReservationResponse yaklasan = reservation("34 ABC 123",
                now.plusDays(1), now.plusDays(1).plusHours(3), ReservationStatus.CONFIRMED);
        ReservationResponse iptal = reservation("35 XYZ 9",
                now.plusDays(2), now.plusDays(2).plusHours(2), ReservationStatus.CANCELLED);
        ReservationResponse gecmis = reservation("06 QQQ 1",
                now.minusDays(5), now.minusDays(5).plusHours(1), ReservationStatus.CONFIRMED);
        when(reservationService.listMyReservations(EMPLOYEE_ID))
                .thenReturn(List.of(yaklasan, iptal, gecmis));

        String reply = resolver.resolve("arac_rezervasyon", "rezervasyonum var mı", authentication)
                .get(VARIABLE);

        assertThat(reply)
                .contains("34 ABC 123")
                .doesNotContain("35 XYZ 9")
                .doesNotContain("06 QQQ 1");
    }

    @Test
    void yaklasanRezervasyonYoksaAcikMesajDoner() {
        when(reservationService.listMyReservations(EMPLOYEE_ID)).thenReturn(List.of());

        assertThat(resolver.resolve("arac_rezervasyon", "rezervasyonum var mı", authentication)
                .get(VARIABLE)).contains("Yaklaşan bir araç rezervasyonun görünmüyor");
    }

    // Kimlik yoksa BASKASININ rezervasyonu donmemeli; sorgu hic atilmaz.
    @Test
    void kimlikYoksaRezervasyonSorgusuAtilmaz() {
        assertThat(resolver.resolve("arac_rezervasyon", "rezervasyonum var mı", null)
                .get(VARIABLE)).contains("giriş yapmış olman gerekiyor");

        verifyNoInteractions(reservationService);
    }

    /**
     * NOBETCI: "rezervasyonum var mı" ifadesindeki "var mı" bir musaitlik ipucu gibi
     * gorunebilir. Rezervasyon dali ONCE calisiyor; arac listesi sorgusu atilmamali.
     */
    @Test
    void rezervasyonSorusuMusaitlikDalinaKaymaz() {
        when(reservationService.listMyReservations(EMPLOYEE_ID)).thenReturn(List.of());

        resolver.resolve("arac_rezervasyon", "rezervasyonum var mı", authentication);

        verifyNoInteractions(vehicleService);
    }

    // Alan ipucu yoksa eski YONLENDIRME metni korunur — rastgele liste basilmaz.
    @Test
    void ipucuYoksaYonlendirmeDoner() {
        // Mesajda BILEREK hicbir ipucu yok: "rezervasyon" yazsaydim rezervasyon dalina
        // giderdi ve test yonlendirmeyi degil onu olcerdi.
        String reply = resolver.resolve("arac_rezervasyon", "araç konusunda yardım", authentication)
                .get(VARIABLE);

        assertThat(reply).contains("Araçlar ekranından");
        verifyNoInteractions(vehicleService);
    }

    private VehicleResponse vehicle(String plate, String model) {
        VehicleResponse response = mock(VehicleResponse.class);
        lenient().when(response.getPlate()).thenReturn(plate);
        lenient().when(response.getModel()).thenReturn(model);
        return response;
    }

    private ReservationResponse reservation(String plate, LocalDateTime start, LocalDateTime end,
                                            ReservationStatus status) {
        ReservationResponse response = mock(ReservationResponse.class);
        lenient().when(response.getVehiclePlate()).thenReturn(plate);
        lenient().when(response.getStartTime()).thenReturn(start);
        lenient().when(response.getEndTime()).thenReturn(end);
        lenient().when(response.getStatus()).thenReturn(status);
        return response;
    }
}
