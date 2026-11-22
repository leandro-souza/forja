package br.com.forja.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EngineTest {
    private final ProtocolEngine.Context spineCare = new ProtocolEngine.Context(true, false, true);

    @Test
    void treinoAEhDedicadoAoGrupoFoco() {
        for (String focus : ProtocolEngine.FOCUS_OPTIONS.keySet()) {
            Plan.Protocol plan = ProtocolEngine.generate(focus, 1, spineCare, null, null);
            assertEquals(4, plan.workouts().size());
            List<String> allowed = new ArrayList<>(ProtocolEngine.focusGroups(focus));
            allowed.addAll(List.of("CORE", "TRAPEZIO", "PANTURRILHAS"));
            for (Plan.Item item : plan.workouts().get(0).items()) {
                assertTrue(allowed.contains(ExerciseCatalog.get(item.exercise()).group()), focus + ": " + item.exercise());
            }
        }
    }

    @Test
    void quemTemDorNaColunaNaoRecebeExercicioDeCargaAxialAlta() {
        Plan.Protocol plan = ProtocolEngine.generate("QUADRICEPS", 1, spineCare, null, null);
        plan.workouts().forEach(w -> w.items().forEach(i -> assertTrue(ExerciseCatalog.get(i.exercise()).spine() <= 2, i.exercise())));
    }

    @Test
    void retornoComecaPorReadaptacaoETerminaEmDeload() {
        Plan.Protocol plan = ProtocolEngine.generate("PEITO", 1, spineCare, null, null);
        assertEquals(Phase.READAPT, plan.phases().get(0));
        assertEquals(Phase.DELOAD, plan.phases().get(plan.phases().size() - 1));
    }

    @Test
    void piramideAumentaCargaEReduzRepeticoes() {
        Plan.Rx rx = ProtocolEngine.prescribe(new Plan.Item("supino_reto_maquina", 4, "REST_PAUSE"), Phase.CHOQUE2, 100.0);
        assertEquals(4, rx.sets().size());
        assertTrue(rx.sets().get(3).suggestedKg() > rx.sets().get(0).suggestedKg());
        assertTrue(rx.sets().get(3).repHigh() < rx.sets().get(0).repHigh());
        assertEquals("REST_PAUSE", rx.sets().get(3).technique());
    }

    @Test
    void proximoProtocoloMantemQuemRespondeuETrocaQuemEstagnou() {
        Plan.Protocol first = ProtocolEngine.generate("PEITO", 1, spineCare, null, null);
        List<Analysis.SessionInfo> sessions = new ArrayList<>();
        List<Analysis.LoggedSet> sets = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 1, 5);
        for (int i = 0; i < 24; i++) {
            Plan.Workout w = first.workouts().get(i % 4);
            sessions.add(new Analysis.SessionInfo(i + 1, w.code(), start.plusDays(i), 0));
            for (Plan.Item item : w.items()) {
                double growth = item.exercise().equals("supino_reto_maquina") ? 0.03 : 0.0;
                sets.add(new Analysis.LoggedSet(item.exercise(), i + 1, start.plusDays(i), 50 * (1 + growth * (i / 4)), 10));
            }
        }
        Analysis.Report report = Analysis.run("PEITO", 36, 36, sessions, sets, List.of());
        Plan.Protocol next = ProtocolEngine.generate("COSTAS", 2, new ProtocolEngine.Context(true, false, false), first, report);
        List<String> chosen = next.workouts().stream().flatMap(w -> w.items().stream()).map(Plan.Item::exercise).toList();
        assertTrue(chosen.contains("supino_reto_maquina"));
        assertFalse(chosen.contains("supino_inclinado_maquina"));
    }

    @Test
    void cardapioFicaPertoDasMetas() {
        Nutrition.Input in = new Nutrition.Input("M", 43, 178, 86, "SEDENTARIO", 6, "07:00", "09:00", 0, true, false);
        Nutrition.Targets t = Nutrition.targets(in);
        for (int d = 0; d < 14; d++) {
            Nutrition.DayMenu menu = Nutrition.menu(LocalDate.of(2026, 11, 23).plusDays(d), in);
            assertTrue(Math.abs(menu.kcal() - t.kcal()) < t.kcal() * 0.10, "kcal no dia " + d + ": " + menu.kcal());
            assertTrue(Math.abs(menu.p() - t.protein()) < 25, "proteina no dia " + d + ": " + menu.p());
        }
    }
}
