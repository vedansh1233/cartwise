package dev.vedansh.cartwise.catalog;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Column(nullable = false, length = 2000)
    private String description;

    /** Selling price in INR. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Original price, shown struck through when the product is on sale. */
    @Column(precision = 12, scale = 2)
    private BigDecimal mrp;

    @Column(nullable = false)
    private int stock;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Category category;

    @Column(length = 500)
    private String imageUrl;

    /** Emoji + accent colour used to render a product tile when there is no photo. */
    @Column(length = 16)
    private String emoji;

    @Column(length = 16)
    private String accent;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private double rating;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Version
    private long version;

    protected Product() {
    }

    public Product(String name, String slug, String description, BigDecimal price, BigDecimal mrp, int stock,
            Category category, String emoji, String accent, double rating) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.price = price;
        this.mrp = mrp;
        this.stock = stock;
        this.category = category;
        this.emoji = emoji;
        this.accent = accent;
        this.rating = rating;
    }

    /** Removes stock for an order; callers must hold a row lock on this product. */
    public void reserve(int qty) {
        if (qty > stock) {
            throw new IllegalStateException("insufficient stock");
        }
        stock -= qty;
    }

    public void restock(int qty) {
        stock += qty;
    }

    public void update(String name, String description, BigDecimal price, BigDecimal mrp, int stock,
            Category category, String imageUrl, String emoji, String accent, boolean active) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.mrp = mrp;
        this.stock = stock;
        this.category = category;
        this.imageUrl = imageUrl;
        this.emoji = emoji;
        this.accent = accent;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getMrp() { return mrp; }
    public int getStock() { return stock; }
    public Category getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public String getEmoji() { return emoji; }
    public String getAccent() { return accent; }
    public boolean isActive() { return active; }
    public double getRating() { return rating; }
    public Instant getCreatedAt() { return createdAt; }
}
