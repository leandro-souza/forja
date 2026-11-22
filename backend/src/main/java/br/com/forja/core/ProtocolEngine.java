package br.com.forja.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Motor de geracao de protocolos.
 * Divisao ABCD em sequencia, com o treino A dedicado ao grupo foco do protocolo.
 * A cada novo protocolo, usa o relatorio do anterior para manter o que respondeu,
 * trocar o que estagnou, ajustar volume e proteger a coluna.
 */
public final class ProtocolEngine {
    private ProtocolEngine() {}

    public static final Map<String, String> FOCUS_OPTIONS = new LinkedHashMap<>();
    static {
        FOCUS_OPTIONS.put("PEITO", "Peitoral");
        FOCUS_OPTIONS.put("COSTAS", "Costas");
        FOCUS_OPTIONS.put("OMBROS", "Ombros");
        FOCUS_OPTIONS.put("BRACOS", "Braços");
        FOCUS_OPTIONS.put("QUADRICEPS", "Quadríceps");
        FOCUS_OPTIONS.put("POSTERIORES", "Posteriores e glúteos");
    }

    public record Context(boolean backPain, boolean kneePain, boolean returning) {}

    private record Slot(String group, int[] sets) {}

    public static List<String> focusGroups(String focus) {
        return switch (focus) {
            case "BRACOS" -> List.of("BICEPS", "TRICEPS");
            case "POSTERIORES" -> List.of("POSTERIORES", "GLUTEOS");
            default -> List.of(focus);
        };
    }

    public static String nextFocusInRotation(String focus) {
        List<String> keys = new ArrayList<>(FOCUS_OPTIONS.keySet());
        return keys.get((keys.indexOf(focus) + 1) % keys.size());
    }

    private static Slot s(String group, int... sets) { return new Slot(group, sets); }

    private static final List<Slot> LEGS = List.of(s("QUADRICEPS", 3, 4, 3), s("POSTERIORES", 4, 3), s("GLUTEOS", 3), s("PANTURRILHAS", 4));

    /** Divisao dos quatro treinos conforme o grupo foco. O primeiro treino e sempre o dia dedicado. */
    private static List<List<Slot>> split(String focus) {
        return switch (focus) {
            case "PEITO" -> List.of(
                    List.of(s("PEITO", 4, 4, 3, 3, 3)),
                    List.of(s("COSTAS", 4, 4, 3, 3), s("BICEPS", 4, 3)),
                    LEGS,
                    List.of(s("OMBROS", 4, 3, 3), s("TRICEPS", 4, 3), s("TRAPEZIO", 3)));
            case "COSTAS" -> List.of(
                    List.of(s("COSTAS", 4, 4, 3, 3, 3)),
                    List.of(s("PEITO", 4, 4, 3, 3), s("TRICEPS", 4, 3)),
                    LEGS,
                    List.of(s("OMBROS", 4, 3, 3), s("BICEPS", 4, 3), s("TRAPEZIO", 3)));
            case "OMBROS" -> List.of(
                    List.of(s("OMBROS", 4, 4, 3, 3, 3), s("TRAPEZIO", 3)),
                    List.of(s("COSTAS", 4, 4, 3, 3), s("BICEPS", 4, 3)),
                    LEGS,
                    List.of(s("PEITO", 4, 4, 3, 3), s("TRICEPS", 4, 3)));
            case "BRACOS" -> List.of(
                    List.of(s("BICEPS", 4, 3, 3), s("TRICEPS", 4, 3, 3)),
                    List.of(s("PEITO", 4, 4, 3), s("OMBROS", 4, 3, 3)),
                    LEGS,
                    List.of(s("COSTAS", 4, 4, 3, 3), s("TRAPEZIO", 3)));
            case "QUADRICEPS" -> List.of(
                    List.of(s("QUADRICEPS", 3, 4, 4, 3), s("PANTURRILHAS", 4)),
                    List.of(s("PEITO", 4, 4, 3, 3), s("TRICEPS", 4, 3)),
                    List.of(s("COSTAS", 4, 4, 3, 3), s("BICEPS", 4, 3)),
                    List.of(s("POSTERIORES", 4, 3), s("GLUTEOS", 3), s("OMBROS", 4, 3, 3)));
            case "POSTERIORES" -> List.of(
                    List.of(s("POSTERIORES", 4, 4, 3), s("GLUTEOS", 4, 3, 3)),
                    List.of(s("PEITO", 4, 4, 3, 3), s("TRICEPS", 4, 3)),
                    List.of(s("COSTAS", 4, 4, 3, 3), s("BICEPS", 4, 3)),
                    List.of(s("QUADRICEPS", 3, 4, 3), s("PANTURRILHAS", 4), s("OMBROS", 4, 3, 3)));
            default -> throw new IllegalArgumentException("Foco invalido: " + focus);
        };
    }

