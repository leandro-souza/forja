package br.com.forja.core;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de nutricao: calcula metas e monta o cardapio diario com alimentos comuns no Nordeste,
 * todos sem lactose. As porcoes sao ajustadas para bater as metas de proteina, carboidrato e gordura.
 * Valores nutricionais aproximados, por 100 g de alimento pronto para consumo (base: tabela TACO).
 */
public final class Nutrition {
    private Nutrition() {}

    public record Food(String code, String name, double kcal, double p, double c, double f, String unit, double unitG, String section) {}

    public record Input(String sex, int age, double heightCm, double weightKg, String jobActivity, int trainingDays,
                        String trainingStart, String trainingEnd, int kcalAdjust, boolean lactoseIntolerant, boolean hasMedicalFlags) {}

    public record Targets(int bmr, int maintenance, int kcal, int protein, int carbs, int fat, double waterL, List<String> notes) {}

    public record Line(String food, String name, double grams, String qty, int kcal, double p, double c, double f) {}

    public record Meal(String time, String name, String option, List<Line> items, int kcal, int p, int c, int f, String note) {}

    public record DayMenu(LocalDate date, List<Meal> meals, int kcal, int p, int c, int f, Targets targets) {}

    public record Supplement(String name, String dose, String when, String why, String level) {}

    public record ShoppingItem(String name, String qty, String section) {}

    private static final Map<String, Food> FOODS = new LinkedHashMap<>();

    private static void food(String code, String name, double kcal, double p, double c, double f, String unit, double unitG, String section) {
        FOODS.put(code, new Food(code, name, kcal, p, c, f, unit, unitG, section));
    }

    static {
        food("cuscuz", "Cuscuz de milho", 113, 2.2, 25.3, 0.7, "fatia média", 130, "Mercearia");
        food("tapioca", "Tapioca (goma hidratada)", 240, 0, 59, 0, "colher de sopa", 20, "Mercearia");
        food("arroz", "Arroz branco cozido", 128, 2.5, 28.1, 0.2, "colher de servir", 45, "Mercearia");
        food("feijao", "Feijão carioca cozido", 76, 4.8, 13.6, 0.5, "concha média", 100, "Mercearia");
        food("feijao_corda", "Feijão de corda cozido", 78, 5.1, 13.5, 0.6, "concha média", 100, "Mercearia");
        food("macaxeira", "Macaxeira cozida", 125, 0.6, 30.1, 0.3, "pedaço médio", 100, "Feira");
        food("inhame", "Inhame cozido", 116, 1.5, 27.0, 0.1, "rodela grossa", 60, "Feira");
        food("batata_doce", "Batata-doce cozida", 77, 0.6, 18.4, 0.1, "unidade pequena", 120, "Feira");
        food("banana_terra", "Banana-da-terra cozida", 128, 1.4, 33.7, 0.2, "unidade", 150, "Feira");
        food("pao", "Pão francês", 300, 8.0, 58.6, 3.1, "unidade", 50, "Padaria");
        food("aveia", "Aveia em flocos", 394, 13.9, 66.6, 8.5, "colher de sopa", 15, "Mercearia");
        food("banana", "Banana-prata", 98, 1.3, 26.0, 0.1, "unidade", 90, "Feira");
        food("mamao", "Mamão", 45, 0.8, 11.6, 0.1, "fatia", 150, "Feira");
        food("manga", "Manga", 64, 0.5, 16.0, 0.2, "unidade pequena", 200, "Feira");
        food("goiaba", "Goiaba", 54, 1.1, 13.0, 0.4, "unidade", 150, "Feira");
        food("abacaxi", "Abacaxi", 48, 0.9, 12.3, 0.1, "fatia", 100, "Feira");
        food("laranja", "Laranja", 46, 1.0, 11.5, 0.1, "unidade", 150, "Feira");
        food("melancia", "Melancia", 33, 0.9, 8.1, 0.0, "fatia", 200, "Feira");
        food("ovo", "Ovo de galinha", 146, 13.3, 0.6, 9.5, "unidade", 50, "Açougue e ovos");
        food("frango", "Peito de frango grelhado ou desfiado", 159, 32.0, 0, 2.5, "filé médio", 120, "Açougue e ovos");
        food("carne_sol", "Carne de sol magra dessalgada", 210, 33.0, 0, 8.0, "pedaço médio", 100, "Açougue e ovos");
        food("patinho", "Carne bovina magra (patinho ou coxão mole)", 219, 35.9, 0, 7.3, "bife médio", 100, "Açougue e ovos");
        food("tilapia", "Filé de tilápia grelhado", 128, 26.0, 0, 2.7, "filé", 120, "Açougue e ovos");
        food("atum", "Atum em água (lata)", 110, 25.0, 0, 1.0, "lata drenada", 120, "Mercearia");
        food("whey", "Whey protein isolado zero lactose", 370, 87.0, 3.0, 1.0, "dosador", 30, "Suplementos");
        food("leite", "Leite zero lactose semidesnatado", 43, 3.2, 4.8, 1.2, "copo", 200, "Laticínios zero lactose");
        food("iogurte", "Iogurte natural zero lactose", 55, 4.0, 6.0, 2.0, "pote", 170, "Laticínios zero lactose");
        food("pasta_amendoim", "Pasta de amendoim integral", 590, 25.0, 20.0, 50.0, "colher de sopa", 15, "Mercearia");
        food("castanha", "Castanha de caju", 570, 18.5, 29.1, 46.3, "punhado", 25, "Mercearia");
        food("azeite", "Azeite de oliva", 884, 0, 0, 100, "colher de sopa", 10, "Mercearia");
        food("abacate", "Abacate", 96, 1.2, 6.0, 8.4, "colher de sopa", 30, "Feira");
        food("mel", "Mel", 309, 0, 84.0, 0, "colher de sopa", 20, "Mercearia");
        food("rapadura", "Rapadura", 352, 1.0, 90.8, 0.1, "pedaço pequeno", 20, "Mercearia");
        food("salada", "Salada crua (alface, tomate, pepino, cebola)", 18, 1.0, 3.5, 0.2, "prato de sobremesa", 100, "Feira");
        food("legumes", "Legumes cozidos (abóbora, quiabo, maxixe, chuchu, cenoura)", 35, 1.2, 7.5, 0.3, "colher de servir", 50, "Feira");
    }

