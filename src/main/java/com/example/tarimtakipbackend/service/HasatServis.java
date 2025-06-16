package com.example.tarimtakipbackend.service;

import com.example.tarimtakipbackend.dto.HasatDetayDto;
import com.example.tarimtakipbackend.dto.HasatFormDto;
import com.example.tarimtakipbackend.entity.Hasatlar;
import com.example.tarimtakipbackend.entity.Kullanici;
import com.example.tarimtakipbackend.repository.EkimRepository;
import com.example.tarimtakipbackend.repository.HasatRepository;
import com.example.tarimtakipbackend.repository.KullaniciRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HasatServis {

    private static final Logger logger = LoggerFactory.getLogger(HasatServis.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private HasatRepository hasatRepository;

    @Autowired
    private KullaniciRepository kullaniciRepository;

    @Autowired
    private EkimRepository ekimRepository; 

    private Integer getCurrentKullaniciId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            return kullaniciRepository.findByKullaniciAdi(username).map(Kullanici::getKullaniciID).orElse(null);
        }
        logger.warn("HasatServis: Giriş yapmış kullanıcı bulunamadı veya anonymousUser.");
        return null;
    }

    private Date toSqlDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return null;
        try {
            return Date.valueOf(LocalDate.parse(dateString));
        } catch (DateTimeParseException e) {
            logger.error("HasatServis: Geçersiz tarih formatı: '{}'. Beklenen format yyyy-MM-dd.", dateString, e);
            throw new IllegalArgumentException("Geçersiz tarih formatı. Lütfen yyyy-MM-dd formatında girin: " + dateString);
        }
    }
    
    private LocalDate toLocalDateFromDb(Object dbDate) {
        if (dbDate == null) return null;
        if (dbDate instanceof java.sql.Date) return ((java.sql.Date) dbDate).toLocalDate();
        if (dbDate instanceof java.sql.Timestamp) return ((java.sql.Timestamp) dbDate).toLocalDateTime().toLocalDate();
        if (dbDate instanceof String) try { return LocalDate.parse((String) dbDate); } catch (DateTimeParseException e) { logger.warn("HasatServis: String'den LocalDate'e çevirme hatası: {}", dbDate, e); }
        return null;
    }

    private LocalDateTime toLocalDateTimeFromDb(Object dbTimestamp) {
        if (dbTimestamp == null) return null;
        if (dbTimestamp instanceof java.sql.Timestamp) return ((java.sql.Timestamp) dbTimestamp).toLocalDateTime();
        if (dbTimestamp instanceof String) try {
            String s = (String) dbTimestamp;
            if (s.contains(".")) s = s.substring(0, s.lastIndexOf('.'));
            return LocalDateTime.parse(s.replace(" ", "T"));
        } catch (DateTimeParseException e) { logger.warn("HasatServis: String'den LocalDateTime'a çevirme hatası: {}", dbTimestamp, e); }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<HasatDetayDto> getAllHasatlarSP() {
        logger.info("getAllHasatlarSP çağrıldı.");
        try {
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spHasat_Listele").getResultList();
            return results.stream().map(row -> new HasatDetayDto(
                    (Integer) row[0],    // HasatID
                    (Integer) row[1],    // EkimID
                    (String) row[2],     // TarlaAdi
                    (String) row[3],     // UrunAdi
                    toLocalDateFromDb(row[4]),  // HasatTarihi
                    (BigDecimal) row[5], // ToplananMiktar
                    (String) row[6],     // HasatBirimi
                    (String) row[7],     // HasatKalitesi
                    (String) row[8],     // DepoBilgisi
                    (BigDecimal) row[9], // HasatMaliyeti
                    (BigDecimal) row[10], // HasatSatisFiyati
                    (String) row[11],    // KaydedenKullanici
                    (String) row[12],    // HasatNotlari
                    toLocalDateTimeFromDb(row[13]) // HasatKayitTarihi (vHasatDetaylari'nda bu sütun olmalı)
            )).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("getAllHasatlarSP sırasında hata: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @SuppressWarnings("unchecked")
    public Optional<HasatDetayDto> getHasatByIdSP(Integer hasatId) {
        if (hasatId == null) return Optional.empty();
        logger.info("getHasatByIdSP çağrıldı ID: {}", hasatId);
        try {
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spHasat_GetirByID @HasatID = :id")
                .setParameter("id", hasatId).getResultList();
            if (results.isEmpty()) return Optional.empty();
            Object[] row = results.get(0);
            return Optional.of(new HasatDetayDto(
                (Integer) row[0], (Integer) row[1], (String) row[2], (String) row[3],
                toLocalDateFromDb(row[4]), (BigDecimal) row[5], (String) row[6], (String) row[7],
                (String) row[8], (BigDecimal) row[9], (BigDecimal) row[10], (String) row[11],
                (String) row[12], toLocalDateTimeFromDb(row[13])
            ));
        } catch (Exception e) {
            logger.error("getHasatByIdSP sırasında hata ID {}: {}", hasatId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Transactional
    public Hasatlar saveHasatSP(HasatFormDto formDto) {
        Integer kaydedenKullaniciId = getCurrentKullaniciId();
        validateHasatForm(formDto, false);
        Date sqlHasatTarihi = toSqlDate(formDto.getHasatTarihi());

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("dbo.spHasat_Ekle");
        query.registerStoredProcedureParameter("EkimID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("HasatTarihi", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("ToplananMiktar", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Birim", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Kalite", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("DepoBilgisi", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Maliyet", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("SatisFiyati", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("KaydedenKullaniciID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Notlar", String.class, ParameterMode.IN);

        query.setParameter("EkimID", formDto.getEkimId());
        query.setParameter("HasatTarihi", sqlHasatTarihi);
        query.setParameter("ToplananMiktar", formDto.getToplananMiktar());
        query.setParameter("Birim", formDto.getBirim());
        query.setParameter("Kalite", formDto.getKalite());
        query.setParameter("DepoBilgisi", formDto.getDepoBilgisi());
        query.setParameter("Maliyet", formDto.getMaliyet());
        query.setParameter("SatisFiyati", formDto.getSatisFiyati());
        query.setParameter("KaydedenKullaniciID", kaydedenKullaniciId);
        query.setParameter("Notlar", formDto.getNotlar());

        List<?> resultList = query.getResultList();
        if (resultList != null && !resultList.isEmpty() && resultList.get(0) instanceof Number) {
            Integer yeniHasatId = ((Number) resultList.get(0)).intValue();
            if (yeniHasatId > 0) {
                logger.info("SP ile yeni hasat eklendi, ID: {}", yeniHasatId);
                return hasatRepository.findById(yeniHasatId)
                    .orElseThrow(() -> new RuntimeException("Eklenen hasat kaydı bulunamadı ID: " + yeniHasatId));
            } else {
                handleSpAddErrorCodes(yeniHasatId, "ekleme");
            }
        }
        throw new RuntimeException("Hasat eklenemedi (SP'den sonuç alınamadı).");
    }
    
    @Transactional
    public boolean updateHasatSP(HasatFormDto formDto) {
        Integer guncelleyenKullaniciId = getCurrentKullaniciId();
        logger.info("updateHasatSP: HasatID='{}'", formDto.getHasatID());
        validateHasatForm(formDto, true); // true for update
        Date sqlHasatTarihi = toSqlDate(formDto.getHasatTarihi());

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("dbo.spHasat_Guncelle"); // Kendi SP adınızla değiştirin
        query.registerStoredProcedureParameter("HasatID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("EkimID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("HasatTarihi", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("ToplananMiktar", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Birim", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Kalite", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("DepoBilgisi", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Maliyet", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("SatisFiyati", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("GuncelleyenKullaniciID", Integer.class, ParameterMode.IN); // SP'nizdeki parametre adı
        query.registerStoredProcedureParameter("Notlar", String.class, ParameterMode.IN);

        query.setParameter("HasatID", formDto.getHasatID());
        query.setParameter("EkimID", formDto.getEkimId());
        query.setParameter("HasatTarihi", sqlHasatTarihi);
        query.setParameter("ToplananMiktar", formDto.getToplananMiktar());
        query.setParameter("Birim", formDto.getBirim());
        query.setParameter("Kalite", formDto.getKalite());
        query.setParameter("DepoBilgisi", formDto.getDepoBilgisi());
        query.setParameter("Maliyet", formDto.getMaliyet());
        query.setParameter("SatisFiyati", formDto.getSatisFiyati());
        query.setParameter("GuncelleyenKullaniciID", guncelleyenKullaniciId);
        query.setParameter("Notlar", formDto.getNotlar());

        List<?> resultList = query.getResultList();
        if (resultList != null && !resultList.isEmpty() && resultList.get(0) instanceof Number) {
            int sonuc = ((Number) resultList.get(0)).intValue();
            if (sonuc == 1) {
                logger.info("SP ile hasat güncellendi, ID: {}", formDto.getHasatID());
                return true;
            } else {
                handleSpUpdateErrorCodes(sonuc, "güncelleme");
                return false; 
            }
        }
        throw new RuntimeException("Hasat güncellenemedi (SP bulunamadı veya beklenmedik sonuç).");
    }

    @Transactional
    public boolean deleteHasatSP(Integer hasatId) {
        logger.info("deleteHasatSP for ID: {}", hasatId);
        if (hasatId == null) {
            throw new IllegalArgumentException("Silinecek hasat için ID belirtilmelidir.");
        }
        
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("dbo.spHasat_Sil"); // Kendi SP adınızla değiştirin
        query.registerStoredProcedureParameter("HasatID", Integer.class, ParameterMode.IN);
        
        query.setParameter("HasatID", hasatId);
        
        List<?> resultList = query.getResultList();
        if (resultList != null && !resultList.isEmpty() && resultList.get(0) instanceof Number) {
            int sonuc = ((Number) resultList.get(0)).intValue();
            if (sonuc == 1) {
                logger.info("SP ile hasat silindi, ID: {}", hasatId);
                return true;
            } else {
                handleSpDeleteErrorCodes(sonuc, "silme");
                return false;
            }
        }
        throw new RuntimeException("Hasat silinemedi (SP bulunamadı veya beklenmedik sonuç).");
    }

    private void validateHasatForm(HasatFormDto formDto, boolean isUpdate) {
        if (isUpdate && formDto.getHasatID() == null) {
            throw new IllegalArgumentException("Güncelleme için Hasat ID zorunludur.");
        }
        if (formDto.getEkimId() == null) {
            throw new IllegalArgumentException("Ekim seçimi zorunludur.");
        }
        if (formDto.getHasatTarihi() == null || formDto.getHasatTarihi().trim().isEmpty()) {
            throw new IllegalArgumentException("Hasat tarihi zorunludur.");
        }
        if (formDto.getToplananMiktar() == null || formDto.getToplananMiktar().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Toplanan miktar pozitif bir değer olmalıdır.");
        }
        if (formDto.getBirim() == null || formDto.getBirim().trim().isEmpty()) {
            throw new IllegalArgumentException("Birim boş olamaz.");
        }
    }

    private void handleSpAddErrorCodes(Integer resultCode, String operation) {
        String baseMsg = "Hasat " + operation + " işlemi başarısız: ";
        if (resultCode == -2) throw new IllegalArgumentException(baseMsg + "Geçersiz Ekim ID.");

        if (resultCode == -4) throw new IllegalArgumentException(baseMsg + "Geçersiz Kaydeden Kullanıcı ID.");
        else throw new RuntimeException(baseMsg + "SP bilinmeyen hata kodu: " + resultCode);
    }

    private void handleSpUpdateErrorCodes(Integer resultCode, String operation) {
        String baseMsg = "Hasat " + operation + " işlemi başarısız: ";
        if (resultCode == -1) throw new IllegalArgumentException(baseMsg + "Kayıt bulunamadı.");
        
        else throw new RuntimeException(baseMsg + "SP bilinmeyen hata kodu: " + resultCode);
    }

    private void handleSpDeleteErrorCodes(Integer resultCode, String operation) {
        String baseMsg = "Hasat " + operation + " işlemi başarısız: ";
        if (resultCode == -1) throw new IllegalArgumentException(baseMsg + "Kayıt bulunamadı.");

        else throw new RuntimeException(baseMsg + "SP bilinmeyen hata kodu: " + resultCode);
    }
}