package com.example.tarimtakipbackend.service;

import com.example.tarimtakipbackend.dto.KullanilanGirdiDetayDto;
import com.example.tarimtakipbackend.dto.KullanilanGirdiFormDto;
import com.example.tarimtakipbackend.entity.Kullanici;
import com.example.tarimtakipbackend.entity.KullanilanGirdiler;
import com.example.tarimtakipbackend.repository.KullaniciRepository;
import com.example.tarimtakipbackend.repository.KullanilanGirdilerRepository;

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
public class KullanilanGirdiServis {

    private static final Logger logger = LoggerFactory.getLogger(KullanilanGirdiServis.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private KullanilanGirdilerRepository kullanilanGirdilerRepository;

    @Autowired
    private KullaniciRepository kullaniciRepository;

    private Integer getCurrentKullaniciId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            Optional<Kullanici> kullaniciOpt = kullaniciRepository.findByKullaniciAdi(username);
            return kullaniciOpt.map(Kullanici::getKullaniciID).orElse(null);
        }
        logger.warn("Giriş yapmış kullanıcı bulunamadı veya anonymousUser.");
        return null;
    }

    private Date toSqlDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return Date.valueOf(LocalDate.parse(dateString));
        } catch (DateTimeParseException e) {
            logger.error("Geçersiz tarih formatı: '{}'. Beklenen format yyyy-MM-dd.", dateString, e);
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
        if (dbDate instanceof String) {
            try {
                return LocalDate.parse((String) dbDate);
            } catch (DateTimeParseException e) {
                logger.warn("String'den LocalDate'e çevirme hatası: {}", dbDate, e);
            }
        }
        return null;
    }
    
    private LocalDateTime toLocalDateTimeFromDb(Object dbTimestamp) {
        if (dbTimestamp == null) return null;
        if (dbTimestamp instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) dbTimestamp).toLocalDateTime();
        }
         if (dbTimestamp instanceof String) {
            try {
                String dateStr = (String) dbTimestamp;
                if (dateStr.contains(".")) {
                    dateStr = dateStr.substring(0, dateStr.lastIndexOf('.'));
                }
                return LocalDateTime.parse(dateStr.replace(" ", "T"));
            } catch (DateTimeParseException e) {
                logger.warn("String'den LocalDateTime'a çevirme hatası: {}", dbTimestamp, e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<KullanilanGirdiDetayDto> getAllKullanilanGirdilerSP() {
        logger.info("getAllKullanilanGirdilerSP çağrıldı.");
        try {
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spKullanilanGirdi_Listele").getResultList();
            logger.info("SP 'dbo.spKullanilanGirdi_Listele' {} sonuç döndürdü.", results.size());

            return results.stream().map(row -> {
                try {
                    // View'ınızın tam sütun sayısını ve sırasını kontrol edin.
                    // Eğer view'ınızda KayitTarihi yoksa, DTO'dan bu alanı kaldırın veya burada null geçin.
                    LocalDateTime kayitTarihi = null;
                    if (row.length > 14 && row[14] != null) { // Güvenli erişim
                        kayitTarihi = toLocalDateTimeFromDb(row[14]);
                    }

                    return new KullanilanGirdiDetayDto(
                            (Integer) row[0],    // KullanimID
                            (String) row[1],     // GirdiAdi
                            (String) row[2],     // GirdiTipi
                            (String) row[3],     // GirdiBirimi
                            toLocalDateFromDb(row[4]), // KullanimTarihi
                            (BigDecimal) row[5], // Miktar
                            (BigDecimal) row[6], // KullanilanGirdiMaliyeti
                            (String) row[7],     // KaydedenKullanici
                            (String) row[8],     // KullanimNotlari
                            (Integer) row[9],    // IliskiliEkimID
                            (String) row[10],    // IliskiliTarlaAdi
                            (String) row[11],    // IliskiliUrunAdi
                            (Integer) row[12],   // IliskiliGorevID
                            (String) row[13],    // IliskiliGorevFaaliyeti
                            kayitTarihi          // KayitTarihi (DTO'da varsa)
                    );
                } catch (Exception e) {
                    logger.error("DTO map'leme sırasında hata oluştu. Satır verisi: {}", java.util.Arrays.toString(row), e);
                    return null; 
                }
            }).filter(dto -> dto != null)
            .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("getAllKullanilanGirdilerSP sırasında genel bir hata oluştu: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @SuppressWarnings("unchecked")
    public Optional<KullanilanGirdiDetayDto> getKullanilanGirdiByIdSP(Integer kullanimId) {
        logger.info("getKullanilanGirdiByIdSP çağrıldı ID: {}", kullanimId);
        if (kullanimId == null) return Optional.empty();
        try {
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spKullanilanGirdi_GetirByID @KullanimID = :id")
                    .setParameter("id", kullanimId)
                    .getResultList();
            if (results.isEmpty()) {
                return Optional.empty();
            }
            Object[] row = results.get(0);
            LocalDateTime kayitTarihi = null;
            if (row.length > 14 && row[14] != null) {
                kayitTarihi = toLocalDateTimeFromDb(row[14]);
            }
            
            return Optional.of(new KullanilanGirdiDetayDto(
                    (Integer) row[0], (String) row[1], (String) row[2], (String) row[3],
                    toLocalDateFromDb(row[4]), (BigDecimal) row[5], (BigDecimal) row[6],
                    (String) row[7], (String) row[8], (Integer) row[9], (String) row[10],
                    (String) row[11], (Integer) row[12], (String) row[13],
                    kayitTarihi
            ));
        } catch (Exception e) {
            logger.error("getKullanilanGirdiByIdSP sırasında hata oluştu ID {}: {}", kullanimId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Transactional
    public KullanilanGirdiler saveKullanilanGirdiSP(KullanilanGirdiFormDto formDto) {
        Integer kaydedenKullaniciId = getCurrentKullaniciId();
        logger.info("saveKullanilanGirdiSP: EkimID='{}', GorevID='{}', GirdiID='{}'",
                formDto.getEkimId(), formDto.getGorevId(), formDto.getGirdiId());

        validateKullanilanGirdiForm(formDto, false);

        Date sqlKullanimTarihi = toSqlDate(formDto.getKullanimTarihi());

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("dbo.spKullanilanGirdi_Ekle");
        query.registerStoredProcedureParameter("EkimID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("GorevID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("GirdiID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("KullanimTarihi", java.sql.Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Miktar", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Maliyet", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("KaydedenKullaniciID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Notlar", String.class, ParameterMode.IN);
        
        query.setParameter("EkimID", formDto.getEkimId());
        query.setParameter("GorevID", formDto.getGorevId());
        query.setParameter("GirdiID", formDto.getGirdiId());
        query.setParameter("KullanimTarihi", sqlKullanimTarihi);
        query.setParameter("Miktar", formDto.getMiktar());
        query.setParameter("Maliyet", formDto.getMaliyet());
        query.setParameter("KaydedenKullaniciID", kaydedenKullaniciId);
        query.setParameter("Notlar", formDto.getNotlar());

        List<?> resultList = query.getResultList();
        
        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
             if (result instanceof Number) { 
                Integer yeniKullanimId = ((Number) result).intValue();
                if (yeniKullanimId > 0) {
                    logger.info("SP ile yeni kullanılan girdi eklendi, ID: {}", yeniKullanimId);
                    return kullanilanGirdilerRepository.findById(yeniKullanimId)
                            .orElseThrow(() -> new RuntimeException("Eklenen girdi kullanımı kaydı veritabanından çekilemedi ID: " + yeniKullanimId));
                } else { 
                    handleSpErrorCodes(yeniKullanimId, "ekleme");
                }
            } else {
                 logger.error("spKullanilanGirdi_Ekle'den beklenmeyen sonuç tipi: {}", result.getClass().getName());
                 throw new RuntimeException("Kullanılan girdi eklenemedi (SP'den beklenmeyen sonuç tipi).");
            }
        }
        throw new RuntimeException("Kullanılan girdi eklenemedi (SP'den ID veya geçerli bir sonuç alınamadı).");
    }

    @Transactional
    public boolean updateKullanilanGirdiSP(KullanilanGirdiFormDto formDto) {
        Integer guncelleyenKullaniciId = getCurrentKullaniciId();
        logger.info("updateKullanilanGirdiSP: KullanimID='{}'", formDto.getKullanimID());
        
        validateKullanilanGirdiForm(formDto, true);

        Date sqlKullanimTarihi = toSqlDate(formDto.getKullanimTarihi());
       
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("dbo.spKullanilanGirdi_Guncelle"); // SP adını doğrulayın
        query.registerStoredProcedureParameter("KullanimID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("EkimID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("GorevID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("GirdiID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("KullanimTarihi", java.sql.Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Miktar", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Maliyet", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("GuncelleyenKullaniciID", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Notlar", String.class, ParameterMode.IN);

        query.setParameter("KullanimID", formDto.getKullanimID());
        query.setParameter("EkimID", formDto.getEkimId());
        query.setParameter("GorevID", formDto.getGorevId());
        query.setParameter("GirdiID", formDto.getGirdiId());
        query.setParameter("KullanimTarihi", sqlKullanimTarihi);
        query.setParameter("Miktar", formDto.getMiktar());
        query.setParameter("Maliyet", formDto.getMaliyet());
        query.setParameter("GuncelleyenKullaniciID", guncelleyenKullaniciId);
        query.setParameter("Notlar", formDto.getNotlar());

        List<?> resultList = query.getResultList();
        if (resultList != null && !resultList.isEmpty() && resultList.get(0) instanceof Number) {
            int sonuc = ((Number) resultList.get(0)).intValue();
            if (sonuc == 1) {
                logger.info("SP ile kullanılan girdi güncellendi, ID: {}", formDto.getKullanimID());
                return true;
            } else {
                handleSpUpdateErrorCodes(sonuc, "güncelleme");
                // handleSpUpdateErrorCodes exception fırlatacağı için return false gereksiz olabilir,
                // ama her ihtimale karşı bırakıyorum.
                return false; 
            }
        }
        throw new RuntimeException("Kullanılan girdi güncellenemedi (SP bulunamadı veya beklenmedik sonuç).");
    }

    @Transactional
    public boolean deleteKullanilanGirdiSP(Integer kullanimId) {
        logger.info("deleteKullanilanGirdiSP for ID: {}", kullanimId);
        if (kullanimId == null) {
            throw new IllegalArgumentException("Silinecek girdi kullanımı için ID belirtilmelidir.");
        }
        
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("dbo.spKullanilanGirdi_Sil"); // SP adını doğrulayın
        query.registerStoredProcedureParameter("KullanimID", Integer.class, ParameterMode.IN);
        
        query.setParameter("KullanimID", kullanimId);
        
        List<?> resultList = query.getResultList();
        if (resultList != null && !resultList.isEmpty() && resultList.get(0) instanceof Number) {
            int sonuc = ((Number) resultList.get(0)).intValue();
            if (sonuc == 1) {
                logger.info("SP ile kullanılan girdi silindi, ID: {}", kullanimId);
                return true;
            } else {
                handleSpDeleteErrorCodes(sonuc, "silme");
                return false;
            }
        }
        throw new RuntimeException("Kullanılan girdi silinemedi (SP bulunamadı veya beklenmedik sonuç).");
    }

    private void validateKullanilanGirdiForm(KullanilanGirdiFormDto formDto, boolean isUpdate) {
        if (isUpdate && formDto.getKullanimID() == null) {
            throw new IllegalArgumentException("Güncelleme işlemi için Kullanım ID belirtilmelidir.");
        }
        if ((formDto.getEkimId() == null && formDto.getGorevId() == null)) {
            throw new IllegalArgumentException("Kullanılan girdi ya bir Ekim ya da bir Görev ile ilişkili olmalıdır.");
        }
        if (formDto.getGirdiId() == null) {
            throw new IllegalArgumentException("Girdi seçimi zorunludur.");
        }
        if (formDto.getKullanimTarihi() == null || formDto.getKullanimTarihi().trim().isEmpty()) {
            throw new IllegalArgumentException("Kullanım tarihi boş olamaz.");
        }
        if (formDto.getMiktar() == null || formDto.getMiktar().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Miktar pozitif bir değer olmalıdır.");
        }
    }

    private void handleSpErrorCodes(Integer resultCode, String operation) {
        String baseMessage = "Kullanılan girdi " + operation + " işlemi başarısız: ";
        if (resultCode == -2) throw new IllegalArgumentException(baseMessage + "Ekim veya Görev ID belirtilmelidir.");
        else if (resultCode == -3) throw new IllegalArgumentException(baseMessage + "Geçersiz Ekim ID.");
        else if (resultCode == -4) throw new IllegalArgumentException(baseMessage + "Geçersiz Görev ID.");
        else if (resultCode == -5) throw new IllegalArgumentException(baseMessage + "Geçersiz Girdi ID.");
        else if (resultCode == -6) throw new IllegalArgumentException(baseMessage + "Geçersiz Kaydeden Kullanıcı ID.");
        else throw new RuntimeException(baseMessage + "Saklı yordam bilinmeyen bir hata kodu döndürdü: " + resultCode);
    }

    private void handleSpUpdateErrorCodes(Integer resultCode, String operation) {
        String baseMessage = "Kullanılan girdi " + operation + " işlemi başarısız: ";
        if (resultCode == -1) throw new IllegalArgumentException(baseMessage + "Kayıt bulunamadı.");
        // SP'nizin döndürdüğü diğer güncelleme hata kodlarını buraya ekleyin:
        else if (resultCode == -2) throw new IllegalArgumentException(baseMessage + "İlişki eksik (Ekim veya Görev ID).");
        else if (resultCode == -3) throw new IllegalArgumentException(baseMessage + "Geçersiz Ekim ID.");
        else if (resultCode == -4) throw new IllegalArgumentException(baseMessage + "Geçersiz Görev ID.");
        else if (resultCode == -5) throw new IllegalArgumentException(baseMessage + "Geçersiz Girdi ID.");
        else if (resultCode == -6) throw new IllegalArgumentException(baseMessage + "Geçersiz Güncelleyen Kullanıcı ID.");
        else throw new RuntimeException(baseMessage + "Saklı yordam bilinmeyen bir hata kodu döndürdü: " + resultCode);
    }

    private void handleSpDeleteErrorCodes(Integer resultCode, String operation) {
        String baseMessage = "Kullanılan girdi " + operation + " işlemi başarısız: ";
        if (resultCode == -1) throw new IllegalArgumentException(baseMessage + "Kayıt bulunamadı.");
        // SP'nizin döndürdüğü diğer silme hata kodlarını buraya ekleyin
        else throw new RuntimeException(baseMessage + "Saklı yordam bilinmeyen bir hata kodu döndürdü: " + resultCode);
    }
}