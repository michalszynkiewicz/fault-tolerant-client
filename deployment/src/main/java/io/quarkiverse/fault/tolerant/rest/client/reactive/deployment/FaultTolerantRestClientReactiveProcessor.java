package io.quarkiverse.fault.tolerant.rest.client.reactive.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class FaultTolerantRestClientReactiveProcessor {

    private static final String FEATURE = "fault-tolerant-rest-client-reactive";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

}
