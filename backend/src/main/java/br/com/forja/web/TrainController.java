package br.com.forja.web;

import br.com.forja.core.Analysis;
import br.com.forja.core.Exercise;
import br.com.forja.core.ExerciseCatalog;
import br.com.forja.core.ProtocolEngine;
import br.com.forja.domain.Protocol;
import br.com.forja.domain.Repositories;
import br.com.forja.domain.SessionLog;
import br.com.forja.domain.SetLog;
import br.com.forja.service.TrainingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Treino do dia, registro de sessoes, evolucao de cargas e ciclo de protocolos. */
@RestController
@RequestMapping("/api")
public class TrainController {
    private final TrainingService training;
    private final Repositories.Protocols protocols;
    private final Repositories.Sessions sessions;
    private final Repositories.Sets sets;
    private final ObjectMapper mapper;

    public TrainController(TrainingService training, Repositories.Protocols protocols, Repositories.Sessions sessions,
                           Repositories.Sets sets, ObjectMapper mapper) {
        this.training = training;
        this.protocols = protocols;
        this.sessions = sessions;
        this.sets = sets;
        this.mapper = mapper;
    }

    public record SessionView(Long id, LocalDate date, String workoutCode, int protocolNumber, Integer week, Integer backPain, Integer energy,
                              Integer durationMin, int sets, double volumeKg) {}

    public record Point(LocalDate date, double e1rm, double topWeight, int topReps) {}

    public record ExerciseSeries(String code, String name, String group, String groupLabel, List<Point> points) {}

    public record ProtocolSummary(Long id, int number, String focus, String focusLabel, LocalDate startDate, LocalDate endDate, String status, JsonNode report) {}

    public record NextRequest(String focus) {}

    public record FocusOption(String code, String label) {}

    @GetMapping("/train")
    public TrainingService.TrainView train(@RequestAttribute Long userId) {
        return training.train(userId);
    }

    @PostMapping("/sessions")
    public SessionLog log(@RequestAttribute Long userId, @RequestBody TrainingService.SessionInput in) {
        return training.log(userId, in);
    }

    @GetMapping("/sessions")
    public List<SessionView> history(@RequestAttribute Long userId) {
        Map<Long, Integer> numbers = new LinkedHashMap<>();
        protocols.findByUserIdOrderByNumberAsc(userId).forEach(p -> numbers.put(p.getId(), p.getNumber()));
        Map<Long, List<SetLog>> bySession = new LinkedHashMap<>();
        sets.findByUserIdOrderByIdAsc(userId).forEach(s -> bySession.computeIfAbsent(s.getSessionId(), k -> new ArrayList<>()).add(s));
        List<SessionView> out = new ArrayList<>();
        for (SessionLog s : sessions.findByUserIdOrderByDateAscIdAsc(userId)) {
            List<SetLog> ss = bySession.getOrDefault(s.getId(), List.of());
            double volume = ss.stream().mapToDouble(x -> (x.getWeight() == null ? 0 : x.getWeight()) * (x.getReps() == null ? 0 : x.getReps())).sum();
            out.add(new SessionView(s.getId(), s.getDate(), s.getWorkoutCode(), numbers.getOrDefault(s.getProtocolId(), 0), s.getWeek(),
                    s.getBackPain(), s.getEnergy(), s.getDurationMin(), ss.size(), Math.round(volume)));
        }
        return out;
    }

    @DeleteMapping("/sessions/{id}")
    public void deleteSession(@RequestAttribute Long userId, @PathVariable Long id) {
        sessions.findById(id).filter(s -> userId.equals(s.getUserId())).ifPresent(s -> {
            sets.deleteAll(sets.findBySessionId(s.getId()));
            sessions.delete(s);
        });
    }

    /** Evolucao de forca por exercicio: melhor serie de cada sessao. */
    @GetMapping("/progress")
    public List<ExerciseSeries> progress(@RequestAttribute Long userId) {
        Map<Long, LocalDate> dates = new LinkedHashMap<>();
        sessions.findByUserIdOrderByDateAscIdAsc(userId).forEach(s -> dates.put(s.getId(), s.getDate()));
        Map<String, LinkedHashMap<Long, SetLog>> best = new LinkedHashMap<>();
        for (SetLog s : sets.findByUserIdOrderByIdAsc(userId)) {
            if (s.getWeight() == null || s.getReps() == null || s.getWeight() <= 0) continue;
            best.computeIfAbsent(s.getExerciseCode(), k -> new LinkedHashMap<>()).merge(s.getSessionId(), s,
                    (a, b) -> Analysis.e1rm(b.getWeight(), b.getReps()) > Analysis.e1rm(a.getWeight(), a.getReps()) ? b : a);
        }
        List<ExerciseSeries> out = new ArrayList<>();
        for (var e : best.entrySet()) {
            Exercise ex = ExerciseCatalog.get(e.getKey());
            List<Point> points = e.getValue().values().stream().filter(s -> dates.containsKey(s.getSessionId()))
                    .map(s -> new Point(dates.get(s.getSessionId()), Math.round(Analysis.e1rm(s.getWeight(), s.getReps()) * 10) / 10.0, s.getWeight(), s.getReps())).toList();
            out.add(new ExerciseSeries(ex.code(), ex.name(), ex.group(), ExerciseCatalog.label(ex.group()), points));
        }
        return out;
    }

    @GetMapping("/protocols")
    public List<ProtocolSummary> protocols(@RequestAttribute Long userId) {
        List<ProtocolSummary> out = new ArrayList<>();
        for (Protocol p : protocols.findByUserIdOrderByNumberAsc(userId)) {
            JsonNode report = null;
            try {
                if (p.getReportJson() != null) report = mapper.readTree(p.getReportJson());
            } catch (Exception ignored) {
                // relatorio antigo ilegivel: segue sem ele
            }
            out.add(new ProtocolSummary(p.getId(), p.getNumber(), p.getFocus(), ProtocolEngine.FOCUS_OPTIONS.get(p.getFocus()), p.getStartDate(), p.getEndDate(), p.getStatus(), report));
        }
        return out;
    }

    @GetMapping("/protocols/focus-options")
    public List<FocusOption> focusOptions() {
        return ProtocolEngine.FOCUS_OPTIONS.entrySet().stream().map(e -> new FocusOption(e.getKey(), e.getValue())).toList();
    }

    @GetMapping("/protocols/analysis")
    public Analysis.Report analysis(@RequestAttribute Long userId) {
        return training.analyze(userId);
    }

    @PostMapping("/protocols/next")
    public TrainingService.TrainView next(@RequestAttribute Long userId, @RequestBody NextRequest req) {
        training.next(userId, req.focus());
        return training.train(userId);
    }
}
