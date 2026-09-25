package com.example.securitypoc.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import graphql.ExecutionResultImpl;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.graphql.support.DefaultGraphQlRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import reactor.core.publisher.Mono;

class OperationSecurityInterceptorTest {
    private final OperationSecurityInterceptor fullPolicy = new OperationSecurityInterceptor(false);
    private final OperationSecurityInterceptor nameOnlyPolicy = new OperationSecurityInterceptor(true);

    @Test
    void rejectsUnknownOperation() {
        var response = fullPolicy.intercept(request("query Unknown { myOrders { id } }", "Unknown"), chain()).block();
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().getFirst().getExtensions()).containsEntry("code", "UNKNOWN_OPERATION");
    }

    @Test
    void rejectsRootFieldMismatch() {
        var response = fullPolicy.intercept(request("query GetOrder { orders { id } }", "GetOrder"), chain()).block();
        assertThat(response).isNotNull();
        assertThat(response.getErrors().getFirst().getExtensions()).containsEntry("code", "ROOT_FIELD_MISMATCH");
    }

    @Test
    void expandsFragmentsAndComparesActualFieldNames() {
        var query = "query GetOrder { ...OrderFields } fragment OrderFields on Query { result: order(id: \"1\") { id } }";
        var response = fullPolicy.intercept(request(query, "GetOrder"), chain()).block();
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void nameOnlyPolicyDemonstratesTheAttack() {
        var response = nameOnlyPolicy.intercept(request("query GetOrder { orders { id } }", "GetOrder"), chain()).block();
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEmpty();
    }

    private WebGraphQlRequest request(String document, String operationName) {
        var graphQlRequest = new DefaultGraphQlRequest(document, operationName, Map.of(), Map.of());
        return new WebGraphQlRequest(
            URI.create("http://localhost/graphql"),
            new HttpHeaders(),
            new LinkedMultiValueMap<>(),
            new HashMap<>(),
            graphQlRequest,
            "test",
            Locale.ENGLISH
        );
    }

    private OperationSecurityInterceptor.Chain chain() {
        return request -> Mono.just(new WebGraphQlResponse(new DefaultExecutionGraphQlResponse(
            request.toExecutionInput(),
            new ExecutionResultImpl(java.util.List.of())
        )));
    }
}
