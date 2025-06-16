package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Validasyon anotasyonları gerekirse eklenebilir
// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorOkumaFormDto {

    private Integer okumaID; // Güncelleme için (bu senaryoda genelde sadece ekleme olur)

    // @NotNull(message = "Sensör seçimi zorunludur.")
    private Integer sensorId;

    // @NotBlank(message = "Okuma zamanı boş olamaz.")
    private String okumaZamani; // HTML datetime-local input'tan String olarak gelir (yyyy-MM-ddTHH:mm)

    // @NotBlank(message = "Okunan değer boş olamaz.")
    private String deger;

    private String birim; // Opsiyonel, sensör tipine göre otomatik gelebilir
}