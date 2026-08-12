package com.company.assistant.vehicle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.company.assistant.common.TurkishText;

/**
 * A-41 (#213): {@code arac_rezervasyon} intent'i icin canli arac bilgisi.
 *
 * <p><b>Var olma sebebi.</b> Sablon tamamen statikti — kullanici somut veri soruyor,
 * "ilgili form uzerinden olusturabilirsin" yonlendirmesi aliyordu. {@code vehicle/} altinda
 * resolver yoktu; diger dokuz modulun hepsinde vardi. Olculdu: "hangi araçlar müsait" 0.627
 * ile esigin altinda kaliyordu, ama esigi gecseydi bile ayni yonlendirmeyi alacakti. Yani bu
 * bir kalibrasyon sorunu degil, YETENEK boslugu.
 *
 * <p><b>Bakimdaki araclar musait sayilmaz.</b> {@code VehicleService.listVehicles(true)} zaten
 * {@link MaintenanceStatus#AVAILABLE} filtresi uyguluyor (FR-38/41); burada tekrar filtre
 * yazilmadi. Musait olmayan bir araci listede gostermek, bu projede en pahali sayilan hata
 * turudur: dogru formatta yanlis bilgi.
 *
 * <p><b>Rezervasyon sorusu BIRINCI SAHISTIR.</b> Kimlik mesajdan degil {@code Authentication}'dan
 * aliniyor (FR-63, ScheduleVariableResolver ile ayni kural). Baskasinin rezervasyonu bu
 * resolver'dan asla donmez.
 *
 * <p>Chatbot yalnizca OKUMA tarafinda: rezervasyon olusturma/iptal yazma islemidir ve ayri bir
 * dogrulama konusu (issue'da kapsam disi).
 */
@Component
public class VehicleVariableResolver {

    private static final Logger log = LoggerFactory.getLogger(VehicleVariableResolver.class);

    private static final String VEHICLE_INTENT = "arac_rezervasyon";
    private static final String VARIABLE = "arac_bilgisi";

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", new Locale("tr"));

    /**
     * Rezervasyon dali IYELIK ekli bicimleri arar, ciplak "rezervasyon"u DEGIL.
     *
     * <p>Elle test (2026-08-12): ilk yazimda kok olarak "rezervasyon" araniyordu ve
     * "rezervasyon yapabileceğim boştaki araçlar" sorusu kullanicinin KENDI kayitlarini
     * donduruyordu. Cumlede "rezervasyon" geciyor ama iyelik eki "yapabileceğim"de —
     * soru kaydi hakkinda degil, yapabilecekleri hakkinda.
     *
     * <p>Alt-dize eslesmesi cekimleri kapsiyor: "rezervasyonum" -> "rezervasyonumu",
     * "rezervasyonlarim" -> "rezervasyonlarimi".
     */
    private static final List<String> RESERVATION_CUES =
            List.of("rezervasyonum", "rezervasyonlarim");
    /**
     * "yapabilecegim" burada: "rezervasyon yapabileceğim araçlar" bir MUSAITLIK sorusudur —
     * kullanici neyi rezerve edebilecegini soruyor.
     */
    private static final List<String> AVAILABILITY_CUES =
            List.of("musait", "bos", "uygun", "yapabilecegim");
    private static final List<String> COUNT_CUES = List.of("kac", "sayisi", "toplam");

    /**
     * Liste ust siniri. Filo buyudukce yanit okunmaz hale gelmesin; kesilirse kalan sayi
     * ACIKCA yaziliyor (OfficeStatusVariableResolver ile ayni gerekce).
     */
    private static final int MAX_ROWS = 10;

    private static final String NO_IDENTITY =
            "Rezervasyonlarını görebilmem için giriş yapmış olman gerekiyor.";
    private static final String NO_AVAILABLE =
            "Şu anda müsait araç görünmüyor. Bakımda olmayan bir araç açıldığında "
                    + "Araçlar ekranından görebilirsin.";
    /**
     * Alan ipucu yoksa eski YONLENDIRME metni korunuyor: "araç" diye soran ama ne istedigini
     * belirtmeyen kullaniciya rastgele bir liste basmak yerine nereye gidecegini soylemek
     * dogru. Statik sablonun tek mesru kaldigi yer burasi.
     */
    private static final String GUIDANCE =
            "Araçlar ekranından müsait araçları görebilir, rezervasyon oluşturabilirsin. "
                    + "Bana \"hangi araçlar müsait\" ya da \"rezervasyonum var mı\" diye de sorabilirsin.";

