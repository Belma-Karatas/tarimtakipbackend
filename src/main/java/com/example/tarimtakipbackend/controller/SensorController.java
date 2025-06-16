package com.example.tarimtakipbackend.controller;

import com.example.tarimtakipbackend.dto.SensorDetayDto;
import com.example.tarimtakipbackend.dto.SensorFormDto;
import com.example.tarimtakipbackend.entity.Sensor;
import com.example.tarimtakipbackend.repository.SensorTipiRepository;
import com.example.tarimtakipbackend.repository.TarlaRepository;
import com.example.tarimtakipbackend.service.SensorServis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/sensorler")
@PreAuthorize("hasRole('ADMIN')")
public class SensorController {

    private static final Logger logger = LoggerFactory.getLogger(SensorController.class);

    @Autowired
    private SensorServis sensorServis;

    @Autowired
    private TarlaRepository tarlaRepository;

    @Autowired
    private SensorTipiRepository sensorTipiRepository;

    @GetMapping
    public String listSensorler(Model model) {
        logger.info("/admin/sensorler GET isteği - Sensörler listeleniyor.");
        model.addAttribute("pageTitle", "Admin - Sensör Yönetimi");
        model.addAttribute("activePage", "sensorler");

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
    public String showSensorEkleForm(Model model) {
        logger.info("/admin/sensorler/ekle GET isteği - Yeni sensör formu gösteriliyor.");
        model.addAttribute("pageTitle", "Admin - Yeni Sensör Ekle");
        model.addAttribute("activePage", "sensorler");

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
    public String showSensorDuzenleForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("/admin/sensorler/duzenle/{} GET isteği - Sensör düzenleme formu gösteriliyor.", id);
        model.addAttribute("pageTitle", "Admin - Sensörü Düzenle");
        model.addAttribute("activePage", "sensorler");

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
            logger.warn("Formda validasyon hataları var: {}", result.getAllErrors());
            model.addAttribute("pageTitle", (sensorFormDto.getSensorID() == null ? "Admin - Yeni Sensör Ekle" : "Admin - Sensörü Düzenle"));
            model.addAttribute("activePage", "sensorler");
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
                // TODO: sensorServis.updateSensorSP(sensorFormDto) implemente edilecek.
                logger.info("Sensör güncelleme (ID: {}) işlemi SensorServis.updateSensorSP çağrılacak (henüz implemente edilmedi).", sensorFormDto.getSensorID());
                successMsg = "Sensör başarıyla güncellendi (Servis implementasyonu bekleniyor). ID: " + sensorFormDto.getSensorID();
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
            String redirectUrl = (sensorFormDto.getSensorID() != null) ? "/admin/sensorler/duzenle/" + sensorFormDto.getSensorID() + "?error" : "/admin/sensorler/ekle?error";
            return "redirect:" + redirectUrl;
        }
    }

    @GetMapping("/sil/{id}")
    public String deleteSensor(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        logger.info("/admin/sensorler/sil/{} GET isteği.", id);
        try {
            boolean silindi = sensorServis.deleteSensorSP(id);
            if (silindi) {
                redirectAttributes.addFlashAttribute("successMessage", "Sensör başarıyla silindi. ID: " + id);
            } else {
                 // Bu bloğa normalde girilmemesi lazım, servis exception fırlatır
                redirectAttributes.addFlashAttribute("errorMessage", "Sensör silinemedi (Beklenmedik durum).");
            }
        } catch (RuntimeException e) {
            logger.error("Sensör silme hatası ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Silme işlemi sırasında bir hata oluştu: " + e.getMessage());
        }
        return "redirect:/admin/sensorler";
    }
}