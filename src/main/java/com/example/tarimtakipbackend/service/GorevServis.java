package com.example.tarimtakipbackend.service;

import com.example.tarimtakipbackend.dto.GorevDetayDto;
import com.example.tarimtakipbackend.dto.GorevFormDto;
import com.example.tarimtakipbackend.entity.Gorev;
import com.example.tarimtakipbackend.entity.Kullanici;
import com.example.tarimtakipbackend.repository.GorevRepository;
import com.example.tarimtakipbackend.repository.KullaniciRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GorevServis {

    private static final Logger logger = LoggerFactory.getLogger(GorevServis.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private GorevRepository gorevRepository;

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

    private LocalDateTime toLocalDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateTimeString, DATETIME_FORMATTER);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(dateTimeString + ":00", DATETIME_FORMATTER);
            } catch (DateTimeParseException e2) {
                logger.error("Geçersiz tarih-saat formatı: '{}'. Beklenen format yyyy-MM-ddTHH:mm veya yyyy-MM-ddTHH:mm:ss.", dateTimeString, e2);
                throw new IllegalArgumentException("Geçersiz tarih-saat formatı. Lütfen yyyy-MM-ddTHH:mm formatında girin: " + dateTimeString);
            }
        }
    }

    private LocalDateTime toLocalDateTimeFromDb(Object dbTimestamp) {
        if (dbTimestamp == null) return null;
        if (dbTimestamp instanceof Timestamp) {
            return ((Timestamp) dbTimestamp).toLocalDateTime();
        }
        logger.warn("Desteklenmeyen veritabanı tarih tipi: {}", dbTimestamp.getClass().getName());
        return null;
    }

    public Optional<Gorev> findById(Integer id) {
        logger.debug("GorevServis findById ID: {}", id);
        return id == null ? Optional.empty() : gorevRepository.findById(id);
    }

    @SuppressWarnings("unchecked")
    public List<GorevDetayDto> getAllGorevlerSP() {
        logger.info("getAllGorevlerSP çağrıldı.");
        try {
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spGorev_Listele").getResultList();
            return results.stream().map(row -> new GorevDetayDto(
                    (Integer) row[0], (String) row[1], (String) row[2], (Integer) row[3],
                    (String) row[4], (String) row[5], (String) row[6], (String) row[7],
                    toLocalDateTimeFromDb(row[8]), toLocalDateTimeFromDb(row[9]), toLocalDateTimeFromDb(row[10]),
                    (String) row[11], (Integer) row[12], toLocalDateTimeFromDb(row[13])
            )).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("getAllGorevlerSP sırasında hata: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<GorevDetayDto> getGorevDetayByIdSP(Integer id) {
        logger.info("getGorevDetayByIdSP ID: {}", id);
        if (id == null) return Optional.empty();
        try {
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spGorev_GetirByID @GorevID = :id")
                    .setParameter("id", id)
                    .getResultList();
            if (results.isEmpty()) return Optional.empty();
            Object[] row = results.get(0);
            return Optional.of(new GorevDetayDto(
                    (Integer) row[0], (String) row[1], (String) row[2], (Integer) row[3],
                    (String) row[4], (String) row[5], (String) row[6], (String) row[7],
                    toLocalDateTimeFromDb(row[8]), toLocalDateTimeFromDb(row[9]), toLocalDateTimeFromDb(row[10]),
                    (String) row[11], (Integer) row[12], toLocalDateTimeFromDb(row[13])
            ));
        } catch (Exception e) {
            logger.error("getGorevDetayByIdSP sırasında hata oluştu ID {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Transactional
    public Gorev saveGorevSP(GorevFormDto gorevDto) {
        Integer talepEdenKullaniciId = getCurrentKullaniciId();
        logger.info("saveGorevSP çağrılıyor. FaaliyetTipiID: {}, TalepEdenKID: {}", gorevDto.getFaaliyetTipiId(), talepEdenKullaniciId);
        LocalDateTime planBas = toLocalDateTime(gorevDto.getPlanlananBaslangicTarihi());
        LocalDateTime planBitis = toLocalDateTime(gorevDto.getPlanlananBitisTarihi());

        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spGorev_Ekle " +
                        "@EkimID = :ekimID, @TarlaID = :tarlaID, @FaaliyetTipiID = :faaliyetID, " +
                        "@Aciklama = :aciklama, @AtananKullaniciID = :atananKID, @TalepEdenKullaniciID = :talepEdenKID, " +
                        "@PlanlananBaslangicTarihi = :planBas, @PlanlananBitisTarihi = :planBitis, " +
                        "@Durum = :durum, @Oncelik = :oncelik")
                .setParameter("ekimID", gorevDto.getEkimId())
                .setParameter("tarlaID", gorevDto.getTarlaId())
                .setParameter("faaliyetID", gorevDto.getFaaliyetTipiId())
                .setParameter("aciklama", gorevDto.getAciklama())
                .setParameter("atananKID", gorevDto.getAtananKullaniciId())
                .setParameter("talepEdenKID", talepEdenKullaniciId)
                .setParameter("planBas", planBas)
                .setParameter("planBitis", planBitis)
                .setParameter("durum", gorevDto.getDurum() != null && !gorevDto.getDurum().trim().isEmpty() ? gorevDto.getDurum() : "Atandı")
                .setParameter("oncelik", gorevDto.getOncelik() != null ? gorevDto.getOncelik() : 3)
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer yeniID = ((Number) result).intValue();
                if (yeniID > 0) {
                    logger.info("SP ile yeni görev eklendi, ID: {}. Flush ve Clear yapılıyor...", yeniID);
                    entityManager.flush(); // Değişiklikleri veritabanına yazmaya zorla
                    entityManager.clear(); // Persistence context'i temizle
                    logger.info("Flush ve Clear tamamlandı. Şimdi findById çağrılıyor...");
                    return gorevRepository.findById(yeniID)
                            .orElseThrow(() -> new RuntimeException("Eklenen görev kaydı veritabanından çekilemedi ID: " + yeniID + " (flush ve clear sonrası bile)"));
                } else if (yeniID == -2) throw new IllegalArgumentException("Görev eklenemedi: Ekim veya Tarla ID belirtilmeli veya Faaliyet Tipi eksik.");
                else if (yeniID == -3) throw new IllegalArgumentException("Görev eklenemedi: Geçersiz Ekim ID.");
                else if (yeniID == -4) throw new IllegalArgumentException("Görev eklenemedi: Geçersiz Tarla ID.");
                else if (yeniID == -5) throw new IllegalArgumentException("Görev eklenemedi: Geçersiz FaaliyetTipiID.");
                else if (yeniID == -6) throw new IllegalArgumentException("Görev eklenemedi: Geçersiz AtananKullaniciID.");
                else if (yeniID == -7) throw new IllegalArgumentException("Görev eklenemedi: Geçersiz TalepEdenKullaniciID.");
                else if (yeniID == -8) throw new IllegalArgumentException("Görev eklenemedi: Bitiş tarihi başlangıç tarihinden önce olamaz.");
                else if (yeniID == -10) throw new IllegalArgumentException("Görev eklenemedi: FaaliyetTipiID boş olamaz.");
                else if (yeniID == -99) throw new RuntimeException("Görev eklenemedi: Veritabanına kayıt sırasında bir SQL hatası oluştu (DB loglarına bakın).");
                else if (yeniID == -98) throw new RuntimeException("Görev eklenemedi: SCOPE_IDENTITY() NULL döndü, INSERT başarısız olabilir.");
                else if (yeniID == -100) throw new RuntimeException("Görev eklenemedi: Beklenmedik bir veritabanı hatası oluştu (CATCH bloğu - DB loglarına bakın).");
                else throw new RuntimeException("Görev eklenemedi. SP bilinmeyen bir hata kodu döndürdü: " + yeniID);
            } else {
                 throw new RuntimeException("Görev eklenemedi (SP'den sayısal bir ID veya hata kodu alınamadı). Sonuç: " + result);
            }
        }
        throw new RuntimeException("Görev eklenemedi (SP'den sonuç alınamadı).");
    }

    @Transactional
    public boolean updateGorevSP(GorevFormDto gorevDto) {
        // ... (Bu metodun içeriği önceki mesajdaki gibi kalabilir, SP hata kodlarınıza göre güncelleyin) ...
        // Örnek olarak saveGorevSP'deki gibi detaylı hata kodu yönetimini buraya da uygulayabilirsiniz.
        // Şimdilik kısa tutuyorum:
        logger.info("updateGorevSP: GorevID='{}'", gorevDto.getGorevID());
        if (gorevDto.getGorevID() == null) {
            throw new IllegalArgumentException("Güncellenecek görev için ID belirtilmelidir.");
        }
        LocalDateTime planlananBaslangic = toLocalDateTime(gorevDto.getPlanlananBaslangicTarihi());
        LocalDateTime planlananBitis = toLocalDateTime(gorevDto.getPlanlananBitisTarihi());
        LocalDateTime tamamlanma = toLocalDateTime(gorevDto.getTamamlanmaTarihi());
        Integer talepEdenKIDForUpdate = getCurrentKullaniciId();

        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spGorev_Guncelle " +
                        "@GorevID = :gorevID, @EkimID = :ekimID, @TarlaID = :tarlaID, @FaaliyetTipiID = :faaliyetID, " +
                        "@Aciklama = :aciklama, @AtananKullaniciID = :atananKID, @TalepEdenKullaniciID = :talepEdenKID, " +
                        "@PlanlananBaslangicTarihi = :planBasTarih, @PlanlananBitisTarihi = :planBitisTarih, " +
                        "@TamamlanmaTarihi = :tamamlanmaTarih, @Durum = :durum, @Oncelik = :oncelik")
                .setParameter("gorevID", gorevDto.getGorevID())
                .setParameter("ekimID", gorevDto.getEkimId())
                .setParameter("tarlaID", gorevDto.getTarlaId())
                .setParameter("faaliyetID", gorevDto.getFaaliyetTipiId())
                .setParameter("aciklama", gorevDto.getAciklama())
                .setParameter("atananKID", gorevDto.getAtananKullaniciId())
                .setParameter("talepEdenKID", talepEdenKIDForUpdate)
                .setParameter("planBasTarih", planlananBaslangic)
                .setParameter("planBitisTarih", planlananBitis)
                .setParameter("tamamlanmaTarih", tamamlanma)
                .setParameter("durum", gorevDto.getDurum())
                .setParameter("oncelik", gorevDto.getOncelik())
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer sonucKodu = ((Number) result).intValue();
                if (sonucKodu == 1) {
                    return true;
                } else if (sonucKodu == -1) throw new RuntimeException("Görev güncellenemedi: Görev bulunamadı.");
                // spGorev_Guncelle'deki diğer hata kodlarını buraya ekleyin
                else throw new RuntimeException("Görev güncellenemedi. SP bilinmeyen sonuç kodu: " + sonucKodu);
            }
        }
        throw new RuntimeException("Görev güncellenemedi (SP'den sonuç alınamadı).");
    }

    @Transactional
    public boolean updateGorevDurumSP(Integer gorevId, String yeniDurum, String tamamlanmaTarihiStr) {
        // ... (Bu metodun içeriği önceki mesajdaki gibi kalabilir, SP hata kodlarınıza göre güncelleyin) ...
        logger.info("updateGorevDurumSP: GorevID='{}', YeniDurum='{}'", gorevId, yeniDurum);
        if (gorevId == null || yeniDurum == null || yeniDurum.trim().isEmpty()) {
            throw new IllegalArgumentException("Görev ID ve Yeni Durum boş olamaz.");
        }
        LocalDateTime tamamlanmaTarihi = toLocalDateTime(tamamlanmaTarihiStr);

        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spGorev_DurumGuncelle @GorevID = :gorevID, @YeniDurum = :yeniDurum, @TamamlanmaTarihi = :tamamlanmaTarihi")
                .setParameter("gorevID", gorevId)
                .setParameter("yeniDurum", yeniDurum)
                .setParameter("tamamlanmaTarihi", tamamlanmaTarihi)
                .getResultList();
        
        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer sonucKodu = ((Number) result).intValue();
                if (sonucKodu == 1) return true;
                else if (sonucKodu == -1) throw new RuntimeException("Görev durumu güncellenemedi: Görev bulunamadı.");
                else throw new RuntimeException("Görev durumu güncellenemedi. SP bilinmeyen sonuç kodu: " + sonucKodu);
            }
        }
        throw new RuntimeException("Görev durumu güncellenemedi (SP'den sonuç alınamadı).");
    }

    @Transactional
    public boolean deleteGorevSP(Integer id) {
        // ... (Bu metodun içeriği önceki mesajdaki gibi kalabilir, SP hata kodlarınıza göre güncelleyin) ...
        logger.info("deleteGorevSP for ID: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("Silinecek görev için ID belirtilmelidir.");
        }
        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spGorev_Sil @GorevID = :gorevID")
                .setParameter("gorevID", id)
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer sonucKodu = ((Number) result).intValue();
                if (sonucKodu == 1) return true;
                else if (sonucKodu == -1) throw new RuntimeException("Görev silinemedi: Görev bulunamadı.");
                else if (sonucKodu == -2) throw new RuntimeException("Görev silinemedi: Bu görevle ilişkili kullanılmış girdi kayıtları var.");
                // spGorev_Sil'deki diğer hata kodlarını buraya ekleyin
                else throw new RuntimeException("Görev silinemedi. SP bilinmeyen sonuç kodu: " + sonucKodu);
            }
        }
        throw new RuntimeException("Görev silinemedi (SP'den sonuç alınamadı).");
    }
}