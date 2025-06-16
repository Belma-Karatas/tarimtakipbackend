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
@Table(name = "Girdiler") // Veritabanındaki tablo adı
public class Girdiler { // Sınıf adını Girdiler olarak bıraktım, çoğul olması DDL ile uyumlu.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GirdiID")
    private Integer girdiID;

    @Column(name = "GirdiAdi", nullable = false, length = 150)
    private String girdiAdi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GirdiTipiID") // Nullable olabilir
    private GirdiTipi girdiTipi; // GirdiTipi entity'sine referans

    @Column(name = "Birim", nullable = false, length = 50)
    private String birim;

    @Column(name = "Aciklama", columnDefinition = "NVARCHAR(MAX)") // Nullable
    private String aciklama;

    @Column(name = "StokTakibiYapilsinMi", columnDefinition = "BIT DEFAULT 0")
    private Boolean stokTakibiYapilsinMi = false;

    @Column(name = "KayitTarihi", columnDefinition = "DATETIME2 DEFAULT GETDATE()", insertable = false, updatable = false)
    private LocalDateTime kayitTarihi;

    // Eğer GirdiTipleri entity'si yoksa onu da oluşturmamız gerekebilir.
    // GirdiTipleri tablosu için DDL'niz vardı. Onun için de bir entity olmalı.
}