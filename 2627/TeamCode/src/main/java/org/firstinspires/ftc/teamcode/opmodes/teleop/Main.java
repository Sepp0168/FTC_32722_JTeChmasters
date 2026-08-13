package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.robot.RobotHardware;

@TeleOp(name = "Main")
public class Main extends LinearOpMode {
    RobotHardware robot = new RobotHardware(this);

    @Override
    public void runOpMode() {
        robot.init();
        waitForStart();

        boolean IsANoobcom = false;

        while (opModeIsActive()) {
            // TODO: driver-controlled logic
            robot.updateAll();
        }
    }
}
