package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.robot.debug.TelemetryServer;
import org.firstinspires.ftc.teamcode.config.RobotConstants;

import org.firstinspires.ftc.teamcode.robot.RobotHardware;

@TeleOp(name = "Driver Controlled")
public class DriverControlled extends LinearOpMode {

    private final RobotHardware robot = new RobotHardware(this);
    private final ElapsedTime stateTimer = new ElapsedTime();

    @Override
    public void runOpMode() {
        robot.init();
        RobotConstants constants = new RobotConstants();

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();

        stateTimer.reset();
        TelemetryServer.getInstance().setStarted(true);
        
        while (opModeIsActive()) {
            // Update all subsystems (runs PID loops and streams telemetry to TelemetryServer)
            robot.updateAll();

            TelemetryServer.getInstance().setCurrentTask("Testing motor PID target angle");
        }

        // Clean up when OpMode stops
        robot.testMotor.stop();
        TelemetryServer.getInstance().setCurrentTask("Stopped");
    }
}