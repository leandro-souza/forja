package br.com.forja.core;

import java.util.List;

/** Estruturas imutaveis que descrevem um protocolo de treino. Sao gravadas em JSON no banco. */
public final class Plan {
    private Plan() {}

    public record Item(String exercise, int sets, String technique) {}

    public record Workout(String code, String title, List<String> groups, List<String> warmup, List<Item> items, String cardio) {}

    public record Protocol(List<Phase> phases, List<Workout> workouts, List<String> rationale) {}

    public record SetRx(int n, String reps, int repLow, int repHigh, String technique, Double suggestedKg) {}

    public record Rx(List<SetRx> sets, String cadence, int restSec, int rir) {}
}
