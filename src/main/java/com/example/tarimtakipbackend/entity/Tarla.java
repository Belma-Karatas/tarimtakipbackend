package com.example.tarimtakipbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Tarlalar")
public class Tarla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TarlaID")
    private Integer tarlaID;

    @Column(name = "TarlaAdi", nullable = false, length = 100)
    private String tarlaAdi;

    @Column(name = "Alan", precision = 10, scale = 2)
    private BigDecimal alan;

    @Column(name = "Aciklama", columnDefinition = "NVARCHAR(MAX)")
    private String aciklama;

    @ManyToOne(fetch = FetchType.LAZY) // İhtiyaç duyulduğunda yüklensin
    @JoinColumn(name = "ToprakTipiID") // Veritabanındaki FK sütunu
    private ToprakTipi toprakTipi; // ToprakTipi entity'sine referans

    @ManyToOne(fetch = FetchType.LAZY) // İhtiyaç duyulduğunda yüklensin
    @JoinColumn(name = "SulamaSistemiID") // Veritabanındaki FK sütunu
    private SulamaSistemi sulamaSistemi; // SulamaSistemi entity'sine referans

    @Column(name = "KayitTarihi", columnDefinition = "DATETIME2 DEFAULT GETDATE()", insertable = false, updatable = false)
    private LocalDateTime kayitTarihi;

    @Column(name = "GuncellemeTarihi", columnDefinition = "DATETIME2 NULL")
    private LocalDateTime guncellemeTarihi;
}