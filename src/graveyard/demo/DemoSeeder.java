package graveyard.demo;

import graveyard.model.*;
import graveyard.service.CemeteryService;
import java.time.LocalDate;
import java.util.*;

/** Optional fictional demo data. Nothing is inserted automatically at normal startup. */
public final class DemoSeeder {
    public static final int TOTAL_PLOTS = 100;
    public static final int DEMO_BURIALS = 15;
    private DemoSeeder() { }

    public static void seed(CemeteryService service) {
        String[] sections = {"A", "B", "C", "D"};
        String[] firstNames = {"Arturo", "Beatriz", "Carlos", "Diana", "Eduardo", "Felisa",
                "Gabriel", "Helena", "Isabel", "Joaquin", "Lourdes", "Manuel", "Nora", "Oscar",
                "Pilar", "Rafael", "Sofia", "Tomas", "Vicente", "Yolanda"};
        String[] lastNames = {"Santos", "Reyes", "Cruz", "Garcia", "Mendoza", "Ramos",
                "Flores", "Torres", "Rivera", "Castillo", "Navarro", "Aquino", "Bautista",
                "Del Rosario", "Villanueva", "Salazar", "Mercado", "Estrada", "Valdez", "Rosales"};
        List<Plot> plots = new ArrayList<>();
        for (String section : sections) {
            for (int i = 1; i <= TOTAL_PLOTS / sections.length; i++) {
                plots.add(new Plot(String.format(Locale.ROOT, "%s-%03d", section, i),
                        "Section " + section, PlotStatus.AVAILABLE));
            }
        }
        List<Plot> shuffled = new ArrayList<>(plots);
        Random random = new Random(20261114L);
        Collections.shuffle(shuffled, random);
        Queue<Plot> pending = new LinkedList<>(shuffled.subList(0, DEMO_BURIALS));
        List<BurialRequest> burials = new ArrayList<>();
        do {
            Plot plot = pending.remove();
            int i = burials.size();
            String name = firstNames[i % firstNames.length] + " " + lastNames[i / firstNames.length];
            LocalDate birth = LocalDate.of(1930 + random.nextInt(40), 1 + random.nextInt(12), 1 + random.nextInt(28));
            LocalDate death = LocalDate.of(2010 + random.nextInt(14), 1 + random.nextInt(12), 1 + random.nextInt(28));
            burials.add(new BurialRequest(name, birth, death, death.plusDays(3), plot.getPlotNumber()));
        } while (!pending.isEmpty());
        service.seedDemo(plots, burials);
    }
}
