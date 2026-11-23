package br.com.forja.service;

import br.com.forja.core.Analysis;
import br.com.forja.core.Exercise;
import br.com.forja.core.ExerciseCatalog;
import br.com.forja.core.Phase;
import br.com.forja.core.Plan;
import br.com.forja.core.ProtocolEngine;
import br.com.forja.domain.Measurement;
import br.com.forja.domain.Profile;
import br.com.forja.domain.Protocol;
import br.com.forja.domain.Repositories;
import br.com.forja.domain.SessionLog;
import br.com.forja.domain.SetLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Regras de treino: protocolo atual, prescricao da semana, registro de sessoes e geracao do proximo protocolo. */
@Service
public class TrainingService {
    public static final int SESSIONS_PER_WEEK = 6;

    private final Repositories.Protocols protocols;
    private final Repositories.Sessions sessions;
    private final Repositories.Sets sets;
    private final Repositories.Profiles profiles;
    private final Repositories.Measurements measurements;
    private final ObjectMapper mapper;

    public TrainingService(Repositories.Protocols protocols, Repositories.Sessions sessions, Repositories.Sets sets,
                           Repositories.Profiles profiles, Repositories.Measurements measurements, ObjectMapper mapper) {
        this.protocols = protocols;
        this.sessions = sessions;
        this.sets = sets;
        this.profiles = profiles;
        this.measurements = measurements;
        this.mapper = mapper;
    }

    public record ExerciseView(String code, String name, String group, String groupLabel, String img, boolean compound, String cue) {}

    public record LastSet(double weight, int reps) {}

    public record WeekPlan(int week, String phase, String scheme, String technique) {}

    public record ItemView(ExerciseView exercise, String technique, Plan.Rx rx, List<LastSet> last, List<ExerciseView> alternatives, List<WeekPlan> weeks) {}

    public record WorkoutView(String code, String title, List<String> warmup, String cardio, int totalSets, LocalDate lastDone, List<ItemView> items) {}

    public record PhaseView(String code, String label, String guidance, int rir) {}

    public record ProtocolView(Long id, int number, String focus, String focusLabel, LocalDate startDate, int weeks, int week,
                               PhaseView phase, List<PhaseView> phases, int sessionsDone, int sessionsTotal, String nextWorkout,
                               boolean finished, List<String> rationale) {}

    public record TrainView(ProtocolView protocol, List<WorkoutView> workouts) {}

    public record SetInput(String exercise, int setNumber, double weight, int reps, Integer rpe) {}

    public record SessionInput(String workoutCode, LocalDate date, Integer backPain, Integer energy, Integer durationMin, String notes, List<SetInput> sets) {}

    public static ExerciseView view(Exercise e) {
        return new ExerciseView(e.code(), e.name(), e.group(), ExerciseCatalog.label(e.group()), e.img(), e.compound(), e.cue());
    }

    /** Resumo da prescricao de um item em cada semana do protocolo, para a tela de plano completo. */
    private static List<WeekPlan> weeks(Plan.Item item, List<Phase> phases) {
        List<WeekPlan> out = new ArrayList<>();
        for (int i = 0; i < phases.size(); i++) {
            Plan.Rx rx = ProtocolEngine.prescribe(item, phases.get(i), null);
            List<String> reps = rx.sets().stream().map(Plan.SetRx::reps).toList();
            boolean same = reps.stream().distinct().count() == 1;
            String unit = "ISO".equals(rx.cadence()) ? " s" : "";
            String scheme = same ? reps.size() + " × " + reps.get(0) + unit : String.join(" / ", reps);
            String technique = rx.sets().get(rx.sets().size() - 1).technique();
            out.add(new WeekPlan(i + 1, phases.get(i).label, scheme, technique));
        }
        return out;
    }

    public Optional<Protocol> current(Long userId) {
        return protocols.findByUserIdOrderByNumberAsc(userId).stream().filter(p -> "ACTIVE".equals(p.getStatus())).reduce((a, b) -> b);
    }

    public Plan.Protocol plan(Protocol p) {
        try {
            return mapper.readValue(p.getPlanJson(), Plan.Protocol.class);
        } catch (Exception e) {
            throw new IllegalStateException("Plano do protocolo ilegivel", e);
        }
    }

    private ProtocolEngine.Context context(Profile profile) {
        return new ProtocolEngine.Context(Boolean.TRUE.equals(profile.getBackPain()), Boolean.TRUE.equals(profile.getKneePain()),
                profile.getMonthsOff() != null && profile.getMonthsOff() >= 1);
    }

    /** Cria o primeiro protocolo do usuario. */
    @Transactional
    public Protocol createFirst(Long userId, Profile profile, String focus) {
        Plan.Protocol plan = ProtocolEngine.generate(focus, 1, context(profile), null, null);
        return save(userId, 1, focus, plan);
    }

