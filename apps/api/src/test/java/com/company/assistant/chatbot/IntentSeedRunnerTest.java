package com.company.assistant.chatbot;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IntentSeedRunnerTest {

    @Test
    void vektorPgvectorFormatinaCevrilir() {
        assertThat(IntentSeedRunner.toVectorLiteral(new float[]{1.0f, -0.5f, 0.25f}))
                .isEqualTo("[1.0,-0.5,0.25]");
    }

    @Test
    void tekElemanliVektor() {
        assertThat(IntentSeedRunner.toVectorLiteral(new float[]{0.5f})).isEqualTo("[0.5]");
    }
}