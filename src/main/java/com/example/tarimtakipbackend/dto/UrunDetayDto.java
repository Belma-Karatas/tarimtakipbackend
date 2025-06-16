package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrunDetayDto {
    private Integer urunID;
    private String urunAdi;
    private Integer kategoriId;
    private String kategoriAdi;
    private String urunBirimi;
    private String urunAciklamasi;
    private LocalDateTime urunKayitTarihi; // View'da bu varsa
    // View'ınızdaki diğer sütunlar varsa buraya ekleyin
}