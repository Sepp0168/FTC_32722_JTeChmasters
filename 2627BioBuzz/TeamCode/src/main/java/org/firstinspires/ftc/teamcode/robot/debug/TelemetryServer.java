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
 *     TelemetryServer.getInstance().start(8000);
 *
 * Update it from anywhere in your code as state changes:
 *
 *     TelemetryServer.getInstance().setOpModeName("AutoRedLeft");
 *     TelemetryServer.getInstance().setPosition(12.4, 30.1, 90.0);
 *     TelemetryServer.getInstance().setCurrentTask("Driving to scoring position");
 *     TelemetryServer.getInstance().setNextPathPoint(24.0, 36.0);
 *     TelemetryServer.getInstance().setVariable("armAngle", arm.getCurrentAngle());
 *     TelemetryServer.getInstance().setPidGraph(45.0, arm.getCurrentAngle());
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
    private volatile String robotState = "waiting";
    private volatile boolean isStarted = false;
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
    private final Map<String, Map<String, Object>> subsystemsTelemetry = new ConcurrentHashMap<>();
    private volatile String pidTargetSubsystem = "";
    private volatile Double pidSetpoint = null;
    private volatile Double pidGraphTarget = null;
    private volatile Double pidGraphActual = null;
    private final java.util.List<JSONObject> pathPoints = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<JSONObject> targets = new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile JSONObject lastException = null;
    private volatile String lastHardwareConfigJson = null;
    private volatile PidListener pidListener = null;

    /** Implement this in a subsystem to receive live PID pushes from the PID Tuner tab. */
    public interface PidListener {
        void onPidUpdate(double proportional, double integral, double derivative, double feedforward);
    }

    /**
     * Implement this (in addition to, or instead of, {@link PidListener}) when you have
     * multiple subsystems and want the PID Tuner's target-subsystem selector to route
     * updates to the correct one, and to receive the setpoint alongside P/I/D/F.
     */
    public interface TargetedPidListener {
        void onPidUpdate(String targetSubsystem, double proportional, double integral, double derivative,
                          double feedforward, Double setpoint);
    }

    private volatile TargetedPidListener targetedPidListener = null;

    /** Registers a callback that also receives the target subsystem name and setpoint. */
    public void setTargetedPidListener(TargetedPidListener listener) {
        this.targetedPidListener = listener;
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
        } catch (IOException bindFailure) {
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
            } catch (IOException acceptFailure) {
                if (running) {
                    // socket error while still supposed to be running -- just keep looping
                }
            }
        }
    }

    private void handleClient(Socket client) {
        try (Socket socket = client;
             BufferedReader requestReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = socket.getOutputStream()) {

            String requestLine = requestReader.readLine();
            if (requestLine == null) return;
            String method = parseMethod(requestLine);
            String path = parsePath(requestLine);

            int contentLength = 0;
            String headerLine;
            while ((headerLine = requestReader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(headerLine.substring(headerLine.indexOf(':') + 1).trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            String requestBody = "";
            if (contentLength > 0) {
                char[] buf = new char[contentLength];
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int bytesRead = requestReader.read(buf, totalRead, contentLength - totalRead);
                    if (bytesRead < 0) break;
                    totalRead += bytesRead;
                }
                requestBody = new String(buf, 0, totalRead);
            }

            byte[] body;
            String contentType;
            int statusCode = 200;

            if ("/telemetry".equals(path)) {
                body = buildSnapshot().toString().getBytes(StandardCharsets.UTF_8);
                contentType = "application/json";
            } else if ("/pid".equals(path) && "POST".equals(method)) {
                // Body: {"p":..,"i":..,"d":..,"f":..,"target":"..","setpoint":..} pushed
                // live from the PID Tuner tab. "target" is the subsystem name chosen in
                // the tuner's target selector and "setpoint" is the slider value; both
                // are optional so older payloads without them still work. Forwarded to
                // whatever the robot code registered via setPidListener(...) and/or
                // setTargetedPidListener(...); no-op if nothing's listening.
                try {
                    JSONObject pidJson = new JSONObject(requestBody);
                    double p = pidJson.optDouble("p", 0);
                    double i = pidJson.optDouble("i", 0);
                    double d = pidJson.optDouble("d", 0);
                    double f = pidJson.optDouble("f", 0);
                    String target = pidJson.optString("target", "");
                    Double setpoint = pidJson.has("setpoint") ? pidJson.optDouble("setpoint") : null;

                    this.pidTargetSubsystem = target;
                    this.pidSetpoint = setpoint;

                    if (pidListener != null) {
                        pidListener.onPidUpdate(p, i, d, f);
                    }
                    if (targetedPidListener != null) {
                        targetedPidListener.onPidUpdate(target, p, i, d, f, setpoint);
                    }
                } catch (Exception malformedPidPayload) {
                    statusCode = 400;
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
            json.put("robotState", robotState);
            json.put("isStarted", isStarted);
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

            JSONObject subsystemsObj = new JSONObject();
            for (Map.Entry<String, Map<String, Object>> subsystemEntry : subsystemsTelemetry.entrySet()) {
                JSONObject subsystemData = new JSONObject();
                for (Map.Entry<String, Object> field : subsystemEntry.getValue().entrySet()) {
                    subsystemData.put(field.getKey(), field.getValue());
                }
                subsystemsObj.put(subsystemEntry.getKey(), subsystemData);
            }
            json.put("subsystems", subsystemsObj);

            if (!pidTargetSubsystem.isEmpty() || pidSetpoint != null) {
                JSONObject pidState = new JSONObject();
                pidState.put("target", pidTargetSubsystem);
                if (pidSetpoint != null) {
                    pidState.put("setpoint", pidSetpoint);
                }
                json.put("pidState", pidState);
            }
            if (pidGraphTarget != null && pidGraphActual != null) {
                json.put("pidTarget", pidGraphTarget);
                json.put("pidActual", pidGraphActual);
            }

            org.json.JSONArray pathArr = new org.json.JSONArray();
            for (JSONObject waypoint : pathPoints) {
                pathArr.put(waypoint);
            }
            json.put("pathPoints", pathArr);

            org.json.JSONArray targetArr = new org.json.JSONArray();
            for (JSONObject targetMarker : targets) {
                targetArr.put(targetMarker);
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
        if (!isStarted && !this.opModeName.isEmpty()) {
            this.robotState = "waiting";
        }
    }

    public void setStarted(boolean started) {
        this.isStarted = started;
        this.robotState = started ? "running" : "waiting";
    }

    public void setRobotState(String state) {
        this.robotState = state == null ? "waiting" : state;
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

    /**
     * Feeds the PID / PIDF Tuner graph. Call this once per loop from the
     * subsystem being tuned, using the same units for both values (degrees,
     * encoder ticks, inches, etc.).
     */
    public void setPidGraph(double target, double actual) {
        this.pidGraphTarget = target;
        this.pidGraphActual = actual;
    }

    /** Clears the PID graph feed so the dashboard falls back to its placeholder graph. */
    public void clearPidGraph() {
        this.pidGraphTarget = null;
        this.pidGraphActual = null;
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

    public void setGamepadState(String gamepadId, double leftStickX, double leftStickY, double rightStickX, double rightStickY,
                                 boolean aPressed, boolean bPressed, boolean xPressed, boolean yPressed) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("lx", leftStickX); obj.put("ly", leftStickY);
            obj.put("rx", rightStickX); obj.put("ry", rightStickY);
            obj.put("a", aPressed); obj.put("b", bPressed);
            obj.put("x", xPressed); obj.put("y", yPressed);
            gamepads.put(gamepadId, obj);
        } catch (Exception ignored) {}
    }

    /**
     * Convenience overload that logs a full FTC SDK {@code Gamepad} object in one call --
     * both sticks, both triggers, both bumpers, and every face/system button -- instead of
     * having to pull each field out by hand. Safe to call every loop; intended to be wired
     * up automatically in {@code RobotHardware.updateAll()} for gamepad1/gamepad2.
     */
    public void setGamepadState(String gamepadId, com.qualcomm.robotcore.hardware.Gamepad gamepad) {
        if (gamepad == null) return;
        try {
            JSONObject obj = new JSONObject();
            obj.put("lx", gamepad.left_stick_x);
            obj.put("ly", gamepad.left_stick_y);
            obj.put("rx", gamepad.right_stick_x);
            obj.put("ry", gamepad.right_stick_y);
            obj.put("leftTrigger", gamepad.left_trigger);
            obj.put("rightTrigger", gamepad.right_trigger);
            obj.put("leftBumper", gamepad.left_bumper);
            obj.put("rightBumper", gamepad.right_bumper);
            obj.put("a", gamepad.a);
            obj.put("b", gamepad.b);
            obj.put("x", gamepad.x);
            obj.put("y", gamepad.y);
            obj.put("dpadUp", gamepad.dpad_up);
            obj.put("dpadDown", gamepad.dpad_down);
            obj.put("dpadLeft", gamepad.dpad_left);
            obj.put("dpadRight", gamepad.dpad_right);
            obj.put("leftStickButton", gamepad.left_stick_button);
            obj.put("rightStickButton", gamepad.right_stick_button);
            obj.put("start", gamepad.start);
            obj.put("back", gamepad.back);
            obj.put("guide", gamepad.guide);
            gamepads.put(gamepadId, obj);
        } catch (Exception ignored) {}
    }

    /** Sets a single named telemetry field for a subsystem (e.g. "Lift", "current", 3.2). */
    public void setSubsystemTelemetry(String subsystem, String key, Object value) {
        if (subsystem == null || key == null) return;
        subsystemsTelemetry
            .computeIfAbsent(subsystem, s -> new ConcurrentHashMap<>())
            .put(key, value);
    }

    /** Replaces/merges a whole batch of telemetry fields for a subsystem at once. */
    public void setSubsystemTelemetry(String subsystem, Map<String, Object> data) {
        if (subsystem == null || data == null) return;
        subsystemsTelemetry
            .computeIfAbsent(subsystem, s -> new ConcurrentHashMap<>())
            .putAll(data);
    }

    /** Clears all telemetry previously reported for one subsystem. */
    public void clearSubsystemTelemetry(String subsystem) {
        if (subsystem == null) return;
        subsystemsTelemetry.remove(subsystem);
    }

    /** Clears telemetry for every subsystem. */
    public void clearAllSubsystemTelemetry() {
        subsystemsTelemetry.clear();
    }

    public void addPathWaypoint(double x, double y) {
        try {
            JSONObject waypoint = new JSONObject();
            waypoint.put("x", x); waypoint.put("y", y);
            pathPoints.add(waypoint);
        } catch (Exception ignored) {}
    }

    public void clearPathWaypoints() {
        pathPoints.clear();
    }

    public void addTargetMarker(String label, double x, double y, String type) {
        try {
            JSONObject targetMarker = new JSONObject();
            targetMarker.put("label", label);
            targetMarker.put("x", x);
            targetMarker.put("y", y);
            targetMarker.put("type", type == null ? "target" : type);
            targets.add(targetMarker);
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
