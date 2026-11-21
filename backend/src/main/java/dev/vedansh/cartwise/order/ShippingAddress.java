package dev.vedansh.cartwise.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ShippingAddress {

    @Column(nullable = false, length = 80)
    private String fullName;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false, length = 200)
    private String line1;

    @Column(nullable = false, length = 60)
    private String city;

    @Column(nullable = false, length = 60)
    private String state;

    @Column(nullable = false, length = 6)
    private String pincode;

    protected ShippingAddress() {
    }

    public ShippingAddress(String fullName, String phone, String line1, String city, String state, String pincode) {
        this.fullName = fullName;
        this.phone = phone;
        this.line1 = line1;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
    }

    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getLine1() { return line1; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPincode() { return pincode; }
}
