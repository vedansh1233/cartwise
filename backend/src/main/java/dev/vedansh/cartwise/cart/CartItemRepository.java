package dev.vedansh.cartwise.cart;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = {"product", "product.category"})
    List<CartItem> findByUserIdOrderByIdAsc(Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    @Modifying
    @Query("delete from CartItem c where c.user.id = :userId")
    void deleteAllForUser(Long userId);
}
