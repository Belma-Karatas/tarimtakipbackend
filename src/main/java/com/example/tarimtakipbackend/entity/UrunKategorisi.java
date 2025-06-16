package com.example.tarimtakipbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set; // Eğer Urunler ile çift yönlü ilişki kuracaksanız

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "UrunKategorileri")
public class UrunKategorisi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KategoriID")
    private Integer kategoriID;

    @Column(name = "KategoriAdi", nullable = false, unique = true, length = 100)
    private String kategoriAdi;

    // Opsiyonel: Eğer bir kategorideki tüm ürünleri UrunKategorisi üzerinden çekmek isterseniz
    // @OneToMany(mappedBy = "kategori", fetch = FetchType.LAZY)
    // private Set<Urun> urunler;
}