package com.example.tarimtakipbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "SensorTipleri")
public class SensorTipi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SensorTipiID")
    private Integer sensorTipiID;

    @Column(name = "TipAdi", nullable = false, unique = true, length = 100)
    private String tipAdi;

    @Column(name = "OlcumBirimi", length = 50)
    private String olcumBirimi;

    @Column(name = "Aciklama", length = 255)
    private String aciklama;
}