package com.example.tarimtakipbackend.controller;

import com.example.tarimtakipbackend.dto.EkimDetayDto;
import com.example.tarimtakipbackend.dto.EkimFormDto;
import com.example.tarimtakipbackend.entity.Ekim;
import com.example.tarimtakipbackend.repository.TarlaRepository;
import com.example.tarimtakipbackend.repository.UrunRepository;
import com.example.tarimtakipbackend.service.EkimServis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections; // Boş liste için
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/ekimler")
@PreAuthorize("hasRole('ADMIN')")
public class EkimController {

    private static final Logger logger = LoggerFactory.getLogger(EkimController.class);

    @Autowired
    private EkimServis ekimServis;

    @Autowired
    private TarlaRepository tarlaRepository;

    @Autowired
    private UrunRepository urunRepository;

    @GetMapping
    public String listEkimler(Model model) {
        logger.info("/admin/ekimler GET isteği - Ekimler listeleniyor.");
        model.addAttribute("pageTitle", "Admin - Ekim Listesi");
        model.addAttribute("activePage", "ekimler");
        try {
            List<EkimDetayDto> ekimler = ekimServis.getAllEkimlerSP();
            model.addAttribute("ekimler", ekimler);
        } catch (Exception e) {
            logger.error("Ekimler listelenirken hata oluştu: ", e);
            model.addAttribute("errorMessage", "Ekimler listelenirken bir hata oluştu: " + e.getMessage());
            model.addAttribute("ekimler", Collections.emptyList()); // List.of() yerine Collections.emptyList() daha iyi
        }
        return "admin/ekimler-liste";
    }

    @GetMapping("/ekle")
    public String showEkimEkleForm(Model model) {
        logger.info("/admin/ekimler/ekle GET isteği - Yeni ekim formu gösteriliyor.");
        model.addAttribute("pageTitle", "Admin - Yeni Ekim Planla/Ekle");
        model.addAttribute("activePage", "ekimler");
        if (!model.containsAttribute("ekimForm")) {
            model.addAttribute("ekimForm", new EkimFormDto());
        }
        model.addAttribute("tarlalar", tarlaRepository.findAll());
        model.addAttribute("urunler", urunRepository.findAll());
        // İsteğe bağlı: Durumlar için bir liste ekleyebilirsiniz
        // model.addAttribute("durumListesi", List.of("Planlandı", "Ekildi", "Gelişiyor", "Hasada Hazır", "Hasat Edildi", "İptal Edildi"));
        return "admin/ekim-form";
    }

    @GetMapping("/duzenle/{id}")
    public String showEkimDuzenleForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("/admin/ekimler/duzenle/{} GET isteği - Ekim düzenleme formu gösteriliyor.", id);
        model.addAttribute("pageTitle", "Admin - Ekimi Düzenle");
        model.addAttribute("activePage", "ekimler");

