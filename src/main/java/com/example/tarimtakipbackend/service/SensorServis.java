package com.example.tarimtakipbackend.service;

import com.example.tarimtakipbackend.dto.SensorDetayDto;
import com.example.tarimtakipbackend.dto.SensorFormDto;
import com.example.tarimtakipbackend.dto.SensorOkumaDetayDto; // YENİ
import com.example.tarimtakipbackend.dto.SensorOkumaFormDto; // YENİ
import com.example.tarimtakipbackend.entity.Kullanici; // YENİ (getCurrentKullaniciId için)
import com.example.tarimtakipbackend.entity.Sensor;
import com.example.tarimtakipbackend.entity.SensorTipi;
import com.example.tarimtakipbackend.entity.Tarla;
import com.example.tarimtakipbackend.repository.KullaniciRepository; // YENİ
import com.example.tarimtakipbackend.repository.SensorRepository;
import com.example.tarimtakipbackend.repository.SensorTipiRepository;
import com.example.tarimtakipbackend.repository.TarlaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; // YENİ
import org.springframework.security.core.context.SecurityContextHolder; // YENİ
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // YENİ
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SensorServis {

    private static final Logger logger = LoggerFactory.getLogger(SensorServis.class);
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME; // YENİ

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private TarlaRepository tarlaRepository;

    @Autowired
    private SensorTipiRepository sensorTipiRepository;

    @Autowired // YENİ
    private KullaniciRepository kullaniciRepository;


    // --- MEVCUT YARDIMCI METOTLAR ---
    private LocalDate stringToLocalDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString);
        } catch (DateTimeParseException e) {
            logger.warn("stringToLocalDate: Geçersiz tarih formatı: '{}'. Beklenen format yyyy-MM-dd.", dateString, e);
            throw new IllegalArgumentException("Geçersiz tarih formatı. Lütfen yyyy-MM-dd formatında girin: " + dateString);
        }
    }

    private LocalDateTime stringToLocalDateTime(String dateTimeString) { // YENİ YARDIMCI METOT
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        try {
            // HTML datetime-local genellikle 'T' içerir, ISO_LOCAL_DATE_TIME bunu anlar.
            return LocalDateTime.parse(dateTimeString, ISO_DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.warn("stringToLocalDateTime: Geçersiz tarih-saat formatı: '{}'. Beklenen format yyyy-MM-ddTHH:mm.", dateTimeString, e);
            throw new IllegalArgumentException("Geçersiz tarih-saat formatı. Lütfen yyyy-MM-ddTHH:mm formatında girin: " + dateTimeString);
        }
    }

    private LocalDate toLocalDateFromDb(Object dbDate) {
        if (dbDate == null) return null;
        if (dbDate instanceof java.sql.Date) {
            return ((java.sql.Date) dbDate).toLocalDate();
        }
        if (dbDate instanceof java.sql.Timestamp) { // SP'den DATE alanı bazen Timestamp olarak gelebilir
            return ((java.sql.Timestamp) dbDate).toLocalDateTime().toLocalDate();
        }
        logger.warn("toLocalDateFromDb: Desteklenmeyen tarih tipi: {}", dbDate.getClass().getName());
        return null;
    }

    private LocalDateTime toLocalDateTimeFromDb(Object dbTimestamp) {
        if (dbTimestamp == null) return null;
        if (dbTimestamp instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) dbTimestamp).toLocalDateTime();
        }
         // SP'den DATETIME2 bazen String olarak gelebilir, özellikle NativeQuery ile Object[] alırken
        if (dbTimestamp instanceof String) {
            try {
                String dateStr = (String) dbTimestamp;
                 // Milisaniye kısmını ve varsa Z (UTC) bilgisini kaldır
                if (dateStr.contains(".")) {
                    dateStr = dateStr.substring(0, dateStr.lastIndexOf('.'));
                }
                dateStr = dateStr.replace("Z", "");
                // SQL Server'dan gelen format genellikle 'yyyy-MM-dd HH:mm:ss' şeklindedir.
                // Bunu LocalDateTime.parse'ın anlayacağı 'yyyy-MM-ddTHH:mm:ss' formatına çevir.
                return LocalDateTime.parse(dateStr.replace(" ", "T"));
            } catch (DateTimeParseException e) {
                logger.warn("String'den LocalDateTime'a çevirme hatası (toLocalDateTimeFromDb): '{}'", dbTimestamp, e);
                // Eğer format hatası devam ederse, SP'nin tam çıktısını loglayıp formatı kontrol etmek gerekebilir.
            }
        }
        logger.warn("toLocalDateTimeFromDb: Desteklenmeyen tarih-saat tipi: {}", dbTimestamp.getClass().getName());
        return null;
    }

    private Integer getCurrentKullaniciId() { // YENİ YARDIMCI METOT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            String username = authentication.getName();
            Optional<Kullanici> kullaniciOpt = kullaniciRepository.findByKullaniciAdi(username);
            return kullaniciOpt.map(Kullanici::getKullaniciID).orElse(null);
        }
        logger.warn("Giriş yapmış kullanıcı bulunamadı veya anonymousUser.");
        return null;
    }

    // --- MEVCUT SENSÖR YÖNETİMİ METOTLARI (Listeleme ve Getirme) ---
    @SuppressWarnings("unchecked")
    public List<SensorDetayDto> getAllSensorlerSP() {
        logger.info("getAllSensorlerSP çağrıldı.");
        try {
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spSensor_Listele").getResultList();
            logger.info("spSensor_Listele {} adet sonuç döndürdü.", results.size());
            return results.stream().map(row -> {
                try {
                    return new SensorDetayDto(
                            (Integer) row[0], (String) row[1], (Integer) row[2], (String) row[3],
                            (String) row[4], (String) row[5], (String) row[6], toLocalDateFromDb(row[7]),
                            (String) row[8], (row[9] instanceof Boolean ? (Boolean) row[9] : (row[9] instanceof Number ? ((Number)row[9]).intValue() == 1 : null)),
                            toLocalDateFromDb(row[10]), toLocalDateTimeFromDb(row[11])
                    );
                } catch (Exception e) {
                    logger.error("SensorDetayDto map'leme sırasında hata oluştu. Satır: {}", (Object)row != null ? java.util.Arrays.toString(row) : "null", e);
                    return null;
                }
            }).filter(dto -> dto != null).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("getAllSensorlerSP sırasında genel bir hata oluştu: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<SensorDetayDto> getSensorDetayByIdSP(Integer id) {
        logger.info("getSensorDetayByIdSP çağrıldı ID: {}", id);
        if (id == null) return Optional.empty();
        try {
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spSensor_GetirByID @SensorID = :id")
                    .setParameter("id", id)
                    .getResultList();
            if (results.isEmpty()) {
                logger.info("getSensorDetayByIdSP: Sensör bulunamadı ID: {}", id);
                return Optional.empty();
            }
            Object[] row = results.get(0);
            return Optional.of(new SensorDetayDto(
                    (Integer) row[0], (String) row[1], (Integer) row[2], (String) row[3],
                    (String) row[4], (String) row[5], (String) row[6], toLocalDateFromDb(row[7]),
                    (String) row[8], (row[9] instanceof Boolean ? (Boolean) row[9] : (row[9] instanceof Number ? ((Number)row[9]).intValue() == 1 : null)),
                    toLocalDateFromDb(row[10]), toLocalDateTimeFromDb(row[11])
            ));
        } catch (Exception e) {
            logger.error("getSensorDetayByIdSP sırasında hata oluştu ID {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<Sensor> findSensorEntityById(Integer id) {
        if (id == null) return Optional.empty();
        return sensorRepository.findById(id);
    }

    // --- SENSÖR EKLEME/SİLME (Düzeltilmiş) ---
    @Transactional
    public Sensor saveSensorSP(SensorFormDto sensorDto) {
        logger.info("saveSensorSP çağrılıyor. Sensör Kodu: {}", sensorDto.getSensorKodu());
        LocalDate kurulumTarihiDate = stringToLocalDate(sensorDto.getKurulumTarihi());
        LocalDate sonBakimTarihiDate = stringToLocalDate(sensorDto.getSonBakimTarihi());

        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spSensor_Ekle " +
                        "@SensorKodu = :sensorKodu, @TarlaID = :tarlaID, @SensorTipiID = :sensorTipiID, " +
                        "@MarkaModel = :markaModel, @KurulumTarihi = :kurulumTarihi, " +
                        "@KonumAciklamasi = :konumAciklamasi, @AktifMi = :aktifMi, @SonBakimTarihi = :sonBakimTarihi")
                .setParameter("sensorKodu", sensorDto.getSensorKodu())
                .setParameter("tarlaID", sensorDto.getTarlaId())
                .setParameter("sensorTipiID", sensorDto.getSensorTipiId())
                .setParameter("markaModel", sensorDto.getMarkaModel())
                .setParameter("kurulumTarihi", kurulumTarihiDate != null ? java.sql.Date.valueOf(kurulumTarihiDate) : null)
                .setParameter("konumAciklamasi", sensorDto.getKonumAciklamasi())
                .setParameter("aktifMi", sensorDto.getAktifMi() != null ? sensorDto.getAktifMi() : true)
                .setParameter("sonBakimTarihi", sonBakimTarihiDate != null ? java.sql.Date.valueOf(sonBakimTarihiDate) : null)
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
             if (result instanceof Number) { // SP tek bir sayısal değer (ID veya hata kodu) döndürüyorsa
                Integer resultCodeOrId = ((Number) result).intValue();
                if (resultCodeOrId > 0) { // Başarılı, dönen değer YeniSensorID
                    Sensor yeniSensor = new Sensor();
                    yeniSensor.setSensorID(resultCodeOrId);
                    yeniSensor.setSensorKodu(sensorDto.getSensorKodu());
                    Tarla tarla = tarlaRepository.findById(sensorDto.getTarlaId())
                            .orElseThrow(() -> new IllegalArgumentException("Geçersiz Tarla ID: " + sensorDto.getTarlaId()));
                    yeniSensor.setTarla(tarla);
                    SensorTipi sensorTipi = sensorTipiRepository.findById(sensorDto.getSensorTipiId())
                            .orElseThrow(() -> new IllegalArgumentException("Geçersiz Sensör Tipi ID: " + sensorDto.getSensorTipiId()));
                    yeniSensor.setSensorTipi(sensorTipi);
                    yeniSensor.setMarkaModel(sensorDto.getMarkaModel());
                    yeniSensor.setKurulumTarihi(kurulumTarihiDate);
                    yeniSensor.setKonumAciklamasi(sensorDto.getKonumAciklamasi());
                    yeniSensor.setAktifMi(sensorDto.getAktifMi() != null ? sensorDto.getAktifMi() : true);
                    yeniSensor.setSonBakimTarihi(sonBakimTarihiDate);
                    logger.info("SP ile yeni sensör eklendi, ID: {}", resultCodeOrId);
                    return yeniSensor;
                } else if (resultCodeOrId == -2) throw new IllegalArgumentException("Sensör eklenemedi: Bu sensör kodu zaten mevcut.");
                else if (resultCodeOrId == -3) throw new IllegalArgumentException("Sensör eklenemedi: Geçersiz Tarla ID.");
                else if (resultCodeOrId == -4) throw new IllegalArgumentException("Sensör eklenemedi: Geçersiz Sensör Tipi ID.");
                else throw new RuntimeException("Sensör eklenemedi. SP bilinmeyen bir hata kodu döndürdü: " + resultCodeOrId);
            } else {
                logger.error("spSensor_Ekle'den beklenmeyen sonuç tipi: {}", result.getClass().getName());
                throw new RuntimeException("Sensör eklenemedi (SP'den beklenmeyen sonuç tipi).");
            }
        }
        throw new RuntimeException("Sensör eklenemedi (SP'den sonuç alınamadı veya boş sonuç döndü).");
    }

    @Transactional // GÜNCELLENMİŞ
    public boolean deleteSensorSP(Integer id) {
        logger.info("deleteSensorSP çağrılıyor ID: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("Silinecek sensör için ID belirtilmelidir.");
        }
        // Varsayım: spSensor_Sil, başarılıysa 1, hata durumunda negatif bir kod veya 0 döndürür.
        // Eğer SP'niz farklı bir yapıda sonuç döndürüyorsa bu kısım ona göre ayarlanmalı.
        // Şimdilik spSensor_Sil'in spTarla_Sil gibi bir yapıda olduğunu varsayıyorum.
        // Eğer spSensor_Sil sadece başarılı/başarısız bir mesaj döndürüyorsa veya hiç sonuç döndürmüyorsa
        // ve hata durumunda SQL EXCEPTION fırlatıyorsa, bu kod farklı olmalı.
        // Biz spTarla_Sil gibi kod döndürdüğünü varsayalım.
        // GERÇEK spSensor_Sil yok, bu yüzden bir örnek yapıyorum:
        // IF EXISTS (SELECT 1 FROM dbo.SensorOkumalari WHERE SensorID = @SensorID)
        // BEGIN SELECT -2 AS Sonuc; RETURN; END -- İlişkili okuma var, silinemez
        // DELETE FROM dbo.Sensorler WHERE SensorID = @SensorID; SELECT 1 AS Sonuc;

        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spSensor_Sil @SensorID = :sensorID") // VARSAYIMSAL SP ADI
                .setParameter("sensorID", id)
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer resultCode = ((Number) result).intValue();
                if (resultCode == 1) {
                    logger.info("SP ile sensör silindi, ID: {}", id);
                    return true;
                } else if (resultCode == -1) throw new RuntimeException("Sensör silinemedi: Sensör bulunamadı.");
                else if (resultCode == -2) throw new RuntimeException("Sensör silinemedi: Bu sensöre ait okuma kayıtları var.");
                // Diğer hata kodları...
                else throw new RuntimeException("Sensör silinemedi. SP bilinmeyen bir hata kodu döndürdü: " + resultCode);
            } else {
                 logger.error("spSensor_Sil'den beklenmeyen sonuç tipi: {}", result.getClass().getName());
                 throw new RuntimeException("Sensör silinemedi (SP'den beklenmeyen sonuç tipi).");
            }
        }
        throw new RuntimeException("Sensör silinemedi (SP'den sonuç alınamadı). Lütfen spSensor_Sil adında bir SP oluşturduğunuzdan emin olun.");
    }


    // --- YENİ SENSÖR OKUMA METOTLARI ---

    @Transactional
    public SensorOkumaDetayDto saveSensorOkumaSP(SensorOkumaFormDto formDto) {
        Integer girenKullaniciId = getCurrentKullaniciId();
        logger.info("saveSensorOkumaSP çağrılıyor. SensorID: {}, GirenKullaniciID: {}", formDto.getSensorId(), girenKullaniciId);

        if (formDto.getSensorId() == null) throw new IllegalArgumentException("Sensör ID boş olamaz.");
        if (formDto.getOkumaZamani() == null || formDto.getOkumaZamani().trim().isEmpty()) throw new IllegalArgumentException("Okuma zamanı boş olamaz.");
        if (formDto.getDeger() == null || formDto.getDeger().trim().isEmpty()) throw new IllegalArgumentException("Okuma değeri boş olamaz.");

        LocalDateTime okumaZamaniLocalDateTime = stringToLocalDateTime(formDto.getOkumaZamani());

        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spSensorOkuma_Ekle " +
                        "@SensorID = :sensorID, @OkumaZamani = :okumaZamani, @Deger = :deger, " +
                        "@Birim = :birim, @GirenKullaniciID = :girenKID")
                .setParameter("sensorID", formDto.getSensorId())
                .setParameter("okumaZamani", okumaZamaniLocalDateTime) // SQL DATETIME2 için LocalDateTime
                .setParameter("deger", formDto.getDeger())
                .setParameter("birim", formDto.getBirim())
                .setParameter("girenKID", girenKullaniciId)
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer yeniOkumaId = ((Number) result).intValue();
                if (yeniOkumaId > 0) {
                    logger.info("SP ile yeni sensör okuması eklendi, ID: {}", yeniOkumaId);
                    // Kaydedilen okumayı detaylarıyla döndürmek için tekrar çekebiliriz.
                    // Şimdilik basitçe yeni ID'yi kullanarak bir DTO oluşturup döndürelim veya SP'nin çıktısını kullanalım.
                    // Hızlı olması için direkt ID ile basit bir DTO döndürüyorum, normalde tekrar çekmek daha iyi.
                    // VEYA spSensorOkuma_GetirBySensorID'yi çağırıp ilk elemanı alabiliriz.
                    // Şimdilik sadece ID'yi kullanarak ve formdan gelenlerle DTO oluşturalım.
                    // SP'miz ID döndürüyor. Kaydedilen veriyi tüm detaylarıyla geri almak için ek sorgu gerekir.
                    // Ya da spSensorOkuma_Ekle SP'si tüm detayları döndürecek şekilde güncellenebilir.
                    // Şu an için sadece temel bir dönüş yapalım.
                    // Gerçekte, getOkumalarBySensorIdSP(formDto.getSensorId()) çağırıp ilk elemanı bulmak daha doğru olurdu.
                    // Ya da bu metodun dönüş tipini void yapıp sadece ID loglanabilir.
                    // Controller'da zaten yönlendirme yapılacak.

                    // Basit bir DTO döndürelim (SP sadece ID döndürdüğü için)
                    SensorOkumaDetayDto detayDto = new SensorOkumaDetayDto();
                    detayDto.setOkumaID(yeniOkumaId);
                    detayDto.setSensorID(formDto.getSensorId());
                    detayDto.setOkumaZamani(okumaZamaniLocalDateTime);
                    detayDto.setDeger(formDto.getDeger());
                    detayDto.setOkumaBirimi(formDto.getBirim());
                    if (girenKullaniciId != null) {
                        kullaniciRepository.findById(girenKullaniciId).ifPresent(k -> detayDto.setGirenKullanici(k.getAd() + " " + k.getSoyad()));
                    }
                    // Diğer alanlar (sensorKodu, tarlaAdi vb.) için ek sorgu gerekir. Şimdilik null kalacaklar.
                    return detayDto;

                } else if (yeniOkumaId == -2) throw new IllegalArgumentException("Sensör okuması eklenemedi: Geçersiz Sensör ID.");
                else if (yeniOkumaId == -3) throw new IllegalArgumentException("Sensör okuması eklenemedi: Geçersiz Giren Kullanıcı ID.");
                else throw new RuntimeException("Sensör okuması eklenemedi. SP bilinmeyen bir hata kodu döndürdü: " + yeniOkumaId);
            } else {
                logger.error("spSensorOkuma_Ekle'den beklenmeyen sonuç tipi: {}", result.getClass().getName());
                throw new RuntimeException("Sensör okuması eklenemedi (SP'den beklenmeyen sonuç tipi).");
            }
        }
        throw new RuntimeException("Sensör okuması eklenemedi (SP'den sonuç alınamadı).");
    }

    @SuppressWarnings("unchecked")
    public List<SensorOkumaDetayDto> getOkumalarBySensorIdSP(Integer sensorId, Integer adet) {
        logger.info("getOkumalarBySensorIdSP çağrıldı. SensorID: {}, Adet: {}", sensorId, adet);
        if (sensorId == null) {
            logger.warn("getOkumalarBySensorIdSP: sensorId null geldi.");
            return Collections.emptyList();
        }
        try {
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spSensorOkuma_GetirBySensorID @SensorID = :sensorID, @Adet = :adet")
                    .setParameter("sensorID", sensorId)
                    .setParameter("adet", adet != null ? adet : 100) // Varsayılan adet
                    .getResultList();

            logger.info("spSensorOkuma_GetirBySensorID (SensorID: {}) {} adet sonuç döndürdü.", sensorId, results.size());

            return results.stream().map(row -> {
                try {
                    return new SensorOkumaDetayDto(
                            // SP'nizin döndürdüğü sütun sırasına göre
                            (row[0] instanceof Number ? ((Number)row[0]).intValue() : null), // OkumaID (BIGINT ise Long)
                            (Integer) row[1],        // SensorID
                            (String) row[2],         // SensorKodu
                            (String) row[3],         // TarlaAdi
                            (String) row[4],         // SensorTipi
                            toLocalDateTimeFromDb(row[5]), // OkumaZamani
                            (String) row[6],         // Deger
                            (String) row[7],         // OkumaBirimi
                            (String) row[8],         // GirenKullanici
                            toLocalDateTimeFromDb(row[9])  // KayitTarihi
                    );
                } catch (Exception e) {
                    logger.error("SensorOkumaDetayDto map'leme sırasında hata. Satır: {}", java.util.Arrays.toString(row), e);
                    return null;
                }
            }).filter(dto -> dto != null).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("getOkumalarBySensorIdSP sırasında hata oluştu. SensorID {}: {}", sensorId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // TODO: Gerekirse spSensorOkuma_ListeleSonOkumalar için de bir metot eklenebilir.
}