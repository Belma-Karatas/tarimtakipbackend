package com.example.tarimtakipbackend.service;

import com.example.tarimtakipbackend.dto.SensorDetayDto;
import com.example.tarimtakipbackend.dto.SensorFormDto;
import com.example.tarimtakipbackend.entity.Sensor;
import com.example.tarimtakipbackend.entity.SensorTipi;
import com.example.tarimtakipbackend.entity.Tarla;
import com.example.tarimtakipbackend.repository.SensorRepository;
import com.example.tarimtakipbackend.repository.SensorTipiRepository;
import com.example.tarimtakipbackend.repository.TarlaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SensorServis {

    private static final Logger logger = LoggerFactory.getLogger(SensorServis.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private TarlaRepository tarlaRepository;

    @Autowired
    private SensorTipiRepository sensorTipiRepository;

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

    private LocalDate toLocalDateFromDb(Object dbDate) {
        if (dbDate == null) return null;
        if (dbDate instanceof java.sql.Date) {
            return ((java.sql.Date) dbDate).toLocalDate();
        }
        if (dbDate instanceof java.sql.Timestamp) {
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
        logger.warn("toLocalDateTimeFromDb: Desteklenmeyen tarih-saat tipi: {}", dbTimestamp.getClass().getName());
        return null;
    }

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

    @Transactional
    public Sensor saveSensorSP(SensorFormDto sensorDto) {
        logger.info("saveSensorSP çağrılıyor. Sensör Kodu: {}", sensorDto.getSensorKodu());
        LocalDate kurulumTarihiDate = stringToLocalDate(sensorDto.getKurulumTarihi());
        LocalDate sonBakimTarihiDate = stringToLocalDate(sensorDto.getSonBakimTarihi());

        List<Object[]> resultList = entityManager.createNativeQuery("EXEC dbo.spSensor_Ekle " +
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
            if (!(resultList.get(0) instanceof Object[])) {
                 logger.error("spSensor_Ekle'den beklenmeyen sonuç formatı.");
                 throw new RuntimeException("Sensör eklenemedi (SP'den beklenmeyen sonuç formatı).");
            }
            Object[] resultRow = (Object[]) resultList.get(0);
            Integer resultCode = (Integer) resultRow[0];
            String resultMessage = (String) resultRow[1];
            logger.info("spSensor_Ekle'den dönen: ResultCode={}, ResultMessage='{}'", resultCode, resultMessage);

            if (resultCode > 0) {
                Sensor yeniSensor = new Sensor();
                yeniSensor.setSensorID(resultCode);
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
                return yeniSensor;
            } else {
                throw new IllegalArgumentException(resultMessage + " (Kod: " + resultCode + ")");
            }
        }
        throw new RuntimeException("Sensör eklenemedi (SP'den sonuç alınamadı veya boş sonuç döndü).");
    }

    @Transactional
    public boolean deleteSensorSP(Integer id) {
        logger.info("deleteSensorSP çağrılıyor ID: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("Silinecek sensör için ID belirtilmelidir.");
        }
        List<Object[]> resultList = entityManager.createNativeQuery("EXEC dbo.spSensor_Sil @SensorID = :sensorID")
                .setParameter("sensorID", id)
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            if (!(resultList.get(0) instanceof Object[])) {
                 logger.error("spSensor_Sil'den beklenmeyen sonuç formatı.");
                 throw new RuntimeException("Sensör silinemedi (SP'den beklenmeyen sonuç formatı).");
            }
            Object[] resultRow = (Object[]) resultList.get(0);
            Integer resultCode = (Integer) resultRow[0];
            String resultMessage = (String) resultRow[1];
            logger.info("spSensor_Sil'den dönen: ResultCode={}, ResultMessage='{}'", resultCode, resultMessage);

            if (resultCode == 1) {
                return true;
            } else {
                // spSensor_Sil içindeki hata kodlarına göre mesajı zenginleştirin
                throw new RuntimeException(resultMessage + " (Kod: " + resultCode + ")");
            }
        }
        throw new RuntimeException("Sensör silinemedi (SP'den sonuç alınamadı veya boş sonuç döndü).");
    }

    public Optional<Sensor> findSensorEntityById(Integer id) {
        if (id == null) return Optional.empty();
        return sensorRepository.findById(id);
    }

    // TODO: updateSensorSP metodu buraya eklenecek.
}