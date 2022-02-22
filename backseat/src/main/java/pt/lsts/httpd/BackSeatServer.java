package pt.lsts.httpd;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.Thread.State;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import pt.lsts.autonomy.MissionExecutive;
import pt.lsts.backseat.BackSeatDriver;
import pt.lsts.imc4j.annotations.Parameter;
import pt.lsts.imc4j.annotations.Periodic;
import pt.lsts.imc4j.net.TcpClient;
import pt.lsts.imc4j.util.PeriodicCallbacks;
import pt.lsts.imc4j.util.PojoConfig;

public class BackSeatServer extends NanoHTTPD {

    protected enum BackSeatType {
        None,
        BackSeatDriver,
        MissionExecutive
    }

    @Parameter(description = "Auto Start on Power On")
    public boolean autoStartOnPowerOn = false;

    public String copyYear = new SimpleDateFormat("yyyy").format(new Date(System.currentTimeMillis()));

    protected boolean allowHotConfig = false;

    protected TcpClient driver;
    protected File output;
    protected BackSeatType type;
    protected String name;

    protected File configServerFile = new File(this.getClass().getSimpleName().toLowerCase() + ".ini");
    protected File configDriverFile;
    protected File logFile;
    protected boolean logJustServe = false;

    public BackSeatServer(TcpClient back_seat, int http_port, boolean allowHotConfig, String configFilePath,
                          String logFilePath, boolean logJustServe) {
        super(http_port);

        this.allowHotConfig = allowHotConfig;

        if (configServerFile.exists()) {
            try {
                loadServerSettings(new String(Files.readAllBytes(configServerFile.toPath())));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.driver = back_seat;
        fillType();

        switch (type) {
            case MissionExecutive:
                ((MissionExecutive) driver).setUseSystemExitOrStop(false);
                break;
            default:
                break;
        }

        configDriverFile = new File(back_seat.getClass().getSimpleName()+".ini");
        if(configFilePath != null)
            configDriverFile = new File(configFilePath);
        if (configDriverFile.exists()) {
            try {
                loadSettings(new String(Files.readAllBytes(configDriverFile.toPath())));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        logFile = logFilePath != null ? new File(logFilePath) : null;
        try { logFile.createNewFile(); } catch (Exception e) {};
        if (!logFile.exists())
            logFile = null;
        if (logFilePath == null)
            this.logJustServe = false;
        else
            this.logJustServe = logJustServe;
        createAndRedirectOutputLog();
        PeriodicCallbacks.register(this);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                stop();
                System.out.println("Server is stopped.\n");
                PeriodicCallbacks.unregister(this);
            }
        }));

        System.out.println("Listening on port " + http_port + "...\n");

        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        }
        catch (IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
            System.exit(-1);
        }

