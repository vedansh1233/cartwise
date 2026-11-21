package dev.vedansh.cartwise.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    /** Emoji shown on the category tile in the storefront. */
    @Column(length = 16)
    private String icon;

    protected Category() {
    }

    public Category(String name, String slug, String icon) {
        this.name = name;
        this.slug = slug;
        this.icon = icon;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getIcon() { return icon; }
}
