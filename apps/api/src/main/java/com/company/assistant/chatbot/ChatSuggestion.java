package com.company.assistant.chatbot;

/**
 * A-22 (#141): tiklanabilir ornek soru ("vitrin sorusu").
 *
 * <p>Tiklandiginda {@code question} metni normal sohbet akisindan gonderilir — yani
 * kullanici yazmis gibi islenir ve chat_message_log'a duser. Bu bilincli: hangi
 * onerilerin tiklandigi kalibrasyon icin degerli veri.
 *
 * <p>{@code label} ile {@code question} su an ayni; ayri tutulmasinin sebebi ileride
 * kisa etiket ("Yemek menusu") ile gonderilen tam soru ("bugun yemekte ne var")
 * ayrisabilsin diye.
 */
public record ChatSuggestion(String label, String question) {}
