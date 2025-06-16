package com.example.tarimtakipbackend.service;

import com.example.tarimtakipbackend.dto.GorevDetayDto;
import com.example.tarimtakipbackend.dto.GorevFormDto;
import com.example.tarimtakipbackend.entity.Ekim;
import com.example.tarimtakipbackend.entity.FaaliyetTipi;
import com.example.tarimtakipbackend.entity.Gorev;
import com.example.tarimtakipbackend.entity.Kullanici;
import com.example.tarimtakipbackend.entity.Tarla;

// Gerekli tüm repository importları
import com.example.tarimtakipbackend.repository.EkimRepository;
import com.example.tarimtakipbackend.repository.FaaliyetTipiRepository;
import com.example.tarimtakipbackend.repository.GorevRepository;
import com.example.tarimtakipbackend.repository.KullaniciRepository;
import com.example.tarimtakipbackend.repository.TarlaRepository;


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

    // Controller'da kullanılan ve Gorev entity'sini manuel oluştururken gerekli olan repository'ler
    @Autowired
    private FaaliyetTipiRepository faaliyetTipiRepository;
    @Autowired
    private EkimRepository ekimRepository;
    @Autowired
    private TarlaRepository tarlaRepository;


    /**
     * Giriş yapmış kullanıcının ID'sini güvenlik bağlamından alır.
     * @return Giriş yapmış kullanıcının ID'si veya null.
     */
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

    /**
     * String formatındaki tarih-saati LocalDateTime'a çevirir.
     * @param dateTimeString yyyy-MM-ddTHH:mm veya yyyy-MM-ddTHH:mm:ss formatında tarih-saat stringi.
     * @return LocalDateTime nesnesi veya null.
     * @throws IllegalArgumentException Geçersiz format durumunda.
     */
    private LocalDateTime toLocalDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateTimeString, DATETIME_FORMATTER);
        } catch (DateTimeParseException e1) {
            // Saniye kısmı eksikse denemek için ":00" ekle
            try {
                return LocalDateTime.parse(dateTimeString + ":00", DATETIME_FORMATTER);
            } catch (DateTimeParseException e2) {
                logger.error("Geçersiz tarih-saat formatı: '{}'. Beklenen format yyyy-MM-ddTHH:mm veya yyyy-MM-ddTHH:mm:ss.", dateTimeString, e2);
                throw new IllegalArgumentException("Geçersiz tarih-saat formatı. Lütfen yyyy-MM-ddTHH:mm formatında girin: " + dateTimeString);
            }
        }
    }

    /**
     * Veritabanından gelen Timestamp nesnesini LocalDateTime'a çevirir.
     * @param dbTimestamp Veritabanından dönen Object.
     * @return LocalDateTime nesnesi veya null.
     */
    private LocalDateTime toLocalDateTimeFromDb(Object dbTimestamp) {
        if (dbTimestamp == null) return null;
        if (dbTimestamp instanceof Timestamp) {
            return ((Timestamp) dbTimestamp).toLocalDateTime();
        }
        logger.warn("Desteklenmeyen veritabanı tarih tipi: {}", dbTimestamp.getClass().getName());
        return null;
    }

    /**
     * Belirtilen ID'ye sahip Görev entity'sini bulur.
     * @param id Görev ID.
     * @return Optional olarak Görev entity'si.
     */
    public Optional<Gorev> findById(Integer id) {
        logger.debug("GorevServis findById ID: {}", id);
        return id == null ? Optional.empty() : gorevRepository.findById(id);
    }

    /**
     * dbo.spGorev_Listele saklı yordamını kullanarak tüm görevleri DTO olarak listeler.
     * @return GorevDetayDto listesi.
     */
    @SuppressWarnings("unchecked")
    public List<GorevDetayDto> getAllGorevlerSP() {
        logger.info("getAllGorevlerSP çağrıldı.");
        try {
            // Saklı yordamı çağır
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spGorev_Listele").getResultList();
            
            // Dönen Object[] listesini GorevDetayDto'ya map et
            return results.stream().map(row -> new GorevDetayDto(
                    (Integer) row[0],           // gorevID
                    (String) row[1],            // faaliyetTipi
                    (String) row[2],            // gorevAciklamasi
                    (Integer) row[3],           // iliskiliEkimID
                    (String) row[4],            // iliskiliTarlaAdi
                    (String) row[5],            // iliskiliUrunAdi
                    (String) row[6],            // atananKullanici
                    (String) row[7],            // talepEdenKullanici
                    toLocalDateTimeFromDb(row[8]),  // planlananBaslangicTarihi
                    toLocalDateTimeFromDb(row[9]),  // planlananBitisTarihi
                    toLocalDateTimeFromDb(row[10]), // tamamlanmaTarihi
                    (String) row[11],           // gorevDurumu
                    (Integer) row[12],          // gorevOnceligi
                    toLocalDateTimeFromDb(row[13])  // gorevKayitTarihi
            )).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("getAllGorevlerSP sırasında hata oluştu: {}", e.getMessage(), e);
            return Collections.emptyList(); // Hata durumunda boş liste döndür
        }
    }

    /**
     * dbo.spGorev_GetirByID saklı yordamını kullanarak belirli bir görevi DTO olarak getirir.
     * @param id Görev ID.
     * @return Optional olarak GorevDetayDto.
     */
    @SuppressWarnings("unchecked")
    public Optional<GorevDetayDto> getGorevDetayByIdSP(Integer id) {
        logger.info("getGorevDetayByIdSP çağrıldı ID: {}", id);
        if (id == null) return Optional.empty();
        try {
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spGorev_GetirByID @GorevID = :id")
                    .setParameter("id", id)
                    .getResultList();
            if (results.isEmpty()) {
                logger.info("Görev bulunamadı ID: {}", id);
                return Optional.empty();
            }
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

    /**
     * dbo.spGorev_Ekle saklı yordamını kullanarak yeni bir görev kaydeder.
     * @param gorevDto Kaydedilecek görevin form verileri.
     * @return Kaydedilen Gorev entity'si.
     * @throws IllegalArgumentException Giriş verileri geçersizse.
     * @throws RuntimeException SP'den beklenmedik bir sonuç veya hata alınırsa.
     */
    @Transactional // Bu metot bir veritabanı işlemi içinde çalışmalı
    public Gorev saveGorevSP(GorevFormDto gorevDto) {
        Integer talepEdenKullaniciId = getCurrentKullaniciId();
        logger.info("saveGorevSP çağrılıyor. FaaliyetTipiID: {}, TalepEdenKID: {}", gorevDto.getFaaliyetTipiId(), talepEdenKullaniciId);
        logger.info("SP'ye gönderilen parametreler: EkimID={}, TarlaID={}, FaaliyetTipiID={}, Aciklama='{}', AtananKullaniciID={}, TalepEdenKullaniciID={}, PlanlananBaslangicTarihi={}, PlanlananBitisTarihi={}, Durum='{}', Oncelik={}",
            gorevDto.getEkimId(), gorevDto.getTarlaId(), gorevDto.getFaaliyetTipiId(), gorevDto.getAciklama(), gorevDto.getAtananKullaniciId(), talepEdenKullaniciId, gorevDto.getPlanlananBaslangicTarihi(), gorevDto.getPlanlananBitisTarihi(), gorevDto.getDurum(), gorevDto.getOncelik());

        LocalDateTime planBas = toLocalDateTime(gorevDto.getPlanlananBaslangicTarihi());
        LocalDateTime planBitis = toLocalDateTime(gorevDto.getPlanlananBitisTarihi());

        List<Object[]> resultList;
        try {
            // Saklı yordamı çağır
            resultList = entityManager.createNativeQuery("EXEC dbo.spGorev_Ekle " +
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
            
            logger.info("SP çağrısı tamamlandı. ResultList boş mu? {}", resultList.isEmpty());
            if (!resultList.isEmpty()) {
                logger.info("ResultList ilk elemanı türü: {}", resultList.get(0).getClass().getName());
                // Eğer SP'den Object[] bekleniyorsa (ResultCode, ResultMessage gibi)
                if (resultList.get(0) instanceof Object[]) {
                    Object[] testRow = (Object[]) resultList.get(0);
                    logger.info("ResultList[0][0] değeri: {}, türü: {}", testRow[0], testRow[0].getClass().getName());
                    logger.info("ResultList[0][1] değeri: {}, türü: {}", testRow[1], testRow[1].getClass().getName());
                } else {
                    // Eğer SP sadece tek bir sayı veya string döndürüyorsa (ResultCode gibi)
                    logger.warn("ResultList'in ilk elemanı Object[] değil (Beklenenden farklı format). Türü: {}", resultList.get(0).getClass().getName());
                    logger.info("ResultList[0] değeri: {}, türü: {}", resultList.get(0), resultList.get(0).getClass().getName());
                }
            }
        } catch (Exception e) {
            // SP çağrısı sırasında fırlayan (THROW edilen) bir SQLException buraya düşer
            logger.error("SP çağrısı sırasında beklenmedik bir hata oluştu: {}", e.getMessage(), e);
            // Hatayı yeniden fırlatarak Spring'in transaction'ı geri almasını sağla
            throw new RuntimeException("Veritabanı işlemi sırasında hata: " + e.getMessage(), e);
        }

        // SP'den dönen sonucu işleme
        if (resultList != null && !resultList.isEmpty()) {
            // SP'nin ResultCode ve ResultMessage döndürdüğü varsayılıyor
            // Eğer SP sadece bir sayı (örn: ID veya hata kodu) döndürüyorsa, Object[]'e cast etmek ClassCastException verir
            // Bu durumda resultRow'u Object[]'e çevirmek yerine direkt ResultList.get(0) alıp tür kontrolü yapmak gerekir.
            // Ama son SP kodunuz 2 sütun (ResultCode, ResultMessage) döndürdüğü için Object[] bekliyoruz.
            if (!(resultList.get(0) instanceof Object[])) {
                 throw new RuntimeException("SP'den beklenmeyen sonuç formatı. Object[] beklenirken " + resultList.get(0).getClass().getName() + " döndü. Lütfen SP dönüş tipini kontrol edin.");
            }
            
            Object[] resultRow = (Object[]) resultList.get(0);
            Integer resultCode = (Integer) resultRow[0];
            String resultMessage = (String) resultRow[1];

            logger.info("SP'den alınan ResultCode: {}, ResultMessage: '{}'", resultCode, resultMessage);

            if (resultCode > 0) { // Başarı durumunda ID döner (pozitif bir sayı)
                logger.info("SP ile yeni görev eklendi, ID: {}. SP Mesajı: '{}'. Geri dönüş nesnesi olu?turuluyor...", resultCode, resultMessage);

                // Yeni eklenen görevin entity'sini manuel olarak oluştur ve döndür
                // Bu kısım, veritabanından çekme hatası yaşanmaması için önemlidir.
                Gorev newGorev = new Gorev();
                newGorev.setGorevID(resultCode); // ResultCode artık ID
                newGorev.setAciklama(gorevDto.getAciklama());
                newGorev.setDurum(gorevDto.getDurum() != null && !gorevDto.getDurum().trim().isEmpty() ? gorevDto.getDurum() : "Atandı");
                newGorev.setOncelik(gorevDto.getOncelik() != null ? gorevDto.getOncelik() : 3);
                newGorev.setPlanlananBaslangicTarihi(planBas);
                newGorev.setPlanlananBitisTarihi(planBitis);
                newGorev.setTamamlanmaTarihi(toLocalDateTime(gorevDto.getTamamlanmaTarihi())); // Tamamlanma tarihi sadece güncellenirken set edilebilir
                newGorev.setKayitTarihi(LocalDateTime.now()); // Varsayılan olarak şimdi set et, DB kendi değerini verecektir
                // newGorev.setGuncellemeTarihi(null); // İlk kayıt olduğu için null

                // İlişkili entity'leri ID'leriyle bulup set et
                // Bu kısım, ilgili repository'lerin doğru import edildiğinden ve @Autowired edildiğinden emin olunmalıdır
                if (gorevDto.getEkimId() != null) {
                    ekimRepository.findById(gorevDto.getEkimId()).ifPresent(newGorev::setEkim);
                }
                if (gorevDto.getTarlaId() != null) {
                    tarlaRepository.findById(gorevDto.getTarlaId()).ifPresent(newGorev::setTarla);
                }
                if (gorevDto.getFaaliyetTipiId() != null) {
                    faaliyetTipiRepository.findById(gorevDto.getFaaliyetTipiId()).ifPresent(newGorev::setFaaliyetTipi);
                }
                if (gorevDto.getAtananKullaniciId() != null) {
                    kullaniciRepository.findById(gorevDto.getAtananKullaniciId()).ifPresent(newGorev::setAtananKullanici);
                }
                if (talepEdenKullaniciId != null) { // Talep eden kullanıcı her zaman mevcut olmalı
                    kullaniciRepository.findById(talepEdenKullaniciId).ifPresent(newGorev::setTalepEdenKullanici);
                }

                logger.info("Yeni Gorev entity'si oluşturuldu ve döndürülüyor. ID: {}", newGorev.getGorevID());
                return newGorev; // Oluşturulan Gorev nesnesini döndür

            } else { // SP'den hata kodu döndüğünde (<= 0)
                String errorMessage = "Görev eklenemedi: " + resultMessage;
                logger.error("SP tarafından hata kodu döndürüldü: {} - {}", resultCode, errorMessage);
                // SP'den dönen özel hata kodlarını burada daha detaylı işleyebiliriz
                if (resultCode == -1) throw new IllegalArgumentException(errorMessage + " (Açıklama boş olamaz)");
                else if (resultCode == -2) throw new IllegalArgumentException(errorMessage + " (Ekim veya Tarla ID belirtilmeli)");
                else if (resultCode == -3) throw new IllegalArgumentException(errorMessage + " (Geçersiz Ekim ID)");
                else if (resultCode == -4) throw new IllegalArgumentException(errorMessage + " (Geçersiz Tarla ID)");
                else if (resultCode == -5) throw new IllegalArgumentException(errorMessage + " (Geçersiz FaaliyetTipiID)");
                else if (resultCode == -6) throw new IllegalArgumentException(errorMessage + " (Geçersiz AtananKullaniciID)");
                else if (resultCode == -7) throw new IllegalArgumentException(errorMessage + " (Geçersiz TalepEdenKullaniciID)");
                else if (resultCode == -8) throw new IllegalArgumentException(errorMessage + " (Bitiş tarihi başlangıç tarihinden önce olamaz)");
                else if (resultCode == -999) throw new RuntimeException(errorMessage + " (Beklenmedik bir veritabanı hatası oluştu, lütfen DB loglarına bakın.)");
                else throw new RuntimeException(errorMessage + " (SP bilinmeyen bir sonuç kodu döndürdü: " + resultCode + ")");
            }
        }
        // Eğer SP'den hiçbir sonuç kümesi dönmezse (çok nadir, genellikle bir hata vardır)
        throw new RuntimeException("Görev eklenemedi (SP'den sonuç alınamadı veya beklenmeyen format).");
    }

    /**
     * dbo.spGorev_Guncelle saklı yordamını kullanarak mevcut bir görevi günceller.
     * NOT: Bu metot da SP'den ResultCode ve ResultMessage döndüğünü varsayar.
     * @param gorevDto Güncellenecek görevin form verileri.
     * @return Güncelleme başarılıysa true, değilse false.
     * @throws IllegalArgumentException Giriş verileri geçersizse.
     * @throws RuntimeException SP'den beklenmedik bir sonuç veya hata alınırsa.
     */
    @Transactional
    public boolean updateGorevSP(GorevFormDto gorevDto) {
        logger.info("updateGorevSP: GorevID='{}'", gorevDto.getGorevID());
        if (gorevDto.getGorevID() == null) {
            throw new IllegalArgumentException("Güncellenecek görev için ID belirtilmelidir.");
        }
        LocalDateTime planlananBaslangic = toLocalDateTime(gorevDto.getPlanlananBaslangicTarihi());
        LocalDateTime planlananBitis = toLocalDateTime(gorevDto.getPlanlananBitisTarihi());
        LocalDateTime tamamlanma = toLocalDateTime(gorevDto.getTamamlanmaTarihi());
        Integer talepEdenKIDForUpdate = getCurrentKullaniciId();

        List<Object[]> resultList;
        try {
            resultList = entityManager.createNativeQuery("EXEC dbo.spGorev_Guncelle " +
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
        } catch (Exception e) {
            logger.error("SP (spGorev_Guncelle) çağrısı sırasında beklenmedik bir hata oluştu: {}", e.getMessage(), e);
            throw new RuntimeException("Veritabanı işlemi sırasında hata: " + e.getMessage(), e);
        }
        
        if (resultList != null && !resultList.isEmpty()) {
            if (!(resultList.get(0) instanceof Object[])) {
                 throw new RuntimeException("SP (spGorev_Guncelle) beklenmeyen sonuç formatı. Object[] beklenirken " + resultList.get(0).getClass().getName() + " döndü.");
            }
            Object[] resultRow = (Object[]) resultList.get(0);
            Integer resultCode = (Integer) resultRow[0];
            String resultMessage = (String) resultRow[1];
            
            logger.info("SP (spGorev_Guncelle) ResultCode: {}, ResultMessage: '{}'", resultCode, resultMessage);

            if (resultCode == 1) { // Başarı kodu 1 ise
                return true;
            } else {
                String errorMessage = "Görev güncellenemedi: " + resultMessage;
                if (resultCode == -1) throw new RuntimeException(errorMessage + " (Görev bulunamadı).");
                else throw new RuntimeException(errorMessage + " (SP bilinmeyen sonuç kodu: " + resultCode + ")");
            }
        }
        throw new RuntimeException("Görev güncellenemedi (SP'den sonuç alınamadı).");
    }

    /**
     * dbo.spGorev_DurumGuncelle saklı yordamını kullanarak bir görevin durumunu günceller.
     * NOT: Bu metot da SP'den ResultCode ve ResultMessage döndüğünü varsayar.
     * @param gorevId Güncellenecek görev ID.
     * @param yeniDurum Görevin yeni durumu.
     * @param tamamlanmaTarihiStr Tamamlanma tarihi (isteğe bağlı).
     * @return Güncelleme başarılıysa true, değilse false.
     * @throws IllegalArgumentException Giriş verileri geçersizse.
     * @throws RuntimeException SP'den beklenmedik bir sonuç veya hata alınırsa.
     */
    @Transactional
    public boolean updateGorevDurumSP(Integer gorevId, String yeniDurum, String tamamlanmaTarihiStr) {
        logger.info("updateGorevDurumSP: GorevID='{}', YeniDurum='{}'", gorevId, yeniDurum);
        if (gorevId == null || yeniDurum == null || yeniDurum.trim().isEmpty()) {
            throw new IllegalArgumentException("Görev ID ve Yeni Durum boş olamaz.");
        }
        LocalDateTime tamamlanmaTarihi = toLocalDateTime(tamamlanmaTarihiStr);

        List<Object[]> resultList;
        try {
            resultList = entityManager.createNativeQuery("EXEC dbo.spGorev_DurumGuncelle @GorevID = :gorevID, @YeniDurum = :yeniDurum, @TamamlanmaTarihi = :tamamlanmaTarihi")
                    .setParameter("gorevID", gorevId)
                    .setParameter("yeniDurum", yeniDurum)
                    .setParameter("tamamlanmaTarihi", tamamlanmaTarihi)
                    .getResultList();
        } catch (Exception e) {
            logger.error("SP (spGorev_DurumGuncelle) çağrısı sırasında beklenmedik bir hata oluştu: {}", e.getMessage(), e);
            throw new RuntimeException("Veritabanı işlemi sırasında hata: " + e.getMessage(), e);
        }
        
        if (resultList != null && !resultList.isEmpty()) {
            if (!(resultList.get(0) instanceof Object[])) {
                 throw new RuntimeException("SP (spGorev_DurumGuncelle) beklenmeyen sonuç formatı. Object[] beklenirken " + resultList.get(0).getClass().getName() + " döndü.");
            }
            Object[] resultRow = (Object[]) resultList.get(0);
            Integer resultCode = (Integer) resultRow[0];
            String resultMessage = (String) resultRow[1];

            logger.info("SP (spGorev_DurumGuncelle) ResultCode: {}, ResultMessage: '{}'", resultCode, resultMessage);

            if (resultCode == 1) return true;
            else {
                String errorMessage = "Görev durumu güncellenemedi: " + resultMessage;
                if (resultCode == -1) throw new RuntimeException(errorMessage + " (Görev bulunamadı).");
                else throw new RuntimeException(errorMessage + " (SP bilinmeyen sonuç kodu: " + resultCode + ")");
            }
        }
        throw new RuntimeException("Görev durumu güncellenemedi (SP'den sonuç alınamadı).");
    }

    /**
     * dbo.spGorev_Sil saklı yordamını kullanarak bir görevi siler.
     * NOT: Bu metot da SP'den ResultCode ve ResultMessage döndüğünü varsayar.
     * @param id Silinecek görev ID.
     * @return Silme başarılıysa true, değilse false.
     * @throws IllegalArgumentException Giriş ID'si boşsa.
     * @throws RuntimeException SP'den beklenmedik bir sonuç veya hata alınırsa.
     */
    @Transactional
    public boolean deleteGorevSP(Integer id) {
        logger.info("deleteGorevSP for ID: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("Silinecek görev için ID belirtilmelidir.");
        }
        List<Object[]> resultList;
        try {
            resultList = entityManager.createNativeQuery("EXEC dbo.spGorev_Sil @GorevID = :gorevID")
                    .setParameter("gorevID", id)
                    .getResultList();
        } catch (Exception e) {
            logger.error("SP (spGorev_Sil) çağrısı sırasında beklenmedik bir hata oluştu: {}", e.getMessage(), e);
            throw new RuntimeException("Veritabanı işlemi sırasında hata: " + e.getMessage(), e);
        }
        
        if (resultList != null && !resultList.isEmpty()) {
            if (!(resultList.get(0) instanceof Object[])) {
                 throw new RuntimeException("SP (spGorev_Sil) beklenmeyen sonuç formatı. Object[] beklenirken " + resultList.get(0).getClass().getName() + " döndü.");
            }
            Object[] resultRow = (Object[]) resultList.get(0);
            Integer resultCode = (Integer) resultRow[0];
            String resultMessage = (String) resultRow[1];

            logger.info("SP (spGorev_Sil) ResultCode: {}, ResultMessage: '{}'", resultCode, resultMessage);

            if (resultCode == 1) return true;
            else {
                String errorMessage = "Görev silinemedi: " + resultMessage;
                if (resultCode == -1) throw new RuntimeException(errorMessage + " (Görev bulunamadı).");
                else if (resultCode == -2) throw new RuntimeException(errorMessage + " (Bu görevle ilişkili kullanılmış girdi kayıtları var).");
                else throw new RuntimeException(errorMessage + " (SP bilinmeyen sonuç kodu: " + resultCode + ")");
            }
        }
        throw new RuntimeException("Görev silinemedi (SP'den sonuç alınamadı).");
    }
}