    public static Plan.Protocol generate(String focus, int number, Context ctx, Plan.Protocol previous, Analysis.Report report) {
        int maxSpine = ctx.backPain() ? 2 : 3;
        Map<String, String> verdicts = new LinkedHashMap<>();
        Map<String, Double> groupGain = new LinkedHashMap<>();
        Set<String> banned = new HashSet<>();
        boolean lowAdherence = false;
        if (report != null) {
            report.exercises().forEach(e -> verdicts.put(e.exercise(), e.verdict()));
            // So os dois grupos que mais evoluiram recebem volume extra
            report.groups().stream().filter(g -> g.gainPct() >= 5).limit(2).forEach(g -> groupGain.put(g.group(), g.gainPct()));
            lowAdherence = report.adherencePct() < 70;
            if (previous != null) {
                for (Plan.Workout w : previous.workouts()) {
                    if (!report.painWorkouts().contains(w.code())) continue;
                    w.items().stream().map(i -> ExerciseCatalog.get(i.exercise())).filter(e -> e.spine() >= 2).forEach(e -> banned.add(e.code()));
                }
            }
        }
        Set<String> usedBefore = new HashSet<>();
        if (previous != null) previous.workouts().forEach(w -> w.items().forEach(i -> usedBefore.add(i.exercise())));

        List<String> rationale = new ArrayList<>();
        String focusLabel = FOCUS_OPTIONS.get(focus);
        rationale.add("Divisão ABCD em sequência, seis sessões por semana. O Treino A é dedicado exclusivamente a " + focusLabel.toLowerCase() + ", o foco deste protocolo.");
        rationale.add(ctx.backPain()
                ? "Todos os exercícios são em máquinas ou polias com o tronco apoiado, para poupar a coluna. Nenhum exercício com barra livre sobre as costas."
                : "Prioridade para máquinas e polias, com progressão de carga registrada série a série.");
        rationale.add("Séries em pirâmide crescente nas semanas de choque: a carga sobe e as repetições caem, como nos protocolos em que você mais evoluiu.");
        if (ctx.returning() && number == 1) rationale.add("Como você está voltando de alguns meses parado, a primeira semana é de readaptação: menos séries e cargas submáximas para tendões e articulações se reacostumarem.");

        List<List<Slot>> split = split(focus);
        String[] codes = {"A", "B", "C", "D"};
        List<Plan.Workout> workouts = new ArrayList<>();
        Set<String> chosenAll = new HashSet<>();
        for (int w = 0; w < 4; w++) {
            List<Plan.Item> items = new ArrayList<>();
            List<String> groups = new ArrayList<>();
            for (Slot slot : split.get(w)) {
                groups.add(slot.group());
                List<Exercise> picked = pick(slot, maxSpine, banned, verdicts, usedBefore, chosenAll, ctx);
                boolean bonus = !lowAdherence && groupGain.getOrDefault(slot.group(), 0.0) >= 5 && (report == null || report.avgBackPain() < 3);
                Exercise lastIsolation = null;
                for (Exercise e : picked) if (!e.compound()) lastIsolation = e;
                boolean restPauseGiven = false;
                for (int i = 0; i < picked.size(); i++) {
                    Exercise e = picked.get(i);
                    int sets = slot.sets()[i];
                    if (bonus && i == 0) sets = Math.min(5, sets + 1);
                    String technique = null;
                    if (e.compound() && !restPauseGiven && !(slot.group().equals("QUADRICEPS") && ctx.kneePain())) { technique = "REST_PAUSE"; restPauseGiven = true; }
                    else if (e == lastIsolation && !(i == 0 && picked.size() > 1)) technique = "DROP_SET";
                    items.add(new Plan.Item(e.code(), sets, technique));
                    chosenAll.add(e.code());
                }
                if (bonus) rationale.add(ExerciseCatalog.label(slot.group()) + " respondeu bem no protocolo anterior (+" + String.valueOf(groupGain.get(slot.group())).replace('.', ',') + "% de força): ganhou uma série a mais no exercício principal.");
            }
            boolean legDay = groups.contains("QUADRICEPS") || groups.contains("POSTERIORES");
            if (w == 0 || w == 3) {
                String[] core = w == 0 ? new String[]{"prancha", "pallof"} : new String[]{"dead_bug", "abdominal_maquina"};
                for (String c : core) items.add(new Plan.Item(c, 3, null));
                groups.add("CORE");
            }
            String title = groups.stream().filter(g -> !g.equals("CORE")).map(ExerciseCatalog::label).reduce((a, b) -> a + ", " + b).orElse("");
            if (w == 0) title = focusLabel + " (dia dedicado)";
            workouts.add(new Plan.Workout(codes[w], title, groups, warmup(legDay, ctx),
                    items, legDay ? null : "15 a 20 minutos de bicicleta ou elíptico em ritmo leve (115 a 130 bpm) depois do treino. Opcional nos dias de muito cansaço."));
        }

        if (report != null) {
            long kept = verdicts.entrySet().stream().filter(e -> e.getValue().equals("RESPONDEU") && chosenAll.contains(e.getKey())).count();
            long swapped = verdicts.entrySet().stream().filter(e -> e.getValue().equals("ESTAGNOU") && !chosenAll.contains(e.getKey())).count();
            if (kept > 0) rationale.add(kept + " exercício(s) que mais geraram ganho no protocolo anterior foram mantidos.");
            if (swapped > 0) rationale.add(swapped + " exercício(s) estagnado(s) foram trocados por variações do mesmo grupo muscular.");
            if (!banned.isEmpty()) rationale.add("Exercícios associados a dor na coluna foram substituídos por opções com o tronco apoiado.");
            if (lowAdherence) rationale.add("O volume foi mantido, sem séries extras, até a adesão passar de 70%.");
        }

        List<Phase> phases = ctx.returning() && number == 1
                ? List.of(Phase.READAPT, Phase.BASE, Phase.CHOQUE1, Phase.CHOQUE2, Phase.CHOQUE3, Phase.DELOAD)
                : List.of(Phase.BASE, Phase.CHOQUE1, Phase.CHOQUE2, Phase.CHOQUE3, Phase.DELOAD);
        return new Plan.Protocol(phases, workouts, rationale);
    }

