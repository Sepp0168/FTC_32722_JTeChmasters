package org.firstinspires.ftc.teamcode.robot.debug;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TelemetryServer -- a tiny HTTP JSON server that runs ON THE ROBOT (Control
 * Hub) and lets FTC Code Utils (the VSCode extension) poll live state over
 * the network: position, current OpMode/task, arbitrary named variables,
 * and the next planned path point.
 *
 * This is intentionally dependency-free -- no NanoHTTPD, no external
 * library -- since it only needs to serve one small JSON blob per request.
 * It uses raw java.net.ServerSocket, which is available on Android/the
 * Control Hub without adding anything to build.gradle.
 *
 * --- Usage ---
 *
 * Start it once, e.g. from RobotHardware.init():
 *
 *     TelemetryServer.getInstance().start(8080);
 *
 * Update it from anywhere in your code as state changes:
 *
 *     TelemetryServer.getInstance().setOpModeName("AutoRedLeft");
 *     TelemetryServer.getInstance().setPosition(12.4, 30.1, 90.0);
 *     TelemetryServer.getInstance().setCurrentTask("Driving to scoring position");
 *     TelemetryServer.getInstance().setNextPathPoint(24.0, 36.0);
 *     TelemetryServer.getInstance().setVariable("armAngle", arm.getCurrentAngle());
 *
 * Stop it when the OpMode ends (optional -- a fresh start() call also
 * safely replaces a running server):
 *
 *     TelemetryServer.getInstance().stop();
 *
 * The Control Hub's IP address (needed on the VSCode side to connect) is
 * shown on the Driver Station's Program & Manage screen, or by running
 * `adb shell ip addr show wlan0` while connected via USB.
 *
 * --- Endpoints ---
 *
 * GET /telemetry  -> JSON snapshot of current state (see buildSnapshot())
 * GET /            -> plain-text confirmation the server is reachable
 */
public class TelemetryServer {

    private static final TelemetryServer INSTANCE = new TelemetryServer();

    public static TelemetryServer getInstance() {
        return INSTANCE;
    }

    private volatile ServerSocket serverSocket;
    private volatile Thread serverThread;
    private volatile boolean running = false;

    // ---- Live state, updated by robot code, read by the HTTP handler ----
    private volatile String opModeName = "";
    private volatile String currentTask = "";
    private volatile double positionX = 0;
    private volatile double positionY = 0;
    private volatile double headingDegrees = 0;
    private volatile Double nextPathX = null;
    private volatile Double nextPathY = null;
    private volatile double batteryVoltage = 12.4;
    private volatile double runtimeSeconds = 0;
    private volatile long startTimeMillis = System.currentTimeMillis();
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> motors = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> gamepads = new ConcurrentHashMap<>();
    private final java.util.List<JSONObject> pathPoints = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<JSONObject> targets = new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile JSONObject lastException = null;
    private volatile String lastHardwareConfigJson = null;
    private volatile PidListener pidListener = null;

    /** Implement this in a subsystem to receive live PID pushes from the PID Tuner tab. */
    public interface PidListener {
        void onPidUpdate(double p, double i, double d, double f);
    }

    /** Registers the callback invoked whenever the PID Tuner tab pushes new P/I/D/F values. */
    public void setPidListener(PidListener listener) {
        this.pidListener = listener;
    }

    private TelemetryServer() {}

