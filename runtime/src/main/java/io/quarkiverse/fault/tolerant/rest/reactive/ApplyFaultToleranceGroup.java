package io.quarkiverse.fault.tolerant.rest.reactive;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

@Target({ METHOD, TYPE }) // todo support type too
@Retention(RUNTIME)
@Documented
@InterceptorBinding
public @interface ApplyFaultToleranceGroup {
    /**
     * @return name of the group producer
     */
    @Nonbinding
    String value();

    /**
     * @return identifier of the group
     */
    @Nonbinding
    String groupKey(); // to identify the group

    /**
     *
     * @return return class of the guarded method
     */
    @Nonbinding
    Class<?> returnType();

    /**
     * @return if {@link FaultToleranceGroup#buildAsync} should be used
     */
    @Nonbinding
    boolean isAsync();
}
