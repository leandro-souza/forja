package br.com.forja.core;

/** Fases do mesociclo. rir = repeticoes que devem sobrar ao final de cada serie. */
public enum Phase {
    READAPT("Readaptação", "Volume baixo para reacostumar articulações e tendões. Termine cada série sentindo que faria mais 4 repetições.", 4),
    BASE("Base", "Cargas moderadas e execução perfeita. Termine cada série com 3 repetições sobrando.", 3),
    CHOQUE1("Choque 1", "Pirâmide crescente: a carga sobe e as repetições caem a cada série. Termine com 2 repetições sobrando.", 2),
    CHOQUE2("Choque 2", "Pirâmide mais pesada, com técnica avançada na última série dos exercícios marcados. Deixe 1 repetição sobrando.", 1),
    CHOQUE3("Choque 3", "Semana mais intensa do protocolo. Nas máquinas, leve a última série até a falha com segurança.", 0),
    DELOAD("Recuperação", "Semana leve: 20% menos carga e metade do volume. É aqui que o corpo consolida os ganhos.", 5);

    public final String label;
    public final String guidance;
    public final int rir;

    Phase(String label, String guidance, int rir) {
        this.label = label;
        this.guidance = guidance;
        this.rir = rir;
    }
}
