package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Validasyon anotasyonları gerekirse eklenebilir (örn: @NotBlank, @NotNull)
// import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorFormDto {
    private Integer sensorID; // Düzenleme için, yeni kayıtta null

    // @NotBlank(message = "Sensör kodu boş olamaz")
    private String sensorKodu;

    // @NotNull(message = "Tarla seçimi zorunludur")
    private Integer tarlaId; // Formdan seçilecek TarlaID

    // @NotNull(message = "Sensör tipi seçimi zorunludur")
    private Integer sensorTipiId; // Formdan seçilecek SensorTipiID

    private String markaModel;
    private String kurulumTarihi; // HTML date input'tan String olarak gelir, sonra LocalDate'e çevrilir
    private String konumAciklamasi;
    private Boolean aktifMi = true; // Varsayılan olarak true
    private String sonBakimTarihi; // HTML date input'tan String olarak gelir
}