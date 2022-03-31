package io.quarkiverse.fault.tolerant.rest.reactive;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.CustomBackoffStrategy;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.api.FibonacciBackoff;

@SuppressWarnings({ "rawtypes", "unused", "unchecked" })
public class FaultToleranceGroupBuilder {

    private final List<Consumer<FaultTolerance.Builder>> constructionChain = new ArrayList<>();

    public FaultToleranceGroupBuilder withDescription(String value) {
        constructionChain.add(builder -> builder.withDescription(value));
        return this;
    }

    public FaultToleranceGroupBuilder.BulkheadBuilder withBulkhead() {
        return new BulkheadBuilder(this);
    }

    public FaultToleranceGroupBuilder.CircuitBreakerBuilder withCircuitBreaker() {
        return new CircuitBreakerBuilder(this);
    }

    public FaultToleranceGroupBuilder.RetryBuilder withRetry() {
        return new RetryBuilder(this);
    }

    public TimeoutBuilder withTimeout() {
        return new TimeoutBuilder(this);
    }

    public FaultToleranceGroup build() {
        return new FaultToleranceGroup(constructionChain);
    }

    /**
     * Configures a bulkhead.
     *
     * @see Bulkhead @Bulkhead
     */
    public static class BulkheadBuilder {

        private final List<Consumer<FaultTolerance.Builder.BulkheadBuilder>> constructionChain = new ArrayList<>();
        private final FaultToleranceGroupBuilder groupBuilder;

        public BulkheadBuilder(FaultToleranceGroupBuilder groupBuilder) {
            this.groupBuilder = groupBuilder;
        }

        public BulkheadBuilder limit(int value) {
            constructionChain.add(builder -> builder.limit(value));
            return this;
        }

        public BulkheadBuilder queueSize(int value) {
            constructionChain.add(builder -> builder.queueSize(value));
            return this;
        }

        public BulkheadBuilder onAccepted(Runnable callback) {
            constructionChain.add(builder -> builder.onAccepted(callback));
            return this;
        }

        public BulkheadBuilder onRejected(Runnable callback) {
            constructionChain.add(builder -> builder.onRejected(callback));
            return this;
        }

        public BulkheadBuilder onFinished(Runnable callback) {
            constructionChain.add(builder -> builder.onFinished(callback));
            return this;
        }

        public FaultToleranceGroupBuilder done() {
            groupBuilder.constructionChain.add(builder -> {
                FaultTolerance.Builder.BulkheadBuilder bulkheadBuilder = builder.withBulkhead();
                constructionChain.forEach(operation -> operation.accept(bulkheadBuilder));
                bulkheadBuilder.done();
            });
            return groupBuilder;
        }
    }

    public static class CircuitBreakerBuilder {
        private final List<Consumer<FaultTolerance.Builder.CircuitBreakerBuilder>> constructionChain = new ArrayList<>();
        private final FaultToleranceGroupBuilder groupBuilder;

        public CircuitBreakerBuilder(FaultToleranceGroupBuilder groupBuilder) {
            this.groupBuilder = groupBuilder;
        }

        public CircuitBreakerBuilder failOn(Collection<Class<? extends Throwable>> value) {
            constructionChain.add(builder -> builder.failOn(value));
            return this;
        }

        public CircuitBreakerBuilder failOn(Class<? extends Throwable> value) {
            return failOn(Collections.singleton(Objects.requireNonNull(value)));
        }

        /**
         * Sets the set of exception types considered success. Defaults to no exception (empty set).
         *
         * @param value collection of exception types, must not be {@code null}
         * @return this circuit breaker builder
         * @see CircuitBreaker#skipOn() @CircuitBreaker.skipOn
         */
        public CircuitBreakerBuilder skipOn(Collection<Class<? extends Throwable>> value) {
            constructionChain.add(builder -> builder.skipOn(value));
            return this;
        }

        /**
         * Equivalent to {@link #skipOn(Collection) skipOn(Collections.singleton(value))}.
         *
         * @param value an exception class, must not be {@code null}
         * @return this circuit breaker builder
         */
        public CircuitBreakerBuilder skipOn(Class<? extends Throwable> value) {
            constructionChain.add(builder -> builder.skipOn(value));
            return this;
        }

        // todo:
        //        public CircuitBreakerBuilder when(Predicate<Throwable> value) {
        //            constructionChain.add(builder -> builder.when(value));
        //            return this;
        //        }

        public CircuitBreakerBuilder delay(long value, ChronoUnit unit) {
            constructionChain.add(builder -> builder.delay(value, unit));
            return this;
        }

        public CircuitBreakerBuilder requestVolumeThreshold(int value) {
            constructionChain.add(builder -> builder.requestVolumeThreshold(value));
            return this;
        }

        public CircuitBreakerBuilder failureRatio(double value) {
            constructionChain.add(builder -> builder.failureRatio(value));
            return this;
        }

