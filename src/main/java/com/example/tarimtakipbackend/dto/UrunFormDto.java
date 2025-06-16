package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrunFormDto {
    private Integer urunID; // Güncelleme için
    private String urunAdi;
    private Integer kategoriId; // Formdan seçilecek KategoriID
    private String aciklama;
    private String birim;
}