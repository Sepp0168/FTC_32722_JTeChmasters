package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.teamcode.robot.subsystems.Arm;
import org.firstinspires.ftc.teamcode.robot.subsystems.Drive;

/**
 * Central access point for every robot subsystem.
 *
 * Generated and maintained by FTC Code Utils.
 */
public class RobotHardware {
    private final LinearOpMode opMode;

    public Arm arm;
    public Drive drive;

    public RobotHardware(LinearOpMode opMode) {
        this.opMode = opMode;
    }

    public void init() {
        arm = new Arm(opMode.hardwareMap);
        drive = new Drive(opMode.hardwareMap);
    }

    public void updateAll() {
        arm.update();
        drive.update();
    }
}
