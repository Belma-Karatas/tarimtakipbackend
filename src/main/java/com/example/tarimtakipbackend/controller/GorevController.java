package com.example.tarimtakipbackend.controller;

import com.example.tarimtakipbackend.dto.GorevDetayDto;
import com.example.tarimtakipbackend.dto.GorevFormDto;
import com.example.tarimtakipbackend.entity.Ekim;
import com.example.tarimtakipbackend.entity.FaaliyetTipi;
import com.example.tarimtakipbackend.entity.Gorev;
import com.example.tarimtakipbackend.entity.Kullanici;
import com.example.tarimtakipbackend.entity.Tarla;
import com.example.tarimtakipbackend.repository.EkimRepository;
import com.example.tarimtakipbackend.repository.FaaliyetTipiRepository;
import com.example.tarimtakipbackend.repository.KullaniciRepository;
import com.example.tarimtakipbackend.repository.TarlaRepository;
import com.example.tarimtakipbackend.service.GorevServis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Kullanıcıları filtrelemek için gerekebilir

@Controller
@RequestMapping("/admin/gorevler")
@PreAuthorize("hasRole('ADMIN')")
public class GorevController {

    private static final Logger logger = LoggerFactory.getLogger(GorevController.class);

    @Autowired
    private GorevServis gorevServis;

    @Autowired
    private EkimRepository ekimRepository;

    @Autowired
    private TarlaRepository tarlaRepository;

    @Autowired
    private FaaliyetTipiRepository faaliyetTipiRepository;

    @Autowired
    private KullaniciRepository kullaniciRepository;

    @GetMapping
    public String listGorevler(Model model) {
        logger.info("/admin/gorevler GET isteği - Görevler listeleniyor.");
        model.addAttribute("pageTitle", "Admin - Görev Listesi");
        model.addAttribute("activePage", "gorevler");
        try {
            List<GorevDetayDto> gorevler = gorevServis.getAllGorevlerSP();
            model.addAttribute("gorevler", gorevler);
        } catch (Exception e) {
            logger.error("Görevler listelenirken hata oluştu: ", e);
            model.addAttribute("errorMessage", "Görevler listelenirken bir hata oluştu: " + e.getMessage());
            model.addAttribute("gorevler", Collections.emptyList());
        }
        return "admin/gorevler-liste";
    }

    @GetMapping("/ekle")
    public String showGorevEkleForm(Model model) {
        logger.info("/admin/gorevler/ekle GET isteği - Yeni görev formu gösteriliyor.");
        model.addAttribute("pageTitle", "Admin - Yeni Görev Ata/Ekle");
        model.addAttribute("activePage", "gorevler");
        if (!model.containsAttribute("gorevForm")) {
            model.addAttribute("gorevForm", new GorevFormDto());
        }
        addDropdownDataToModel(model);
        return "admin/gorev-form";
    }

    @GetMapping("/duzenle/{id}")
    public String showGorevDuzenleForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("/admin/gorevler/duzenle/{} GET isteği - Görev düzenleme formu gösteriliyor.", id);
        model.addAttribute("pageTitle", "Admin - Görevi Düzenle");
        model.addAttribute("activePage", "gorevler");

