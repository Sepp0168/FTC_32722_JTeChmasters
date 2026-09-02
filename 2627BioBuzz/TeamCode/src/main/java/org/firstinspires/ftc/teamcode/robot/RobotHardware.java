package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import org.firstinspires.ftc.teamcode.robot.debug.TelemetryServer;
import org.firstinspires.ftc.teamcode.robot.subsystems.TestMotor;

/**
 * Central access point for every robot subsystem.
 *
 * Generated and maintained by FTC Code Utils. Starts TelemetryServer on
 * port 8000 and automatically publishes battery/gamepad state for the
 * Live Telemetry & Robot Debugger dashboard.
 */
public class RobotHardware {
    private static final int TELEMETRY_SERVER_PORT = 8000;

    private final LinearOpMode opMode;
    private VoltageSensor voltageSensor;

    public TestMotor testMotor;

    public RobotHardware(LinearOpMode opMode) {
        this.opMode = opMode;
    }

    public void init() {
        testMotor = new TestMotor(opMode.hardwareMap);

        TelemetryServer.getInstance().start(TELEMETRY_SERVER_PORT);
        TelemetryServer.getInstance().setOpModeName(opMode.getClass().getSimpleName());

        for (VoltageSensor sensor : opMode.hardwareMap.voltageSensor) {
            voltageSensor = sensor;
            break;
        }
    }

    public void updateAll() {
        testMotor.update();

        if (voltageSensor != null) {
            TelemetryServer.getInstance().setBatteryVoltage(voltageSensor.getVoltage());
        }
        TelemetryServer.getInstance().setRuntimeSeconds(opMode.getRuntime());
        TelemetryServer.getInstance().setGamepadState("gamepad1", opMode.gamepad1);
        TelemetryServer.getInstance().setGamepadState("gamepad2", opMode.gamepad2);
    }
}
