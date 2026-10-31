package com.iam.platform.infrastructure.cache;

import com.iam.platform.application.idempotency.IdempotencyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisIdempotencyStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisIdempotencyStore store;

    @BeforeEach
    void setUp() {
        store = new RedisIdempotencyStore(redisTemplate);
    }

    @Test
    void acquiringNewKeyClearsStaleResponse() {
        stubValueOperations();
        when(valueOperations.setIfAbsent("iam:idem:fp:key", "fingerprint", Duration.ofSeconds(60)))
                .thenReturn(true);

        assertEquals(IdempotencyStore.Status.ACQUIRED, store.acquire("key", "fingerprint", 60));
        verify(redisTemplate).delete("iam:idem:resp:key");
    }

    @Test
    void existingFingerprintWithResponseIsCompleted() {
        stubValueOperations();
        when(valueOperations.setIfAbsent("iam:idem:fp:key", "fingerprint", Duration.ofSeconds(60)))
                .thenReturn(false);
        when(valueOperations.get("iam:idem:fp:key")).thenReturn("fingerprint");
        when(redisTemplate.hasKey("iam:idem:resp:key")).thenReturn(true);

        assertEquals(IdempotencyStore.Status.COMPLETED, store.acquire("key", "fingerprint", 60));
    }

    @Test
    void differentFingerprintConflicts() {
        stubValueOperations();
        when(valueOperations.setIfAbsent("iam:idem:fp:key", "new", Duration.ofSeconds(60)))
                .thenReturn(false);
        when(valueOperations.get("iam:idem:fp:key")).thenReturn("old");

        assertEquals(IdempotencyStore.Status.CONFLICT, store.acquire("key", "new", 60));
    }

    @Test
    void completeStoresResponseAndAlignsFingerprintTtl() {
        stubValueOperations();
        store.complete("key", 201, "body", 60);

        verify(valueOperations).set("iam:idem:resp:key", "201\nbody", Duration.ofSeconds(60));
        verify(redisTemplate).expire("iam:idem:fp:key", Duration.ofSeconds(60));
    }

    @Test
    void releaseRemovesBothKeys() {
        store.release("key");

        verify(redisTemplate).delete(List.of("iam:idem:fp:key", "iam:idem:resp:key"));
    }

    private void stubValueOperations() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
}
