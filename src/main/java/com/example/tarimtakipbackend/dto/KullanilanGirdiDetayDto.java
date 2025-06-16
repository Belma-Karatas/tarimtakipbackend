package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate; // Kullanım Tarihi için
import java.time.LocalDateTime; // Kayıt Tarihi için (eğer view'da varsa)

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KullanilanGirdiDetayDto {
    private Integer kullanimID;
    private String girdiAdi;
    private String girdiTipi; // GirdiTipleri.TipAdi
    private String girdiBirimi; // Girdiler.Birim
    private LocalDate kullanimTarihi; // SQL DATE -> LocalDate
    private BigDecimal miktar;
    private BigDecimal kullanilanGirdiMaliyeti; // KullanilanGirdiler.Maliyet
    private String kaydedenKullanici; // Kullanicilar.Ad + ' ' + Kullanicilar.Soyad
    private String kullanimNotlari;
    private Integer iliskiliEkimID;
    private String iliskiliTarlaAdi;
    private String iliskiliUrunAdi;
    private Integer iliskiliGorevID;
    private String iliskiliGorevFaaliyeti; // FaaliyetTipleri.TipAdi (görev üzerinden)
    private LocalDateTime kayitTarihi; // KullanilanGirdiler.KayitTarihi (eğer view'da varsa)

    // Eğer vKullanilanGirdiDetaylari view'ınızın veya spKullanilanGirdi_Listele SP'nizin
    // döndürdüğü sütunlar farklıysa, bu DTO'yu ona göre güncellememiz gerekebilir.
    // Özellikle tarih tipleri (DATE vs DATETIME2) ve sayısal tipler (DECIMAL vs INT) önemlidir.
}