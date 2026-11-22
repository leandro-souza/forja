package br.com.forja.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catalogo de exercicios em maquinas e polias. A ordem dentro do grupo define a prioridade de escolha. */
public final class ExerciseCatalog {
    private static final Map<String, Exercise> BY_CODE = new LinkedHashMap<>();

    public static final Map<String, String> GROUP_LABELS = new LinkedHashMap<>();

    static {
        GROUP_LABELS.put("PEITO", "Peitoral");
        GROUP_LABELS.put("COSTAS", "Costas");
        GROUP_LABELS.put("OMBROS", "Ombros");
        GROUP_LABELS.put("TRAPEZIO", "Trapézio");
        GROUP_LABELS.put("BICEPS", "Bíceps");
        GROUP_LABELS.put("TRICEPS", "Tríceps");
        GROUP_LABELS.put("QUADRICEPS", "Quadríceps");
        GROUP_LABELS.put("POSTERIORES", "Posteriores de coxa");
        GROUP_LABELS.put("GLUTEOS", "Glúteos e quadril");
        GROUP_LABELS.put("PANTURRILHAS", "Panturrilhas");
        GROUP_LABELS.put("CORE", "Abdômen e core");

        String apoio = "Mantenha as costas e a cabeça apoiadas no encosto durante toda a série.";
        // Peitoral
        add("supino_reto_maquina", "Supino reto na máquina articulada", "PEITO", "Leverage_Chest_Press", true, 1, "Ajuste o banco para as pegadas ficarem na linha do meio do peito. Escápulas juntas e para baixo, desça controlando até alongar o peitoral e empurre sem travar os cotovelos. " + apoio);
        add("supino_inclinado_maquina", "Supino inclinado na máquina articulada", "PEITO", "Leverage_Incline_Chest_Press", true, 1, "Pegadas na linha da parte alta do peito. Cotovelos a cerca de 45° do tronco, desça em 3 segundos e suba sem tirar os ombros do encosto.");
        add("peck_deck", "Crucifixo na máquina (peck deck)", "PEITO", "Butterfly", false, 1, "Cotovelos levemente flexionados e na altura dos ombros. Feche até as mãos quase se tocarem, segure 1 segundo e volte devagar até sentir o peitoral alongar.");
        add("crossover_baixa", "Crossover na polia baixa", "PEITO", "Low_Cable_Crossover", false, 1, "Polias embaixo, um pé à frente e tronco firme. Suba as mãos em arco até a altura do rosto, apertando a parte alta do peito, e desça controlando.");
        add("supino_declinado_maquina", "Supino declinado na máquina", "PEITO", "Leverage_Decline_Chest_Press", true, 1, "Empurre para a frente e para baixo, pensando em aproximar os braços na linha inferior do peito. " + apoio);
        add("crossover_alta", "Crossover na polia alta", "PEITO", "Cable_Crossover", false, 1, "Polias no alto, leve inclinação do tronco à frente sem arredondar a lombar. Desça as mãos em arco até cruzarem à frente do quadril e volte devagar.");
        add("supino_vertical", "Supino vertical sentado (chest press)", "PEITO", "Machine_Bench_Press", true, 1, "Banco regulado para as mãos ficarem na altura do peito. Empurre à frente sem desencostar os ombros e volte até os cotovelos passarem um pouco da linha do tronco.");
        add("crucifixo_polia_banco", "Crucifixo no banco com polias", "PEITO", "Flat_Bench_Cable_Flyes", false, 1, "Deitado no banco entre as polias baixas, abra os braços com cotovelos semiflexionados e feche em arco acima do peito.");
        add("supino_inclinado_smith", "Supino inclinado no Smith", "PEITO", "Smith_Machine_Incline_Bench_Press", true, 1, "Banco a 30°, barra descendo na parte alta do peito. Pés firmes no chão e lombar em posição neutra.");
        // Costas
        add("puxada_aberta", "Puxada alta com barra aberta", "COSTAS", "Wide-Grip_Lat_Pulldown", true, 1, "Coxas travadas no apoio, peito aberto. Puxe a barra até a parte alta do peito levando os cotovelos para baixo e para trás, sem balançar o tronco.");
        add("remada_maquina_apoio", "Remada na máquina com apoio no peito", "COSTAS", "Leverage_Iso_Row", true, 1, "Peito colado no apoio do começo ao fim: é isso que protege a lombar. Puxe os cotovelos para trás juntando as escápulas e volte até alongar as costas.");
        add("puxada_triangulo", "Puxada alta com triângulo", "COSTAS", "V-Bar_Pulldown", true, 1, "Pegada neutra, puxe o triângulo até o peito mantendo o tronco quase vertical. Controle a volta até estender os braços.");
        add("pulldown", "Pulldown com braços estendidos", "COSTAS", "Straight-Arm_Pulldown", false, 1, "Braços quase estendidos, leve inclinação à frente com abdômen contraído. Leve a barra até as coxas usando as dorsais, sem dobrar os cotovelos.");
        add("puxada_articulada", "Puxada articulada na máquina", "COSTAS", "Leverage_High_Row", true, 1, "Peito no apoio, puxe as alavancas de cima para baixo trazendo os cotovelos junto ao corpo.");
        add("remada_cavalinho_apoio", "Remada cavalinho com apoio no peito", "COSTAS", "Lying_T-Bar_Row", true, 1, "Use sempre a versão com apoio para o peito. Puxe a barra em direção ao abdômen e desça controlando, sem tirar o peito do apoio.");
        add("puxada_supinada", "Puxada alta com pegada supinada", "COSTAS", "Underhand_Cable_Pulldowns", true, 1, "Palmas voltadas para você, mãos na largura dos ombros. Puxe até o peito com os cotovelos colados ao corpo.");
        add("remada_unilateral_polia", "Remada unilateral sentado na polia", "COSTAS", "Seated_One-arm_Cable_Pulley_Rows", true, 1, "Um braço por vez, tronco ereto e parado. Puxe o cotovelo para trás rente ao corpo.");
        add("remada_baixa", "Remada baixa com triângulo", "COSTAS", "Seated_Cable_Rows", true, 2, "Coluna neutra e tronco parado: quem trabalha são os braços e as escápulas. Se a lombar reclamar, troque pela remada com apoio no peito.");
        // Ombros
        add("desenvolvimento_maquina", "Desenvolvimento de ombros na máquina", "OMBROS", "Leverage_Shoulder_Press", true, 1, "Banco ajustado para as mãos partirem da altura das orelhas. Empurre para cima sem travar os cotovelos e sem desencostar a lombar do encosto.");
        add("elevacao_lateral_polia", "Elevação lateral na polia", "OMBROS", "Standing_Low-Pulley_Deltoid_Raise", false, 1, "Um braço por vez, cabo passando pela frente do corpo. Eleve até a linha do ombro com o cotovelo levemente flexionado e desça em 3 segundos.");
        add("crucifixo_inverso", "Crucifixo inverso na máquina", "OMBROS", "Reverse_Machine_Flyes", false, 1, "Peito no apoio, braços na altura dos ombros. Abra até a linha do tronco apertando a parte de trás dos ombros.");
        add("face_pull", "Face pull na polia com corda", "OMBROS", "Face_Pull", false, 1, "Polia na altura do rosto. Puxe a corda em direção à testa abrindo os cotovelos e girando os punhos para trás. Ótimo para a postura dos ombros.");
        add("elevacao_lateral_maquina", "Elevação lateral sentado", "OMBROS", "Cable_Seated_Lateral_Raise", false, 1, "Sentado, tronco ereto. Eleve os braços até a linha dos ombros, cotovelos liderando o movimento.");
        add("elevacao_frontal_polia", "Elevação frontal na polia", "OMBROS", "Front_Cable_Raise", false, 1, "De costas para a polia baixa, eleve o braço à frente até a altura dos olhos, sem jogar o tronco para trás.");
        add("desenvolvimento_smith", "Desenvolvimento sentado no Smith", "OMBROS", "Smith_Machine_Overhead_Shoulder_Press", true, 2, "Banco a 80–90° com encosto. Barra descendo à frente do rosto até a altura do queixo.");
        // Trapezio
        add("encolhimento_maquina", "Encolhimento na máquina", "TRAPEZIO", "Leverage_Shrug", false, 2, "Eleve os ombros em direção às orelhas, segure 1 segundo e desça devagar. Não gire os ombros e mantenha o abdômen firme.");
        add("encolhimento_polia", "Encolhimento na polia baixa", "TRAPEZIO", "Cable_Shrugs", false, 2, "Barra reta na polia baixa, braços estendidos. Suba os ombros na vertical e desça controlando.");
        // Biceps
        add("rosca_scott_maquina", "Rosca Scott na máquina", "BICEPS", "Machine_Preacher_Curls", false, 1, "Braços totalmente apoiados. Suba contraindo o bíceps e desça em 3 segundos até quase estender os cotovelos.");
        add("rosca_martelo_corda", "Rosca martelo na polia com corda", "BICEPS", "Cable_Hammer_Curls_-_Rope_Attachment", false, 1, "Pegada neutra na corda, cotovelos colados ao tronco. Suba sem balançar o corpo.");
        add("rosca_polia_barra", "Rosca direta na polia com barra", "BICEPS", "Standing_Biceps_Cable_Curl", false, 1, "Cotovelos fixos ao lado do corpo, abdômen contraído. A polia mantém tensão constante: não descanse embaixo.");
        add("rosca_maquina", "Rosca bíceps na máquina", "BICEPS", "Machine_Bicep_Curl", false, 1, "Ajuste o banco para o cotovelo alinhar com o eixo da máquina. Movimento completo, sem tirar os braços do apoio.");
        add("rosca_polia_alta", "Rosca bíceps na polia alta", "BICEPS", "High_Cable_Curls", false, 1, "Entre as duas polias altas, braços abertos na linha dos ombros. Flexione levando as mãos em direção às orelhas.");
        add("rosca_scott_polia", "Rosca Scott na polia", "BICEPS", "Cable_Preacher_Curl", false, 1, "Banco Scott de frente para a polia baixa. Tensão contínua do início ao fim.");
        // Triceps
        add("triceps_corda", "Tríceps na polia com corda", "TRICEPS", "Triceps_Pushdown_-_Rope_Attachment", false, 1, "Cotovelos colados ao tronco. Estenda os braços abrindo a corda no final e volte até os antebraços passarem da horizontal.");
        add("triceps_frances_corda", "Tríceps francês na polia com corda", "TRICEPS", "Cable_Rope_Overhead_Triceps_Extension", false, 1, "De costas para a polia, cotovelos apontados para a frente e próximos da cabeça. Estenda os braços sem arquear a lombar; um pé à frente ajuda.");
        add("triceps_barra", "Tríceps na polia com barra reta", "TRICEPS", "Triceps_Pushdown", false, 1, "Pegada pronada, ombros para baixo. Empurre a barra até estender totalmente os cotovelos.");
        add("triceps_maquina", "Tríceps na máquina", "TRICEPS", "Machine_Triceps_Extension", false, 1, "Braços apoiados, estenda os cotovelos por completo e volte devagar.");
        add("paralelas_maquina", "Paralelas na máquina", "TRICEPS", "Dip_Machine", true, 1, "Tronco ereto para enfatizar o tríceps, cotovelos apontando para trás. Empurre até estender os braços.");
        add("triceps_testa_polia", "Tríceps testa na polia", "TRICEPS", "Cable_Lying_Triceps_Extension", false, 1, "Deitado no banco, cotovelos apontados para o teto e parados. Só os antebraços se movem.");
        add("triceps_barra_v", "Tríceps na polia com barra V", "TRICEPS", "Triceps_Pushdown_-_V-Bar_Attachment", false, 1, "Mesma execução da barra reta, com punhos em posição mais confortável.");
        // Quadriceps
        add("extensora", "Cadeira extensora", "QUADRICEPS", "Leg_Extensions", false, 1, "Encosto ajustado para o joelho alinhar com o eixo da máquina. Estenda até o fim, segure 1 segundo e desça em 3 segundos.");
        add("leg_press_45", "Leg press 45°", "QUADRICEPS", "Leg_Press", true, 2, "Pés na largura dos ombros. Desça só até onde o quadril continua colado no banco: se a lombar arredondar, o movimento foi longe demais. Não trave os joelhos em cima.");
        add("hack", "Agachamento hack na máquina", "QUADRICEPS", "Hack_Squat", true, 2, "Costas inteiras no encosto, abdômen contraído. Desça até as coxas ficarem paralelas à plataforma e suba empurrando pelo meio do pé.");
        add("extensora_unilateral", "Cadeira extensora unilateral", "QUADRICEPS", "Single-Leg_Leg_Extension", false, 1, "Uma perna por vez para corrigir diferenças entre os lados. Comece pela perna mais fraca.");
        add("leg_press_fechado", "Leg press 45° com pés fechados", "QUADRICEPS", "Narrow_Stance_Leg_Press", true, 2, "Pés próximos e mais baixos na plataforma para enfatizar o quadríceps. Mesma regra: quadril sempre colado no banco.");
        add("agachamento_smith", "Agachamento no Smith", "QUADRICEPS", "Smith_Machine_Squat", true, 3, "Carga direta sobre a coluna. Só entra no protocolo de quem não relata dor nas costas.");
        // Posteriores
        add("mesa_flexora", "Mesa flexora", "POSTERIORES", "Lying_Leg_Curls", false, 1, "Quadril colado no banco durante todo o movimento. Flexione até o apoio encostar perto dos glúteos e desça em 3 segundos.");
        add("cadeira_flexora", "Cadeira flexora", "POSTERIORES", "Seated_Leg_Curl", false, 1, "Trave bem as coxas. Flexione os joelhos por completo e controle a volta até quase estender.");
        add("flexora_em_pe", "Flexora em pé unilateral", "POSTERIORES", "Standing_Leg_Curl", false, 1, "Uma perna por vez, tronco apoiado. Suba o calcanhar em direção ao glúteo sem girar o quadril.");
        // Gluteos e quadril
        add("abdutora", "Cadeira abdutora", "GLUTEOS", "Thigh_Abductor", false, 1, "Tronco levemente inclinado à frente com a coluna reta. Abra as pernas ao máximo, segure 1 segundo e volte devagar.");
        add("elevacao_pelvica", "Elevação pélvica na máquina ou no Smith", "GLUTEOS", "Barbell_Hip_Thrust", true, 2, "Escápulas apoiadas no banco, queixo levemente recolhido. Suba o quadril contraindo os glúteos, sem hiperestender a lombar no topo.");
        add("gluteo_polia", "Extensão de quadril na polia", "GLUTEOS", "One-Legged_Cable_Kickback", false, 1, "Tornozeleira na polia baixa, tronco apoiado e firme. Leve a perna para trás apertando o glúteo, sem arquear a lombar.");
        add("adutora", "Cadeira adutora", "GLUTEOS", "Thigh_Adductor", false, 1, "Feche as pernas de forma controlada e volte devagar até sentir alongar a parte interna da coxa.");
        // Panturrilhas
        add("panturrilha_sentado", "Panturrilha sentado na máquina", "PANTURRILHAS", "Seated_Calf_Raise", false, 1, "Desça o calcanhar até alongar bem, pause 1 segundo embaixo e suba o máximo que conseguir.");
        add("panturrilha_leg", "Panturrilha no leg press", "PANTURRILHAS", "Calf_Press_On_The_Leg_Press_Machine", false, 1, "Só a ponta dos pés na plataforma, joelhos quase estendidos. Amplitude completa, sem quicar.");
        add("panturrilha_em_pe", "Panturrilha em pé na máquina", "PANTURRILHAS", "Standing_Calf_Raises", false, 2, "Joelhos estendidos, corpo alinhado. Suba na ponta dos pés e desça alongando.");
        // Core
        add("prancha", "Prancha ventral", "CORE", "Plank", false, 1, "Cotovelos sob os ombros, corpo em linha reta, glúteos e abdômen contraídos. Pare quando o quadril começar a cair.");
        add("dead_bug", "Dead bug", "CORE", "Dead_Bug", false, 1, "Deitado de costas, lombar encostada no chão. Estenda braço e perna opostos sem deixar a lombar descolar.");
        add("abdominal_maquina", "Abdominal na máquina", "CORE", "Ab_Crunch_Machine", false, 1, "Enrole o tronco aproximando as costelas do quadril. O movimento é do abdômen, não dos braços.");
        add("pallof", "Pallof press na polia", "CORE", "Pallof_Press", false, 1, "De lado para a polia, empurre o pegador à frente do peito e resista à rotação do tronco. Excelente para estabilizar a coluna.");
        add("abdominal_polia", "Abdominal na polia com corda", "CORE", "Cable_Crunch", false, 1, "Ajoelhado, corda ao lado da cabeça. Flexione a coluna levando os cotovelos em direção aos joelhos.");
    }

    private ExerciseCatalog() {}

    private static void add(String code, String name, String group, String img, boolean compound, int spine, String cue) {
        BY_CODE.put(code, new Exercise(code, name, group, img, compound, spine, cue));
    }

    public static Exercise get(String code) {
        Exercise e = BY_CODE.get(code);
        if (e == null) throw new IllegalArgumentException("Exercicio desconhecido: " + code);
        return e;
    }

    public static List<Exercise> all() { return new ArrayList<>(BY_CODE.values()); }

    public static List<Exercise> ofGroup(String group) {
        return BY_CODE.values().stream().filter(e -> e.group().equals(group)).toList();
    }

    public static String label(String group) { return GROUP_LABELS.getOrDefault(group, group); }
}