        if (!model.containsAttribute("ekimForm")) { // Hata sonrası redirect'ten gelmiyorsa
            // EkimDetayDto'dan ziyade direkt Ekim entity'sini çekip EkimFormDto'ya maplemek daha iyi olabilir
            // çünkü Ekim entity'sinde Tarla ve Urun nesneleri direkt var, ID'lerini oradan alabiliriz.
            Optional<Ekim> ekimEntityOptional = ekimServis.findById(id); // EkimServis'e eklediğimiz metot
            if (ekimEntityOptional.isPresent()) {
                Ekim ekimEntity = ekimEntityOptional.get();
                EkimFormDto ekimForm = new EkimFormDto();
                ekimForm.setEkimID(ekimEntity.getEkimID());
                if (ekimEntity.getTarla() != null) {
                    ekimForm.setTarlaId(ekimEntity.getTarla().getTarlaID());
                }
                if (ekimEntity.getUrun() != null) {
                    ekimForm.setUrunId(ekimEntity.getUrun().getUrunID());
                }
                if (ekimEntity.getEkimTarihi() != null) {
                    ekimForm.setEkimTarihi(ekimEntity.getEkimTarihi().toString()); // LocalDate to String (yyyy-MM-dd)
                }
                if (ekimEntity.getPlanlananHasatTarihi() != null) {
                    ekimForm.setPlanlananHasatTarihi(ekimEntity.getPlanlananHasatTarihi().toString());
                }
                ekimForm.setEkilenMiktarAciklama(ekimEntity.getEkilenMiktarAciklama());
                ekimForm.setDurum(ekimEntity.getDurum());
                ekimForm.setNotlar(ekimEntity.getNotlar());
                model.addAttribute("ekimForm", ekimForm);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Ekim kaydı bulunamadı ID: " + id);
                return "redirect:/admin/ekimler";
            }
        }
        // Her durumda (yeni, düzenleme veya hata sonrası) dropdown'ları doldur
        model.addAttribute("tarlalar", tarlaRepository.findAll());
        model.addAttribute("urunler", urunRepository.findAll());
        // model.addAttribute("durumListesi", ...);
        return "admin/ekim-form";
    }

    @PostMapping("/kaydet")
    public String saveEkim(@ModelAttribute("ekimForm") EkimFormDto ekimForm,
                           BindingResult result,
                           RedirectAttributes redirectAttributes,
                           Model model) { // Hata durumunda formu direkt göstermek için Model

        logger.info("/admin/ekimler/kaydet POST isteği - TarlaID: {}, UrunID: {}", ekimForm.getTarlaId(), ekimForm.getUrunId());

        // Basit validasyonlar
        if (ekimForm.getTarlaId() == null) {
            result.rejectValue("tarlaId", "NotNull.ekimForm.tarlaId", "Tarla seçimi zorunludur.");
        }
        if (ekimForm.getUrunId() == null) {
            result.rejectValue("urunId", "NotNull.ekimForm.urunId", "Ürün seçimi zorunludur.");
        }
        if (ekimForm.getEkimTarihi() == null || ekimForm.getEkimTarihi().trim().isEmpty()) {
            result.rejectValue("ekimTarihi", "NotEmpty.ekimForm.ekimTarihi", "Ekim tarihi zorunludur.");
        }
        // İleri düzey validasyonlar (tarih formatı, gelecekteki tarih vb.) DTO üzerinde anotasyonlarla veya özel validator ile yapılabilir.

        if (result.hasErrors()) {
            logger.warn("Formda validasyon hataları var: {}", result.getAllErrors());
            // Hata varsa, formu redirect yapmadan direkt tekrar gösteriyoruz
            model.addAttribute("pageTitle", (ekimForm.getEkimID() == null ? "Admin - Yeni Ekim Planla/Ekle" : "Admin - Ekimi Düzenle"));
            model.addAttribute("activePage", "ekimler");
            model.addAttribute("tarlalar", tarlaRepository.findAll());
            model.addAttribute("urunler", urunRepository.findAll());
            // model.addAttribute("durumListesi", ...);
            // ekimForm zaten @ModelAttribute ile modelde
            return "admin/ekim-form";
        }

        try {
            if (ekimForm.getEkimID() == null) { // Yeni kayıt
                Ekim kaydedilenEkim = ekimServis.saveEkimSP(ekimForm);
                redirectAttributes.addFlashAttribute("successMessage", "Ekim başarıyla eklendi. ID: " + kaydedilenEkim.getEkimID());
            } else { // Güncelleme
                boolean guncellendi = ekimServis.updateEkimSP(ekimForm);
                if (guncellendi) {
                    redirectAttributes.addFlashAttribute("successMessage", "Ekim başarıyla güncellendi. ID: " + ekimForm.getEkimID());
                } else {
                    // Bu durum genellikle servis katmanında bir exception fırlatıldığında catch bloğuna düşer.
                    // Eğer servis boolean dönüp false veriyorsa, bu özel bir durumdur.
                    redirectAttributes.addFlashAttribute("errorMessage", "Ekim güncellenirken bir sorun oluştu (Servis false döndü).");
                    redirectAttributes.addFlashAttribute("ekimForm", ekimForm); // Formu tekrar doldurmak için
                    return "redirect:/admin/ekimler/duzenle/" + ekimForm.getEkimID() + "?error";
                }
            }
            return "redirect:/admin/ekimler"; // Başarılı olursa listeye yönlendir

        } catch (RuntimeException e) { // IllegalArgumentException da RuntimeException'dan türediği için bu blok yakalar
            logger.error("Ekim kaydı/güncellemesi sırasında hata: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "İşlem sırasında bir hata oluştu: " + e.getMessage());
            redirectAttributes.addFlashAttribute("ekimForm", ekimForm);

            String redirectUrl = (ekimForm.getEkimID() != null) ?
                    "/admin/ekimler/duzenle/" + ekimForm.getEkimID() + "?error" :
                    "/admin/ekimler/ekle?error";
            return "redirect:" + redirectUrl;
        }
    }

    @GetMapping("/sil/{id}")
    public String deleteEkim(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        logger.info("/admin/ekimler/sil/{} GET isteği.", id);
        try {
            boolean silindi = ekimServis.deleteEkimSP(id);
            if (silindi) {
                redirectAttributes.addFlashAttribute("successMessage", "Ekim başarıyla silindi. ID: " + id);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Ekim silinemedi (Servis false döndü).");
            }
        } catch (RuntimeException e) {
            logger.error("Ekim silme hatası ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Silme işlemi sırasında bir hata oluştu: " + e.getMessage());
        }
        return "redirect:/admin/ekimler";
    }
}