    private record It(String food, double grams, char role) {}

    private record Option(String name, List<It> items) {}

    private record Slot(String time, String name, List<Option> options, String note) {}

    private static It it(String food, double grams, char role) { return new It(food, grams, role); }

    private static Option opt(String name, It... items) { return new Option(name, List.of(items)); }

    // Papeis: P = fonte de proteina, C = fonte de carboidrato, F = fonte de gordura, X = porcao fixa
    private static final List<Option> PRE = List.of(
            opt("Tapioca com ovos e banana", it("tapioca", 80, 'C'), it("ovo", 100, 'P'), it("banana", 90, 'X'), it("mel", 10, 'X')),
            opt("Cuscuz com ovos e mamão", it("cuscuz", 200, 'C'), it("ovo", 100, 'P'), it("mamao", 150, 'X')),
            opt("Pão com ovos e rapadura", it("pao", 100, 'C'), it("ovo", 100, 'P'), it("banana", 90, 'X'), it("rapadura", 20, 'X')));
    private static final List<Option> POS = List.of(
            opt("Vitamina de banana com whey e aveia", it("whey", 35, 'P'), it("leite", 300, 'X'), it("banana", 180, 'C'), it("aveia", 40, 'C'), it("pasta_amendoim", 15, 'F')),
            opt("Cuscuz com frango desfiado e laranja", it("cuscuz", 260, 'C'), it("frango", 120, 'P'), it("laranja", 300, 'X'), it("azeite", 5, 'F')),
            opt("Macaxeira com carne de sol e mamão", it("macaxeira", 250, 'C'), it("carne_sol", 110, 'P'), it("mamao", 150, 'X')),
            opt("Whey com tapioca e manga", it("whey", 35, 'P'), it("tapioca", 100, 'C'), it("manga", 200, 'X'), it("castanha", 20, 'F')));
    private static final List<Option> ALMOCO = List.of(
            opt("Arroz, feijão e frango grelhado", it("arroz", 200, 'C'), it("feijao", 150, 'X'), it("frango", 170, 'P'), it("salada", 100, 'X'), it("legumes", 100, 'X'), it("azeite", 10, 'F'), it("abacaxi", 100, 'X')),
            opt("Baião de dois com carne de sol", it("arroz", 180, 'C'), it("feijao_corda", 150, 'X'), it("carne_sol", 150, 'P'), it("macaxeira", 100, 'C'), it("salada", 100, 'X'), it("melancia", 200, 'X')),
            opt("Arroz, feijão e tilápia", it("arroz", 200, 'C'), it("feijao", 150, 'X'), it("tilapia", 200, 'P'), it("legumes", 150, 'X'), it("azeite", 10, 'F'), it("laranja", 150, 'X')),
            opt("Arroz, feijão de corda e carne magra", it("arroz", 200, 'C'), it("feijao_corda", 150, 'X'), it("patinho", 150, 'P'), it("salada", 100, 'X'), it("legumes", 100, 'X'), it("azeite", 5, 'F'), it("goiaba", 150, 'X')));
    private static final List<Option> LANCHE = List.of(
            opt("Pão com atum e abacate", it("pao", 100, 'C'), it("atum", 120, 'P'), it("abacate", 60, 'F'), it("goiaba", 150, 'X')),
            opt("Iogurte com aveia, banana e castanha", it("iogurte", 340, 'X'), it("whey", 20, 'P'), it("aveia", 40, 'C'), it("banana", 90, 'C'), it("castanha", 25, 'F')),
            opt("Tapioca com frango e manga", it("tapioca", 80, 'C'), it("frango", 100, 'P'), it("manga", 200, 'X'), it("castanha", 15, 'F')),
            opt("Cuscuz com ovos e mamão", it("cuscuz", 200, 'C'), it("ovo", 150, 'P'), it("mamao", 150, 'X')));
    private static final List<Option> JANTAR = List.of(
            opt("Inhame com frango e legumes", it("inhame", 300, 'C'), it("frango", 170, 'P'), it("legumes", 150, 'X'), it("azeite", 10, 'F')),
            opt("Batata-doce com tilápia e salada", it("batata_doce", 300, 'C'), it("tilapia", 200, 'P'), it("salada", 100, 'X'), it("azeite", 10, 'F')),
            opt("Cuscuz com carne de sol e abóbora", it("cuscuz", 260, 'C'), it("carne_sol", 140, 'P'), it("legumes", 150, 'X')),
            opt("Banana-da-terra com ovos e frango", it("banana_terra", 200, 'C'), it("ovo", 100, 'P'), it("frango", 100, 'P'), it("salada", 100, 'X')),
            opt("Macaxeira com carne magra e salada", it("macaxeira", 250, 'C'), it("patinho", 150, 'P'), it("salada", 100, 'X'), it("azeite", 5, 'F')));
    private static final List<Option> CEIA = List.of(
            opt("Iogurte com aveia e pasta de amendoim", it("iogurte", 170, 'X'), it("aveia", 30, 'C'), it("pasta_amendoim", 20, 'F')),
            opt("Ovos mexidos com pão", it("ovo", 100, 'P'), it("pao", 50, 'C'), it("abacate", 45, 'F')),
            opt("Leite com aveia, banana e castanha", it("leite", 300, 'X'), it("aveia", 30, 'C'), it("banana", 90, 'X'), it("castanha", 20, 'F')));

