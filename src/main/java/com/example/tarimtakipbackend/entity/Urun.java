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
@Table(name = "Urunler")
public class Urun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UrunID")
    private Integer urunID;

    @Column(name = "UrunAdi", nullable = false, length = 100)
    private String urunAdi;

    @ManyToOne(fetch = FetchType.LAZY) // Ürün bilgisi çekildiğinde kategori hemen yüklenmeyebilir (performans)
    @JoinColumn(name = "KategoriID") // Veritabanındaki FK sütununun adı
    private UrunKategorisi kategori; // UrunKategorisi entity'sine referans

    @Column(name = "Aciklama", columnDefinition = "NVARCHAR(MAX)")
    private String aciklama;

    @Column(name = "Birim", length = 50)
    private String birim;

    @Column(name = "KayitTarihi", columnDefinition = "DATETIME2 DEFAULT GETDATE()", insertable = false, updatable = false)
    private LocalDateTime kayitTarihi;

    // GuncellemeTarihi sütunu Urunler tablonuzda varsa ekleyebilirsiniz:
    // @Column(name = "GuncellemeTarihi", columnDefinition = "DATETIME2 NULL")
    // private LocalDateTime guncellemeTarihi;
}