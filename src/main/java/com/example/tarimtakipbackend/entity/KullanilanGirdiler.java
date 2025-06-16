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
@Table(name = "KullanilanGirdiler")
public class KullanilanGirdiler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KullanimID")
    private Integer kullanimID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EkimID") // Nullable
    private Ekim ekim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GorevID") // Nullable
    private Gorev gorev;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GirdiID", nullable = false)
    private Girdiler girdi; // Entity adınızın Girdiler olduğunu varsayıyorum. Eğer Girdi ise Girdi yapın.

    @Column(name = "KullanimTarihi", nullable = false)
    private LocalDate kullanimTarihi;

    @Column(name = "Miktar", nullable = false, precision = 10, scale = 2)
    private BigDecimal miktar;

    @Column(name = "Maliyet", precision = 12, scale = 2) // Nullable
    private BigDecimal maliyet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "KaydedenKullaniciID") // Nullable
    private Kullanici kaydedenKullanici;

    @Column(name = "Notlar", length = 500) // Nullable
    private String notlar;

    @Column(name = "KayitTarihi", columnDefinition = "DATETIME2 DEFAULT GETDATE()", insertable = false, updatable = false)
    private LocalDateTime kayitTarihi;

    // DB'deki CHECK kısıtlaması (CK_Kullanim_EkimVeyaGorev) JPA seviyesinde direkt uygulanmaz,
    // bu mantık servis katmanında veya DTO validasyonu ile kontrol edilmelidir.
}