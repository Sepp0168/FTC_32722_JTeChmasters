package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robot.debug.TelemetryServer;

import org.firstinspires.ftc.teamcode.robot.RobotHardware;

@TeleOp(name = "Driver Controlled")
public class DriverControlled extends LinearOpMode {
    RobotHardware robot = new RobotHardware(this);

    @Override
    public void runOpMode() {
        TelemetryServer.getInstance().start(8000);
        TelemetryServer.getInstance().setOpModeName(robot.getClass().getSimpleName());
        TelemetryServer.getInstance().setCurrentTask("Waiting for start");
        robot.init();
        // TODO: add any additional initialization logic here
        waitForStart();

        while (opModeIsActive()) {
            robot.updateAll();
            TelemetryServer.getInstance().setCurrentTask("Starting motor test");
            robot.testMotor.forward();
            TelemetryServer.getInstance().setCurrentTask("Testing motor");
            sleep(500);
            TelemetryServer.getInstance().setCurrentTask("Stopping motor test");
            robot.testMotor.stop();
            TelemetryServer.getInstance().setCurrentTask("Stopped motor test");
            sleep(1000);
        }
    }
}