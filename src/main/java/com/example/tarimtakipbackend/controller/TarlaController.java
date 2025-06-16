package com.example.tarimtakipbackend.controller;

import com.example.tarimtakipbackend.dto.TarlaDetayDto; // DTO importu
import com.example.tarimtakipbackend.entity.Tarla;
import com.example.tarimtakipbackend.repository.SulamaSistemiRepository;
import com.example.tarimtakipbackend.repository.ToprakTipiRepository;
import com.example.tarimtakipbackend.service.TarlaServis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/tarlalar")
@PreAuthorize("hasRole('ADMIN')")
public class TarlaController {

    private static final Logger logger = LoggerFactory.getLogger(TarlaController.class);

    @Autowired
    private TarlaServis tarlaServis;

    @Autowired
    private ToprakTipiRepository toprakTipiRepository;

    @Autowired
    private SulamaSistemiRepository sulamaSistemiRepository;

    @GetMapping
    public String listTarlalar(Model model) {
        logger.info("/admin/tarlalar GET isteği - Tarlalar listeleniyor (SP ile).");
        model.addAttribute("pageTitle", "Admin - Tarla Listesi");
        model.addAttribute("activePage", "tarlalar");
        try {
            // TarlaServis'ten DTO listesini alıyoruz
            List<TarlaDetayDto> tarlalar = tarlaServis.getAllTarlalarSP();
            model.addAttribute("tarlalar", tarlalar);
        } catch (Exception e) {
            logger.error("Tarlalar SP ile listelenirken hata oluştu: ", e);
            model.addAttribute("errorMessage", "Tarlalar listelenirken bir hata oluştu.");
            model.addAttribute("tarlalar", List.of()); // Boş liste gönder
        }
        return "admin/tarlalar-liste"; // Bu view, TarlaDetayDto listesine uygun olmalı
    }

    @GetMapping("/ekle")
    public String showTarlaEkleForm(Model model) {
        logger.info("/admin/tarlalar/ekle GET isteği - Yeni tarla formu gösteriliyor.");
        model.addAttribute("pageTitle", "Admin - Yeni Tarla Ekle");
        model.addAttribute("activePage", "tarlalar");
        if (!model.containsAttribute("tarlaForm")) {
            model.addAttribute("tarlaForm", new Tarla()); // Form için boş Tarla nesnesi
        }
        model.addAttribute("toprakTipleri", toprakTipiRepository.findAll());
        model.addAttribute("sulamaSistemleri", sulamaSistemiRepository.findAll());
        return "admin/tarla-form";
    }

    @GetMapping("/duzenle/{id}")
    public String showTarlaDuzenleForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("/admin/tarlalar/duzenle/{} GET isteği - Tarla düzenleme formu gösteriliyor.", id);
        model.addAttribute("pageTitle", "Admin - Tarlayı Düzenle");
        model.addAttribute("activePage", "tarlalar");

