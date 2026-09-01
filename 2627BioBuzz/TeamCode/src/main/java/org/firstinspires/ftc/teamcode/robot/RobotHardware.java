package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.teamcode.robot.subsystems.TestMotor;
import org.firstinspires.ftc.teamcode.robot.debug.TelemetryServer;

/**
 * Central access point for every robot subsystem.
 *
 * Generated and maintained by FTC Code Utils.
 */
public class RobotHardware {
    private final LinearOpMode opMode;

    public TestMotor testMotor;

    public RobotHardware(LinearOpMode opMode) {
        this.opMode = opMode;
    }

    public void init() {
        testMotor = new TestMotor(opMode.hardwareMap);
        TelemetryServer.getInstance().start(8000);
        TelemetryServer.getInstance().setOpModeName(opMode.getClass().getSimpleName());
    }

    public void updateAll() {
        testMotor.update();
    }
}