    private Protocol save(Long userId, int number, String focus, Plan.Protocol plan) {
        Protocol p = new Protocol();
        p.setUserId(userId);
        p.setNumber(number);
        p.setFocus(focus);
        p.setStartDate(LocalDate.now());
        p.setStatus("ACTIVE");
        try {
            p.setPlanJson(mapper.writeValueAsString(plan));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return protocols.save(p);
    }

    public TrainView train(Long userId) {
        Protocol p = current(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Preencha a avaliação inicial para gerar seu primeiro protocolo."));
        Plan.Protocol plan = plan(p);
        List<SessionLog> done = sessions.findByUserIdOrderByDateAscIdAsc(userId).stream().filter(s -> p.getId().equals(s.getProtocolId())).toList();
        int total = plan.phases().size() * SESSIONS_PER_WEEK;
        int weekIndex = Math.min(done.size() / SESSIONS_PER_WEEK, plan.phases().size() - 1);
        Phase phase = plan.phases().get(weekIndex);

        // Historico de todas as series do usuario, por exercicio e sessao (inclui protocolos anteriores)
        Map<String, LinkedHashMap<Long, List<SetLog>>> history = new LinkedHashMap<>();
        for (SetLog s : sets.findByUserIdOrderByIdAsc(userId)) {
            history.computeIfAbsent(s.getExerciseCode(), k -> new LinkedHashMap<>()).computeIfAbsent(s.getSessionId(), k -> new ArrayList<>()).add(s);
        }
        boolean backPain = profiles.findByUserId(userId).map(pr -> Boolean.TRUE.equals(pr.getBackPain())).orElse(false);

        List<WorkoutView> views = new ArrayList<>();
        for (Plan.Workout w : plan.workouts()) {
            List<ItemView> items = new ArrayList<>();
            int totalSets = 0;
            for (Plan.Item item : w.items()) {
                Exercise ex = ExerciseCatalog.get(item.exercise());
                List<List<SetLog>> bySession = new ArrayList<>(history.getOrDefault(ex.code(), new LinkedHashMap<>()).values());
                Double e1rm = null;
                List<LastSet> last = List.of();
                if (!bySession.isEmpty()) {
                    // Referencia de forca: melhor serie da ultima sessao em que o exercicio foi feito
                    e1rm = bySession.get(bySession.size() - 1).stream()
                            .filter(s -> s.getWeight() != null && s.getWeight() > 0 && s.getReps() != null)
                            .map(s -> Analysis.e1rm(s.getWeight(), s.getReps())).max(Double::compare).orElse(null);
                    last = bySession.get(bySession.size() - 1).stream().map(s -> new LastSet(s.getWeight() == null ? 0 : s.getWeight(), s.getReps() == null ? 0 : s.getReps())).toList();
                }
                Plan.Rx rx = ProtocolEngine.prescribe(item, phase, e1rm);
                totalSets += rx.sets().size();
                List<ExerciseView> alternatives = ExerciseCatalog.ofGroup(ex.group()).stream()
                        .filter(a -> !a.code().equals(ex.code()) && a.spine() <= (backPain ? 2 : 3)).limit(4).map(TrainingService::view).toList();
                items.add(new ItemView(view(ex), item.technique(), rx, last, alternatives, weeks(item, plan.phases())));
            }
            LocalDate lastDone = done.stream().filter(s -> w.code().equals(s.getWorkoutCode())).map(SessionLog::getDate).reduce((a, b) -> b).orElse(null);
            views.add(new WorkoutView(w.code(), w.title(), w.warmup(), w.cardio(), totalSets, lastDone, items));
        }
        String next = plan.workouts().get(done.size() % plan.workouts().size()).code();
        List<PhaseView> phases = plan.phases().stream().map(ph -> new PhaseView(ph.name(), ph.label, ph.guidance, ph.rir)).toList();
        ProtocolView pv = new ProtocolView(p.getId(), p.getNumber(), p.getFocus(), ProtocolEngine.FOCUS_OPTIONS.get(p.getFocus()), p.getStartDate(),
                plan.phases().size(), weekIndex + 1, phases.get(weekIndex), phases, done.size(), total, next, done.size() >= total, plan.rationale());
        return new TrainView(pv, views);
    }

    @Transactional
    public SessionLog log(Long userId, SessionInput in) {
        Protocol p = current(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum protocolo ativo."));
        long done = sessions.findByUserIdOrderByDateAscIdAsc(userId).stream().filter(s -> p.getId().equals(s.getProtocolId())).count();
        SessionLog s = new SessionLog();
        s.setUserId(userId);
        s.setProtocolId(p.getId());
        s.setWorkoutCode(in.workoutCode());
        s.setDate(in.date() == null ? LocalDate.now() : in.date());
        s.setWeek((int) (done / SESSIONS_PER_WEEK) + 1);
        s.setBackPain(in.backPain() == null ? 0 : in.backPain());
        s.setEnergy(in.energy());
        s.setDurationMin(in.durationMin());
        s.setNotes(in.notes());
        s = sessions.save(s);
        if (in.sets() != null) {
            for (SetInput si : in.sets()) {
                if (si.reps() <= 0) continue;
                ExerciseCatalog.get(si.exercise());
                SetLog l = new SetLog();
                l.setUserId(userId);
                l.setSessionId(s.getId());
                l.setProtocolId(p.getId());
                l.setExerciseCode(si.exercise());
                l.setSetNumber(si.setNumber());
                l.setWeight(si.weight());
                l.setReps(si.reps());
                l.setRpe(si.rpe());
                sets.save(l);
            }
        }
        return s;
    }

    /** Relatorio do protocolo atual, sem alterar nada. */
    public Analysis.Report analyze(Long userId) {
        Protocol p = current(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum protocolo ativo."));
        return analyze(userId, p);
    }

    private Analysis.Report analyze(Long userId, Protocol p) {
        Plan.Protocol plan = plan(p);
        List<SessionLog> done = sessions.findByUserIdOrderByDateAscIdAsc(userId).stream().filter(s -> p.getId().equals(s.getProtocolId())).toList();
        Map<Long, LocalDate> dates = new LinkedHashMap<>();
        done.forEach(s -> dates.put(s.getId(), s.getDate()));
        List<Analysis.SessionInfo> infos = done.stream().map(s -> new Analysis.SessionInfo(s.getId(), s.getWorkoutCode(), s.getDate(), s.getBackPain() == null ? 0 : s.getBackPain())).toList();
        List<Analysis.LoggedSet> logged = sets.findByUserIdOrderByIdAsc(userId).stream()
                .filter(s -> p.getId().equals(s.getProtocolId()) && s.getWeight() != null && s.getReps() != null)
                .map(s -> new Analysis.LoggedSet(s.getExerciseCode(), s.getSessionId(), dates.get(s.getSessionId()), s.getWeight(), s.getReps())).toList();
        // Medidas do periodo, incluindo a ultima anterior ao inicio como ponto de partida
        List<Measurement> all = measurements.findByUserIdOrderByDateAsc(userId);
        List<Analysis.Measure> ms = new ArrayList<>();
        Measurement before = null;
        for (Measurement m : all) {
            if (m.getDate().isBefore(p.getStartDate())) before = m;
            else ms.add(measure(m));
        }
        if (before != null && java.time.temporal.ChronoUnit.DAYS.between(before.getDate(), p.getStartDate()) <= 14) ms.add(0, measure(before));
        long days = java.time.temporal.ChronoUnit.DAYS.between(p.getStartDate(), LocalDate.now()) + 1;
        int expectedSoFar = (int) Math.ceil(days * SESSIONS_PER_WEEK / 7.0);
        return Analysis.run(p.getFocus(), plan.phases().size() * SESSIONS_PER_WEEK, expectedSoFar, infos, logged, ms);
    }

    private static Analysis.Measure measure(Measurement m) {
        Map<String, Double> circ = new LinkedHashMap<>();
        if (m.getChest() != null) circ.put("chest", m.getChest());
        if (m.getShoulders() != null) circ.put("shoulders", m.getShoulders());
        if (m.getArm() != null) circ.put("arm", m.getArm());
        if (m.getWaist() != null) circ.put("waist", m.getWaist());
        if (m.getHip() != null) circ.put("hip", m.getHip());
        if (m.getThigh() != null) circ.put("thigh", m.getThigh());
        if (m.getCalf() != null) circ.put("calf", m.getCalf());
        return new Analysis.Measure(m.getDate(), m.getWeightKg(), circ);
    }

    /** Encerra o protocolo atual com seu relatorio e gera o proximo, adaptado aos resultados. */
    @Transactional
    public Protocol next(Long userId, String focus) {
        Protocol cur = current(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum protocolo ativo."));
        Profile profile = profiles.findByUserId(userId).orElseThrow();
        Analysis.Report report = analyze(userId, cur);
        String chosen = focus != null && ProtocolEngine.FOCUS_OPTIONS.containsKey(focus) ? focus : report.suggestedFocus();
        ProtocolEngine.Context ctx = new ProtocolEngine.Context(Boolean.TRUE.equals(profile.getBackPain()) || report.avgBackPain() >= 3,
                Boolean.TRUE.equals(profile.getKneePain()), false);
        Plan.Protocol plan = ProtocolEngine.generate(chosen, cur.getNumber() + 1, ctx, plan(cur), report);
        try {
            cur.setReportJson(mapper.writeValueAsString(report));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        cur.setStatus("DONE");
        cur.setEndDate(LocalDate.now());
        protocols.save(cur);
        if (report.kcalAdjust() != 0) {
            profile.setKcalAdjust((profile.getKcalAdjust() == null ? 0 : profile.getKcalAdjust()) + report.kcalAdjust());
            profiles.save(profile);
        }
        return save(userId, cur.getNumber() + 1, chosen, plan);
    }
}
