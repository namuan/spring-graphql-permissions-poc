package com.example.securitypoc.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.parser.Parser;
import graphql.ErrorType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
public class OperationSecurityInterceptor implements WebGraphQlInterceptor {
    private record OperationContract(OperationDefinition.Operation operation, String rootField) {
    }

    private static final Map<String, OperationContract> CONTRACTS = contracts();

    private final boolean nameOnlyEnabled;

    public OperationSecurityInterceptor(
        @Value("${app.security.name-only-operations-enabled:false}") boolean nameOnlyEnabled
    ) {
        this.nameOnlyEnabled = nameOnlyEnabled;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        Document document;
        try {
            document = Parser.parse(request.getDocument());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GraphQL document");
        }

        OperationDefinition operation = selectedOperation(document, request.getOperationName());
        if (operation == null || operation.getName() == null) {
            return Mono.just(errorResponse(request, "ANONYMOUS_OPERATION", "A named operation is required"));
        }

        String operationName = operation.getName();
        OperationContract contract = CONTRACTS.get(operationName);
        if (contract == null) {
            return Mono.just(errorResponse(request, "UNKNOWN_OPERATION", "Operation is not allowed"));
        }
        if (nameOnlyEnabled) {
            return chain.next(request);
        }
        if (contract.operation() != operation.getOperation()) {
            return Mono.just(errorResponse(request, "OPERATION_TYPE_MISMATCH", "Operation type is not allowed"));
        }

        Map<String, FragmentDefinition> fragments = document.getDefinitionsOfType(FragmentDefinition.class).stream()
            .collect(Collectors.toMap(FragmentDefinition::getName, Function.identity()));
        List<Field> rootFields = new ArrayList<>();
        collectRootFields(operation.getSelectionSet(), fragments, rootFields);
        if (rootFields.size() != 1) {
            return Mono.just(errorResponse(request, "ROOT_FIELD_MISMATCH", "Operation must select exactly one root field"));
        }
        Field rootField = rootFields.getFirst();
        if (!contract.rootField().equals(rootField.getName())) {
            return Mono.just(errorResponse(request, "ROOT_FIELD_MISMATCH", "Operation root field is not allowed"));
        }
        return chain.next(request);
    }

    private OperationDefinition selectedOperation(Document document, String requestedName) {
        List<OperationDefinition> operations = document.getDefinitionsOfType(OperationDefinition.class);
        if (requestedName == null) {
            return operations.size() == 1 ? operations.getFirst() : null;
        }
        return operations.stream()
            .filter(operation -> requestedName.equals(operation.getName()))
            .findFirst()
            .orElse(null);
    }

    private void collectRootFields(
        SelectionSet selectionSet,
        Map<String, FragmentDefinition> fragments,
        List<Field> fields
    ) {
        if (selectionSet == null) {
            return;
        }
        for (Selection<?> selection : selectionSet.getSelections()) {
            if (selection instanceof Field field) {
                fields.add(field);
            } else if (selection instanceof InlineFragment inlineFragment) {
                collectRootFields(inlineFragment.getSelectionSet(), fragments, fields);
            } else if (selection instanceof FragmentSpread fragmentSpread) {
                FragmentDefinition fragment = fragments.get(fragmentSpread.getName());
                if (fragment != null) {
                    collectRootFields(fragment.getSelectionSet(), fragments, fields);
                }
            }
        }
    }

    private WebGraphQlResponse errorResponse(WebGraphQlRequest request, String code, String message) {
        ExecutionInput input = request.toExecutionInput();
        ExecutionResult result = new ExecutionResultImpl(new OperationSecurityError(code, message));
        return new WebGraphQlResponse(new DefaultExecutionGraphQlResponse(input, result));
    }

    private static Map<String, OperationContract> contracts() {
        Map<String, OperationContract> contracts = new HashMap<>();
        contracts.put("GetOrder", new OperationContract(OperationDefinition.Operation.QUERY, "order"));
        contracts.put("GetMyOrders", new OperationContract(OperationDefinition.Operation.QUERY, "myOrders"));
        contracts.put("GetOrders", new OperationContract(OperationDefinition.Operation.QUERY, "orders"));
        contracts.put("CreateOrder", new OperationContract(OperationDefinition.Operation.MUTATION, "createOrder"));
        contracts.put("UpdateOrder", new OperationContract(OperationDefinition.Operation.MUTATION, "updateOrder"));
        contracts.put("CancelOrder", new OperationContract(OperationDefinition.Operation.MUTATION, "cancelOrder"));
        contracts.put("DeleteOrder", new OperationContract(OperationDefinition.Operation.MUTATION, "deleteOrder"));
        return Map.copyOf(contracts);
    }

    private static final class OperationSecurityError implements GraphQLError {
        private final String code;
        private final String message;

        private OperationSecurityError(String code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public List<graphql.language.SourceLocation> getLocations() {
            return List.of();
        }

        @Override
        public ErrorType getErrorType() {
            return ErrorType.ValidationError;
        }

        @Override
        public Map<String, Object> getExtensions() {
            return Map.of("code", code, "category", "OPERATION_SECURITY");
        }
    }
}
