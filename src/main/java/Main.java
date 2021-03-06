import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

public class Main {
    private static JFrame frame;
    private static JLabel text;
    private static JProgressBar bar;
    private static JLabel bText;

    private static final String IP_RANGE_REGEX = ".*(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})-(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3}).*";

    private static final int HEIGHT = 70; // + OsUtil.getOSType().getDisplayOffset()
    private static final int WIDTH = 200;
    private static final long CHECK_RESOLUTION = 250;

    private static final int START_DAY = 1; // The day that all data resets. 0
    // is sunday, 6 is saturday

    public static boolean update = false;

    public static Settings settings = Settings.load();

    private static ScreenSnapper snapper;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        MouseAdapter mouseListener = new MouseAdapter() {
            private int lastX = Integer.MIN_VALUE;
            private int lastY = Integer.MIN_VALUE;
            // private static boolean drag = true;
            private int grappleX = Integer.MIN_VALUE;
            private int grappleY = Integer.MIN_VALUE;

            @Override
            public void mousePressed(MouseEvent e) {
                doPop(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastX = Integer.MIN_VALUE;
                lastY = Integer.MIN_VALUE;
                grappleX = Integer.MIN_VALUE;
                grappleY = Integer.MIN_VALUE;

                snapper.snapToScreen();

                settings.frameLocation = frame.getLocationOnScreen();
                settings.save();

                doPop(e);
            }

            private void doPop(MouseEvent e) {
                // System.out.println(e.isPopupTrigger());
                if (e.isPopupTrigger()) {
                    // drag = false;
                    PopupMenu menu = new PopupMenu();
                    menu.show(e.getComponent(), e.getX(), e.getY());

                    new Thread(() -> {
                        while (menu.isVisible()) {
                            try {
                                Thread.sleep(CHECK_RESOLUTION);
                                menu.updateText();

                                //System.out.println("Updating menu...");
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }

                        //System.out.println("Exiting menu updater...");
                    }).start();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // Prints modifier (ex. 4 = Right Click, 8 = Scroll Wheel, 16 = Left Click)
                // System.out.println(MouseEvent.BUTTON3_DOWN_MASK + " | " + e.getModifiersEx());

                // For dragging the window around the screen
                if (e.getModifiersEx() != MouseEvent.BUTTON3_DOWN_MASK) {
                    int currentX = e.getXOnScreen();
                    int currentY = e.getYOnScreen();

                    if (lastX == Integer.MIN_VALUE) {
                        lastX = currentX;
                        lastY = currentY;
                    }

                    if (grappleX == Integer.MIN_VALUE) {
                        grappleX = e.getX() + (e.getComponent() instanceof JFrame ? 0 : e.getComponent().getX());
                        grappleY = e.getY() + (e.getComponent() instanceof JFrame ? 0 : e.getComponent().getY());
                    }

                    int dX = (currentX - lastX);
                    int dY = (currentY - lastY);

                    int xOff = currentX - dX - grappleX;
                    int yOff = currentY - dY - grappleY;

                    frame.setLocation(xOff, yOff);

                    lastX = currentX;
                    lastY = currentY;
                }
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
                bar.setToolTipText("Auto updates every " + settings.secondsToUpdate + " second(s)");
            }
        };

        frame = new JFrame("Internet Usage");
        frame.addMouseMotionListener(mouseListener);
        frame.addMouseListener(mouseListener);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WIDTH, HEIGHT);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null); // Starts window in centre of screen
        frame.getContentPane().setLayout(null);
        frame.setUndecorated(true);

        snapper = new ScreenSnapper(frame, 22); // Set up the screen snapper

        if (settings != null) {
            settings.setMissing();

            setAlwaysOnTop(settings.isAlwaysOnTop);
            frame.setLocation(settings.frameLocation);
        } else {
            frame.setVisible(true);
            settings = new Settings(isAlwaysOnTop(), 60, frame.getLocationOnScreen());
            frame.setVisible(false);
            settings.save();
        }

        text = new JLabel();
        text.addMouseMotionListener(mouseListener);
        text.addMouseListener(mouseListener);
        frame.getContentPane().add(text);

        bText = new JLabel();
        bText.addMouseMotionListener(mouseListener);
        bText.addMouseListener(mouseListener);
        frame.getContentPane().add(bText);

        bar = new JProgressBar();
        bar.setBounds(16, 16, WIDTH - 32, 20);
        bar.setToolTipText("Auto updates every " + settings.secondsToUpdate + " second(s)");
        bar.addMouseMotionListener(mouseListener);
        bar.addMouseListener(mouseListener);
        frame.getContentPane().add(bar);

        frame.setVisible(true);

        while (true) {
            update = false;
            updateBarText("<html><font color=\"#4191E1\">" + bText.getText() + "</font></html>");

            try {
                URL url = new URL("http://192.168.1.1/");
                InputStream is = url.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                lastQuotas.clear();
                lastQuotas = new ArrayList<Usage>(quotas);
                quotas.clear();

                long used = -1;
                long max = -1;
                IPAddress ip = null;
                ArrayList<IPRange> ranges = new ArrayList<IPRange>();
                String line;
                while ((line = br.readLine()) != null) {
                    if (ip != null) { // finds max and used

                        if (line.matches(IP_RANGE_REGEX)) {
                            //System.out.println(line);

                            if (line.split("\\[").length == 4) {
                                String ipRange = line.split("\\[")[2].split("]")[0].replaceAll("\"", "").trim();
                                String[] ipRanges = ipRange.split(",");

                                for (String ipRangeEntry : ipRanges) {
                                    IPRange range = new IPRange(ipRangeEntry);
                                    if (!rangesContains(ranges, range)) {
                                        ranges.add(range);
                                    }
                                    // System.out.println(ipRange);
                                }
                            }

                            if (line.startsWith("quotaUsed[") && line.split("\\[").length == 4) {
                                used = parseUsageValues(line);

                            } else if (line.startsWith("quotaLimits[") && line.split("\\[").length == 4) {
                                max = parseUsageValues(line);
                            }

                            // System.out.println(line);

                            if (used != -1 && max != -1 && !ranges.isEmpty()) {
                                for (IPRange range : ranges) {
                                    // System.out.println(range);

                                    Usage usage = Usage.createUsage(range, used, max, findUsage(range, lastQuotas));
                                    quotas.add(usage);

                                    if (range.isIPWithinRange(ip)) {
                                        mainUsage = usage;
                                    }
                                }

                                used = -1;
                                max = -1;
                                ranges.clear();
                            }
                        }

                        // Finds the IP
                    } else if (line.contains("var connectedIp = ")) {
                        String[] split1 = line.split("\"");
                        ip = new IPAddress(split1[1]);

                        //System.out.println("Connected IP: " + ip);
                    }
                }

                // Calculate total speed
                long totalSpeed = 0;
                for (Usage usage : quotas) {
                    totalSpeed += usage.downloadSpeed;
                }

                // If speed is over 0, calculate percentages
                if (totalSpeed > 0) {
                    for (Usage usage : quotas) {
                        usage.percentOfTotalSpeed = percentWithPrecision(Math.round(usage.downloadSpeed), totalSpeed, 100);
                    }
                }

                // Set the current usage from the previous one, if there is no previous one, use
                // your own
                curUsage = findUsage(curUsage);
                curUsage = (curUsage == null ? mainUsage : curUsage);

                updatePercentage();

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!hasWarnedStopped && mainUsage.downloadUsed >= mainUsage.downloadTotal) {
                hasWarnedStopped = true;

                new Popup("You have reached your maximum", "data usage of "
                        + percentWithPrecision(mainUsage.downloadTotal, BYTE_IN_A_GIG, 1) + "G" + " for this week.");
            }

            if (!hasWarnedOver && !hasWarnedStopped) {
                int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                int daysSinceStartDay = today - START_DAY;

                if (today - 1 < START_DAY) {
                    daysSinceStartDay += 7;
                }

                long designedUsage = (mainUsage.downloadTotal / 7) * daysSinceStartDay;

                if (mainUsage.downloadUsed > designedUsage) {
                    hasWarnedOver = true;

                    String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

                    new Popup("You have exceeded your data usage", "designed for up to this day. (Only",
                            percentWithPrecision(designedUsage, BYTE_IN_A_GIG, 1) + "G should be used by "
                                    + days[today - 1] + ")",
                            "You have used " + percentWithPrecision(mainUsage.downloadUsed, BYTE_IN_A_GIG, 1) + "G");
                }
            }

            double millisecondsToWait = 1000 * settings.secondsToUpdate; // Normal: 1000 * 300
            double millisecondsPassed = 0;

            while (millisecondsToWait > millisecondsPassed && !update) {
                try {
                    /*
                     * In-case it loses focus I've had this happen many times and I'm not sure
                     * what's causing it But this might fix it
                     *
                     * if(isAlwaysOnTop()) { setAlwaysOnTop(false); setAlwaysOnTop(true); }
                     *
                     * This minimizes fullscreen windows, don't use this
                     */

                    Thread.sleep(CHECK_RESOLUTION);
                    millisecondsPassed += CHECK_RESOLUTION;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static long parseUsageValues(String line) {
        long used;
        String values = line.split("\\[")[3].split("]")[0].replaceAll("\"", "").trim();
        String uss = values.split(",")[1];
        uss = uss.substring(1);
        used = Long.parseLong(uss);
        return used;
    }

    public static boolean rangesContains(ArrayList<IPRange> ranges, IPRange object) {
        for (IPRange range : ranges) {
            if (range.equals(object)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasWarnedOver = false;
    private static boolean hasWarnedStopped = false;

    public static final long BYTE_IN_A_GIG = 1024 * 1024 * 1024;
    public static final long BYTE_IN_A_MB = 1024 * 1024;
    public static final long BYTE_IN_A_KB = 1024;
    private static final double PRECISION = 100;

    public static final long NANOS_IN_A_SECOND = 1000000000;

    private static final Color LOW_USAGE = new Color(67, 181, 129);
    private static final Color MEDIUM_USAGE = new Color(250, 166, 26);
    private static final Color HIGH_USAGE = new Color(240, 71, 71);

    private static double percentage = 0;
    public static Usage mainUsage = null;
    public static Usage curUsage = null;
    public static ArrayList<Usage> quotas = new ArrayList<Usage>();
    public static ArrayList<Usage> lastQuotas = new ArrayList<Usage>();

    public static Usage findUsage(Usage usage) {
        if (usage != null)
            return findUsage(usage.ipRange, quotas);
        else
            return null;
    }

    public static Usage findUsage(String ipRange) {
        if (ipRange != null)
            return findUsage(new IPRange(ipRange), quotas);
        else
            return null;
    }

    public static Usage findUsage(Usage usage, ArrayList<Usage> quotas) {
        if (usage != null)
            return findUsage(usage.ipRange, quotas);
        else
            return null;
    }

    public static Usage findUsage(String ipRange, ArrayList<Usage> quotas) {
        if (ipRange != null)
            return findUsage(new IPRange(ipRange), quotas);
        else
            return null;
    }

    public static Usage findUsage(IPRange ipRange, ArrayList<Usage> quotas) {
        for (Usage quotaUsage : quotas) {
            if (quotaUsage.ipRange.equals(ipRange)) {
                return quotaUsage;
            }
        }

        return null;
    }

    public static double percentWithPrecision(long numerator, long denominator, int percentModifier) {
        return Math.round(((double) numerator / (double) denominator) * percentModifier * PRECISION) / PRECISION;
    }

    public static double roundToPrecision(double number) {
        return Math.round(number * PRECISION) / PRECISION;
    }

    public static void updatePercentage() {
        percentage = percentWithPrecision(curUsage.downloadUsed, curUsage.downloadTotal, 100);

        //System.out.println("Used Data: " + curUsage.downloadUsed);
        //System.out.println("Maximum Data: " + curUsage.downloadTotal);
        //System.out.println("Percent Used: " + percentage);
        //System.out.println("Speed: " + curUsage.downloadSpeed);
        //System.out.println("Percent of Total Speed: " + curUsage.percentOfTotalSpeed);

        updateBar(percentage);
        updateText("<html>Used: " + percentWithPrecision(curUsage.downloadUsed, BYTE_IN_A_GIG, 1) + "/"
                + percentWithPrecision(curUsage.downloadTotal, BYTE_IN_A_GIG, 1) + "G " + curUsage.getDownloadSpeedString());
    }

    public static String getUsageColourHex(Usage usage) {
        Color from = usage.percentOfTotalSpeed < 50 ? LOW_USAGE : MEDIUM_USAGE;
        Color to = usage.percentOfTotalSpeed < 50 ? MEDIUM_USAGE : HIGH_USAGE;

        float percentFromTo = (float) ((usage.percentOfTotalSpeed < 50 ? usage.percentOfTotalSpeed
                : usage.percentOfTotalSpeed - 50) * 2);

        return colorToHex(colourFromPercentage(from, to, percentFromTo));
    }

    public static Color colourFromPercentage(Color to, Color from, float percent) {
        return colourFromDecimal(to, from, percent / 100);
    }

    public static Color colourFromDecimal(Color to, Color from, float decimalPercent) {
        float inverse_blending = 1 - decimalPercent;

        float red = from.getRed() * decimalPercent + to.getRed() * inverse_blending;
        float green = from.getGreen() * decimalPercent + to.getGreen() * inverse_blending;
        float blue = from.getBlue() * decimalPercent + to.getBlue() * inverse_blending;

        return new Color(red / 255, green / 255, blue / 255);
    }

    public static String colorToHex(Color color) {
        String rgb = Integer.toHexString(color.getRGB());
        return rgb.substring(2);
    }

    public static void updateBar(double percent) {
        bar.setValue((int) Math.round(percent));

        putTextCentered(WIDTH, roundToPrecision(percent) + "%", bText, 15);
    }

    public static void updateBarText(String newText) {
        putTextCentered(WIDTH, newText, bText, 15);
    }

    public static void updateText(String newText) {
        putTextCentered(WIDTH, newText, text, 40);
    }

    public static void putTextCentered(int windowWidth, String text, JLabel label, int yPos) {
        label.setText(text);
        int width = label.getGraphics().getFontMetrics().stringWidth(text.replaceAll("<[^<>]*>", ""));
        label.setBounds((windowWidth / 2) - (width / 2), yPos, 300, 20);
    }

    public static boolean isAlwaysOnTop() {
        return frame.isAlwaysOnTop();
    }

    public static boolean isAlwaysOnTopSupported() {
        return frame.isAlwaysOnTopSupported();
    }

    public static void setAlwaysOnTop(boolean alwaysOnTop) {
        if (isAlwaysOnTopSupported()) {
            frame.setAlwaysOnTop(alwaysOnTop);
        }
    }
}
