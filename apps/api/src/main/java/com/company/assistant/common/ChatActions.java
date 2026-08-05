package com.company.assistant.common;

/**
 * A-30 (#185): resolver'in, urettigi yanit icin intent'in VARSAYILAN butonunu ezmesini saglayan
 * anahtarlar.
 *
 * <p>A-22'de her buton intent basina sabit tanimlanmisti ve zaafi orada da not edilmisti:
 * bir intent birden fazla soruyu karsiliyorsa tek buton hepsine uymuyor. {@code calisma_duzeni}
 * bunun canli ornegi — hem "kimler ofiste" (buton: calisan listesi) hem "bu hafta plamin ne"
 * (buton: kendi cizelgem) ayni intent'e dusuyor.
 *
 * <p>Anahtar degisken haritasina yazilir; {@code ChatMessageService} template render'indan
 * ONCE haritadan siler, dolayisiyla kullaniciya sizmaz.
 */
public final class ChatActions {

    /** Degisken haritasinda buton hedefini tasiyan anahtar. */
    public static final String OVERRIDE_KEY = "__action_target";

    /** "Bu yanitta hicbir buton gosterme" — intent'in varsayilan butonunu bastirir. */
    public static final String NONE = "none";

    /** Kullanicinin kendi haftalik calisma duzeni ekrani. */
    public static final String MY_SCHEDULE = "my_schedule";

    private ChatActions() {
    }
}
