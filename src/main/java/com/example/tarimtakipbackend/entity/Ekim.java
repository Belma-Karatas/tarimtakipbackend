package com.example.tarimtakipbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Ekimler")
public class Ekim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EkimID")
    private Integer ekimID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TarlaID", nullable = false)
    private Tarla tarla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UrunID", nullable = false)
    private Urun urun;

    @Column(name = "EkimTarihi", nullable = false)
    private LocalDate ekimTarihi;

    @Column(name = "PlanlananHasatTarihi")
    private LocalDate planlananHasatTarihi;

    @Column(name = "EkilenMiktarAciklama", length = 150)
    private String ekilenMiktarAciklama;

    @Column(name = "Durum", length = 50) // DB'de DEFAULT 'Planlandı'
    private String durum;

    @Column(name = "Notlar", columnDefinition = "NVARCHAR(MAX)")
    private String notlar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "KaydedenKullaniciID") // Null olabilir
    private Kullanici kaydedenKullanici;

    @Column(name = "KayitTarihi", columnDefinition = "DATETIME2 DEFAULT GETDATE()", insertable = false, updatable = false)
    private LocalDateTime kayitTarihi;

    @Column(name = "GuncellemeTarihi", columnDefinition = "DATETIME2 NULL")
    private LocalDateTime guncellemeTarihi;
}