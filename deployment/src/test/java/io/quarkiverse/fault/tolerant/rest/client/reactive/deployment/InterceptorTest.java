package io.quarkiverse.fault.tolerant.rest.client.reactive.deployment;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fault.tolerant.rest.reactive.ApplyFaultToleranceGroup;
import io.quarkus.test.QuarkusUnitTest;

public class InterceptorTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest();

    @Inject
    MyBean bean;

    // mstodo remove?
    @Test
    void test() {
        bean.failOnce();
    }

    @ApplicationScoped
    public static class MyBean {
        AtomicInteger failures = new AtomicInteger();

        @ApplyFaultToleranceGroup(value = "idempotent", isAsync = false, groupKey = "fff", returnType = String.class)
        String failOnce() {
            if (failures.getAndIncrement() < 1) {
                throw new RuntimeException("Failure");
            }
            return "success on attempt " + failures.get();
        }
    }
}
