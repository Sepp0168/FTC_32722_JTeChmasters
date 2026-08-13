package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.teamcode.robot.subsystems.Arm;
import org.firstinspires.ftc.teamcode.robot.subsystems.Drive;
import org.firstinspires.ftc.teamcode.robot.subsystems.Intake;

/**
 * Central access point for every robot subsystem.
 *
 * Generated and maintained by FTC Code Utils.
 */
public class RobotHardware {
    private final LinearOpMode opMode;

    public Arm arm;
    public Drive drive;
    public Intake intake;

    public RobotHardware(LinearOpMode opMode) {
        this.opMode = opMode;
    }

    public void init() {
        arm = new Arm(opMode.hardwareMap);
        drive = new Drive(opMode.hardwareMap);
        intake = new Intake(opMode.hardwareMap);
    }

    public void updateAll() {
        arm.update();
        drive.update();
        intake.update();
    }
}
