import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;

public class PopupMenu extends JPopupMenu {
    public ArrayList<JCheckBoxMenuItemUsage> usageOptions = new ArrayList<JCheckBoxMenuItemUsage>();

    public PopupMenu() {
        JMenuItem update = new JMenuItem("Update");
        update.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Main.update = true;
            }
        });
        add(update);

        JMenuItem close = new JMenuItem("Close");
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                System.exit(0);
            }
        });
        add(close);

        if (Main.isAlwaysOnTopSupported()) {
            JCheckBoxMenuItem displayOver = new JCheckBoxMenuItem("Always On Top");
            displayOver.setSelected(Main.isAlwaysOnTop());
            displayOver.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Main.setAlwaysOnTop(!Main.isAlwaysOnTop());

                    Main.settings.isAlwaysOnTop = Main.isAlwaysOnTop();
                    Main.settings.save();
                }
            });
            add(displayOver);
        }

        addSeparator();

        JCheckBoxMenuItem oneMinute = new JCheckBoxMenuItem("Update every minute");
        oneMinute.setSelected(Main.settings.secondsToUpdate == 60);
        oneMinute.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Main.settings.secondsToUpdate = 60;
            }
        });
        add(oneMinute);

        JCheckBoxMenuItem fiveMinute = new JCheckBoxMenuItem("Update every 5 minutes");
        fiveMinute.setSelected(Main.settings.secondsToUpdate == 300);
        fiveMinute.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Main.settings.secondsToUpdate = 300;
            }
        });
        add(fiveMinute);

        addSeparator();

        JMenuItem displayOver = new JMenuItem("Rename Range");
        displayOver.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                new TextboxPopup(Main.curUsage.ipRange);
            }
        });
        add(displayOver);

        addSeparator();

        add(new JLabel("Available Ranges")); // Header

        ArrayList<Usage> sortedQuotas = new ArrayList<Usage>(Main.quotas);
        sortedQuotas.sort(new Comparator<Usage>() {
            @Override
            public int compare(Usage o1, Usage o2) {
                return (o1.ipRange.end.equals(o2.ipRange.start) ? 0
                        : (o1.ipRange.end.lessThan(o2.ipRange.start) ? -1 : 1));
            }

        });

        for (Usage usage : sortedQuotas) {
            JCheckBoxMenuItemUsage usageOption = new JCheckBoxMenuItemUsage(usage);
            usageOption.setSelected(Main.curUsage == usage);
            usageOption.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Main.curUsage = usageOption.usage;
                    Main.updatePercentage();
                }
            });

            usageOptions.add(usageOption);

            add(usageOption);
        }
    }

    public void updateText() {
        for (JCheckBoxMenuItemUsage usageOption : usageOptions) {
            usageOption.updateText();

            boolean isSelected = Main.curUsage == usageOption.usage;

            if (isSelected && !usageOption.isSelected())
                usageOption.setSelected(true);
            else if (!isSelected && usageOption.isSelected())
                usageOption.setSelected(false);
        }
    }
}
