package graveyard;

import graveyard.demo.DemoSeeder;
import graveyard.exception.CemeteryException;
import graveyard.model.*;
import graveyard.service.CemeteryService;
import java.util.Arrays;

/** Optional backend console harness. Main is the entry point for the integrated GUI. */
public final class BackendApp {
    private BackendApp() { }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Commands: init | demo | summary | plots | search [name or plot]");
            return;
        }
        try {
            if (!Arrays.asList("init", "demo", "summary", "plots", "search").contains(args[0])) {
                throw new IllegalArgumentException("Unknown command: " + args[0]);
            }
            var service = CemeteryService.openDefault();
            switch (args[0]) {
                case "init" -> System.out.println("Database is ready.");
                case "demo" -> {
                    DemoSeeder.seed(service);
                    System.out.println ("Added 100 plots and 15 fictional burials; 85 plots are available.");
                }
                case "summary" -> {
                    CemeterySummary summary = service.getSummary();
                    System.out.printf("Total: %d | Available: %d | Occupied: %d | Occupancy: %.1f%%%n",
                            summary.totalPlots(), summary.availablePlots(), summary.occupiedPlots(), summary.occupancyPercent());
                }
                case "plots" -> {
                    for (Plot plot : service.getPlots()) System.out.printf("%s | %s | %s%n",
                            plot.getPlotNumber(), plot.getSection(), plot.getStatus().getLabel());
                }
                case "search" -> {
                    String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    for (BurialRecord record : service.searchBurials(query)) {
                        Person person = record.getDeceased(); // Upcasting and dynamic dispatch.
                        Long id = record.getRecordId(); // Primitive long -> wrapper Long (autoboxing).
                        long numericId = id; // Unboxing.
                        System.out.printf("%d | %s | Burial: %s | Plot: %s%n", numericId,
                                person.getSummary(), record.getBurialDate(), record.getPlotNumber());
                    }
                }
                default -> throw new IllegalArgumentException("Unknown command.");
            }
        } catch (CemeteryException | IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            if (Boolean.getBoolean("graveyard.debug")) ex.printStackTrace();
            System.exit(1);
        }
    }
}
