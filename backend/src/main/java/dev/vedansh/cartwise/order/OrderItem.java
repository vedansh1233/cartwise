package dev.vedansh.cartwise.order;

import java.math.BigDecimal;

import dev.vedansh.cartwise.catalog.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** A line of an order. Name and price are copied at purchase time so later price edits never change old orders. */
@Entity
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private CustomerOrder order;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Product product;

    @Column(nullable = false, length = 120)
    private String productName;

    @Column(length = 16)
    private String emoji;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {
    }

    OrderItem(CustomerOrder order, Product product, int quantity) {
        this.order = order;
        this.product = product;
        this.productName = product.getName();
        this.emoji = product.getEmoji();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public String getProductName() { return productName; }
    public String getEmoji() { return emoji; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
