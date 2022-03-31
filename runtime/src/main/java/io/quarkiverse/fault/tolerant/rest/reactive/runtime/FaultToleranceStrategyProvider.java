package io.quarkiverse.fault.tolerant.rest.reactive.runtime;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.quarkiverse.fault.tolerant.rest.reactive.FaultToleranceGroup;
import io.quarkiverse.fault.tolerant.rest.reactive.FaultToleranceGroupProducer;

@ApplicationScoped
public class FaultToleranceStrategyProvider {
    @Inject
    Instance<FaultToleranceGroupProducer> faultToleranceGroupProducers;

    private final Map<String, FaultToleranceGroupProducer> producerByName = new HashMap<>();

    @PostConstruct
    void setUp() {
        List<FaultToleranceGroupProducer> producers = faultToleranceGroupProducers.stream()
                .sorted(Comparator.comparing(FaultToleranceGroupProducer::getPriority))
                .collect(Collectors.toList());
        for (FaultToleranceGroupProducer producer : producers) {
            producerByName.put(producer.getName(), producer);
        }
    }

    public FaultToleranceGroup get(String name) {
        return producerByName.get(name).create();
    }

}