    /** Escolhe os exercicios de um grupo: primeiro os que responderam, depois novidades, por ultimo os estagnados. */
    private static List<Exercise> pick(Slot slot, int maxSpine, Set<String> banned, Map<String, String> verdicts,
                                       Set<String> usedBefore, Set<String> chosenAll, Context ctx) {
        List<Exercise> candidates = new ArrayList<>(ExerciseCatalog.ofGroup(slot.group()).stream()
                .filter(e -> e.spine() <= maxSpine && !banned.contains(e.code()) && !chosenAll.contains(e.code())).toList());
        List<Exercise> catalogOrder = List.copyOf(candidates);
        candidates.sort(Comparator.comparingInt((Exercise e) -> rank(e, verdicts, usedBefore)).thenComparingInt(catalogOrder::indexOf));
        int n = Math.min(slot.sets().length, candidates.size());
        List<Exercise> picked = new ArrayList<>(candidates.subList(0, n));
        // Multiarticulares primeiro; no quadriceps a extensora abre o treino como pre-ativacao que poupa os joelhos
        picked.sort(Comparator.comparingInt((Exercise e) -> e.compound() ? 0 : 1).thenComparingInt(catalogOrder::indexOf));
        if (slot.group().equals("QUADRICEPS")) {
            picked.stream().filter(e -> e.code().equals("extensora")).findFirst().ifPresent(e -> { picked.remove(e); picked.add(0, e); });
        }
        return picked;
    }

