package com.example.tarimtakipbackend.controller;

import com.example.tarimtakipbackend.dto.SensorDetayDto;
import com.example.tarimtakipbackend.dto.SensorFormDto;
import com.example.tarimtakipbackend.dto.SensorOkumaDetayDto;
import com.example.tarimtakipbackend.dto.SensorOkumaFormDto;
import com.example.tarimtakipbackend.entity.Sensor;
import com.example.tarimtakipbackend.repository.SensorTipiRepository;
import com.example.tarimtakipbackend.repository.TarlaRepository;
import com.example.tarimtakipbackend.service.SensorServis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/sensorler")
public class SensorController {

    private static final Logger logger = LoggerFactory.getLogger(SensorController.class);
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private SensorServis sensorServis;

    @Autowired
    private TarlaRepository tarlaRepository;

    @Autowired
    private SensorTipiRepository sensorTipiRepository;

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CALISAN')")
    public String listSensorler(Model model, Authentication authentication) {
        logger.info("/admin/sensorler GET isteği - Sensörler listeleniyor.");
        model.addAttribute("pageTitle", "Sensör Listesi");

        if (isAdmin(authentication)) {
            model.addAttribute("activePage", "sensorYonetimi");
        } else {
            model.addAttribute("activePage", "sensorler");
        }

        try {
            List<SensorDetayDto> sensorler = sensorServis.getAllSensorlerSP();
            model.addAttribute("sensorler", sensorler);
            logger.info("Sensörler SP'den alındı, Adet: {}", sensorler != null ? sensorler.size() : 0);
        } catch (Exception e) {
            logger.error("Sensörler listelenirken hata oluştu: ", e);
            model.addAttribute("errorMessage", "Sensörler listelenirken bir hata oluştu: " + e.getMessage());
            model.addAttribute("sensorler", Collections.emptyList());
        }
        return "admin/sensorler-liste";
    }

    @GetMapping("/ekle")
    @PreAuthorize("hasRole('ADMIN')")
    public String showSensorEkleForm(Model model) {
        logger.info("/admin/sensorler/ekle GET isteği - Yeni sensör formu gösteriliyor.");
        model.addAttribute("pageTitle", "Admin - Yeni Sensör Ekle");
        model.addAttribute("activePage", "sensorYonetimi");

        if (!model.containsAttribute("sensorForm")) {
            SensorFormDto formDto = new SensorFormDto();
            formDto.setAktifMi(true);
            model.addAttribute("sensorForm", formDto);
        }
        model.addAttribute("tarlalar", tarlaRepository.findAll());
        model.addAttribute("sensorTipleri", sensorTipiRepository.findAll());

        return "admin/sensor-form";
    }

    @GetMapping("/duzenle/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String showSensorDuzenleForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("/admin/sensorler/duzenle/{} GET isteği - Sensör düzenleme formu gösteriliyor.", id);
        model.addAttribute("pageTitle", "Admin - Sensörü Düzenle");
        model.addAttribute("activePage", "sensorYonetimi");

        if (!model.containsAttribute("sensorForm")) {
            Optional<Sensor> sensorOpt = sensorServis.findSensorEntityById(id);
            if (sensorOpt.isPresent()) {
                Sensor sensor = sensorOpt.get();
                SensorFormDto formDto = new SensorFormDto();
                formDto.setSensorID(sensor.getSensorID());
                formDto.setSensorKodu(sensor.getSensorKodu());
                if (sensor.getTarla() != null) formDto.setTarlaId(sensor.getTarla().getTarlaID());
                if (sensor.getSensorTipi() != null) formDto.setSensorTipiId(sensor.getSensorTipi().getSensorTipiID());
                formDto.setMarkaModel(sensor.getMarkaModel());
                if (sensor.getKurulumTarihi() != null) formDto.setKurulumTarihi(sensor.getKurulumTarihi().toString());
                formDto.setKonumAciklamasi(sensor.getKonumAciklamasi());
                formDto.setAktifMi(sensor.getAktifMi());
                if (sensor.getSonBakimTarihi() != null) formDto.setSonBakimTarihi(sensor.getSonBakimTarihi().toString());
                model.addAttribute("sensorForm", formDto);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Düzenlenecek sensör bulunamadı. ID: " + id);
                return "redirect:/admin/sensorler";
            }
        }
        model.addAttribute("tarlalar", tarlaRepository.findAll());
        model.addAttribute("sensorTipleri", sensorTipiRepository.findAll());
        return "admin/sensor-form";
    }

