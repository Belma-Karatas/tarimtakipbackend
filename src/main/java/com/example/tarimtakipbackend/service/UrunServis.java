package com.example.tarimtakipbackend.service;

import com.example.tarimtakipbackend.dto.UrunDetayDto;
import com.example.tarimtakipbackend.dto.UrunFormDto;
import com.example.tarimtakipbackend.entity.Urun;
import com.example.tarimtakipbackend.repository.UrunKategorisiRepository;
import com.example.tarimtakipbackend.repository.UrunRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UrunServis {

    private static final Logger logger = LoggerFactory.getLogger(UrunServis.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UrunRepository urunRepository;

    @Autowired
    private UrunKategorisiRepository urunKategorisiRepository;

    @SuppressWarnings("unchecked")
    public List<UrunDetayDto> getAllUrunlerSP() {
        logger.info("getAllUrunlerSP çağrıldı.");
        // SP'nizin (veya kullandığı view'ın) döndürdüğü sütun sırasına göre map'leme:
        // 0: UrunID (Integer)
        // 1: UrunAdi (String)
        // 2: KategoriID (Integer) <<<--- YENİ SÜTUN
        // 3: KategoriAdi (String)
        // 4: UrunBirimi (String)
        // 5: UrunAciklamasi (String)
        // 6: UrunKayitTarihi (Timestamp -> LocalDateTime)
        List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spUrun_Listele").getResultList();
        return results.stream().map(row -> new UrunDetayDto(
                (Integer) row[0],    // UrunID
                (String) row[1],     // UrunAdi
                (Integer) row[2],    // KategoriID  <-- GÜNCELLENDİ
                (String) row[3],     // KategoriAdi
                (String) row[4],     // UrunBirimi
                (String) row[5],     // UrunAciklamasi
                (row[6] != null ? ((Timestamp) row[6]).toLocalDateTime() : null) // UrunKayitTarihi
        )).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public Optional<UrunDetayDto> getUrunDetayByIdSP(Integer id) {
        logger.info("getUrunDetayByIdSP çağrıldı ID: {}", id);
        // SP'nizin (veya kullandığı view'ın) döndürdüğü sütun sırasına göre map'leme:
        List<Object[]> results = entityManager.createNativeQuery("EXEC dbo.spUrun_GetirByID @UrunID = :id")
                .setParameter("id", id)
                .getResultList();
        if (results.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = results.get(0);
        return Optional.of(new UrunDetayDto(
                (Integer) row[0],    // UrunID
                (String) row[1],     // UrunAdi
                (Integer) row[2],    // KategoriID  <-- GÜNCELLENDİ
                (String) row[3],     // KategoriAdi
                (String) row[4],     // UrunBirimi
                (String) row[5],     // UrunAciklamasi
                (row[6] != null ? ((Timestamp) row[6]).toLocalDateTime() : null) // UrunKayitTarihi
        ));
    }

    // saveUrunSP, updateUrunSP, deleteUrunSP metotları aynı kalabilir,
    // çünkü onlar UrunFormDto kullanıyor ve KategoriID'yi zaten alıyorlar.
    // Sadece emin olmak için önceki mesajdaki UrunServis kodunu kullanabilirsiniz.
    // Ben sadece getAll ve getById kısımlarını buraya aldım.
    // Önceki mesajımdaki tam UrunServis kodunu kullanmanız daha iyi olur.
    // O kodda saveUrunSP, updateUrunSP, deleteUrunSP metotları da vardı.
    // BURAYA ÖNCEKİ MESAJDAKİ TAM URUNSERVIS KODUNU YAPIŞTIRIN, SADECE
    // getAllUrunlerSP ve getUrunDetayByIdSP METOTLARINDAKİ MAPLEMEYİ
    // YUKARIDAKİ GİBİ GÜNCELLEDİĞİNİZDEN EMİN OLUN.
    // (Önceki mesajdaki UrunServis.java içeriğini buraya tekrar yapıştırıyorum, map'leme düzeltilmiş haliyle)

    @Transactional
    public Urun saveUrunSP(UrunFormDto urunDto) {
        logger.info("saveUrunSP: UrunAdi='{}', KategoriID='{}'", urunDto.getUrunAdi(), urunDto.getKategoriId());
        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spUrun_Ekle @UrunAdi = :urunAdi, @KategoriID = :kategoriID, @Aciklama = :aciklama, @Birim = :birim")
                .setParameter("urunAdi", urunDto.getUrunAdi())
                .setParameter("kategoriID", urunDto.getKategoriId())
                .setParameter("aciklama", urunDto.getAciklama())
                .setParameter("birim", urunDto.getBirim())
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer yeniUrunId = ((Number) result).intValue();
                if (yeniUrunId > 0) {
                    logger.info("SP ile yeni ürün eklendi, ID: {}", yeniUrunId);
                    return urunRepository.findById(yeniUrunId)
                            .orElseThrow(() -> new RuntimeException("Eklenen ürün veritabanından çekilemedi ID: " + yeniUrunId));
                } else if (yeniUrunId == -2) {
                    throw new IllegalArgumentException("Ürün eklenemedi: Geçersiz Kategori ID (" + urunDto.getKategoriId() + "). Böyle bir kategori bulunmuyor.");
                }
            }
        }
        throw new RuntimeException("Ürün eklenemedi (SP'den ID veya geçerli bir hata kodu alınamadı).");
    }

    @Transactional
    public boolean updateUrunSP(UrunFormDto urunDto) {
        logger.info("updateUrunSP: ID={}, UrunAdi='{}', KategoriID='{}'", urunDto.getUrunID(), urunDto.getUrunAdi(), urunDto.getKategoriId());
        if (urunDto.getUrunID() == null) {
            throw new IllegalArgumentException("Güncellenecek ürün için ID belirtilmelidir.");
        }

        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spUrun_Guncelle @UrunID = :urunID, @UrunAdi = :urunAdi, @KategoriID = :kategoriID, @Aciklama = :aciklama, @Birim = :birim")
                .setParameter("urunID", urunDto.getUrunID())
                .setParameter("urunAdi", urunDto.getUrunAdi())
                .setParameter("kategoriID", urunDto.getKategoriId())
                .setParameter("aciklama", urunDto.getAciklama())
                .setParameter("birim", urunDto.getBirim())
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer sonucKodu = ((Number) result).intValue();
                if (sonucKodu == 1) {
                    logger.info("SP ile ürün güncellendi, ID: {}", urunDto.getUrunID());
                    return true;
                } else if (sonucKodu == -1) {
                    throw new RuntimeException("Ürün güncellenemedi: Ürün bulunamadı (ID: " + urunDto.getUrunID() + ").");
                } else if (sonucKodu == -2) {
                    throw new IllegalArgumentException("Ürün güncellenemedi: Geçersiz Kategori ID (" + urunDto.getKategoriId() + ").");
                } else {
                    throw new RuntimeException("Ürün güncellenemedi. SP'den bilinmeyen sonuç kodu: " + sonucKodu);
                }
            }
        }
        throw new RuntimeException("Ürün güncellenemedi (SP'den sonuç alınamadı veya beklenmeyen format).");
    }

    @Transactional
    public boolean deleteUrunSP(Integer id) {
        logger.info("deleteUrunSP for ID: {}", id);
        List<?> resultList = entityManager.createNativeQuery("EXEC dbo.spUrun_Sil @UrunID = :urunID")
                .setParameter("urunID", id)
                .getResultList();

        if (resultList != null && !resultList.isEmpty()) {
            Object result = resultList.get(0);
            if (result instanceof Number) {
                Integer sonucKodu = ((Number) result).intValue();
                if (sonucKodu == 1) {
                    logger.info("SP ile ürün silindi, ID: {}", id);
                    return true;
                } else if (sonucKodu == -1) {
                    throw new RuntimeException("Ürün silinemedi: Silinecek ürün bulunamadı (ID: " + id + ").");
                } else if (sonucKodu == -2) {
                    throw new RuntimeException("Ürün silinemedi (ID: " + id + "): Bu ürünle ilişkili ekim kayıtları var.");
                } else {
                    throw new RuntimeException("Ürün silinemedi. SP'den bilinmeyen sonuç kodu: " + sonucKodu);
                }
            }
        }
        throw new RuntimeException("Ürün silinemedi (SP'den sonuç alınamadı veya beklenmeyen format).");
    }
} // class sonu