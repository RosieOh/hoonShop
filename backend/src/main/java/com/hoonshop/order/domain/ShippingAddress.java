package com.hoonshop.order.domain;

import com.hoonshop.common.domain.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * 배송지 값 객체.
 *
 * <p>주문에 <b>복사</b>됩니다. 회원 주소록을 참조만 하면, 고객이 나중에 주소를 수정했을 때
 * 과거 주문의 배송지까지 바뀌어 "어디로 보냈는지"를 알 수 없게 됩니다.
 */
@Embeddable
public final class ShippingAddress implements Serializable {

    private static final Pattern PHONE = Pattern.compile("^01[016789]-?\\d{3,4}-?\\d{4}$");
    private static final Pattern ZIPCODE = Pattern.compile("^\\d{5}$");

    @Column(name = "recipient", nullable = false, length = 40)
    private String recipient;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "zipcode", nullable = false, length = 10)
    private String zipcode;

    @Column(name = "address1", nullable = false, length = 200)
    private String address1;

    @Column(name = "address2", length = 200)
    private String address2;

    protected ShippingAddress() {
    }

    private ShippingAddress(String recipient, String phone, String zipcode, String address1,
                            String address2) {
        if (recipient == null || recipient.trim().length() < 2) {
            throw new DomainException("INVALID_ADDRESS", "받는 분 이름을 2자 이상 입력해 주세요.");
        }
        if (phone == null || !PHONE.matcher(phone.trim()).matches()) {
            throw new DomainException("INVALID_ADDRESS",
                    "휴대폰 번호 11자리를 입력해 주세요. 예: 010-1234-5678");
        }
        if (zipcode == null || !ZIPCODE.matcher(zipcode.trim()).matches()) {
            throw new DomainException("INVALID_ADDRESS", "우편번호는 숫자 5자리입니다.");
        }
        if (address1 == null || address1.isBlank()) {
            throw new DomainException("INVALID_ADDRESS", "기본 주소를 입력해 주세요.");
        }
        this.recipient = recipient.trim();
        this.phone = phone.trim();
        this.zipcode = zipcode.trim();
        this.address1 = address1.trim();
        this.address2 = address2 == null ? "" : address2.trim();
    }

    public static ShippingAddress of(String recipient, String phone, String zipcode,
                                     String address1, String address2) {
        return new ShippingAddress(recipient, phone, zipcode, address1, address2);
    }

    public String recipient() {
        return recipient;
    }

    public String phone() {
        return phone;
    }

    public String zipcode() {
        return zipcode;
    }

    public String address1() {
        return address1;
    }

    public String address2() {
        return address2;
    }
}