    /** Starts the server on the given port. Safe to call again to restart on a new port. */
    public synchronized void start(int port) {
        stop();
        startTimeMillis = System.currentTimeMillis();
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            return; // port busy or unavailable -- fail silently, this is a debug tool
        }
        running = true;
        serverThread = new Thread(this::acceptLoop, "TelemetryServer");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    /** Stops the server, if running. */
    public synchronized void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                handleClient(client);
            } catch (IOException e) {
                if (running) {
                    // socket error while still supposed to be running -- just keep looping
                }
            }
        }
    }

    private void handleClient(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = client.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine == null) return;
            String method = parseMethod(requestLine);
            String path = parsePath(requestLine);

            int contentLength = 0;
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            String requestBody = "";
            if (contentLength > 0) {
                char[] buf = new char[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int n = in.read(buf, read, contentLength - read);
                    if (n < 0) break;
                    read += n;
                }
                requestBody = new String(buf, 0, read);
            }

            byte[] body;
            String contentType;
            int statusCode = 200;

            if ("/telemetry".equals(path)) {
                body = buildSnapshot().toString().getBytes(StandardCharsets.UTF_8);
                contentType = "application/json";
            } else if ("/pid".equals(path) && "POST".equals(method)) {
                // Body: {"p":..,"i":..,"d":..,"f":..,"name":".."} pushed live from
                // the PID Tuner tab. Forward it to whatever the robot code
                // registered via setPidListener(...); no-op if nothing's listening.
                if (pidListener != null) {
                    try {
                        JSONObject pidJson = new JSONObject(requestBody);
                        pidListener.onPidUpdate(
                            pidJson.optDouble("p", 0),
                            pidJson.optDouble("i", 0),
                            pidJson.optDouble("d", 0),
                            pidJson.optDouble("f", 0)
                        );
                    } catch (Exception e) {
                        statusCode = 400;
                    }
                }
                body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                contentType = "application/json";
            } else if ("/sync/hardware".equals(path) && "POST".equals(method)) {
                // The Hardware Map Editor pushed its config -- stash it so a
                // future GET (or robot code) can read it back.
                this.lastHardwareConfigJson = requestBody;
                body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                contentType = "application/json";
            } else if ("/sync/hardware".equals(path) && "GET".equals(method)) {
                body = (lastHardwareConfigJson != null ? lastHardwareConfigJson : "{\"devices\":[]}").getBytes(StandardCharsets.UTF_8);
                contentType = "application/json";
            } else {
                body = "TelemetryServer is running.".getBytes(StandardCharsets.UTF_8);
                contentType = "text/plain";
            }

            String header = "HTTP/1.1 " + statusCode + " " + (statusCode == 200 ? "OK" : "Bad Request") + "\r\n"
                    + "Content-Type: " + contentType + "; charset=utf-8\r\n"
                    + "Content-Length: " + body.length + "\r\n"
                    + "Access-Control-Allow-Origin: *\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(body);
            out.flush();
        } catch (IOException ignored) {
            // client disconnected mid-response, or similar -- not fatal, just drop it
        }
    }

    private static String parseMethod(String requestLine) {
        String[] parts = requestLine.split(" ");
        return parts.length >= 1 ? parts[0] : "GET";
    }

    private static String parsePath(String requestLine) {
        // requestLine looks like: "GET /telemetry HTTP/1.1"
        String[] parts = requestLine.split(" ");
        return parts.length >= 2 ? parts[1] : "/";
    }

    private JSONObject buildSnapshot() {
        JSONObject json = new JSONObject();
        try {
            json.put("opModeName", opModeName.isEmpty() ? "Idle" : opModeName);
            json.put("currentTask", currentTask.isEmpty() ? "Ready" : currentTask);
            json.put("batteryVoltage", batteryVoltage);
            json.put("runtimeSeconds", runtimeSeconds > 0 ? runtimeSeconds : (System.currentTimeMillis() - startTimeMillis) / 1000.0);

            JSONObject position = new JSONObject();
            position.put("x", positionX);
            position.put("y", positionY);
            position.put("headingDegrees", headingDegrees);
            json.put("position", position);

            if (nextPathX != null && nextPathY != null) {
                JSONObject nextPoint = new JSONObject();
                nextPoint.put("x", nextPathX);
                nextPoint.put("y", nextPathY);
                json.put("nextPathPoint", nextPoint);
            }

            JSONObject motorObj = new JSONObject();
            for (Map.Entry<String, JSONObject> entry : motors.entrySet()) {
                motorObj.put(entry.getKey(), entry.getValue());
            }
            json.put("motors", motorObj);

            JSONObject gpadObj = new JSONObject();
            for (Map.Entry<String, JSONObject> entry : gamepads.entrySet()) {
                gpadObj.put(entry.getKey(), entry.getValue());
            }
            json.put("gamepads", gpadObj);

            org.json.JSONArray pathArr = new org.json.JSONArray();
            for (JSONObject pt : pathPoints) {
                pathArr.put(pt);
            }
            json.put("pathPoints", pathArr);

            org.json.JSONArray targetArr = new org.json.JSONArray();
            for (JSONObject tg : targets) {
                targetArr.put(tg);
            }
            json.put("targets", targetArr);

            JSONObject vars = new JSONObject();
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                vars.put(entry.getKey(), entry.getValue());
            }
            json.put("variables", vars);

            if (lastException != null) {
                json.put("lastException", lastException);
            }

            json.put("timestampMillis", System.currentTimeMillis());
        } catch (Exception ignored) {
        }
        return json;
    }

    // ---- Setters called from robot code as state changes ----

    public void setOpModeName(String name) {
        this.opModeName = name == null ? "" : name;
    }

    public void setCurrentTask(String task) {
        this.currentTask = task == null ? "" : task;
    }

    public void setBatteryVoltage(double voltage) {
        this.batteryVoltage = voltage;
    }

    public void setRuntimeSeconds(double seconds) {
        this.runtimeSeconds = seconds;
    }

    public void setPosition(double x, double y, double headingDegrees) {
        this.positionX = x;
        this.positionY = y;
        this.headingDegrees = headingDegrees;
    }

    public void setNextPathPoint(double x, double y) {
        this.nextPathX = x;
        this.nextPathY = y;
    }

    public void clearNextPathPoint() {
        this.nextPathX = null;
        this.nextPathY = null;
    }

    public void setMotorState(String key, int ticks, double power) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("ticks", ticks);
            obj.put("power", power);
            motors.put(key, obj);
        } catch (Exception ignored) {}
    }

    public void setGamepadState(String gamepadId, double lx, double ly, double rx, double ry, boolean a, boolean b, boolean x, boolean y) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("lx", lx); obj.put("ly", ly);
            obj.put("rx", rx); obj.put("ry", ry);
            obj.put("a", a); obj.put("b", b);
            obj.put("x", x); obj.put("y", y);
            gamepads.put(gamepadId, obj);
        } catch (Exception ignored) {}
    }

    public void addPathWaypoint(double x, double y) {
        try {
            JSONObject pt = new JSONObject();
            pt.put("x", x); pt.put("y", y);
            pathPoints.add(pt);
        } catch (Exception ignored) {}
    }

    public void clearPathWaypoints() {
        pathPoints.clear();
    }

    public void addTargetMarker(String label, double x, double y, String type) {
        try {
            JSONObject tg = new JSONObject();
            tg.put("label", label);
            tg.put("x", x);
            tg.put("y", y);
            tg.put("type", type == null ? "target" : type);
            targets.add(tg);
        } catch (Exception ignored) {}
    }

    public void clearTargetMarkers() {
        targets.clear();
    }

    public void reportException(String message, String stackTrace) {
        try {
            JSONObject exc = new JSONObject();
            exc.put("message", message == null ? "Unknown exception" : message);
            exc.put("stackTrace", stackTrace == null ? "" : stackTrace);
            exc.put("timestamp", System.currentTimeMillis());
            this.lastException = exc;
        } catch (Exception ignored) {}
    }

    public void clearException() {
        this.lastException = null;
    }

    public void setVariable(String name, Object value) {
        if (name == null) return;
        variables.put(name, value);
    }

    public void clearVariable(String name) {
        variables.remove(name);
    }

    public void clearAllVariables() {
        variables.clear();
    }

    /** Raw JSON last pushed by "Sync Hardware Config with Robot", or null if nothing's been pushed yet. */
    public String getLastHardwareConfigJson() {
        return lastHardwareConfigJson;
    }
}
