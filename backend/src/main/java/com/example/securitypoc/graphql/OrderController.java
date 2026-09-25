package com.example.securitypoc.graphql;

import com.example.securitypoc.domain.NotFoundException;
import com.example.securitypoc.domain.OrderEntity;
import com.example.securitypoc.domain.OrderRepository;
import com.example.securitypoc.domain.TenantRepository;
import com.example.securitypoc.domain.UserEntity;
import com.example.securitypoc.domain.UserRepository;
import com.example.securitypoc.security.AuthorizationService;
import com.example.securitypoc.security.CurrentUser;
import com.example.securitypoc.security.CurrentUserService;
import com.example.securitypoc.security.OrderAction;
import com.example.securitypoc.security.Permission;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
@Transactional
public class OrderController {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;

    public OrderController(
        OrderRepository orderRepository,
        UserRepository userRepository,
        TenantRepository tenantRepository,
        CurrentUserService currentUserService,
        AuthorizationService authorizationService
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
    }

    @QueryMapping
    public OrderEntity order(@Argument String id, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = currentUserService.requireCurrentUser(jwt);
        OrderEntity order = findTenantOrder(parseId(id), user.tenantId());
        authorizationService.requireOrderAccess(user, order, OrderAction.READ);
        return order;
    }

    @QueryMapping
    public List<OrderEntity> myOrders(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = currentUserService.requireCurrentUser(jwt);
        return orderRepository.findByTenant_IdAndCustomer_IdOrderByIdDesc(user.tenantId(), user.appUserId());
    }

    @QueryMapping
    public List<OrderEntity> orders(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = currentUserService.requireCurrentUser(jwt);
        authorizationService.requirePermission(user, Permission.ORDER_READ);
        return orderRepository.findByTenant_IdOrderByIdDesc(user.tenantId());
    }

    @MutationMapping
    public OrderEntity createOrder(@Argument CreateOrderInput input, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = currentUserService.requireCurrentUser(jwt);
        authorizationService.requirePermission(user, Permission.ORDER_CREATE);
        validateCreateInput(input);
        UserEntity customer = userRepository.findById(user.appUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));
        return orderRepository.save(new OrderEntity(
            tenantRepository.getReferenceById(user.tenantId()),
            customer,
            input.description().trim(),
            input.amount()
        ));
    }

    @MutationMapping
    public OrderEntity updateOrder(
        @Argument String id,
        @Argument UpdateOrderInput input,
        @AuthenticationPrincipal Jwt jwt
    ) {
        CurrentUser user = currentUserService.requireCurrentUser(jwt);
        OrderEntity order = findTenantOrder(parseId(id), user.tenantId());
        authorizationService.requireOrderAccess(user, order, OrderAction.UPDATE);
        validateUpdateInput(input);
        order.update(input.description() == null ? null : input.description().trim(), input.amount());
        return order;
    }

    @MutationMapping
    public OrderEntity cancelOrder(@Argument String id, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = currentUserService.requireCurrentUser(jwt);
        OrderEntity order = findTenantOrder(parseId(id), user.tenantId());
        authorizationService.requireOrderAccess(user, order, OrderAction.CANCEL);
        if (order.getStatus() != com.example.securitypoc.domain.OrderStatus.OPEN) {
            throw new IllegalArgumentException("Only open orders can be cancelled");
        }
        order.cancel();
        return order;
    }

    @MutationMapping
    public Boolean deleteOrder(@Argument String id, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = currentUserService.requireCurrentUser(jwt);
        OrderEntity order = findTenantOrder(parseId(id), user.tenantId());
        authorizationService.requireOrderAccess(user, order, OrderAction.DELETE);
        orderRepository.delete(order);
        return true;
    }

    @SchemaMapping(typeName = "Order", field = "internalNotes")
    public String internalNotes(OrderEntity order, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = currentUserService.requireCurrentUser(jwt);
        authorizationService.requirePermission(user, Permission.ORDER_READ_INTERNAL);
        if (!user.tenantId().equals(order.getTenant().getId())) {
            throw new com.example.securitypoc.security.AuthorizationException();
        }
        return order.getInternalNotes();
    }

    @SchemaMapping(typeName = "Order", field = "customer")
    public UserEntity customer(OrderEntity order) {
        return order.getCustomer();
    }

    private OrderEntity findTenantOrder(Long id, String tenantId) {
        return orderRepository.findByIdAndTenant_Id(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException exception) {
            throw new NotFoundException("Order not found");
        }
    }

    private void validateCreateInput(CreateOrderInput input) {
        if (input == null || input.description() == null || input.description().isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        validateAmount(input.amount());
    }

    private void validateUpdateInput(UpdateOrderInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Input is required");
        }
        if (input.description() != null && input.description().isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        if (input.amount() != null) {
            validateAmount(input.amount());
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
