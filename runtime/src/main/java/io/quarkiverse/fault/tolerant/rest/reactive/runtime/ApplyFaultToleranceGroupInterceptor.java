package io.quarkiverse.fault.tolerant.rest.reactive.runtime;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.quarkiverse.fault.tolerant.rest.reactive.ApplyFaultToleranceGroup;
import io.quarkiverse.fault.tolerant.rest.reactive.FaultToleranceGroup;
import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.Priority;
import io.smallrye.faulttolerance.api.FaultTolerance;

@Priority(1)
@ApplyFaultToleranceGroup(value = "", groupKey = "", isAsync = false, returnType = Object.class)
@Interceptor
public class ApplyFaultToleranceGroupInterceptor {
    private final Map<Method, FaultTolerance<Object>> faultToleranceForMethod = new HashMap<>();
    @Inject
    FaultToleranceStrategyProvider provider;

    @SuppressWarnings("unchecked")
    @AroundInvoke
    Object wrapInFaultTolerance(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        FaultTolerance<Object> faultTolerance = faultToleranceForMethod.get(method);
        if (faultTolerance == null) {
            //            FaultToleranceStrategyProvider provider = Arc.container().instance(FaultToleranceStrategyProvider.class).get();
            Collection<ApplyFaultToleranceGroup> annotations = (Collection<ApplyFaultToleranceGroup>) context.getContextData()
                    .get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
            ApplyFaultToleranceGroup groupAnnotation = annotations.iterator().next();
            String groupName = groupAnnotation.value();
            FaultToleranceGroup faultToleranceGroup = provider.get(groupName);

            if (faultToleranceGroup == null) {
                faultTolerance = NO_FAULT_TOLERANCE;
            } else {
                if (groupAnnotation.isAsync()) {
                    faultTolerance = (FaultTolerance<Object>) faultToleranceGroup.build(groupAnnotation.returnType());
                } else {
                    faultTolerance = (FaultTolerance<Object>) faultToleranceGroup.build(groupAnnotation.returnType());
                }
            }
            faultToleranceForMethod.put(method, faultTolerance);
        }
        return faultTolerance == NO_FAULT_TOLERANCE ? context.proceed() : faultTolerance.call(() -> {
            try {
                return context.proceed();
            } catch (Exception any) {
                throw new RuntimeException("any"); // mstodo something smarter
            }
        });
    }

    private static final FaultTolerance NO_FAULT_TOLERANCE = action -> null;
}
