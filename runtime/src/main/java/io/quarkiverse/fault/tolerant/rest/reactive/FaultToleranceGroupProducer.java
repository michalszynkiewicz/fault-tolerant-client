package io.quarkiverse.fault.tolerant.rest.reactive;

public interface FaultToleranceGroupProducer {
    FaultToleranceGroup create();

    String getName();

    default int getPriority() {
        return 10;
    }
}
