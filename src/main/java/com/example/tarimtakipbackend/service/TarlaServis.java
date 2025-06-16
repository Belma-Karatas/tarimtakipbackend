package com.example.tarimtakipbackend.service;

import com.example.tarimtakipbackend.dto.TarlaDetayDto; // Eklendi
import com.example.tarimtakipbackend.entity.Tarla;
// import com.example.tarimtakipbackend.repository.TarlaRepository; // Artık direkt kullanmayacağız
import com.example.tarimtakipbackend.repository.ToprakTipiRepository; // Formda dropdown için gerekli olabilir
import com.example.tarimtakipbackend.repository.SulamaSistemiRepository; // Formda dropdown için gerekli olabilir
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp; // SP'den dönen DATETIME2 için
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TarlaServis {

    private static final Logger logger = LoggerFactory.getLogger(TarlaServis.class);

    @PersistenceContext
    private EntityManager entityManager;

    // Dropdown'lar için hala repository'lere ihtiyaç duyabiliriz TarlaController'da
    @Autowired
    private ToprakTipiRepository toprakTipiRepository;
    @Autowired
    private SulamaSistemiRepository sulamaSistemiRepository;


    @SuppressWarnings("unchecked")
    public List<TarlaDetayDto> getAllTarlalarSP() {
        logger.info("getAllTarlalarSP çağrıldı.");
        List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spTarla_Listele").getResultList();
        return results.stream().map(row -> new TarlaDetayDto(
                (Integer) row[0],          // TarlaID
                (String) row[1],           // TarlaAdi
                (row[2] != null ? ((BigDecimal) row[2]) : null), // Alan
                (String) row[3],           // TarlaAciklamasi (vTarlaDetaylari'dan)
                (String) row[4],           // ToprakTipi (vTarlaDetaylari'dan)
                (String) row[5],           // SulamaSistemi (vTarlaDetaylari'dan)
                (row[6] != null ? ((Timestamp) row[6]).toLocalDateTime() : null), // TarlaKayitTarihi
                (row[7] != null ? ((Timestamp) row[7]).toLocalDateTime() : null)  // TarlaGuncellemeTarihi
                // vTarlaDetaylari view'ınızdaki sütun sırasına ve tiplerine göre bu maplemeyi DİKKATLİCE yapın!
        )).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public Optional<TarlaDetayDto> getTarlaDetayByIdSP(Integer id) {
        logger.info("getTarlaDetayByIdSP çağrıldı ID: {}", id);
        List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spTarla_GetirByID @TarlaID = :id")
                .setParameter("id", id)
                .getResultList();
        if (results.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = results.get(0);
        return Optional.of(new TarlaDetayDto(
                (Integer) row[0], (String) row[1], (row[2] != null ? (BigDecimal) row[2] : null), (String) row[3],
                (String) row[4], (String) row[5],
                (row[6] != null ? ((Timestamp) row[6]).toLocalDateTime() : null),
                (row[7] != null ? ((Timestamp) row[7]).toLocalDateTime() : null)
        ));
    }

    @Transactional
    public Tarla saveTarlaSP(Tarla tarla, Integer toprakTipiId, Integer sulamaSistemiId) {
        // Tarla nesnesinden bilgileri alıp SP'ye gönderiyoruz
        logger.info("saveTarlaSP: TarlaAdi='{}'", tarla.getTarlaAdi());
        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spTarla_Ekle @TarlaAdi = :tarlaAdi, @Alan = :alan, @Aciklama = :aciklama, @ToprakTipiID = :toprakTipiID, @SulamaSistemiID = :sulamaSistemiID")
                .setParameter("tarlaAdi", tarla.getTarlaAdi())
                .setParameter("alan", tarla.getAlan())
                .setParameter("aciklama", tarla.getAciklama())
                .setParameter("toprakTipiID", toprakTipiId)
                .setParameter("sulamaSistemiID", sulamaSistemiId)
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer yeniTarlaId = ((Number) result).intValue();
                if (yeniTarlaId > 0) {
                    tarla.setTarlaID(yeniTarlaId);
                    // Diğer alanlar zaten tarla nesnesinde var. Kayıt ve Güncelleme Tarihleri DB'de set ediliyor.
                    // İlişkili nesneleri (ToprakTipi, SulamaSistemi) ID ile tekrar çekip set edebiliriz gerekirse.
                    if(toprakTipiId != null) tarla.setToprakTipi(toprakTipiRepository.findById(toprakTipiId).orElse(null));
                    if(sulamaSistemiId != null) tarla.setSulamaSistemi(sulamaSistemiRepository.findById(sulamaSistemiId).orElse(null));
                    logger.info("SP ile yeni tarla eklendi, ID: {}", yeniTarlaId);
                    return tarla; // ID'si ve ilişkileri set edilmiş tarla
                } else if (yeniTarlaId == -2) { throw new IllegalArgumentException("Tarla eklenemedi: Geçersiz Toprak Tipi ID.");
                } else if (yeniTarlaId == -3) { throw new IllegalArgumentException("Tarla eklenemedi: Geçersiz Sulama Sistemi ID."); }
            }
        }
        throw new RuntimeException("Tarla eklenemedi (SP'den ID veya hata kodu alınamadı).");
    }

    @Transactional
    public boolean updateTarlaSP(Tarla tarla, Integer toprakTipiId, Integer sulamaSistemiId) {
        logger.info("updateTarlaSP: ID={}", tarla.getTarlaID());
        if (tarla.getTarlaID() == null) {
            throw new IllegalArgumentException("Güncellenecek tarla için ID belirtilmelidir.");
        }
        // GuncellemeTarihi SP içinde GETDATE() ile set ediliyor.

        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spTarla_Guncelle @TarlaID = :tarlaID, @TarlaAdi = :tarlaAdi, @Alan = :alan, @Aciklama = :aciklama, @ToprakTipiID = :toprakTipiID, @SulamaSistemiID = :sulamaSistemiID")
                .setParameter("tarlaID", tarla.getTarlaID())
                .setParameter("tarlaAdi", tarla.getTarlaAdi())
                .setParameter("alan", tarla.getAlan())
                .setParameter("aciklama", tarla.getAciklama())
                .setParameter("toprakTipiID", toprakTipiId)
                .setParameter("sulamaSistemiID", sulamaSistemiId)
                .getResultList();
        
        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer sonucKodu = ((Number) result).intValue();
                if (sonucKodu == 1) { logger.info("SP ile tarla güncellendi, ID: {}", tarla.getTarlaID()); return true; }
                else if (sonucKodu == -1) { throw new RuntimeException("Tarla güncellenemedi: Tarla bulunamadı."); }
                else if (sonucKodu == -2) { throw new IllegalArgumentException("Tarla güncellenemedi: Geçersiz Toprak Tipi ID."); }
                else if (sonucKodu == -3) { throw new IllegalArgumentException("Tarla güncellenemedi: Geçersiz Sulama Sistemi ID."); }
                else { throw new RuntimeException("Tarla güncellenemedi. SP'den bilinmeyen sonuç kodu: " + sonucKodu); }
            }
        }
        throw new RuntimeException("Tarla güncellenemedi (SP'den sonuç alınamadı).");
    }

    @Transactional
    public boolean deleteTarlaSP(Integer id) {
        logger.info("deleteTarlaSP for ID: {}", id);
        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spTarla_Sil @TarlaID = :tarlaID")
                .setParameter("tarlaID", id)
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer sonucKodu = ((Number) result).intValue();
                if (sonucKodu == 1) { logger.info("SP ile tarla silindi, ID: {}", id); return true; }
                else if (sonucKodu == -1) { throw new RuntimeException("Tarla silinemedi: Silinecek tarla bulunamadı."); }
                else if (sonucKodu == -2) { throw new RuntimeException("Tarla silinemedi: Bu tarlada ekim kayıtları var."); }
                else if (sonucKodu == -3) { throw new RuntimeException("Tarla silinemedi: Bu tarlaya atanmış görevler var."); }
                else if (sonucKodu == -4) { throw new RuntimeException("Tarla silinemedi: Bu tarlada tanımlı sensörler var."); }
                else { throw new RuntimeException("Tarla silinemedi. SP'den bilinmeyen sonuç kodu: " + sonucKodu); }
            }
        }
        throw new RuntimeException("Tarla silinemedi (SP'den sonuç alınamadı).");
    }
}