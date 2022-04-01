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
    @Nonbinding
    String value();
}