    private static int rank(Exercise e, Map<String, String> verdicts, Set<String> usedBefore) {
        String v = verdicts.get(e.code());
        if ("RESPONDEU".equals(v)) return 0;
        if ("ESTAGNOU".equals(v)) return 4;
        if (verdicts.isEmpty()) return 1;                       // primeiro protocolo: ordem do catalogo
        if ("ESTAVEL".equals(v) && e.compound()) return 1;      // multiarticular estavel segue para dar continuidade
        if (!usedBefore.contains(e.code())) return 2;           // variacao nova
        return 3;                                               // isolado estavel: roda para variar o estimulo
    }

    private static List<String> warmup(boolean legDay, Context ctx) {
        List<String> w = new ArrayList<>();
        w.add("5 minutos de bicicleta ou esteira em ritmo leve.");
        if (ctx.backPain()) w.add("Gato-camelo: 10 repetições lentas. Bird-dog: 2 séries de 8 por lado.");
        if (legDay) {
            w.add("Mobilidade de tornozelo e quadril: 10 repetições por lado.");
            w.add("Ponte de glúteos no solo: 2 séries de 12.");
        } else {
            w.add("Rotação externa de ombro na polia: 2 séries de 15 com carga leve.");
            w.add("Retração escapular na polia ou elástico: 2 séries de 15.");
        }
        w.add("No primeiro exercício, 2 séries de aquecimento: 12 repetições com metade da carga e 8 com 70%.");
        return w;
    }

    private static final String[] PYRAMID_1 = {"12-15", "10-12", "8-10", "8-10", "8-10"};
    private static final String[] PYRAMID_2 = {"10-12", "8-10", "6-8", "6-8", "6-8"};

    /** Prescricao de series, repeticoes, cadencia e descanso de um item para a fase da semana. */
    public static Plan.Rx prescribe(Plan.Item item, Phase phase, Double e1rm) {
        Exercise ex = ExerciseCatalog.get(item.exercise());
        boolean core = ex.group().equals("CORE");
        boolean choque = phase == Phase.CHOQUE1 || phase == Phase.CHOQUE2 || phase == Phase.CHOQUE3;
        int n = switch (phase) {
            case READAPT -> Math.max(2, item.sets() - 2);
            case BASE -> Math.max(2, item.sets() - 1);
            case DELOAD -> 2;
            default -> item.sets();
        };
        List<Plan.SetRx> sets = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String reps;
            if (ex.code().equals("prancha")) reps = "30-45";
            else if (core) reps = "12-15";
            else reps = switch (phase) {
                case READAPT, DELOAD -> "12-15";
                case BASE -> "10-12";
                case CHOQUE1 -> PYRAMID_1[i];
                default -> ex.compound() ? PYRAMID_2[i] : PYRAMID_1[i];
            };
            int lo = Integer.parseInt(reps.split("-")[0]), hi = Integer.parseInt(reps.split("-")[1]);
            String technique = (i == n - 1 && item.technique() != null && (phase == Phase.CHOQUE2 || phase == Phase.CHOQUE3)) ? item.technique() : null;
            Double kg = null;
            if (e1rm != null && !core) {
                // Carga para o meio da faixa de repeticoes, a partir da melhor serie da ultima sessao.
                // Quem bate o topo da faixa recebe mais carga na sessao seguinte; quem fica abaixo, menos.
                double w = e1rm / (1 + ((lo + hi) / 2.0) / 30.0);
                if (phase == Phase.DELOAD) w *= 0.8;
                kg = (double) Math.round(w);
            }
            sets.add(new Plan.SetRx(i + 1, reps, lo, hi, technique, kg));
        }
        int rest = core ? 45 : ex.compound() ? (choque ? 150 : phase == Phase.BASE ? 120 : 90) : (choque ? 90 : phase == Phase.BASE ? 75 : 60);
        String cadence = ex.code().equals("prancha") ? "ISO" : ex.compound() ? "3010" : "3011";
        return new Plan.Rx(sets, cadence, rest, phase.rir);
    }
}
