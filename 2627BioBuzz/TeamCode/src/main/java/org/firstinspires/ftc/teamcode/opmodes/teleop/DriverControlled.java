package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.robot.debug.TelemetryServer;

import org.firstinspires.ftc.teamcode.robot.RobotHardware;

@TeleOp(name = "Driver Controlled")
public class DriverControlled extends LinearOpMode {

    private final RobotHardware robot = new RobotHardware(this);
    private final ElapsedTime stateTimer = new ElapsedTime();

    private enum TestState {
        MOTOR_RUNNING,
        MOTOR_STOPPED
    }

    private TestState currentState = TestState.MOTOR_STOPPED;

    @Override
    public void runOpMode() {
        robot.init();

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();

        stateTimer.reset();
        robot.testMotor.forward();
        currentState = TestState.MOTOR_RUNNING;
        TelemetryServer.getInstance().setCurrentTask("Testing motor");

        while (opModeIsActive()) {
            // Update all subsystems (runs PID loops and streams telemetry to TelemetryServer)
            robot.updateAll();

            // Handle manual gamepad input if driver presses buttons
            if (gamepad1.a) {
                robot.testMotor.forward();
                TelemetryServer.getInstance().setCurrentTask("Manual Forward (Gamepad A)");
            } else if (gamepad1.b) {
                robot.testMotor.stop();
                TelemetryServer.getInstance().setCurrentTask("Manual Stop (Gamepad B)");
            } else if (gamepad1.right_trigger > 0.05) {
                double targetVel = gamepad1.right_trigger * 2500.0;
                robot.testMotor.setTargetVelocity(targetVel);
                TelemetryServer.getInstance().setCurrentTask("Manual Speed Control (Trigger)");
            } else {
                // Non-blocking test sequence cycle (2s RUN / 2s STOP)
                if (currentState == TestState.MOTOR_RUNNING && stateTimer.seconds() >= 2.0) {
                    robot.testMotor.stop();
                    currentState = TestState.MOTOR_STOPPED;
                    stateTimer.reset();
                    TelemetryServer.getInstance().setCurrentTask("Motor Test Idle");
                } else if (currentState == TestState.MOTOR_STOPPED && stateTimer.seconds() >= 2.0) {
                    robot.testMotor.forward();
                    currentState = TestState.MOTOR_RUNNING;
                    stateTimer.reset();
                    TelemetryServer.getInstance().setCurrentTask("Testing motor (2000 ticks/sec)");
                }
            }

            // Driver Station Telemetry
            telemetry.addData("Task", TelemetryServer.getInstance().getLastHardwareConfigJson() != null ? "Sync Active" : "Running");
            telemetry.addData("Motor Target Vel", robot.testMotor.getTargetVelocity());
            telemetry.addData("Motor Running", robot.testMotor.isRunning());
            telemetry.update();
        }

        // Clean up when OpMode stops
        robot.testMotor.stop();
        TelemetryServer.getInstance().setCurrentTask("Stopped");
    }
}