        if (!model.containsAttribute("tarlaForm")) {
            Optional<TarlaDetayDto> tarlaDetayOptional = tarlaServis.getTarlaDetayByIdSP(id);
            if (tarlaDetayOptional.isPresent()) {
                TarlaDetayDto detay = tarlaDetayOptional.get();
                // Formu doldurmak için Tarla nesnesine dönüştürüyoruz (veya bir TarlaFormDto)
                Tarla tarlaForm = new Tarla();
                tarlaForm.setTarlaID(detay.getTarlaID());
                tarlaForm.setTarlaAdi(detay.getTarlaAdi());
                tarlaForm.setAlan(detay.getAlan());
                tarlaForm.setAciklama(detay.getTarlaAciklamasi());
                // ToprakTipi ve SulamaSistemi nesnelerini ID ile bulup set etmemiz gerekebilir
                // Eğer TarlaDetayDto'da ID'ler varsa:
                // Örneğin, TarlaDetayDto'ya toprakTipiId ve sulamaSistemiId alanları eklenirse:
                // if(detay.getToprakTipiId() != null) toprakTipiRepository.findById(detay.getToprakTipiId()).ifPresent(tarlaForm::setToprakTipi);
                // if(detay.getSulamaSistemiId() != null) sulamaSistemiRepository.findById(detay.getSulamaSistemiId()).ifPresent(tarlaForm::setSulamaSistemi);
                // Şimdilik bu DTO'da bu ID'ler yok, bu yüzden formda seçilen ID'ler kullanılacak.
                // Ya da Tarla entity'sini direkt TarlaServis.getTarlaById() ile çekebiliriz (eğer DTO yerine entity döndürüyorsa)
                model.addAttribute("tarlaForm", tarlaForm);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Tarla bulunamadı ID: " + id);
                return "redirect:/admin/tarlalar";
            }
        }
        model.addAttribute("toprakTipleri", toprakTipiRepository.findAll());
        model.addAttribute("sulamaSistemleri", sulamaSistemiRepository.findAll());
        return "admin/tarla-form";
    }

    @PostMapping("/kaydet")
    public String saveTarla(@ModelAttribute("tarlaForm") Tarla tarlaForm,
                            BindingResult result,
                            @RequestParam(name = "toprakTipiIdSecilen", required = false) Integer toprakTipiId,
                            @RequestParam(name = "sulamaSistemiIdSecilen", required = false) Integer sulamaSistemiId,
                            RedirectAttributes redirectAttributes,
                            Model model) {

        logger.info("/admin/tarlalar/kaydet POST isteği: {}", tarlaForm.getTarlaAdi());
        model.addAttribute("activePage", "tarlalar"); // Hata durumunda layout için

        if (tarlaForm.getTarlaAdi() == null || tarlaForm.getTarlaAdi().trim().isEmpty()) {
            result.rejectValue("tarlaAdi", "NotEmpty", "Tarla adı boş olamaz.");
        }

        if (result.hasErrors()) {
            logger.warn("Formda validasyon hataları var: {}", result.getAllErrors());
            model.addAttribute("tarlaForm", tarlaForm); // Hatalı formu ve verilerini geri gönder
            model.addAttribute("toprakTipleri", toprakTipiRepository.findAll());
            model.addAttribute("sulamaSistemleri", sulamaSistemiRepository.findAll());
            model.addAttribute("pageTitle", (tarlaForm.getTarlaID() == null ? "Admin - Yeni Tarla Ekle" : "Admin - Tarlayı Düzenle"));
            // Hata varsa formu redirect yapmadan direkt göstererek validasyon mesajlarının görünmesini sağlarız.
            return "admin/tarla-form";
        }

        try {
            if (tarlaForm.getTarlaID() == null) { // Yeni kayıt
                Tarla kaydedilenTarla = tarlaServis.saveTarlaSP(tarlaForm, toprakTipiId, sulamaSistemiId);
                redirectAttributes.addFlashAttribute("successMessage", "Tarla başarıyla eklendi. ID: " + kaydedilenTarla.getTarlaID());
            } else { // Güncelleme
                tarlaServis.updateTarlaSP(tarlaForm, toprakTipiId, sulamaSistemiId);
                redirectAttributes.addFlashAttribute("successMessage", "Tarla başarıyla güncellendi. ID: " + tarlaForm.getTarlaID());
            }
        } catch (RuntimeException e) { // IllegalArgumentException da buraya düşer
            logger.error("Tarla kaydı/güncellemesi sırasında hata: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "İşlem sırasında bir hata oluştu: " + e.getMessage());
            String redirectUrl = (tarlaForm.getTarlaID() != null) ?
                    "/admin/tarlalar/duzenle/" + tarlaForm.getTarlaID() :
                    "/admin/tarlalar/ekle";
            // Hata durumunda formu ve girilen değerleri redirect ile geri göndermek için flash attribute kullan
            redirectAttributes.addFlashAttribute("tarlaForm", tarlaForm);
            // Eğer BindingResult'ı da redirect ile göndermek istersen, o biraz daha karmaşık olabilir.
            // Şimdilik sadece hata mesajını gönderiyoruz.
            return "redirect:" + redirectUrl + "?error"; // ?error ekleyerek formda hata olduğunu belirt
        }
        return "redirect:/admin/tarlalar";
    }

    @GetMapping("/sil/{id}")
    public String deleteTarla(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        logger.info("/admin/tarlalar/sil/{} GET isteği.", id);
        try {
            tarlaServis.deleteTarlaSP(id);
            redirectAttributes.addFlashAttribute("successMessage", "Tarla başarıyla silindi. ID: " + id);
        } catch (RuntimeException e) {
            logger.error("Tarla silme hatası ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/tarlalar";
    }
}