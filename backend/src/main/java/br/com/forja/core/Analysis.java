package br.com.forja.core;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Analisa os registros de um protocolo e produz o relatorio que orienta o proximo. */
public final class Analysis {
    private Analysis() {}

    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private static String f(String pattern, Object... args) { return String.format(BR, pattern, args); }

    public record LoggedSet(String exercise, long sessionId, LocalDate date, double weight, int reps) {}

    public record SessionInfo(long id, String workout, LocalDate date, int backPain) {}

    public record Measure(LocalDate date, Double weight, Map<String, Double> circ) {}

    public record ExerciseResult(String exercise, String name, String group, int sessions, double startE1rm, double endE1rm, double gainPct, String verdict) {}

    public record GroupResult(String group, String label, double gainPct, int sets, boolean focus) {}

    public record Report(int sessions, int expectedSessions, int adherencePct, List<ExerciseResult> exercises,
                         List<GroupResult> groups, Double weightChangeKg, Double weeklyRatePct,
                         Map<String, Double> circChanges, double avgBackPain, List<String> painWorkouts,
                         String focus, String focusVerdict, int kcalAdjust, String suggestedFocus, List<String> insights) {}

    /** 1RM estimado pela formula de Epley. */
    public static double e1rm(double weight, int reps) {
        return weight * (1 + Math.min(reps, 20) / 30.0);
    }

