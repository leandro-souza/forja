package br.com.forja.web;

import br.com.forja.core.Nutrition;
import br.com.forja.domain.Profile;
import br.com.forja.domain.Repositories;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Metas nutricionais, cardapio do dia, suplementos e lista de compras. */
@RestController
@RequestMapping("/api/nutrition")
public class NutritionController {
    private final Repositories.Profiles profiles;

    public NutritionController(Repositories.Profiles profiles) {
        this.profiles = profiles;
    }

    public record PlanView(Nutrition.Targets targets, List<Nutrition.Supplement> supplements) {}

    private Nutrition.Input input(Long userId) {
        Profile p = profiles.findByUserId(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Preencha a avaliação inicial primeiro."));
        int age = p.getBirthDate() == null ? 35 : Period.between(p.getBirthDate(), LocalDate.now()).getYears();
        boolean flags = notBlank(p.getMedications()) || notBlank(p.getConditions()) || Boolean.TRUE.equals(p.getParqAlert());
        return new Nutrition.Input(p.getSex() == null ? "M" : p.getSex(), age, p.getHeightCm(), p.getWeightKg(), p.getJobActivity(),
                p.getTrainingDays() == null ? 6 : p.getTrainingDays(), time(p.getTrainingStart(), "07:00"), time(p.getTrainingEnd(), "09:00"),
                p.getKcalAdjust() == null ? 0 : p.getKcalAdjust(), Boolean.TRUE.equals(p.getLactoseIntolerant()), flags);
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static String time(String value, String fallback) { return value != null && value.matches("\\d{2}:\\d{2}") ? value : fallback; }

    @GetMapping("/plan")
    public PlanView plan(@RequestAttribute Long userId) {
        Nutrition.Input in = input(userId);
        return new PlanView(Nutrition.targets(in), Nutrition.supplements(in));
    }

    @GetMapping("/menu")
    public Nutrition.DayMenu menu(@RequestAttribute Long userId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Nutrition.menu(date == null ? LocalDate.now() : date, input(userId));
    }

    @GetMapping("/shopping")
    public List<Nutrition.ShoppingItem> shopping(@RequestAttribute Long userId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Nutrition.shopping(date == null ? LocalDate.now() : date, input(userId));
    }
}
