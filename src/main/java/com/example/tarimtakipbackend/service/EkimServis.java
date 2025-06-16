package com.example.tarimtakipbackend.service;

import com.example.tarimtakipbackend.dto.EkimDetayDto;
import com.example.tarimtakipbackend.dto.EkimFormDto;
import com.example.tarimtakipbackend.entity.Ekim;
import com.example.tarimtakipbackend.entity.Kullanici; // Kullanici entity importu
import com.example.tarimtakipbackend.repository.EkimRepository;
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

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections; // Boş liste için
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EkimServis {

    private static final Logger logger = LoggerFactory.getLogger(EkimServis.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EkimRepository ekimRepository;

    @Autowired
    private KullaniciRepository kullaniciRepository;

    // Giriş yapmış kullanıcının ID'sini alır
    private Integer getCurrentKullaniciId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            Optional<Kullanici> kullaniciOpt = kullaniciRepository.findByKullaniciAdi(username);
            // Kullanici entity'sinde ID alanı 'kullaniciID' ise Lombok getKullaniciID() üretir.
            return kullaniciOpt.map(Kullanici::getKullaniciID).orElse(null);
        }
        logger.warn("Giriş yapmış kullanıcı bulunamadı veya anonymousUser.");
        return null;
    }

    // String tarihi (yyyy-MM-dd) java.sql.Date'e çevirir
    private Date toSqlDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return Date.valueOf(LocalDate.parse(dateString));
        } catch (DateTimeParseException e) {
            logger.error("Geçersiz tarih formatı: '{}'. Beklenen format yyyy-MM-dd.", dateString, e);
            // Kullanıcıya daha anlamlı bir mesaj vermek için spesifik bir exception fırlatılabilir.
            throw new IllegalArgumentException("Geçersiz tarih formatı. Lütfen yyyy-MM-dd formatında girin: " + dateString);
        }
    }

    // java.sql.Date veya java.sql.Timestamp nesnesini LocalDate'e çevirir
    private LocalDate toLocalDate(Object sqlDateOrTimestamp) {
        if (sqlDateOrTimestamp == null) return null;
        if (sqlDateOrTimestamp instanceof java.sql.Date) {
            return ((java.sql.Date) sqlDateOrTimestamp).toLocalDate();
        }
        if (sqlDateOrTimestamp instanceof java.sql.Timestamp) { // DATETIME2 genellikle Timestamp olarak gelir
            return ((java.sql.Timestamp) sqlDateOrTimestamp).toLocalDateTime().toLocalDate();
        }
        logger.warn("Desteklenmeyen tarih tipi LocalDate'e çevrilmeye çalışıldı: {}", sqlDateOrTimestamp.getClass().getName());
        // Varsayılan olarak null döndür veya bir istisna fırlat
        return null;
        // throw new IllegalArgumentException("Desteklenmeyen tarih tipi: " + sqlDateOrTimestamp.getClass().getName());
    }

    // Ekim entity'sini ID ile bulur (Controller'da düzenleme formu için gerekli)
    public Optional<Ekim> findById(Integer id) {
        logger.debug("EkimServis findById çağrıldı ID: {}", id);
        if (id == null) {
            return Optional.empty();
        }
        return ekimRepository.findById(id);
    }


    @SuppressWarnings("unchecked")
    public List<EkimDetayDto> getAllEkimlerSP() {
        logger.info("getAllEkimlerSP çağrıldı.");
        try {
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spEkim_Listele").getResultList();
            return results.stream().map(row -> new EkimDetayDto(
                    (Integer) row[0], // EkimID
                    (String) row[1],  // TarlaAdi
                    (String) row[2],  // UrunAdi
                    toLocalDate(row[3]), // EkimTarihi
                    toLocalDate(row[4]), // PlanlananHasatTarihi
                    (String) row[5],  // EkilenMiktarAciklama
                    (String) row[6],  // EkimDurumu
                    (String) row[7],  // EkimNotlari
                    (String) row[8],  // KaydedenKullanici
                    (row[9] instanceof Timestamp ? ((Timestamp) row[9]).toLocalDateTime() : null), // EkimKayitTarihi
                    (row[10] instanceof Timestamp ? ((Timestamp) row[10]).toLocalDateTime() : null) // EkimGuncellemeTarihi
            )).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("getAllEkimlerSP sırasında hata oluştu: {}", e.getMessage(), e);
            return Collections.emptyList(); // Hata durumunda boş liste döndür
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<EkimDetayDto> getEkimDetayByIdSP(Integer id) {
        logger.info("getEkimDetayByIdSP çağrıldı ID: {}", id);
        if (id == null) return Optional.empty();
        try {
            List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spEkim_GetirByID @EkimID = :id")
                    .setParameter("id", id)
                    .getResultList();
            if (results.isEmpty()) {
                return Optional.empty();
            }
            Object[] row = results.get(0);
            return Optional.of(new EkimDetayDto(
                    (Integer) row[0], (String) row[1], (String) row[2], toLocalDate(row[3]),
                    toLocalDate(row[4]), (String) row[5], (String) row[6], (String) row[7],
                    (String) row[8],
                    (row[9] instanceof Timestamp ? ((Timestamp) row[9]).toLocalDateTime() : null),
                    (row[10] instanceof Timestamp ? ((Timestamp) row[10]).toLocalDateTime() : null)
            ));
        } catch (Exception e) {
            logger.error("getEkimDetayByIdSP sırasında hata oluştu ID {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Transactional
    public Ekim saveEkimSP(EkimFormDto ekimDto) {
        Integer kaydedenKullaniciId = getCurrentKullaniciId();
        // if (kaydedenKullaniciId == null) {
        //     throw new IllegalStateException("Ekim kaydı için giriş yapmış kullanıcı bulunamadı.");
        // }

        logger.info("saveEkimSP: TarlaID='{}', UrunID='{}', KaydedenKullaniciID='{}'",
                ekimDto.getTarlaId(), ekimDto.getUrunId(), kaydedenKullaniciId);

        Date sqlEkimTarihi = toSqlDate(ekimDto.getEkimTarihi());
        Date sqlPlanlananHasatTarihi = toSqlDate(ekimDto.getPlanlananHasatTarihi());

        if (sqlEkimTarihi == null) {
            throw new IllegalArgumentException("Ekim tarihi boş olamaz.");
        }

        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spEkim_Ekle " +
                        "@TarlaID = :tarlaID, @UrunID = :urunID, @EkimTarihi = :ekimTarihi, " +
                        "@PlanlananHasatTarihi = :planlananHasatTarihi, @EkilenMiktarAciklama = :ekilenMiktar, " +
                        "@Durum = :durum, @Notlar = :notlar, @KaydedenKullaniciID = :kaydedenKID")
                .setParameter("tarlaID", ekimDto.getTarlaId())
                .setParameter("urunID", ekimDto.getUrunId())
                .setParameter("ekimTarihi", sqlEkimTarihi)
                .setParameter("planlananHasatTarihi", sqlPlanlananHasatTarihi) // SP null kabul ediyorsa sorun yok
                .setParameter("ekilenMiktar", ekimDto.getEkilenMiktarAciklama())
                .setParameter("durum", ekimDto.getDurum() != null && !ekimDto.getDurum().trim().isEmpty() ? ekimDto.getDurum() : "Planlandı")
                .setParameter("notlar", ekimDto.getNotlar())
                .setParameter("kaydedenKID", kaydedenKullaniciId) // SP null kabul ediyorsa sorun yok
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer yeniEkimId = ((Number) result).intValue();
                if (yeniEkimId > 0) {
                    logger.info("SP ile yeni ekim eklendi, ID: {}", yeniEkimId);
                    return ekimRepository.findById(yeniEkimId)
                            .orElseThrow(() -> new RuntimeException("Eklenen ekim kaydı veritabanından çekilemedi ID: " + yeniEkimId));
                } else if (yeniEkimId == -2) throw new IllegalArgumentException("Ekim eklenemedi: Geçersiz Tarla ID.");
                else if (yeniEkimId == -3) throw new IllegalArgumentException("Ekim eklenemedi: Geçersiz Ürün ID.");
                else if (yeniEkimId == -4) throw new IllegalArgumentException("Ekim eklenemedi: Geçersiz Kaydeden Kullanıcı ID.");
                else if (yeniEkimId == -5) throw new IllegalArgumentException("Ekim eklenemedi: Planlanan hasat tarihi ekim tarihinden önce olamaz.");
                // Diğer özel hata kodları SP'nizden geliyorsa buraya ekleyin
                else throw new RuntimeException("Ekim eklenemedi. Saklı yordam bilinmeyen bir hata kodu döndürdü: " + yeniEkimId);
            }
        }
        throw new RuntimeException("Ekim eklenemedi (SP'den ID veya geçerli bir sonuç alınamadı).");
    }

    @Transactional
    public boolean updateEkimSP(EkimFormDto ekimDto) {
        Integer guncelleyenKullaniciId = getCurrentKullaniciId();
        logger.info("updateEkimSP: EkimID='{}', GuncelleyenKullaniciID='{}'", ekimDto.getEkimID(), guncelleyenKullaniciId);
        if (ekimDto.getEkimID() == null) {
            throw new IllegalArgumentException("Güncellenecek ekim için ID belirtilmelidir.");
        }

        Date sqlEkimTarihi = toSqlDate(ekimDto.getEkimTarihi());
        Date sqlPlanlananHasatTarihi = toSqlDate(ekimDto.getPlanlananHasatTarihi());

        if (sqlEkimTarihi == null) {
            throw new IllegalArgumentException("Ekim tarihi boş olamaz.");
        }
        // SP'niz güncelleyen kullanıcıyı `KaydedenKullaniciID` olarak mı alıyor yoksa ayrı bir parametresi mi var?
        // Şimdilik `KaydedenKullaniciID`'yi güncelleyen kullanıcı olarak set ediyorum.
        // SP'nizdeki `spEkim_Guncelle`'nin parametrelerini kontrol edin.
        Integer kullaniciIdParam = guncelleyenKullaniciId; // Veya ekimDto.getKaydedenKullaniciId() (eğer formda varsa ve değiştirilmiyorsa)

        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spEkim_Guncelle " +
                        "@EkimID = :ekimID, @TarlaID = :tarlaID, @UrunID = :urunID, @EkimTarihi = :ekimTarihi, " +
                        "@PlanlananHasatTarihi = :planlananHasatTarihi, @EkilenMiktarAciklama = :ekilenMiktar, " +
                        "@Durum = :durum, @Notlar = :notlar, @KaydedenKullaniciID = :kaydedenKID")
                .setParameter("ekimID", ekimDto.getEkimID())
                .setParameter("tarlaID", ekimDto.getTarlaId())
                .setParameter("urunID", ekimDto.getUrunId())
                .setParameter("ekimTarihi", sqlEkimTarihi)
                .setParameter("planlananHasatTarihi", sqlPlanlananHasatTarihi)
                .setParameter("ekilenMiktar", ekimDto.getEkilenMiktarAciklama())
                .setParameter("durum", ekimDto.getDurum())
                .setParameter("notlar", ekimDto.getNotlar())
                .setParameter("kaydedenKID", kullaniciIdParam) // Bu parametrenin SP'nizdeki karşılığını kontrol edin
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer sonucKodu = ((Number) result).intValue();
                if (sonucKodu == 1) {
                    logger.info("SP ile ekim güncellendi, ID: {}", ekimDto.getEkimID());
                    return true;
                }
                else if (sonucKodu == -1) throw new RuntimeException("Ekim güncellenemedi: Ekim kaydı bulunamadı.");
                else if (sonucKodu == -2) throw new IllegalArgumentException("Ekim güncellenemedi: Geçersiz Tarla ID.");
                else if (sonucKodu == -3) throw new IllegalArgumentException("Ekim güncellenemedi: Geçersiz Ürün ID.");
                else if (sonucKodu == -4) throw new IllegalArgumentException("Ekim güncellenemedi: Geçersiz Kaydeden Kullanıcı ID.");
                else if (sonucKodu == -5) throw new IllegalArgumentException("Ekim güncellenemedi: Planlanan hasat tarihi ekim tarihinden önce olamaz.");
                else throw new RuntimeException("Ekim güncellenemedi. SP'den bilinmeyen sonuç kodu: " + sonucKodu);
            }
        }
        throw new RuntimeException("Ekim güncellenemedi (SP'den sonuç alınamadı).");
    }

    @Transactional
    public boolean deleteEkimSP(Integer id) {
        logger.info("deleteEkimSP for ID: {}", id);
        if (id == null) {
             throw new IllegalArgumentException("Silinecek ekim için ID belirtilmelidir.");
        }
        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spEkim_Sil @EkimID = :ekimID")
                .setParameter("ekimID", id)
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer sonucKodu = ((Number) result).intValue();
                if (sonucKodu == 1) {
                    logger.info("SP ile ekim silindi, ID: {}", id);
                    return true;
                }
                else if (sonucKodu == -1) throw new RuntimeException("Ekim silinemedi: Silinecek ekim kaydı bulunamadı.");
                else if (sonucKodu == -2) throw new RuntimeException("Ekim silinemedi: Bu ekimle ilişkili görevler var.");
                else if (sonucKodu == -3) throw new RuntimeException("Ekim silinemedi: Bu ekimde kullanılmış girdiler var.");
                else if (sonucKodu == -4) throw new RuntimeException("Ekim silinemedi: Bu ekimden yapılmış hasat kayıtları var.");
                else throw new RuntimeException("Ekim silinemedi. SP'den bilinmeyen sonuç kodu: " + sonucKodu);
            }
        }
        throw new RuntimeException("Ekim silinemedi (SP'den sonuç alınamadı).");
    }
}