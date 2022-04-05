package io.quarkiverse.fault.tolerant.rest.reactive.runtime;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.quarkiverse.fault.tolerant.rest.reactive.ApplyFaultToleranceGroup;
import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.Priority;
import io.smallrye.faulttolerance.api.FaultTolerance;

@Priority(1)
@ApplyFaultToleranceGroup("")
@Interceptor
public class ApplyFaultToleranceGroupInterceptor {
    private final Map<Method, FaultTolerance<?>> faultToleranceForMethod = new HashMap<>();

    @AroundInvoke
    Object wrapInFaultTolerance(InvocationContext context) {
        Object bindings = context.getContextData().get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
        return null;
    }
}