        if (!model.containsAttribute("gorevForm")) {
            Optional<Gorev> gorevEntityOptional = gorevServis.findById(id);
            if (gorevEntityOptional.isPresent()) {
                Gorev gorev = gorevEntityOptional.get();
                GorevFormDto gorevForm = new GorevFormDto();
                gorevForm.setGorevID(gorev.getGorevID());
                if (gorev.getEkim() != null) gorevForm.setEkimId(gorev.getEkim().getEkimID());
                if (gorev.getTarla() != null) gorevForm.setTarlaId(gorev.getTarla().getTarlaID());
                if (gorev.getFaaliyetTipi() != null) gorevForm.setFaaliyetTipiId(gorev.getFaaliyetTipi().getFaaliyetTipiID());
                gorevForm.setAciklama(gorev.getAciklama());
                if (gorev.getAtananKullanici() != null) gorevForm.setAtananKullaniciId(gorev.getAtananKullanici().getKullaniciID());
                if (gorev.getPlanlananBaslangicTarihi() != null) gorevForm.setPlanlananBaslangicTarihi(gorev.getPlanlananBaslangicTarihi().toString());
                if (gorev.getPlanlananBitisTarihi() != null) gorevForm.setPlanlananBitisTarihi(gorev.getPlanlananBitisTarihi().toString());
                if (gorev.getTamamlanmaTarihi() != null) gorevForm.setTamamlanmaTarihi(gorev.getTamamlanmaTarihi().toString());
                gorevForm.setDurum(gorev.getDurum());
                gorevForm.setOncelik(gorev.getOncelik());
                model.addAttribute("gorevForm", gorevForm);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Görev kaydı bulunamadı ID: " + id);
                return "redirect:/admin/gorevler";
            }
        }
        addDropdownDataToModel(model);
        return "admin/gorev-form";
    }

    @PostMapping("/kaydet")
    public String saveGorev(@ModelAttribute("gorevForm") GorevFormDto gorevForm,
                           BindingResult result,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        logger.info("/admin/gorevler/kaydet POST isteği - FaaliyetTipiID: {}", gorevForm.getFaaliyetTipiId());

        // Basit validasyonlar
        if (gorevForm.getEkimId() == null && gorevForm.getTarlaId() == null) {
            result.rejectValue("tarlaId", "NotNull.gorevForm.iliskiliAlan", "Görev bir Ekim veya Tarla ile ilişkili olmalıdır.");
        }
        if (gorevForm.getFaaliyetTipiId() == null) {
            result.rejectValue("faaliyetTipiId", "NotNull.gorevForm.faaliyetTipiId", "Faaliyet tipi seçimi zorunludur.");
        }
        if (gorevForm.getAciklama() == null || gorevForm.getAciklama().trim().isEmpty()) {
            result.rejectValue("aciklama", "NotEmpty.gorevForm.aciklama", "Açıklama zorunludur.");
        }
        // Planlanan bitiş tarihi, başlangıç tarihinden önce olamaz kontrolü SP'de var.
        // İstenirse burada da eklenebilir, ancak SP hatası zaten yakalanacaktır.

        if (result.hasErrors()) {
            logger.warn("Formda validasyon hataları var: {}", result.getAllErrors());
            model.addAttribute("pageTitle", (gorevForm.getGorevID() == null ? "Admin - Yeni Görev Ata/Ekle" : "Admin - Görevi Düzenle"));
            model.addAttribute("activePage", "gorevler");
            addDropdownDataToModel(model);
            return "admin/gorev-form";
        }

        try {
            if (gorevForm.getGorevID() == null) { // Yeni kayıt
                Gorev kaydedilenGorev = gorevServis.saveGorevSP(gorevForm);
                redirectAttributes.addFlashAttribute("successMessage", "Görev başarıyla eklendi. ID: " + kaydedilenGorev.getGorevID());
            } else { // Güncelleme
                boolean guncellendi = gorevServis.updateGorevSP(gorevForm);
                if (guncellendi) {
                    redirectAttributes.addFlashAttribute("successMessage", "Görev başarıyla güncellendi. ID: " + gorevForm.getGorevID());
                } else {
                    // Bu durum genellikle servis katmanında bir exception fırlatıldığında catch bloğuna düşer.
                    redirectAttributes.addFlashAttribute("errorMessage", "Görev güncellenirken bir sorun oluştu (Servis false döndü).");
                    redirectAttributes.addFlashAttribute("gorevForm", gorevForm);
                    return "redirect:/admin/gorevler/duzenle/" + gorevForm.getGorevID() + "?error";
                }
            }
            return "redirect:/admin/gorevler";

        } catch (RuntimeException e) { // IllegalArgumentException da RuntimeException'dan türediği için bu blok yakalar
            logger.error("Görev kaydı/güncellemesi sırasında hata: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            // Fırlatılan exception'ın mesajını doğrudan alıp flash attribute'a ekle
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("gorevForm", gorevForm); // Form verilerini koru

            String redirectUrl = (gorevForm.getGorevID() != null) ?
                    "/admin/gorevler/duzenle/" + gorevForm.getGorevID() + "?error" :
                    "/admin/gorevler/ekle?error";
            return "redirect:" + redirectUrl;
        }
    }

    @GetMapping("/sil/{id}")
    public String deleteGorev(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        logger.info("/admin/gorevler/sil/{} GET isteği.", id);
        try {
            boolean silindi = gorevServis.deleteGorevSP(id);
            if (silindi) {
                redirectAttributes.addFlashAttribute("successMessage", "Görev başarıyla silindi. ID: " + id);
            } else {
                // Bu durum da servis exception fırlatmadığında olur, normalde beklenmez
                redirectAttributes.addFlashAttribute("errorMessage", "Görev silinemedi (Servis false döndü).");
            }
        } catch (RuntimeException e) {
            logger.error("Görev silme hatası ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Silme işlemi sırasında bir hata oluştu: " + e.getMessage());
        }
        return "redirect:/admin/gorevler";
    }

    // Form için dropdown verilerini model'e ekleyen yardımcı metot
    private void addDropdownDataToModel(Model model) {
        model.addAttribute("ekimler", ekimRepository.findAll());
        model.addAttribute("tarlalar", tarlaRepository.findAll());
        model.addAttribute("faaliyetTipleri", faaliyetTipiRepository.findAll());
        // Atanacak kullanıcılar için sadece aktif olanları ve belirli bir rolü (örn: Çalışan) filtreleyebilirsiniz.
        // Örnek: List<Kullanici> calisanlar = kullaniciRepository.findAll().stream()
        //                               .filter(k -> k.getAktifMi() && "Çalışan".equals(k.getRol().getRolAdi()))
        //                               .collect(Collectors.toList());
        // model.addAttribute("kullanicilar", calisanlar);
        model.addAttribute("kullanicilar", kullaniciRepository.findAll()); // Şimdilik tümü
        model.addAttribute("durumListesi", List.of("Atandı", "Devam Ediyor", "Tamamlandı", "İptal Edildi", "Beklemede"));
        model.addAttribute("oncelikListesi", List.of(1, 2, 3, 4, 5));
    }
}