        public CircuitBreakerBuilder successThreshold(int value) {
            constructionChain.add(builder -> builder.successThreshold(value));
            return this;
        }

        public CircuitBreakerBuilder name(String value) {
            constructionChain.add(builder -> builder.name(value));
            return this;
        }

        public CircuitBreakerBuilder onStateChange(Consumer<CircuitBreakerState> callback) {
            constructionChain.add(builder -> builder.onStateChange(callback));
            return this;
        }

        public CircuitBreakerBuilder onSuccess(Runnable callback) {
            constructionChain.add(builder -> builder.onSuccess(callback));
            return this;
        }

        public CircuitBreakerBuilder onFailure(Runnable callback) {
            constructionChain.add(builder -> builder.onFailure(callback));
            return this;
        }

        public CircuitBreakerBuilder onPrevented(Runnable callback) {
            constructionChain.add(builder -> builder.onPrevented(callback));
            return this;
        }

        public FaultToleranceGroupBuilder done() {
            groupBuilder.constructionChain.add(builder -> {
                FaultTolerance.Builder.CircuitBreakerBuilder bulkheadBuilder = builder.withCircuitBreaker();
                constructionChain.forEach(operation -> operation.accept(bulkheadBuilder));
                bulkheadBuilder.done();
            });
            return groupBuilder;
        }

    }

    public static class RetryBuilder {
        private final List<Consumer<FaultTolerance.Builder.RetryBuilder>> constructionChain = new ArrayList<>();
        private final FaultToleranceGroupBuilder groupBuilder;

        public RetryBuilder(FaultToleranceGroupBuilder groupBuilder) {
            this.groupBuilder = groupBuilder;
        }

        public RetryBuilder maxRetries(int value) {
            constructionChain.add(builder -> builder.maxRetries(value));
            return this;
        }

        public RetryBuilder delay(long value, ChronoUnit unit) {
            constructionChain.add(builder -> builder.delay(value, unit));
            return this;
        }

        public RetryBuilder maxDuration(long value, ChronoUnit unit) {
            constructionChain.add(builder -> builder.maxDuration(value, unit));
            return this;
        }

        public RetryBuilder jitter(long value, ChronoUnit unit) {
            constructionChain.add(builder -> builder.jitter(value, unit));
            return this;
        }

        public RetryBuilder retryOn(Collection<Class<? extends Throwable>> value) {
            constructionChain.add(builder -> builder.retryOn(value));
            return this;
        }

        public RetryBuilder retryOn(Class<? extends Throwable> value) {
            constructionChain.add(builder -> builder.retryOn(value));
            return this;
        }

        public RetryBuilder abortOn(Collection<Class<? extends Throwable>> value) {
            constructionChain.add(builder -> builder.abortOn(value));
            return this;
        }

        public RetryBuilder abortOn(Class<? extends Throwable> value) {
            constructionChain.add(builder -> builder.abortOn(value));
            return this;
        }
        // TODO:
        //        public RetryBuilder when(Predicate<Throwable> value) {
        //            constructionChain.add(builder -> builder.when(value));
        //            return this;
        //        }

        public ExponentialBackoffBuilder withExponentialBackoff() {
            return new ExponentialBackoffBuilder(this);
        }

        public FibonacciBackoffBuilder withFibonacciBackoff() {
            return new FibonacciBackoffBuilder(this);
        }

        public CustomBackoffBuilder withCustomBackoff() {
            return new CustomBackoffBuilder(this);
        }

        public RetryBuilder onRetry(Runnable callback) {
            constructionChain.add(builder -> builder.onRetry(callback));
            return this;
        }

        public RetryBuilder onSuccess(Runnable callback) {
            constructionChain.add(builder -> builder.onSuccess(callback));
            return this;
        }

        public RetryBuilder onFailure(Runnable callback) {
            constructionChain.add(builder -> builder.onFailure(callback));
            return this;
        }

        public FaultToleranceGroupBuilder done() {
            groupBuilder.constructionChain.add(builder -> {
                FaultTolerance.Builder.RetryBuilder retryBuilder = builder.withRetry();
                constructionChain.forEach(operation -> operation.accept(retryBuilder));
                retryBuilder.done();
            });
            return groupBuilder;
        }

        /**
         * Configures an exponential backoff for retry.
         *
         * @see ExponentialBackoff @ExponentialBackoff
         */
        public static class ExponentialBackoffBuilder<T, R> {
            private final RetryBuilder retryBuilder;
            private final List<Consumer<FaultTolerance.Builder.RetryBuilder.ExponentialBackoffBuilder>> constructionChain = new ArrayList<>();

            public ExponentialBackoffBuilder(RetryBuilder retryBuilder) {
                this.retryBuilder = retryBuilder;
            }

            public ExponentialBackoffBuilder factor(int value) {
                constructionChain.add(builder -> builder.factor(value));
                return this;
            }

            public ExponentialBackoffBuilder maxDelay(long value, ChronoUnit unit) {
                constructionChain.add(builder -> builder.maxDelay(value, unit));
                return this;
            }