    public static Targets targets(Input in) {
        double bmr = 10 * in.weightKg() + 6.25 * in.heightCm() - 5 * in.age() + ("F".equals(in.sex()) ? -161 : 5);
        double factor = switch (in.jobActivity() == null ? "SEDENTARIO" : in.jobActivity()) {
            case "ATIVO" -> 1.8;
            case "MODERADO" -> 1.7;
            default -> 1.55;
        };
        if (in.trainingDays() <= 3) factor -= 0.15;
        int maintenance = (int) Math.round(bmr * factor / 10) * 10;
        int kcal = (int) Math.round(maintenance * 1.10 / 50) * 50 + in.kcalAdjust();
        int protein = (int) Math.round(in.weightKg() * 2.0);
        int fat = (int) Math.round(in.weightKg() * 0.9);
        int carbs = Math.max(150, (kcal - protein * 4 - fat * 9) / 4);
        double water = Math.round(in.weightKg() * 0.04 * 10) / 10.0;
        List<String> notes = new ArrayList<>();
        notes.add("Superávit de cerca de 10% sobre a manutenção: o suficiente para ganhar massa magra de forma rápida sem acumular gordura em excesso.");
        notes.add("Meta de ganho: 0,25% a 0,5% do peso por semana (" + String.format("%.0f a %.0f g", in.weightKg() * 2.5, in.weightKg() * 5) + "). Acima disso o excedente tende a virar gordura.");
        notes.add("Proteína em 2 g por kg de peso, distribuída em 5 a 6 refeições.");
        if (in.kcalAdjust() != 0) notes.add("Ajuste automático de " + (in.kcalAdjust() > 0 ? "+" : "") + in.kcalAdjust() + " kcal aplicado com base na sua evolução de peso.");
        if (in.lactoseIntolerant()) notes.add("Cardápio sem lactose: leite e iogurte sempre na versão zero lactose, e whey na versão isolada.");
        return new Targets((int) Math.round(bmr), maintenance, kcal, protein, carbs, fat, water, notes);
    }