    @PostMapping("/kaydet")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveSensor(@ModelAttribute("sensorForm") SensorFormDto sensorFormDto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        logger.info("/admin/sensorler/kaydet POST isteği. Sensör ID: {}", sensorFormDto.getSensorID());

        if (sensorFormDto.getSensorKodu() == null || sensorFormDto.getSensorKodu().trim().isEmpty()) {
            result.rejectValue("sensorKodu", "NotEmpty", "Sensör kodu boş olamaz.");
        }
        if (sensorFormDto.getTarlaId() == null) {
            result.rejectValue("tarlaId", "NotNull", "Tarla seçimi zorunludur.");
        }
        if (sensorFormDto.getSensorTipiId() == null) {
            result.rejectValue("sensorTipiId", "NotNull", "Sensör tipi seçimi zorunludur.");
        }
        try {
            if(sensorFormDto.getKurulumTarihi() != null && !sensorFormDto.getKurulumTarihi().trim().isEmpty()){
                LocalDate.parse(sensorFormDto.getKurulumTarihi());
            }
            if(sensorFormDto.getSonBakimTarihi() != null && !sensorFormDto.getSonBakimTarihi().trim().isEmpty()){
                LocalDate.parse(sensorFormDto.getSonBakimTarihi());
            }
        } catch (DateTimeParseException e) {
            String fieldName = (e.getParsedString() != null && sensorFormDto.getKurulumTarihi() != null && e.getParsedString().contains(sensorFormDto.getKurulumTarihi())) ? "kurulumTarihi" : "sonBakimTarihi";
            result.rejectValue(fieldName, "Pattern", "Tarih formatı geçersiz (yyyy-MM-dd).");
        }

        if (result.hasErrors()) {
            logger.warn("Sensör formunda validasyon hataları var: {}", result.getAllErrors());
            model.addAttribute("pageTitle", (sensorFormDto.getSensorID() == null ? "Admin - Yeni Sensör Ekle" : "Admin - Sensörü Düzenle"));
            model.addAttribute("activePage", "sensorYonetimi");
            model.addAttribute("tarlalar", tarlaRepository.findAll());
            model.addAttribute("sensorTipleri", sensorTipiRepository.findAll());
            return "admin/sensor-form";
        }

        try {
            String successMsg;
            if (sensorFormDto.getSensorID() == null) {
                Sensor kaydedilenSensor = sensorServis.saveSensorSP(sensorFormDto);
                successMsg = "Sensör başarıyla eklendi. ID: " + kaydedilenSensor.getSensorID();
            } else {
                logger.warn("Sensör güncelleme (ID: {}) için updateSensorSP henüz implemente edilmedi veya çağrılmıyor.", sensorFormDto.getSensorID());
                redirectAttributes.addFlashAttribute("warningMessage", "Sensör güncelleme fonksiyonu henüz tam olarak aktif değil.");
                successMsg = "Sensör güncelleme işlemi için backend'de düzenleme gerekiyor. ID: " + sensorFormDto.getSensorID();
            }
            redirectAttributes.addFlashAttribute("successMessage", successMsg);
            return "redirect:/admin/sensorler";

        } catch (IllegalArgumentException e) {
            logger.error("Sensör kaydetme/güncelleme sırasında hata: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("sensorForm", sensorFormDto);
            String redirectUrl = (sensorFormDto.getSensorID() != null) ? "/admin/sensorler/duzenle/" + sensorFormDto.getSensorID() + "?error" : "/admin/sensorler/ekle?error";
            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            logger.error("Sensör kaydetme/güncelleme sırasında beklenmedik hata: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "İşlem sırasında beklenmedik bir hata oluştu.");
            redirectAttributes.addFlashAttribute("sensorForm", sensorFormDto);
            String redirectUrl = (sensorFormDto.getSensorID() != null) ? "/admin/sensorler/duzenle/" + sensorFormDto.getSensorID() + "?error_unexpected" : "/admin/sensorler/ekle?error_unexpected";
            return "redirect:" + redirectUrl;
        }
    }

    @GetMapping("/sil/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteSensor(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        logger.info("/admin/sensorler/sil/{} GET isteği.", id);
        try {
            boolean silindi = sensorServis.deleteSensorSP(id);
            if (silindi) {
                redirectAttributes.addFlashAttribute("successMessage", "Sensör başarıyla silindi. ID: " + id);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Sensör silinemedi (Beklenmedik durum).");
            }
        } catch (RuntimeException e) {
            logger.error("Sensör silme hatası ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Silme işlemi sırasında bir hata oluştu: " + e.getMessage());
        }
        return "redirect:/admin/sensorler";
    }

    @GetMapping("/{sensorId}/okumalar")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALISAN')")
    public String listSensorOkumalari(@PathVariable("sensorId") Integer sensorId, Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        logger.info("/admin/sensorler/{}/okumalar GET isteği.", sensorId);
        Optional<SensorDetayDto> sensorDetayOpt = sensorServis.getSensorDetayByIdSP(sensorId);

        if (sensorDetayOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sensör bulunamadı ID: " + sensorId);
            return "redirect:/admin/sensorler";
        }

        model.addAttribute("sensor", sensorDetayOpt.get());
        model.addAttribute("pageTitle", sensorDetayOpt.get().getSensorKodu() + " - Sensör Okumaları");

        if (isAdmin(authentication)) {
            model.addAttribute("activePage", "sensorYonetimi");
        } else {
            model.addAttribute("activePage", "sensorler");
        }

        try {
            List<SensorOkumaDetayDto> okumalar = sensorServis.getOkumalarBySensorIdSP(sensorId, 200);
            model.addAttribute("okumalar", okumalar);
        } catch (Exception e) {
            logger.error("Sensör okumaları listelenirken hata oluştu (SensorID: {}): {}",sensorId, e.getMessage(), e);
            model.addAttribute("errorMessageOkumalar", "Okumalar listelenirken bir hata oluştu.");
            model.addAttribute("okumalar", Collections.emptyList());
        }
        return "admin/sensor-okumalari-liste";
    }

    @GetMapping("/okuma/ekle")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALISAN')")
    public String showSensorOkumaEkleForm(@RequestParam(name = "sensorId", required = false) Integer sensorId, Model model) {
        logger.info("/admin/sensorler/okuma/ekle GET isteği. Önceden seçili Sensor ID: {}", sensorId);
        model.addAttribute("pageTitle", "Yeni Sensör Okuması Ekle");
        model.addAttribute("activePage", "sensorOkumaEkle");

        SensorOkumaFormDto formDto = new SensorOkumaFormDto();
        if (sensorId != null) {
            formDto.setSensorId(sensorId);
            sensorServis.getSensorDetayByIdSP(sensorId).ifPresent(s -> formDto.setBirim(s.getOlcumBirimi()));
        }
        formDto.setOkumaZamani(LocalDateTime.now().format(ISO_DATETIME_FORMATTER));

        model.addAttribute("sensorOkumaForm", formDto);
        model.addAttribute("sensorlerListesi", sensorServis.getAllSensorlerSP());

        return "admin/sensor-okuma-form";
    }

    @PostMapping("/okuma/kaydet")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALISAN')")
    public String saveSensorOkuma(@ModelAttribute("sensorOkumaForm") SensorOkumaFormDto sensorOkumaFormDto,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        logger.info("/admin/sensorler/okuma/kaydet POST isteği. SensorID: {}", sensorOkumaFormDto.getSensorId());

        if (sensorOkumaFormDto.getSensorId() == null) {
            result.rejectValue("sensorId", "NotNull", "Sensör seçimi zorunludur.");
        }
        if (sensorOkumaFormDto.getOkumaZamani() == null || sensorOkumaFormDto.getOkumaZamani().trim().isEmpty()) {
            result.rejectValue("okumaZamani", "NotEmpty", "Okuma zamanı boş olamaz.");
        } else {
            try {
                LocalDateTime.parse(sensorOkumaFormDto.getOkumaZamani(), ISO_DATETIME_FORMATTER);
            } catch (DateTimeParseException e) {
                result.rejectValue("okumaZamani", "Pattern", "Okuma zamanı formatı geçersiz (yyyy-MM-ddTHH:mm).");
            }
        }
        if (sensorOkumaFormDto.getDeger() == null || sensorOkumaFormDto.getDeger().trim().isEmpty()) {
            result.rejectValue("deger", "NotEmpty", "Okuma değeri boş olamaz.");
        }

        if (result.hasErrors()) {
            logger.warn("Sensör okuma formunda validasyon hataları var: {}", result.getAllErrors());
            model.addAttribute("pageTitle", "Yeni Sensör Okuması Ekle");
            model.addAttribute("activePage", "sensorOkumaEkle");
            model.addAttribute("sensorlerListesi", sensorServis.getAllSensorlerSP());
            return "admin/sensor-okuma-form";
        }

        try {
            sensorServis.saveSensorOkumaSP(sensorOkumaFormDto);
            redirectAttributes.addFlashAttribute("successMessage", "Sensör okuması başarıyla kaydedildi.");
            if (sensorOkumaFormDto.getSensorId() != null) {
                return "redirect:/admin/sensorler/" + sensorOkumaFormDto.getSensorId() + "/okumalar";
            }
            return "redirect:/admin/sensorler";

        } catch (IllegalArgumentException e) {
            logger.error("Sensör okuması kaydı sırasında hata: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("sensorOkumaForm", sensorOkumaFormDto);
            String redirectSuffix = (sensorOkumaFormDto.getSensorId() != null) ? "?sensorId=" + sensorOkumaFormDto.getSensorId() + "&error" : "?error";
            return "redirect:/admin/sensorler/okuma/ekle" + redirectSuffix;
        } catch (Exception e) {
            logger.error("Sensör okuması kaydı sırasında beklenmedik hata: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Beklenmedik bir hata oluştu: " + e.getMessage());
            redirectAttributes.addFlashAttribute("sensorOkumaForm", sensorOkumaFormDto);
            String redirectSuffix = (sensorOkumaFormDto.getSensorId() != null) ? "?sensorId=" + sensorOkumaFormDto.getSensorId() + "&error_unexpected" : "?error_unexpected";
            return "redirect:/admin/sensorler/okuma/ekle" + redirectSuffix;
        }
    }
}