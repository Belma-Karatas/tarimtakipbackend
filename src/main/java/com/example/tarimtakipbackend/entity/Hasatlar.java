package com.example.tarimtakipbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Hasatlar")
public class Hasatlar { // Sınıf adını tablonuzla eşleşmesi için çoğul yaptım

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HasatID")
    private Integer hasatID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EkimID", nullable = false)
    private Ekim ekim; // Ekim entity'sine referans

    @Column(name = "HasatTarihi", nullable = false)
    private LocalDate hasatTarihi;

    @Column(name = "ToplananMiktar", nullable = false, precision = 12, scale = 2)
    private BigDecimal toplananMiktar;

    @Column(name = "Birim", nullable = false, length = 50)
    private String birim;

    @Column(name = "Kalite", length = 100) // Nullable
    private String kalite;

    @Column(name = "DepoBilgisi", length = 150) // Nullable
    private String depoBilgisi;

    @Column(name = "Maliyet", precision = 12, scale = 2) // Nullable
    private BigDecimal maliyet;

    @Column(name = "SatisFiyati", precision = 12, scale = 2) // Nullable
    private BigDecimal satisFiyati;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "KaydedenKullaniciID") // Nullable
    private Kullanici kaydedenKullanici; // Kullanici entity'sine referans

    @Column(name = "Notlar", columnDefinition = "NVARCHAR(MAX)") // Nullable
    private String notlar;

    @Column(name = "KayitTarihi", columnDefinition = "DATETIME2 DEFAULT GETDATE()", insertable = false, updatable = false)
    private LocalDateTime kayitTarihi;

    // Tablonuzdaki ON DELETE CASCADE kuralı JPA'da @OnDelete anotasyonu ile belirtilebilir,
    // ama genellikle veritabanı seviyesinde bırakmak daha iyidir.
    // Eğer özellikle JPA'nın bunu yönetmesini isterseniz:
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "EkimID", nullable = false)
    // @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    // private Ekim ekim;
    // Ancak bu Hibernate'e özgüdür ve veritabanındaki kural yeterlidir.
}