    public static DayMenu menu(LocalDate date, Input in) {
        Targets t = targets(in);
        int seed = (int) date.toEpochDay();
        boolean morning = LocalTime.parse(in.trainingStart()).isBefore(LocalTime.of(10, 0));
        String pre = LocalTime.parse(in.trainingStart()).minusMinutes(45).toString();
        String pos = LocalTime.parse(in.trainingEnd()).plusMinutes(30).toString();
        List<Slot> slots = morning ? List.of(
                new Slot(pre, "Pré-treino", PRE, "Refeição leve e de digestão rápida, 45 minutos antes do treino."),
                new Slot(pos, "Pós-treino", POS, "Até 1 hora depois do treino. É o café da manhã reforçado do dia."),
                new Slot("12:30", "Almoço", ALMOCO, null),
                new Slot("16:00", "Lanche da tarde", LANCHE, null),
                new Slot("19:30", "Jantar", JANTAR, null),
                new Slot("21:30", "Ceia", CEIA, "Proteína e gordura boa antes de dormir ajudam na recuperação."))
                : List.of(
                new Slot("07:00", "Café da manhã", POS, null),
                new Slot("10:00", "Lanche da manhã", CEIA, null),
                new Slot("12:30", "Almoço", ALMOCO, null),
                new Slot(pre, "Pré-treino", PRE, "Refeição leve, 45 minutos antes do treino."),
                new Slot(pos, "Pós-treino", JANTAR, "Até 1 hora depois do treino."),
                new Slot("21:30", "Ceia", LANCHE, null));

        List<Option> chosen = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            List<Option> o = slots.get(i).options();
            chosen.add(o.get(Math.floorMod(seed + i * 2, o.size())));
        }
        // Escalas por papel para bater as metas do dia
        double pFix = 0, pVar = 0, cFix = 0, cVar = 0, fFix = 0, fVar = 0;
        for (Option o : chosen) for (It i : o.items()) {
            Food fd = FOODS.get(i.food());
            if (i.role() == 'P') pVar += fd.p() * i.grams() / 100; else pFix += fd.p() * i.grams() / 100;
        }
        double ps = clamp((t.protein() - pFix) / pVar, 0.6, 1.8);
        for (Option o : chosen) for (It i : o.items()) {
            Food fd = FOODS.get(i.food());
            double g = i.grams() * (i.role() == 'P' ? ps : 1);
            if (i.role() == 'C') cVar += fd.c() * g / 100; else cFix += fd.c() * g / 100;
        }
        double cs = clamp((t.carbs() - cFix) / cVar, 0.4, 2.6);
        for (Option o : chosen) for (It i : o.items()) {
            Food fd = FOODS.get(i.food());
            double g = i.grams() * (i.role() == 'P' ? ps : i.role() == 'C' ? cs : 1);
            if (i.role() == 'F') fVar += fd.f() * g / 100; else fFix += fd.f() * g / 100;
        }
        double fs = fVar > 0 ? clamp((t.fat() - fFix) / fVar, 0.3, 2.5) : 1;

