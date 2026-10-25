package com.iam.platform.infrastructure.crypto;

import com.iam.platform.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoComponentTest {

    private final CryptoComponent crypto = TestFixtures.crypto();

    @Test
    void passwordHashVerifiesOnlyTheOriginalPassword() {
        String stored = crypto.hashPassword("Secret@1");

        assertThat(stored).doesNotContain("Secret@1");
        assertThat(crypto.verifyPassword("Secret@1", stored)).isTrue();
        assertThat(crypto.verifyPassword("wrong", stored)).isFalse();
        assertThat(crypto.verifyPassword(null, stored)).isFalse();
        assertThat(crypto.verifyPassword("Secret@1", "no-separator")).isFalse();
    }

    @Test
    void samePasswordHashesDifferentlyBecauseOfSalt() {
        assertThat(crypto.hashPassword("a")).isNotEqualTo(crypto.hashPassword("a"));
    }

    @Test
    void encryptDecryptRoundTrip() {
        String cipher = crypto.encrypt("13812345678");

        assertThat(cipher).isNotEqualTo("13812345678");
        assertThat(crypto.decrypt(cipher)).isEqualTo("13812345678");
    }

    @Test
    void hashIsDeterministic() {
        assertThat(crypto.hash("x")).isEqualTo(crypto.hash("x")).hasSize(64);
    }

    @Test
    void masksNameAndPhone() {
        assertThat(crypto.maskName("张三丰")).isEqualTo("张**");
        assertThat(crypto.maskName("张")).isEqualTo("张");
        assertThat(crypto.maskName(null)).isEmpty();
        assertThat(crypto.maskPhone("13812345678")).isEqualTo("138****5678");
        assertThat(crypto.maskPhone("123")).isEqualTo("123");
    }
}
