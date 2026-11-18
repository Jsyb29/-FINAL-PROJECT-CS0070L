package graveyard;

import graveyard.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point of the Simple Graveyard System.
 * The window opens immediately; MainFrame opens the database on a background thread.
 */
public class Main {

    public static void main(String[] args) {
    graveyard.service.CemeteryService service =
            graveyard.service.CemeteryService.openDefault();

    if (service.getSummary().totalPlots() == 0) {
        graveyard.demo.DemoSeeder.seed(service);
    }

    try {
        UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());
    } catch (Exception ignored) {
        // Use the default look
    }

    SwingUtilities.invokeLater(
            () -> new MainFrame().setVisible(true));
}
}