    private final VehicleService vehicleService;
    private final ReservationService reservationService;

    public VehicleVariableResolver(VehicleService vehicleService,
                                   ReservationService reservationService) {
        this.vehicleService = vehicleService;
        this.reservationService = reservationService;
    }

    public Map<String, String> resolve(String intentName, String message,
                                       Authentication authentication) {
        if (!VEHICLE_INTENT.equals(intentName)) {
            return Map.of();
        }

        String text = TurkishText.foldToAscii(message);

        // Rezervasyon dali ONCE: "rezervasyonum var mı" ifadesinde "var mi" bir musaitlik
        // ipucu gibi gorunebilir, oysa soru kullanicinin KENDI kaydi hakkinda. Siralamanin
        // guvenli olmasi ipucunun IYELIK ekli olmasina bagli — bkz. RESERVATION_CUES.
        if (containsAny(text, RESERVATION_CUES)) {
            return Map.of(VARIABLE, myReservations(authentication));
        }
        if (containsAny(text, COUNT_CUES)) {
            return Map.of(VARIABLE, count(containsAny(text, AVAILABILITY_CUES)));
        }
        if (containsAny(text, AVAILABILITY_CUES)) {
            return Map.of(VARIABLE, availableVehicles());
        }
        return Map.of(VARIABLE, GUIDANCE);
    }

    private String availableVehicles() {
        List<VehicleResponse> vehicles = vehicleService.listVehicles(true);
        if (vehicles.isEmpty()) {
            return NO_AVAILABLE;
        }
        return "Müsait araçlar (" + vehicles.size() + "):\n" + vehicleRows(vehicles);
    }

    private String vehicleRows(List<VehicleResponse> vehicles) {
        return rows(vehicles.stream()
                .map(v -> "• " + v.getPlate() + " — " + v.getModel())
                .toList());
    }

    private String count(boolean onlyAvailable) {
        int total = vehicleService.listVehicles(onlyAvailable).size();
        return onlyAvailable
                ? "Şu anda " + total + " araç müsait."
                : "Filoda toplam " + total + " araç var.";
    }

    /**
     * Yalnizca GECERLI ve GELECEK rezervasyonlar. Iki filtre de gerekli:
     * {@code listMyReservations} iptal edilmisleri de donuyor ve gecmis bir kayit
     * "rezervasyonun var" cevabini yanlis kilar.
     */
    private String myReservations(Authentication authentication) {
        Integer employeeId = employeeId(authentication);
        if (employeeId == null) {
            return NO_IDENTITY;
        }

        LocalDateTime now = LocalDateTime.now();
        List<ReservationResponse> upcoming = reservationService.listMyReservations(employeeId).stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> r.getEndTime() != null && r.getEndTime().isAfter(now))
                .toList();

        if (upcoming.isEmpty()) {
            // "Yok" tek basina cikmaz sokak: kullanici rezervasyonunu soruyorsa asil niyeti
            // buyuk ihtimalle rezervasyon YAPMAK. Musait araclari da basmak cevabi
            // eyleme donusturuyor; intent'in butonu zaten Araclar ekranina goturuyor.
            List<VehicleResponse> available = vehicleService.listVehicles(true);
            if (available.isEmpty()) {
                return "Yaklaşan bir araç rezervasyonun görünmüyor. Şu anda müsait araç da yok.";
            }
            return "Yaklaşan bir araç rezervasyonun görünmüyor.\n\n"
                    + "Rezervasyon yapabileceğin müsait araçlar:\n" + vehicleRows(available);
        }
        return "Yaklaşan rezervasyonların:\n" + rows(upcoming.stream()
                .map(r -> "• " + r.getVehiclePlate() + " — "
                        + r.getStartTime().format(STAMP) + " / " + r.getEndTime().format(STAMP))
                .toList());
    }

    /** Ust siniri asan liste kesilir ve kalan sayi acikca yazilir. */
    private String rows(List<String> lines) {
        String body = lines.stream().limit(MAX_ROWS).collect(Collectors.joining("\n"));
        return lines.size() > MAX_ROWS
                ? body + "\n• ve " + (lines.size() - MAX_ROWS) + " tane daha"
                : body;
    }

    private Integer employeeId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        try {
            return Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            log.warn("Authentication name sayisal degil: {}", authentication.getName());
            return null;
        }
    }

    private boolean containsAny(String text, List<String> cues) {
        return cues.stream().anyMatch(text::contains);
    }
}