        if (autoStartOnPowerOn) {
            try {
                Thread.sleep(5000);
            }
            catch (Exception e) {
            }

            try {
                startBackSeat();
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        while(true) {
            try {
                Thread.sleep(1000);
            }
            catch (Exception e) {
            }
        }
    }

    @Periodic(1000)
    public void checkDateAndRedirectLog() {
        try {
            if (Integer.parseInt(copyYear) < 2017)
                copyYear = new SimpleDateFormat("yyyy").format(new Date(System.currentTimeMillis()));
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        try {
            String yearStr = output.getName().substring(0, 4);
            int year = Integer.parseInt(yearStr);
            if (year != Integer.parseInt(copyYear)) {
                createAndRedirectOutputLog();
            }
        }
        catch (NumberFormatException e) {
            // not doing anything
        }
        catch (Exception e) {
            System.out.println("log file output '" + output.getName() + "' is not date parsable :: " + e.getMessage());
        }
    }

    private void createAndRedirectOutputLog() {
        if (logFile != null && logFile.exists()) {
            output = logFile;
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMdd_HHmmss");
            output = new File("log/" + sdf.format(new Date()) + ".log");
        }

        if (!logJustServe) {
            try {
                System.out.println("Redirecting output to " + output.getAbsolutePath());
                PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(output)), true);
                System.setOut(ps);
                System.setErr(ps);
                System.out.println("Done redirecting output to " + output.getAbsolutePath());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void fillType() {
        name = driver.getClass().getSimpleName();
        name = name.replaceAll("([a-z0-9])([A-Z])", "$1 $2");

        if (driver instanceof BackSeatDriver)
            type = BackSeatType.BackSeatDriver;
        else if (driver instanceof MissionExecutive)
            type = BackSeatType.MissionExecutive;
        else
            type = BackSeatType.None;
    }

    private String settings() throws Exception {
        StringBuilder sb = new StringBuilder();

        for (Field f : driver.getClass().getDeclaredFields()) {
            Parameter p = f.getAnnotation(Parameter.class);
            f.setAccessible(true);
            if (p != null) {
                sb.append("# " + p.description() + "\n");
                Object value = f.get(driver);
                if (value instanceof String[])
                    value = String.join(", ", ((String[]) value));
                sb.append(f.getName() + "=" + value + "\n\n");
            }
        }

        return sb.toString();
    }

    private void loadServerSettings(String settings) throws Exception {
        if (settings == null || settings.isEmpty())
            return;

        Properties props = new Properties();
        props.load(new ByteArrayInputStream(settings.getBytes()));
        PojoConfig.setProperties(this, props);
    }

    private void loadSettings(String settings) throws Exception {
        if (settings == null || settings.isEmpty())
            return;

        Properties props = new Properties();
        props.load(new ByteArrayInputStream(settings.getBytes()));
        PojoConfig.setProperties(driver, props);
    }

    private void startBackSeat() throws Exception {
        if (driver.isAlive()) {
            System.out.println("Trying to starting a running " + name + ", please stop it first.");
            return;
        }

        System.out.println("Starting " + name + "...");

        if (driver.getState() == State.TERMINATED) {
            createNewDriverAndConfigure();
        }

        try {
            switch (type) {
                case MissionExecutive:
                    ((MissionExecutive) driver).setup();
                    break;
                default:
                    break;
            }

            driver.connect();

            switch (type) {
                case MissionExecutive:
                    ((MissionExecutive) driver).launch();
                    break;
                default:
                    break;
            }

            System.out.println("Started " + name);
        }
        catch (Exception e) {
            System.out.println("Error Starting " + name);
            try {
                stopBackSeat();
            }
            catch (Exception e2) {
                e2.printStackTrace();
            }
            throw e;
        }
    }

    private void saveSettings() throws Exception {
        // File config = new File(driver.getClass().getSimpleName()+".ini");
        PojoConfig.writeProperties(driver, configDriverFile);
    }

    private void stopBackSeat() throws Exception {
        System.out.println("Stopping " + name + "...");

        try {
            PeriodicCallbacks.unregister(driver);
            driver.disconnect();
            driver.interrupt();

            createNewDriverAndConfigure();

            System.out.println("Stopped " + name);
        }
        catch (Exception e) {
            System.out.println("Error Stopping " + name);
            throw e;
        }
    }

    /**
     * @throws Exception
     */
    private void createNewDriverAndConfigure() throws Exception {
        driver = PojoConfig.create(driver.getClass(), PojoConfig.getProperties(driver));
        switch (type) {
            case MissionExecutive:
                ((MissionExecutive) driver).setUseSystemExitOrStop(false);
                break;
            default:
                break;
        }
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms,
            Map<String, String> files) {

        if (uri.equals("/state")) {
            try {
                return newChunkedResponse(Status.OK, "text/plain",
                        new ByteArrayInputStream(String.valueOf(driver.isAlive()).getBytes()));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (uri.equals("/logbook")) {
            try {
                return newChunkedResponse(Status.OK, "text/plain", new FileInputStream(output));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (uri.equals("/style.css")) {
            try {
                InputStream stream = this.getClass().getResourceAsStream("style.css");
                return newChunkedResponse(Status.OK, "text/css", stream);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (uri.equals("/util.js")) {
            try {
                InputStream stream = this.getClass().getResourceAsStream("util.js");
                return newChunkedResponse(Status.OK, "text/javascript", stream);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (uri.equals("/manifest.json")) {
            try (InputStream inStream = this.getClass().getResourceAsStream("manifest.json");
                    Scanner s = new Scanner(inStream)) {
                s.useDelimiter("\\A");
                String manifest = s.hasNext() ? s.next() : "";

                manifest = manifest.replaceFirst("\\$\\$SHORTNAME\\$\\$", name.replace(" ", "").substring(0, 12));
                manifest = manifest.replaceFirst("\\$\\$NAME\\$\\$", name);

                return newChunkedResponse(Status.OK, "text/json", new ByteArrayInputStream(manifest.getBytes()));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        String cmd = "none";

        if (parms.get("cmd") != null) {
            cmd = parms.get("cmd");
        }

        switch (cmd) {
            case "Start":
                try {
                    startBackSeat();
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
                break;
            case "Stop":
                try {
                    stopBackSeat();
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
                break;
            case "Save":
                try {
                    if (!driver.isAlive() || allowHotConfig) {
                        try {
                            loadSettings(parms.get("settings"));
                            saveSettings();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Backseat settings are not allowed at this moment. Wait till stop first.");
                    }

                    String checkAutoStart = parms.get("autoStart");
                    autoStartOnPowerOn = checkAutoStart != null && checkAutoStart.equalsIgnoreCase("checked");
                    System.out.println("Auto start on power on " + (autoStartOnPowerOn ? "enabled" : "disabled"));
                    PojoConfig.writeProperties(this, configServerFile);
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
                break;
            default:
                break;
        }

        String settings = "";
        try {
            settings = settings();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try (InputStream inStream = this.getClass().getResourceAsStream("index.html");
             Scanner s = new Scanner(inStream)) {
            s.useDelimiter("\\A");
            String indexHtml = s.hasNext() ? s.next() : "";

            indexHtml = indexHtml.replaceFirst("\\(\\(-TITLE-\\)\\)", name);
            indexHtml = indexHtml.replaceFirst("\\(\\(-NAME-\\)\\)", name);
            indexHtml = indexHtml.replaceFirst("\\(\\(-START-STOP-\\)\\)", !driver.isAlive() ? "Start" : "Stop");
            indexHtml = indexHtml.replaceFirst("\\(\\(-CHECKED-\\)\\)", autoStartOnPowerOn ? " checked=\"checked\"" : "");
            indexHtml = indexHtml.replaceFirst("\\(\\(-SETTINGS-\\)\\)", settings);
            indexHtml = indexHtml.replaceFirst("\\(\\(-COPY-YEARS-\\)\\)", copyYear);

            return newChunkedResponse(Status.OK, "text/html", new ByteArrayInputStream(indexHtml.getBytes()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse("<html><body>503</body></html>>");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java -jar BackSeatServer.jar <class> <port>");
            System.err.println("    <--hot-config>?         - To allow change of config while running");
            System.err.println("    <--config <file_path>>? - To allow change of config while running");
            System.err.println("    <--log <file_path>>?    - To set the name of the log file");
            System.err.println("    <--log-just-serve>?     - This will make the log just being served and not written (needs --log flag");
            System.err.println("    <class>                 - The full class name to run");
            System.err.println("    <port>                  - The http server port for this service");
            System.exit(1);
        }

        boolean hotConfig = false;
        String configFilePath = null;
        String logFilePath = null;
        boolean logJustServe = false;

        for (int i = 2; i < args.length; i++) {
            if ("--hot-config".equalsIgnoreCase(args[i].trim())) {
                hotConfig = true;
            } else if ("--config".equalsIgnoreCase(args[i].trim())) {
                configFilePath = args[++i].trim();
            } else if ("--log".equalsIgnoreCase(args[i].trim())) {
                logFilePath = args[++i].trim();
            } if ("--log-just-serve".equalsIgnoreCase(args[i].trim())) {
                logJustServe = true;
            }
        }

        if (logFilePath == null) {
            logJustServe = false;
        }

        new BackSeatServer((TcpClient) Class.forName(args[0]).getDeclaredConstructor().newInstance(),
                Integer.parseInt(args[1]), hotConfig, configFilePath, logFilePath, logJustServe);
        System.exit(0);
    }
}