    public static Report run(String focus, int expectedSessions, int expectedSoFar, List<SessionInfo> sessions, List<LoggedSet> sets, List<Measure> measures) {
        // Melhor 1RM estimado de cada exercicio em cada sessao, em ordem cronologica
        Map<String, TreeMap<Long, Double>> best = new LinkedHashMap<>();
        Map<String, Integer> setCount = new LinkedHashMap<>();
        Map<Long, LocalDate> sessionDate = new LinkedHashMap<>();
        sessions.forEach(s -> sessionDate.put(s.id(), s.date()));
        for (LoggedSet s : sets) {
            if (s.weight() <= 0 || s.reps() <= 0) continue;
            best.computeIfAbsent(s.exercise(), k -> new TreeMap<>()).merge(s.sessionId(), e1rm(s.weight(), s.reps()), Math::max);
            setCount.merge(ExerciseCatalog.get(s.exercise()).group(), 1, Integer::sum);
        }

        List<ExerciseResult> exercises = new ArrayList<>();
        Map<String, List<Double>> gainsByGroup = new LinkedHashMap<>();
        for (var entry : best.entrySet()) {
            Exercise ex = ExerciseCatalog.get(entry.getKey());
            if (ex.group().equals("CORE")) continue;
            List<Double> series = new ArrayList<>(entry.getValue().values());
            int n = series.size();
            double start = series.subList(0, Math.min(2, n)).stream().mapToDouble(d -> d).max().orElse(0);
            double end = series.subList(Math.max(0, n - 3), n).stream().mapToDouble(d -> d).max().orElse(0);
            double gain = start > 0 ? (end - start) / start * 100 : 0;
            String verdict = n < 3 ? "POUCOS_DADOS" : gain >= 4 ? "RESPONDEU" : gain < 1 ? "ESTAGNOU" : "ESTAVEL";
            exercises.add(new ExerciseResult(ex.code(), ex.name(), ex.group(), n, round1(start), round1(end), round1(gain), verdict));
            if (n >= 3) gainsByGroup.computeIfAbsent(ex.group(), k -> new ArrayList<>()).add(gain);
        }
        exercises.sort(Comparator.comparingDouble(ExerciseResult::gainPct).reversed());

        List<String> focusGroups = ProtocolEngine.focusGroups(focus);
        List<GroupResult> groups = new ArrayList<>();
        for (var e : gainsByGroup.entrySet()) {
            double avg = e.getValue().stream().mapToDouble(d -> d).average().orElse(0);
            groups.add(new GroupResult(e.getKey(), ExerciseCatalog.label(e.getKey()), round1(avg), setCount.getOrDefault(e.getKey(), 0), focusGroups.contains(e.getKey())));
        }
        groups.sort(Comparator.comparingDouble(GroupResult::gainPct).reversed());

        // Medidas corporais: primeira e ultima do periodo
        Double weightChange = null, weeklyRate = null;
        Map<String, Double> circ = new LinkedHashMap<>();
        List<Measure> ms = measures.stream().sorted(Comparator.comparing(Measure::date)).toList();
        if (ms.size() >= 2) {
            Measure first = ms.get(0), last = ms.get(ms.size() - 1);
            if (first.weight() != null && last.weight() != null && ChronoUnit.DAYS.between(first.date(), last.date()) >= 14) {
                weightChange = round1(last.weight() - first.weight());
                double weeks = Math.max(1, ChronoUnit.DAYS.between(first.date(), last.date()) / 7.0);
                weeklyRate = Math.round((last.weight() - first.weight()) / first.weight() / weeks * 1000) / 10.0;
            }
            for (var e : last.circ().entrySet()) {
                Double before = first.circ().get(e.getKey());
                if (before != null && e.getValue() != null) circ.put(e.getKey(), round1(e.getValue() - before));
            }
        }

        double avgPain = sessions.stream().mapToInt(SessionInfo::backPain).average().orElse(0);
        Map<String, List<Integer>> painByWorkout = new LinkedHashMap<>();
        sessions.forEach(s -> painByWorkout.computeIfAbsent(s.workout(), k -> new ArrayList<>()).add(s.backPain()));
        List<String> painWorkouts = painByWorkout.entrySet().stream()
                .filter(e -> e.getValue().stream().mapToInt(i -> i).average().orElse(0) >= 4).map(Map.Entry::getKey).toList();

        int kcalAdjust = 0;
        if (weeklyRate != null) {
            if (weeklyRate < 0.15) kcalAdjust = 200;
            else if (weeklyRate < 0.25) kcalAdjust = 100;
            else if (weeklyRate > 0.6) kcalAdjust = -150;
        }
        Double waist = circ.get("waist");
        if (waist != null && waist > 2 && kcalAdjust > 0) kcalAdjust = 0;

        // Avaliacao do dia dedicado: grupo foco contra a media dos demais
        double focusGain = groups.stream().filter(GroupResult::focus).mapToDouble(GroupResult::gainPct).average().orElse(Double.NaN);
        double othersGain = groups.stream().filter(g -> !g.focus()).mapToDouble(GroupResult::gainPct).average().orElse(Double.NaN);
        String focusLabel = ProtocolEngine.FOCUS_OPTIONS.get(focus);
        String focusVerdict;
        if (Double.isNaN(focusGain) || Double.isNaN(othersGain)) {
            focusVerdict = "Ainda não há registros suficientes para avaliar o dia dedicado a " + focusLabel + ". São necessárias pelo menos 3 sessões por exercício.";
        } else if (focusGain >= othersGain + 1.5) {
            focusVerdict = f("O dia dedicado funcionou: %s ganhou %.1f%% de força contra %.1f%% na média dos demais grupos.", focusLabel, focusGain, othersGain);
        } else if (focusGain <= othersGain - 1.5) {
            focusVerdict = f("O dia dedicado não superou a divisão comum: %s ganhou %.1f%% contra %.1f%% dos demais grupos. Vale repetir o teste com outro grupo antes de concluir.", focusLabel, focusGain, othersGain);
        } else {
            focusVerdict = f("Resultado equivalente: %s ganhou %.1f%% e os demais grupos %.1f%%. O dia dedicado não atrapalhou, mas também não se destacou.", focusLabel, focusGain, othersGain);
        }

        // Proximo foco sugerido: o grupo que menos evoluiu entre as opcoes ainda nao testadas neste protocolo
        String suggested = null;
        double worst = Double.MAX_VALUE;
        for (String option : ProtocolEngine.FOCUS_OPTIONS.keySet()) {
            if (option.equals(focus)) continue;
            List<String> gs = ProtocolEngine.focusGroups(option);
            double g = groups.stream().filter(r -> gs.contains(r.group())).mapToDouble(GroupResult::gainPct).average().orElse(Double.NaN);
            if (!Double.isNaN(g) && g < worst) { worst = g; suggested = option; }
        }
        if (suggested == null) suggested = ProtocolEngine.nextFocusInRotation(focus);

        boolean running = sessions.size() < expectedSessions;
        int base = Math.max(1, Math.min(expectedSessions, expectedSoFar));
        int adherence = Math.min(100, Math.round(sessions.size() * 100f / base));
        List<String> insights = new ArrayList<>();
        insights.add(running
                ? f("Protocolo em andamento: %d de %d sessões feitas. Adesão de %d%% em relação ao previsto até hoje.", sessions.size(), expectedSessions, adherence)
                : f("Você completou as %d sessões do protocolo.", expectedSessions));
        if (adherence < 70) insights.add("A adesão está abaixo de 70%. Antes de aumentar o volume, o próximo protocolo prioriza a regularidade.");
        exercises.stream().filter(e -> e.verdict().equals("RESPONDEU")).limit(3)
                .forEach(e -> insights.add(f("%s respondeu muito bem (+%.1f%% de força) e será mantido.", e.name(), e.gainPct())));
        exercises.stream().filter(e -> e.verdict().equals("ESTAGNOU"))
                .forEach(e -> insights.add(f("%s estagnou (%+.1f%%) e será trocado por uma variação.", e.name(), e.gainPct())));
        if (weeklyRate != null) {
            String msg = f("Seu peso variou %+.1f kg (%+.2f%% por semana). ", weightChange, weeklyRate);
            if (kcalAdjust > 0) msg += "O ganho está lento: a dieta sobe " + kcalAdjust + " kcal.";
            else if (kcalAdjust < 0) msg += "O ganho está acima do ideal e tende a virar gordura: a dieta desce " + (-kcalAdjust) + " kcal.";
            else msg += "Está dentro da faixa ideal de 0,25% a 0,5% por semana: a dieta se mantém.";
            insights.add(msg);
        } else {
            insights.add("O ajuste automático da dieta precisa de duas pesagens com pelo menos 14 dias de intervalo dentro do protocolo. Pese-se toda semana.");
        }
        if (waist != null && waist > 2) insights.add(f("A cintura aumentou %.1f cm. Sinal de ganho de gordura acima do desejado; as calorias não serão aumentadas.", waist));
        if (!painWorkouts.isEmpty()) insights.add("Dor na coluna média igual ou acima de 4 nos treinos " + String.join(", ", painWorkouts) + ". Os exercícios com maior carga na coluna desses treinos serão substituídos.");
        return new Report(sessions.size(), expectedSessions, adherence, exercises, groups, weightChange, weeklyRate, circ,
                round1(avgPain), painWorkouts, focus, focusVerdict, kcalAdjust, suggested, insights);
    }

    private static double round1(double v) { return Math.round(v * 10) / 10.0; }
}
