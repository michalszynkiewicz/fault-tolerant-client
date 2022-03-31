package io.quarkiverse.fault.tolerant.rest.reactive;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import io.smallrye.faulttolerance.api.FaultTolerance;

public class FaultToleranceGroup {

    @SuppressWarnings("rawtypes")
    private final List<Consumer<FaultTolerance.Builder>> constructionChain;

    FaultToleranceGroup(List<Consumer<FaultTolerance.Builder>> constructionChain) {
        this.constructionChain = constructionChain;
    }

    public <T> FaultTolerance<T> build(Class<T> type) {
        FaultTolerance.Builder<T, FaultTolerance<T>> ftBuilder = FaultTolerance.create();
        configure(ftBuilder);
        return ftBuilder.build();
    }

    public <T> FaultTolerance<CompletionStage<T>> buildAsync(Class<T> type) {
        FaultTolerance.Builder<CompletionStage<T>, FaultTolerance<CompletionStage<T>>> ftBuilder = FaultTolerance.createAsync();
        configure(ftBuilder);
        return ftBuilder.build();
    }

    @SuppressWarnings("rawtypes")
    private <T> void configure(FaultTolerance.Builder<T, FaultTolerance<T>> ftBuilder) {
        for (Consumer<FaultTolerance.Builder> builderConsumer : constructionChain) {
            builderConsumer.accept(ftBuilder);
        }
    }
}