        DayMenu first = build(date, t, slots, chosen, ps, cs, fs);
        // Segunda passada: fecha a diferenca de calorias que sobra apos o arredondamento das porcoes
        double gap = t.kcal() - first.kcal();
        if (Math.abs(gap) < 60 || cVar <= 0) return first;
        double cs2 = clamp(cs * (1 + gap / 4 / (cVar * cs)), 0.4, 2.6);
        return build(date, t, slots, chosen, ps, cs2, fs);
    }

    private static DayMenu build(LocalDate date, Targets t, List<Slot> slots, List<Option> chosen, double ps, double cs, double fs) {
        List<Meal> meals = new ArrayList<>();
        double tk = 0, tp = 0, tc = 0, tf = 0;
        for (int m = 0; m < slots.size(); m++) {
            List<Line> lines = new ArrayList<>();
            double mk = 0, mp = 0, mc = 0, mf = 0;
            for (It i : chosen.get(m).items()) {
                Food fd = FOODS.get(i.food());
                double g = roundPortion(fd, i.grams() * switch (i.role()) { case 'P' -> ps; case 'C' -> cs; case 'F' -> fs; default -> 1; });
                double k = fd.kcal() * g / 100, p = fd.p() * g / 100, c = fd.c() * g / 100, f = fd.f() * g / 100;
                lines.add(new Line(fd.code(), fd.name(), g, qty(fd, g), (int) Math.round(k), r1(p), r1(c), r1(f)));
                mk += k; mp += p; mc += c; mf += f;
            }
            meals.add(new Meal(slots.get(m).time(), slots.get(m).name(), chosen.get(m).name(), lines,
                    (int) Math.round(mk), (int) Math.round(mp), (int) Math.round(mc), (int) Math.round(mf), slots.get(m).note()));
            tk += mk; tp += mp; tc += mc; tf += mf;
        }
        return new DayMenu(date, meals, (int) Math.round(tk), (int) Math.round(tp), (int) Math.round(tc), (int) Math.round(tf), t);
    }

    /** Lista de compras para os proximos 7 dias de cardapio. */
    public static List<ShoppingItem> shopping(LocalDate from, Input in) {
        Map<String, Double> grams = new LinkedHashMap<>();
        for (int d = 0; d < 7; d++) for (Meal m : menu(from.plusDays(d), in).meals()) for (Line l : m.items()) grams.merge(l.food(), l.grams(), Double::sum);
        List<ShoppingItem> out = new ArrayList<>();
        for (Food fd : FOODS.values()) {
            Double g = grams.get(fd.code());
            if (g == null) continue;
            String qty = switch (fd.code()) {
                case "ovo" -> Math.round(g / 50) + " unidades";
                case "pao", "banana", "banana_terra", "laranja", "goiaba", "manga" -> Math.round(g / fd.unitG()) + " unidades";
                case "atum" -> Math.round(g / 120) + " latas";
                case "leite" -> String.format("%.1f L", g / 1000).replace('.', ',');
                default -> g >= 1000 ? String.format("%.1f kg", g / 1000).replace('.', ',') : Math.round(g / 10) * 10 + " g";
            };
            String suffix = switch (fd.code()) {
                case "arroz" -> " (cerca de " + Math.round(g / 2.6 / 10) * 10 + " g cru)";
                case "feijao", "feijao_corda" -> " (cerca de " + Math.round(g / 2.5 / 10) * 10 + " g cru)";
                case "cuscuz" -> " (cerca de " + Math.round(g / 2.3 / 10) * 10 + " g de flocão)";
                case "frango", "patinho", "tilapia", "carne_sol" -> " (cerca de " + String.format("%.1f kg", g / 0.72 / 1000).replace('.', ',') + " cru)";
                default -> "";
            };
            out.add(new ShoppingItem(fd.name(), qty + suffix, fd.section()));
        }
        return out;
    }

    public static List<Supplement> supplements(Input in) {
        List<Supplement> s = new ArrayList<>();
        s.add(new Supplement("Creatina monohidratada", "5 g por dia", "Todos os dias, em qualquer horário. Pode ir na vitamina do pós-treino.",
                "O suplemento com mais evidência para ganho de força e massa magra. Não precisa de fase de saturação nem de pausas. Beba bastante água.", "Essencial"));
        s.add(new Supplement("Whey protein isolado" + (in.lactoseIntolerant() ? " (zero lactose)" : ""), "30 a 35 g",
                "No pós-treino e, se faltar proteína no dia, no lanche da tarde.",
                "Praticidade para fechar a meta de proteína. O isolado tem lactose praticamente zero; se ainda assim incomodar, use proteína de carne, de arroz ou de ervilha.", "Essencial"));
        int caffeine = (int) Math.round(in.weightKg() * 3 / 50) * 50;
        s.add(new Supplement("Cafeína", caffeine + " mg (ou 2 xícaras de café forte)", "30 a 45 minutos antes do treino. Evite depois das 14h para não prejudicar o sono.",
                "Aumenta disposição e desempenho nas séries pesadas." + (in.hasMedicalFlags() ? " Como você informou medicação ou condição de saúde, confirme com seu médico antes de usar." : ""), "Recomendado"));
        s.add(new Supplement("Ômega-3", "1 a 2 g de EPA + DHA por dia", "Junto ao almoço ou jantar.",
                "Apoio à saúde cardiovascular e articular. Dispensável se você come peixe gordo, como sardinha, duas ou mais vezes por semana.", "Opcional"));
        s.add(new Supplement("Vitamina D3", "Conforme exame de sangue", "Junto a uma refeição com gordura.",
                "Só vale suplementar se o exame de 25-OH vitamina D indicar nível baixo. Peça ao médico no próximo check-up.", "Sob exame"));
        s.add(new Supplement("Hipercalórico ou maltodextrina", "Só se necessário", "No pós-treino.",
                "Use apenas se não conseguir comer todo o cardápio. Comida de verdade vem primeiro. Muitos hipercalóricos têm lactose: confira o rótulo.", "Situacional"));
        return s;
    }

    private static double roundPortion(Food fd, double g) {
        return switch (fd.code()) {
            case "whey" -> Math.max(1, Math.round(g / 15)) * 15.0;
            case "ovo", "pao" -> Math.max(1, Math.round(g / fd.unitG())) * fd.unitG();
            case "banana", "banana_terra", "laranja", "goiaba" -> Math.max(1, Math.round(g / fd.unitG())) * fd.unitG();
            default -> g > 60 ? Math.round(g / 10) * 10.0 : Math.max(5, Math.round(g / 5) * 5.0);
        };
    }

    private static String qty(Food fd, double g) {
        double units = g / fd.unitG();
        String u = Math.abs(units - Math.round(units)) < 0.15 ? String.valueOf(Math.round(units)) : String.format("%.1f", units).replace('.', ',');
        boolean liquid = fd.code().equals("leite");
        return Math.round(g) + (liquid ? " ml" : " g") + " (" + u + " " + (units >= 1.5 ? plural(fd.unit()) : fd.unit()) + ")";
    }

    /** Plural simples da medida caseira: flexiona as palavras antes de " de ". */
    private static String plural(String unit) {
        int cut = unit.indexOf(" de ");
        String head = cut < 0 ? unit : unit.substring(0, cut), tail = cut < 0 ? "" : unit.substring(cut);
        StringBuilder sb = new StringBuilder();
        for (String w : head.split(" ")) sb.append(sb.isEmpty() ? "" : " ").append(w.endsWith("r") ? w + "es" : w + "s");
        return sb + tail;
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private static double r1(double v) { return Math.round(v * 10) / 10.0; }
}
