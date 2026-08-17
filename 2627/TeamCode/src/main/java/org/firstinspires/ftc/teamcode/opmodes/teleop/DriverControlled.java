package org.firstinspires.ftc.teamcode.opmodes.teleop;








import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.robot.RobotHardware;

@TeleOp(name = "Driver Controlled")
public class DriverControlled extends LinearOpMode {
    RobotHardware robot = new RobotHardware(this);



    @Override
    public void runOpMode() {
        robot.init();
        // TODO: add any additional initialization logic here
        waitForStart();

        while (opModeIsActive()) {
            // TODO: driver-controlled logic
            robot.updateAll();
            if (gamepad1.a) {
                // do nothing
            }
        }
    }
}
