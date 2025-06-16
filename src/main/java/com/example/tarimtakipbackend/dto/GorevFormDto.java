package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GorevFormDto {
    private Integer gorevID;
    private Integer ekimId; // İlişkili Ekim (opsiyonel)
    private Integer tarlaId; // İlişkili Tarla (opsiyonel, ekim yoksa)
    private Integer faaliyetTipiId;
    private String aciklama;
    private Integer atananKullaniciId;
    // TalepEdenKullaniciId genellikle giriş yapan kullanıcı olur, backend'de set edilebilir
    private String planlananBaslangicTarihi; // String, LocalDateTime'a çevrilecek
    private String planlananBitisTarihi;     // String, LocalDateTime'a çevrilecek
    private String tamamlanmaTarihi;         // String, LocalDateTime'a çevrilecek (güncellemede)
    private String durum;
    private Integer oncelik;
}