package com.company.assistant.chatbot;

/**
 * A-22 (#141): yanitin altinda gosterilen yonlendirme butonu.
 *
 * <p><b>target SEMANTIK bir hedeftir, web URL'i DEGIL</b> ("directory_employees",
 * "shuttle_routes"). Istemci onu kendi navigasyonuna cevirir. URL dondurmek Faz 2'de
 * mobil istemciyi kirardi — ayni yanit iki platformda da kullanilabilmeli.
 *
 * <p>Butonun anlami "chatbot'un sinirinin otesine kopru": listenin kesildigi, kapsamin
 * dar kaldigi ya da islemin baska bir ekranda yapildigi durumlar. Cevabin zaten tam
 * oldugu intent'lerde buton YOKTUR (or. prosedur yanitlari adimlariyla birlikte doner).
 */
public record ChatAction(String target, String label) {}
