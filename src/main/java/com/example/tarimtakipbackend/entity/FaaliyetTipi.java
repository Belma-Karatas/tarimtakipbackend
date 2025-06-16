package com.example.tarimtakipbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "FaaliyetTipleri")
public class FaaliyetTipi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FaaliyetTipiID")
    private Integer faaliyetTipiID;

    @Column(name = "TipAdi", nullable = false, unique = true, length = 100)
    private String tipAdi;

    @Column(name = "Aciklama", length = 255)
    private String aciklama;
}