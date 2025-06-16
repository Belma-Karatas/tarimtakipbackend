package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data // BU ANOTASYON ÖNEMLİ!
@NoArgsConstructor
@AllArgsConstructor
public class GorevDetayDto {
    private Integer gorevID; // Bu alanın adı Thymeleaf'teki ile aynı olmalı (case-sensitive)
    private String faaliyetTipi;
    private String gorevAciklamasi;
    private Integer iliskiliEkimID;
    private String iliskiliTarlaAdi;
    private String iliskiliUrunAdi;
    private String atananKullanici;
    private String talepEdenKullanici;
    private LocalDateTime planlananBaslangicTarihi;
    private LocalDateTime planlananBitisTarihi;
    private LocalDateTime tamamlanmaTarihi;
    private String gorevDurumu;
    private Integer gorevOnceligi;
    private LocalDateTime gorevKayitTarihi;
}