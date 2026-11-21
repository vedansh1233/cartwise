package dev.vedansh.cartwise.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    Page<CustomerOrder> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "user"})
    Optional<CustomerOrder> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"user"})
    Page<CustomerOrder> findByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<CustomerOrder> findAllBy(Pageable pageable);

    @Query("select coalesce(sum(o.total), 0) from CustomerOrder o where o.status <> dev.vedansh.cartwise.order.OrderStatus.CANCELLED")
    BigDecimal revenue();

    @Query("select o.status, count(o) from CustomerOrder o group by o.status")
    List<Object[]> countByStatus();

    @Query("""
            select i.productName, sum(i.quantity), sum(i.unitPrice * i.quantity)
            from OrderItem i where i.order.status <> dev.vedansh.cartwise.order.OrderStatus.CANCELLED
            group by i.productName order by sum(i.quantity) desc
            """)
    List<Object[]> topProducts(Pageable pageable);
}
