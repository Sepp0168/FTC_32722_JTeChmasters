# Wiring TelemetryServer into RobotHardware

Copy `TelemetryServer.java` into:

```text
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/robot/debug/TelemetryServer.java
```

FTC Code Utils generated `RobotHardware.java` files now start the server
automatically on port `8000`, publish battery voltage, publish runtime, and
publish full gamepad state from `updateAll()`.

Port `8000` is the default Control Hub telemetry port for this extension. Do
not use `8080`; it is reserved/disallowed on FTC Control Hubs.

## Minimal RobotHardware Pattern

```java
package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import org.firstinspires.ftc.teamcode.robot.debug.TelemetryServer;
import org.firstinspires.ftc.teamcode.robot.subsystems.*;

public class RobotHardware {
    private static final int TELEMETRY_SERVER_PORT = 8000;

    private final LinearOpMode opMode;
    private VoltageSensor voltageSensor;

    public Drivetrain drive;
    public Arm arm;

    public RobotHardware(LinearOpMode opMode) {
        this.opMode = opMode;
    }

    public void init() {
        drive = new Drivetrain(opMode.hardwareMap);
        arm = new Arm(opMode.hardwareMap);

        TelemetryServer.getInstance().start(TELEMETRY_SERVER_PORT);
        TelemetryServer.getInstance().setOpModeName(opMode.getClass().getSimpleName());

        for (VoltageSensor sensor : opMode.hardwareMap.voltageSensor) {
            voltageSensor = sensor;
            break;
        }
    }

    public void start() {
        TelemetryServer.getInstance().setStarted(true);
    }

    public void updateAll() {
        drive.update();
        arm.update();

        if (voltageSensor != null) {
            TelemetryServer.getInstance().setBatteryVoltage(voltageSensor.getVoltage());
        }
        TelemetryServer.getInstance().setRuntimeSeconds(opMode.getRuntime());
        TelemetryServer.getInstance().setGamepadState("gamepad1", opMode.gamepad1);
        TelemetryServer.getInstance().setGamepadState("gamepad2", opMode.gamepad2);
    }
}
```

Call `robot.start()` immediately after `waitForStart()` if you want the
dashboard status to switch from Waiting to Running.

## Subsystem Telemetry

Any subsystem can report directly to the dashboard:

```java
TelemetryServer.getInstance().setSubsystemTelemetry("Arm", "angleDeg", getAngleDegrees());
TelemetryServer.getInstance().setSubsystemTelemetry("Arm", "targetDeg", targetDegrees);
TelemetryServer.getInstance().setSubsystemTelemetry("Arm", "motorPower", motor.getPower());
```

Batch updates are also supported:

```java
Map<String, Object> data = new HashMap<>();
data.put("heightTicks", liftMotor.getCurrentPosition());
data.put("atTarget", atTarget());
TelemetryServer.getInstance().setSubsystemTelemetry("Lift", data);
```

The Home dashboard renders these under Live Telemetry -> Subsystem Telemetry.

## Autonomous Task Telemetry

Update the current task and next path point at each step:

```java
TelemetryServer.getInstance().setCurrentTask("Driving to scoring position");
TelemetryServer.getInstance().setNextPathPoint(24.0, 36.0);
```

## Wi-Fi Deployment

1. Turn on the Control Hub Wi-Fi access point.
2. Join your computer to the robot Wi-Fi network.
3. In FTC Code Utils, click Upload via Wi-Fi.

The extension runs:

```bash
adb connect 192.168.43.1:5555
./gradlew :TeamCode:installDebug
```

## PID(F) Tuning

For a single default controller:

```java
TelemetryServer.getInstance().setPidListener((p, i, d, f) -> {
    arm.pid.kP = p;
    arm.pid.kI = i;
    arm.pid.kD = d;
    arm.pid.kF = f;
});
```

For multiple targetable subsystems and setpoints:

```java
TelemetryServer.getInstance().setTargetedPidListener((target, p, i, d, f, setpoint) -> {
    if ("Arm".equals(target)) {
        arm.pid.kP = p;
        arm.pid.kI = i;
        arm.pid.kD = d;
        arm.pid.kF = f;
        if (setpoint != null) {
            arm.setTargetDegrees(setpoint);
        }
    }
});
```

Recommended tuning order:

1. Set I, D, and F to 0.
2. Increase P until the subsystem moves toward the setpoint quickly but begins to overshoot.
3. Increase D until overshoot and oscillation settle.
4. Add I only if a steady-state error remains.
5. Add F for predictable feedforward needs such as gravity or velocity.
6. Use the Step button to jump the setpoint and watch rise time, overshoot, and settling.

The tuner posts JSON to `/pid`:

```json
{
  "p": 0.012,
  "i": 0.0,
  "d": 0.001,
  "f": 0.0,
  "target": "Arm",
  "setpoint": 45.0
}
```

## Hardware Config Sync

`GET /sync/hardware` and `POST /sync/hardware` let the extension read/write
the JSON from `.ftc/hardware.json`. The server stores the last received blob in
memory so robot code can inspect it through `getLastHardwareConfigJson()`.
