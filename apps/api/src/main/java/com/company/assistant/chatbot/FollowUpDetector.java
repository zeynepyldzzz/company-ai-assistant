package com.company.assistant.chatbot;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * A-37 (#203): "peki yarin?" tipi TAKIP sorularini tanir.
 *
 * <p>Kullanicilar sohbet gibi davraniyor ama sistem her mesaji bagimsiz degerlendiriyordu:
 * menu sorulduktan sonra gelen "yarin" mesaji tek basina hicbir kategoriye guvenilir sekilde
 * eslesmiyor ve "anlamadim" doniyordu.
 *
 * <p><b>Kural bilerek cok dar.</b> Yalnizca mesajin TAMAMI zaman ifadesinden (ve "peki", "ya"
 * gibi baglaclardan) ibaretse devreye girer. "Son intent'i hatirla, kisa mesaj gelirse onu
 * kullan" gibi bir kestirme akla yatkin gorunur ama sessizce yanlis cevap uretir:
 *
 * <pre>
 *   — bugun yemekte ne var?   -> (menu)
 *   — kadikoy                 -> kullanici SERVIS soruyor
 * </pre>
 *
 * "kadikoy" bir zaman ifadesi olmadigi icin baglam hic devreye girmez ve mesaj normal
 * siniflandirmaya gider. Bu projede yanlis cevabin bedeli olculmus durumda (A-26).
 */
public final class FollowUpDetector {

    /**
     * Baglamin uygulanabilecegi intent'ler: cevaplari bir ZAMAN parametresine bagli olanlar.
     * "yarin" mesaji rehber_kisi ya da servis_guzergah baglaminda anlamsizdir.
     */
    private static final Set<String> TIME_AWARE_INTENTS = Set.of("yemek_menusu", "calisma_duzeni");

    /**
     * Zaman belirten parcalar. Ay adlari ve sayisal tarihler de dahil, cunku "17 agustos"
     * da gecerli bir takip sorusudur.
     *
     * <p><b>Sonda kelime siniri YOK, en fazla 3 harflik ek kabul ediliyor.</b> Turkce sondan
     * eklemeli: "yarini da soyle", "cumaya bak", "pazartesiyi goster". Kelime siniriyla
     * kapatilsaydi bu cumlelerin hicbiri takip sayilmazdi. Ayni yaklasim
     * {@code TurkishText.NEGATION} icin de kullanilmis.
     *
     * <p>Gun adlarinda bilesik olanlar ONCE geliyor: "cuma" once yazilsaydi "cumartesi"
     * icin eslesir, geriye "rtesi" kalir ve mesaj takip sayilmazdi.
     *
     * <p>"bu" BILEREK yok: uc harflik ek toleransiyla "bunu", "buna" gibi kelimeleri
     * yakalardi. "bu hafta" ifadesi zaten "hafta" uzerinden eslesiyor.
     */
    private static final Pattern TIME_TOKENS = Pattern.compile(
            "\\b(bugun|yarin|dun|obur|onceki|evvelki|gun|haftaya|hafta|gelecek|gecen|"
                    + "pazartesi|cumartesi|carsamba|persembe|pazar|sali|cuma|"
                    + "ocak|subat|mart|nisan|mayis|haziran|temmuz|agustos|eylul|ekim|kasim|aralik|"
                    + "\\d{1,2}([./-]\\d{1,2}([./-]\\d{2,4})?)?)[a-z]{0,3}");

    /**
     * Takip sorusuna eslik eden baglac ve fiiller: "yarini da SOYLE", "cumaya BAKAR MISIN".
     *
     * <p>Liste yalnizca ICERIKSIZ kelimeleri barindirir. "ne" ve "var" BILEREK yok: eklenirse
     * "bugun ne var" mesaji da takip sayilir ve onceki intent calisma_duzeni ise menu sorusu
     * calisma duzeni olarak yorumlanir. Anlam tasiyan hicbir kelime buraya eklenmemeli —
     * kuralin darligi bu listeye bagli.
     */
    private static final Pattern FILLER = Pattern.compile(
            "\\b(peki|ya|de|da|ki|icin|lutfen|acaba|bana|bir)\\b"
                    + "|\\b(soyle|sole|goster|ver|bak|bakar|bakabilir|misin|musun|misiniz|"
                    + "musunuz|mi|mu)[a-z]{0,3}");

    private FollowUpDetector() {
    }

    /** Onceki intent baglam tasiyabilir mi. */
    public static boolean isTimeAware(String intentName) {
        return intentName != null && TIME_AWARE_INTENTS.contains(intentName);
    }

    /**
     * Mesaj SADECE zaman ifadesinden mi ibaret.
     *
     * <p>"yarin" ve "peki yarin" -> true. "yarin ne var" -> false; o mesaj zaten kendi
     * basina dogru kategoriye gidiyor, baglama ihtiyaci yok.
     *
     * @param foldedText {@link com.company.assistant.common.TurkishText#foldToAscii} cikisi
     */
    public static boolean isTimeOnly(String foldedText) {
        if (foldedText == null || foldedText.isBlank()) {
            return false;
        }
        if (!TIME_TOKENS.matcher(foldedText).find()) {
            return false;
        }
        String rest = TIME_TOKENS.matcher(foldedText).replaceAll(" ");
        rest = FILLER.matcher(rest).replaceAll(" ");
        // Geriye harf/rakam kalmadiysa mesajin tamami zaman ifadesiydi.
        return rest.replaceAll("[^a-z0-9]", "").isEmpty();
    }
}
