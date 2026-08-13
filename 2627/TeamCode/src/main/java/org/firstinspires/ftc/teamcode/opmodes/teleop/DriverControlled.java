package org.firstinspires.ftc.teamcode.opmodes.teleop;



import org.firstinspires.ftc.teamcode.robot.subsystems.Drive;
import org.firstinspires.ftc.teamcode.robot.subsystems.Arm;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.robot.RobotHardware;

@TeleOp(name = "Driver Controlled")
public class DriverControlled extends LinearOpMode {
    RobotHardware robot = new RobotHardware(this);
    public Drive drive;

    public Arm arm;


    @Override
    public void runOpMode() {
        robot.init();
        drive = robot.drive;

        arm = robot.arm;

        waitForStart();

        boolean helloThereImSeppieweppie;

        while (opModeIsActive()) {
            // TODO: driver-controlled logic
            robot.updateAll();
        }
    }
}
