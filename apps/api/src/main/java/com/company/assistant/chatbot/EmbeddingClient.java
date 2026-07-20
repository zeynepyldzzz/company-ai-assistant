package com.company.assistant.chatbot;

public interface EmbeddingClient {

    /**
     * Verilen metni embedding vektörüne çevirir.
     * @return 1024 boyutlu vektör (bge-m3)
     */
    float[] embed(String text);
}