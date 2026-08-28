package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.teamcode.robot.subsystems.Arm;
import org.firstinspires.ftc.teamcode.robot.subsystems.TestMotor;

/**
 * Central access point for every robot subsystem.
 *
 * Generated and maintained by FTC Code Utils.
 */
public class RobotHardware {
    private final LinearOpMode opMode;

    public Arm arm;
    public TestMotor testMotor;

    public RobotHardware(LinearOpMode opMode) {
        this.opMode = opMode;
    }

    public void init() {
        arm = new Arm(opMode.hardwareMap);
        testMotor = new TestMotor(opMode.hardwareMap);
    }

    public void updateAll() {
        arm.update();
        testMotor.update();
    }
}
