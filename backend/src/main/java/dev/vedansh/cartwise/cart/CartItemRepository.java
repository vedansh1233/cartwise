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

    /** Ids only, so no product is loaded before checkout has locked it. Ascending, which is the lock order. */
    @Query("select c.product.id from CartItem c where c.user.id = :userId order by c.product.id")
    List<Long> findProductIdsForUser(Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    @Modifying
    @Query("delete from CartItem c where c.user.id = :userId")
    void deleteAllForUser(Long userId);
}
