package io.quarkiverse.fault.tolerant.rest.reactive;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IdempotentGroupProducerImpl implements FaultToleranceGroupProducer {
    @Override
    public FaultToleranceGroup create() {
        return new FaultToleranceGroupBuilder().withRetry().done().build();
    }

    @Override
    public String getName() {
        return "idempotent";
    }
}
