package com.example.tarimtakipbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GirdiTipleri")
public class GirdiTipi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GirdiTipiID")
    private Integer girdiTipiID;

    @Column(name = "TipAdi", nullable = false, unique = true, length = 100)
    private String tipAdi;

    @Column(name = "Aciklama", length = 255) // Nullable
    private String aciklama;

    // Eğer Girdiler ile çift yönlü ilişki kurmak isterseniz (bir GirdiTipi'ne ait tüm Girdiler):
    // @OneToMany(mappedBy = "girdiTipi", fetch = FetchType.LAZY)
    // private java.util.Set<Girdiler> girdiler;
}