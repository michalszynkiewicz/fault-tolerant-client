package io.quarkiverse.fault.tolerant.rest.client.reactive.deployment;

import static java.util.function.Predicate.not;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.DELETE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.GET;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HEAD;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OPTIONS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.POST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PUT;

import java.io.Closeable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkiverse.fault.tolerant.rest.reactive.ApplyFaultToleranceGroup;
import io.quarkiverse.fault.tolerant.rest.reactive.Idempotent;
import io.quarkiverse.fault.tolerant.rest.reactive.NonIdempotent;
import io.quarkiverse.fault.tolerant.rest.reactive.runtime.ApplyFaultToleranceGroupInterceptor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.runtime.MockedThroughWrapper;

class FaultTolerantRestClientReactiveProcessor {
    private static final Set<DotName> HTTP_OPERATIONS = Set.of(GET, POST, PUT, DELETE, OPTIONS, HEAD);
    private static final Set<DotName> NON_IDEMPOTENT_OPERATIONS = Set.of(POST);
    private static final Set<DotName> IDEMPOTENT_OPERATIONS = Set.copyOf(HTTP_OPERATIONS).stream()
            .filter(not(NON_IDEMPOTENT_OPERATIONS::contains)).collect(Collectors.toSet());

    private static final DotName IDEMPOTENT = DotName.createSimple(Idempotent.class.getName());
    private static final DotName NON_IDEMPOTENT = DotName.createSimple(NonIdempotent.class.getName());

    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class.getName());

    private static final String FEATURE = "fault-tolerant-rest-client-reactive";
    private static final Set<DotName> SKIPPED_INTERFACES = Set.of(DotName.createSimple(Closeable.class.getName()),
            DotName.createSimple(MockedThroughWrapper.class.getName()), DotName.createSimple(AutoCloseable.class.getName()));

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerInterceptor(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(ApplyFaultToleranceGroup.class,
                ApplyFaultToleranceGroupInterceptor.class).setUnremovable().build()); // mstodo setUnremovable is not necessary
    }

    @BuildStep
    void addFaultTolerance(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<AnnotationsTransformerBuildItem> annotationTransformers) {
        IndexView index = indexBuildItem.getIndex();
        Set<AnnotationInstance> registerRestClientAnnos = new HashSet<>(index.getAnnotations(REGISTER_REST_CLIENT));

        Set<ClassInfo> classesToScan = registerRestClientAnnos.stream()
                .map(AnnotationInstance::target)
                .map(AnnotationTarget::asClass)
                .collect(Collectors.toSet());
        Set<ClassInfo> scannedClasses = new HashSet<>();
        Map<MethodInfo, String> faultToleranceGroupsForInterfaces = new HashMap<>();
        while (!classesToScan.isEmpty()) {
            ClassInfo toScan = classesToScan.iterator().next();
            classesToScan.remove(toScan);
            scannedClasses.add(toScan);

            for (MethodInfo method : toScan.methods()) {
                if (isHttpOperationMethod(method)) {
                    String faultToleranceGroup = null;
                    if (isOfType(method, IDEMPOTENT_OPERATIONS, IDEMPOTENT, NON_IDEMPOTENT)) {
                        faultToleranceGroup = "idempotent";
                    } else if (isOfType(method, NON_IDEMPOTENT_OPERATIONS, NON_IDEMPOTENT, IDEMPOTENT)) {
                        faultToleranceGroup = "nonIdempotent";
                    }
                    if (faultToleranceGroup != null) {
                        faultToleranceGroupsForInterfaces.put(method, faultToleranceGroup);
                    }
                    // todo support for custom groups
                } else if (isReturningAnObject(method)) {
                    ClassInfo possibleSubInterface = index.getClassByName(method.returnType().name());
                    if (possibleSubInterface != null && !scannedClasses.contains(possibleSubInterface)) {
                        classesToScan.add(possibleSubInterface);
                    }
                }
            }
        }

        Map<MethodInfo, String> faultToleranceGroups = new ConcurrentHashMap<>(); // mstodo remove?
        // we gathered interface methods, let's find their implementations now:
        for (Map.Entry<MethodInfo, String> methodEntry : faultToleranceGroupsForInterfaces.entrySet()) {
            MethodInfo interfaceMethod = methodEntry.getKey();
            for (ClassInfo implementor : index.getAllKnownImplementors(interfaceMethod.declaringClass().name())) {
                if (implementor.name().toString().endsWith("CDIWrapper")) {
                    // CDIWrapper has to have all the methods of our interest defined, they have the same signature
                    MethodInfo implementorMethod = implementor.method(interfaceMethod.name(),
                            interfaceMethod.parameters().toArray(Type.EMPTY_ARRAY));
                    faultToleranceGroups.put(implementorMethod, methodEntry.getValue());
                }
            }
        }

        annotationTransformers.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public void transform(TransformationContext transformationContext) {
                MethodInfo method = transformationContext.getTarget().asMethod();
                ClassInfo wrapperClass = method.declaringClass();
                if (wrapperClass.name().toString().endsWith("CDIWrapper")) {
                    for (DotName interfaceName : wrapperClass.interfaceNames()) {
                        if (SKIPPED_INTERFACES.contains(interfaceName)) {
                            continue;
                        }

                        ClassInfo interfaceClass = index.getClassByName(interfaceName);
                        MethodInfo interfaceMethod = interfaceClass.method(method.name(),
                                method.parameters().toArray(Type.EMPTY_ARRAY));
                        if (interfaceMethod != null) {
                            String groupName = faultToleranceGroupsForInterfaces.get(interfaceMethod);
                            if (groupName != null) {
                                transformationContext.transform().add(
                                        DotName.createSimple(ApplyFaultToleranceGroup.class.getName()),
                                        AnnotationValue.createStringValue("value", groupName));
                            }
                        }
                    }
                }
            }

            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.METHOD;
            }
        }));
    }

    private boolean isHttpOperationMethod(MethodInfo method) {
        return HTTP_OPERATIONS.stream().anyMatch(anno -> method.annotation(anno) != null);
    }

    private boolean isReturningAnObject(MethodInfo method) {
        String packagePrefix = method.returnType().name().packagePrefix();
        return method.returnType().kind() == Type.Kind.CLASS && (packagePrefix == null || !packagePrefix.startsWith("java"));
    }

    private boolean isOfType(MethodInfo method, Set<DotName> httpMethods,
            DotName operationType, DotName oppositeOperationType) {
        if (method.annotation(operationType) != null) {
            return true;
        }

        for (DotName idempotentOperation : httpMethods) {
            if (method.annotation(idempotentOperation) != null) {
                return method.annotation(oppositeOperationType) == null;
            }
        }
        return false;
    }
}
