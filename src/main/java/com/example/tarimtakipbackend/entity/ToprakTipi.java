package com.example.tarimtakipbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ToprakTipleri")
public class ToprakTipi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ToprakTipiID")
    private Integer toprakTipiID;

    @Column(name = "TipAdi", nullable = false, unique = true, length = 100)
    private String tipAdi;

    // Eğer Tarlalar ile çift yönlü ilişki kurmak istersen (ToprakTipinden Tarlalara)
    // @OneToMany(mappedBy = "toprakTipi")
    // private java.util.Set<Tarla> tarlalar;
}