            public RetryBuilder done() {
                retryBuilder.constructionChain.add(builder -> {
                    FaultTolerance.Builder.RetryBuilder.ExponentialBackoffBuilder exponentialBackoff = builder
                            .withExponentialBackoff();
                    constructionChain.forEach(operation -> operation.accept(exponentialBackoff));
                    exponentialBackoff.done();
                });
                return retryBuilder;
            }

        }

        /**
         * Configures a Fibonacci backoff for retry.
         *
         * @see FibonacciBackoff @FibonacciBackoff
         */
        public static class FibonacciBackoffBuilder<T, R> {
            private final RetryBuilder retryBuilder;
            private final List<Consumer<FaultTolerance.Builder.RetryBuilder.FibonacciBackoffBuilder>> constructionChain = new ArrayList<>();

            public FibonacciBackoffBuilder(RetryBuilder retryBuilder) {
                this.retryBuilder = retryBuilder;
            }

            /**
             * Sets the maximum delay between retries. Defaults to 1 minute.
             *
             * @param value the maximum delay, must be &gt;= 0
             * @param unit the maximum delay unit, must not be {@code null}
             * @return this fibonacci backoff builder
             * @see FibonacciBackoff#maxDelay() @FibonacciBackoff.maxDelay
             * @see FibonacciBackoff#maxDelayUnit() @FibonacciBackoff.maxDelayUnit
             */
            public FibonacciBackoffBuilder<T, R> maxDelay(long value, ChronoUnit unit) {
                constructionChain.add(builder -> builder.maxDelay(value, unit));
                return this;
            }

            //            default FaultTolerance.Builder.RetryBuilder.FibonacciBackoffBuilder<T, R> with(Consumer<FaultTolerance.Builder.RetryBuilder.FibonacciBackoffBuilder<T, R>> consumer) {
            //                consumer.accept(this);
            //                return this;
            //            }

            public RetryBuilder done() {
                retryBuilder.constructionChain.add(builder -> {
                    FaultTolerance.Builder.RetryBuilder.FibonacciBackoffBuilder backoff = builder.withFibonacciBackoff();
                    constructionChain.forEach(operation -> operation.accept(backoff));
                    backoff.done();
                });
                return retryBuilder;
            }
        }

        public static class CustomBackoffBuilder {
            private final RetryBuilder retryBuilder;
            private final List<Consumer<FaultTolerance.Builder.RetryBuilder.CustomBackoffBuilder>> constructionChain = new ArrayList<>();

            public CustomBackoffBuilder(RetryBuilder retryBuilder) {
                this.retryBuilder = retryBuilder;
            }

            public CustomBackoffBuilder strategy(Supplier<CustomBackoffStrategy> value) {
                constructionChain.add(builder -> builder.strategy(value));
                return this;
            }

            //            default FaultTolerance.Builder.RetryBuilder.FibonacciBackoffBuilder<T, R> with(Consumer<FaultTolerance.Builder.RetryBuilder.FibonacciBackoffBuilder<T, R>> consumer) {
            //                consumer.accept(this);
            //                return this;
            //            }

            public RetryBuilder done() {
                retryBuilder.constructionChain.add(builder -> {
                    FaultTolerance.Builder.RetryBuilder.CustomBackoffBuilder backoff = builder.withCustomBackoff();
                    constructionChain.forEach(operation -> operation.accept(backoff));
                    backoff.done();
                });
                return retryBuilder;
            }

            //            default FaultTolerance.Builder.RetryBuilder.CustomBackoffBuilder<T, R> with(Consumer<FaultTolerance.Builder.RetryBuilder.CustomBackoffBuilder<T, R>> consumer) {
            //                consumer.accept(this);
            //                return this;
            //            }
        }
    }

    public static class TimeoutBuilder {

        private final List<Consumer<FaultTolerance.Builder.TimeoutBuilder>> constructionChain = new ArrayList<>();
        private final FaultToleranceGroupBuilder groupBuilder;

        public TimeoutBuilder(FaultToleranceGroupBuilder groupBuilder) {
            this.groupBuilder = groupBuilder;
        }

        public TimeoutBuilder duration(long value, ChronoUnit unit) {
            constructionChain.add(builder -> builder.duration(value, unit));
            return this;
        }

        public TimeoutBuilder onTimeout(Runnable callback) {
            constructionChain.add(builder -> builder.onTimeout(callback));
            return this;
        }

        public TimeoutBuilder onFinished(Runnable callback) {
            constructionChain.add(builder -> builder.onFinished(callback));
            return this;
        }

        public FaultToleranceGroupBuilder done() {
            groupBuilder.constructionChain.add(builder -> {
                FaultTolerance.Builder.TimeoutBuilder timeoutBuilder = builder.withTimeout();
                constructionChain.forEach(operation -> operation.accept(timeoutBuilder));
                timeoutBuilder.done();
            });
            return groupBuilder;
        }
    }
}
