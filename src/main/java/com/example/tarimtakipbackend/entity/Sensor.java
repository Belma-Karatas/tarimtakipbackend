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
@Table(name = "Sensorler")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SensorID")
    private Integer sensorID;

    @Column(name = "SensorKodu", nullable = false, unique = true, length = 50)
    private String sensorKodu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TarlaID", nullable = false)
    private Tarla tarla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SensorTipiID", nullable = false)
    private SensorTipi sensorTipi;

    @Column(name = "MarkaModel", length = 100)
    private String markaModel;

    @Column(name = "KurulumTarihi")
    private LocalDate kurulumTarihi;

    @Column(name = "KonumAciklamasi", length = 255)
    private String konumAciklamasi;

    @Column(name = "AktifMi", columnDefinition = "BIT DEFAULT 1")
    private Boolean aktifMi = true;

    @Column(name = "SonBakimTarihi")
    private LocalDate sonBakimTarihi;

    @Column(name = "KayitTarihi", columnDefinition = "DATETIME2 DEFAULT GETDATE()", insertable = false, updatable = false)
    private LocalDateTime kayitTarihi;
}
