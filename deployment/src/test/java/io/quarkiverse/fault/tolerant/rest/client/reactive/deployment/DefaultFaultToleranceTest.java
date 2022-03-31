package io.quarkiverse.fault.tolerant.rest.client.reactive.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fault.tolerant.rest.reactive.Idempotent;
import io.quarkiverse.fault.tolerant.rest.reactive.NonIdempotent;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultFaultToleranceTest {
    public static final String SECOND_ATTEMPT = "second attempt";
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Client.class, FailOnFirstAttemptResource.class);
                }
            });

    @RestClient
    Client client;

    @Test
    void shouldAutomaticallyRetryGet() {
        assertThat(client.getThatShouldBeRetried()).isEqualTo(SECOND_ATTEMPT);
    }

    @Test
    void shouldNotRetryGetIfMarkedNonIdempotent() {
        assertThatThrownBy(() -> client.getThatShouldFail()).isInstanceOf(ClientWebApplicationException.class);
    }

    @Path("/foo")
    @RegisterRestClient(baseUri = "http://localhost:8081")
    public interface Client {
        @GET
        @Path("/fail-on-first-attempt/1")
        String getThatShouldBeRetried();

        @GET
        @NonIdempotent
        @Path("/fail-on-first-attempt/2")
        String getThatShouldFail();

        @POST
        @Path("/fail-on-first-attempt/3")
        @Idempotent
        String postThatShouldBeRetried();

        @POST
        @Path("/fail-on-first-attempt/4")
        String postThatShouldFail();
    }

    @Path("/fail-on-first-attempt/{id}")
    public static class FailOnFirstAttemptResource {
        private static final Set<String> shouldSucceed = new HashSet<>();

        @GET
        public Response get(@PathParam("id") String id) {
            if (shouldSucceed.add(id)) {
                return Response.status(500, "first attempt - failure").build();
            } else {
                return Response.ok(SECOND_ATTEMPT).build();
            }
        }

        @POST
        public Response post(@PathParam("id") String id) {
            if (shouldSucceed.add(id)) {
                return Response.status(500, "first attempt - failure").build();
            } else {
                return Response.ok(SECOND_ATTEMPT).build();
            }
        }
    }
}
