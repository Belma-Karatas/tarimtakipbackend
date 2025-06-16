package com.example.tarimtakipbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Gorevler")
public class Gorev {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GorevID")
    private Integer gorevID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EkimID") // Null olabilir
    private Ekim ekim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TarlaID") // Null olabilir (ama EkimID veya TarlaID'den biri olmalı)
    private Tarla tarla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FaaliyetTipiID", nullable = false)
    private FaaliyetTipi faaliyetTipi;

    @Column(name = "Aciklama", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String aciklama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AtananKullaniciID") // Null olabilir
    private Kullanici atananKullanici;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TalepEdenKullaniciID") // Null olabilir
    private Kullanici talepEdenKullanici;

    @Column(name = "PlanlananBaslangicTarihi")
    private LocalDateTime planlananBaslangicTarihi;

    @Column(name = "PlanlananBitisTarihi")
    private LocalDateTime planlananBitisTarihi;

    @Column(name = "TamamlanmaTarihi")
    private LocalDateTime tamamlanmaTarihi;

    @Column(name = "Durum", length = 50) // DB'de DEFAULT 'Atandı'
    private String durum;

    @Column(name = "Oncelik") // DB'de DEFAULT 3
    private Integer oncelik;

    @Column(name = "KayitTarihi", columnDefinition = "DATETIME2 DEFAULT GETDATE()", insertable = false, updatable = false)
    private LocalDateTime kayitTarihi;

    @Column(name = "GuncellemeTarihi", columnDefinition = "DATETIME2 NULL")
    private LocalDateTime guncellemeTarihi;
}