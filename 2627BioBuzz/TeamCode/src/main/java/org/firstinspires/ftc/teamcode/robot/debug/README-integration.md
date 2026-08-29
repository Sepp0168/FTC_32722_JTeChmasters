# Wiring TelemetryServer into RobotHardware

Drop `TelemetryServer.java` into:

```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/robot/debug/TelemetryServer.java
```

Then update `RobotHardware.java` (from the architecture guide) to start it on
init and feed it position/task updates. Minimal example:

```java
package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.teamcode.robot.subsystems.*;
import org.firstinspires.ftc.teamcode.robot.debug.TelemetryServer;

public class RobotHardware {
    private final LinearOpMode opMode;

    public Drivetrain drive;
    public Arm arm;
    public Intake intake;
    public Lift lift;

    public RobotHardware(LinearOpMode opMode) {
        this.opMode = opMode;
    }

    public void init() {
        drive = new Drivetrain(opMode.hardwareMap);
        arm = new Arm(opMode.hardwareMap);
        intake = new Intake(opMode.hardwareMap);
        lift = new Lift(opMode.hardwareMap);

        // Start the live debug server -- safe no-op cost if nobody's polling it.
        TelemetryServer.getInstance().start(8080);
        TelemetryServer.getInstance().setOpModeName(opMode.getClass().getSimpleName());
    }

    public void updateAll() {
        arm.update();
        lift.update();

        // Push whatever's useful for live debugging -- add more as you need it.
        TelemetryServer.getInstance().setPosition(
            drive.getX(), drive.getY(), drive.getHeadingDegrees()
        );
        TelemetryServer.getInstance().setVariable("armAngle", arm.getCurrentAngle());
    }
}
```

In an autonomous OpMode, also call `setCurrentTask(...)` and
`setNextPathPoint(...)` at each step of your sequence so the dashboard shows
what the robot is doing and where it's headed next:

```java
TelemetryServer.getInstance().setCurrentTask("Driving to scoring position");
TelemetryServer.getInstance().setNextPathPoint(24.0, 36.0);
```

Find the Control Hub's IP address on the Driver Station's Program & Manage
screen (or `adb shell ip addr show wlan0` over USB), then use that IP in
FTC Code Utils' Connect field, port `8080`.

## Live PID tuning

To let the PID Tuner tab drive a real PIDF controller on the robot, register
a listener once (e.g. in `RobotHardware.init()`):

```java
TelemetryServer.getInstance().setPidListener((p, i, d, f) -> {
    arm.pid.kP = p;
    arm.pid.kI = i;
    arm.pid.kD = d;
    arm.pid.kF = f;
});
```

Every time you move a slider (or the tab's "Live push" is on), the tab
`POST`s to `/pid` and your listener gets called with the new values --
no redeploy needed while iterating.

## Hardware config sync

`GET /sync/hardware` and `POST /sync/hardware` let "Sync Hardware Config
with Robot" (sidebar Tools menu) read/write the JSON the Hardware Map
Editor stores as `.ftc/hardware.json`. The server just holds the last blob
it received in memory -- if you want the robot to actually validate device
names against it at init time, read `TelemetryServer.getInstance()`'s
stashed config yourself (there's no built-in parser on the robot side on
purpose, to avoid pulling in a JSON dependency you don't already have).

