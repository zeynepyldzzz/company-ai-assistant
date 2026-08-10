package com.company.assistant.directory;
import com.company.assistant.auth.Role;

import jakarta.persistence.*;

@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * A-35 (#196): ad ve soyad ayri kolonlar. Tek {@code name} kolonu, rehberin ada gore
     * siralanmasina ve A-34'un "kelime basi" aramasinin bosluk karakteriyle taklit
     * edilmesine sebep oluyordu.
     */
    @Column(name = "first_name", nullable = false)
    private String firstName;

    /**
     * NULL olabilir: V49 oncesinde tek kelimeli kaydedilmis calisanlar var (hepsi test
     * hesabi). YENI kayitlarda zorunlu — kural {@code AdminEmployeeRequest} tarafinda.
     */
    @Column(name = "last_name")
    private String lastName;

    private String email;

    private String phone;

    @Column(name = "office_status")
    private String officeStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled = false;

    /** A-29 (#178): sistem tarafindan uretilen gecici sifrede true; kullanici kendi sifresini belirleyince false. */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;


    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    /**
     * A-35 (#196): tam ad TURETILIR, saklanmaz. Boylece rehber, chatbot resolver'lari,
     * DTO'lar ve PDF uretimi dahil butun {@code getName()} cagiranlari degismeden calisir.
     *
     * <p>Soyadi olmayan kayitta sondaki bosluk BIRAKILMAZ; aksi halde adlar arayuzde
     * gorunmeyen bir bosluk tasir ve esitlik karsilastirmalari sessizce kayar.
     *
     * <p>Bu bir JPA alani DEGIL: {@code @Transient} olarak isaretli, JPQL'den erisilemez.
     * Sorgular {@code firstName}/{@code lastName} uzerinden yazilir.
     */
    @Transient
    public String getName() {
        return lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName;
    }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getOfficeStatus() { return officeStatus; }
    public void setOfficeStatus(String officeStatus) { this.officeStatus = officeStatus; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    public boolean isTotpEnabled() { return totpEnabled; }
    public void setTotpEnabled(boolean totpEnabled) { this.totpEnabled = totpEnabled; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
}
