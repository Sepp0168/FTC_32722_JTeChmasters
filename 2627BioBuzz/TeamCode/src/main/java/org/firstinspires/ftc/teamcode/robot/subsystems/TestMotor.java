package org.firstinspires.ftc.teamcode.robot.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.config.HardwareNames;
import org.firstinspires.ftc.teamcode.robot.debug.TelemetryServer;
import org.firstinspires.ftc.teamcode.util.PIDController;

public class TestMotor {

    private static final double MOTOR_TICKS_PER_REV = 288.0;
    private static final double GEAR_RATIO_1 = 5.0;
    private static final double GEAR_RATIO_2 = 4.0;

    private static final double TOTAL_GEAR_RATIO =
            GEAR_RATIO_1 * GEAR_RATIO_2;

    private static final double MECHANISM_TICKS_PER_REV =
            MOTOR_TICKS_PER_REV * TOTAL_GEAR_RATIO;

    private final DcMotor testMotor;

    private final PIDController motorPID =
            new PIDController(
                    0.002,
                    0.0,
                    0.0001,
                    0.0
            );

    private double targetAngle = 0.0;
    private boolean running = false;

    public TestMotor(HardwareMap hardwareMap) {
        testMotor = hardwareMap.get(
                DcMotor.class,
                HardwareNames.TEST_MOTOR
        );

        testMotor.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE
        );

        testMotor.setMode(
                DcMotor.RunMode.STOP_AND_RESET_ENCODER
        );

        testMotor.setMode(
                DcMotor.RunMode.RUN_WITHOUT_ENCODER
        );

        TelemetryServer.getInstance().setTargetedPidListener((target, p, i, d, f, setpoint) -> {
            if ("testMotor".equals(target)) {
                motorPID.setPIDF(p, i, d, f);
                if (setpoint != null) {
                    setTargetAngle(setpoint);
                }
            }
        });
    }

    public void setTargetAngle(double angle) {
        targetAngle = angle;
        running = true;
        motorPID.reset();
    }

    public double getTargetAngle() {
        return targetAngle;
    }

    public double getCurrentAngle() {
        return ticksToDegrees(
            testMotor.getCurrentPosition()
        );
    }

    private double degreesToTicks(double degrees) {
        return (degrees / 360.0) * MECHANISM_TICKS_PER_REV;
    }

    private double ticksToDegrees(double ticks) {
        return (ticks / MECHANISM_TICKS_PER_REV) * 360.0;
    }

    public void update() {
        double currentTicks = testMotor.getCurrentPosition();
        double targetTicks = degreesToTicks(targetAngle);

        if (running) {
            double power = motorPID.calculate(
                    targetTicks,
                    currentTicks
            );

            power = Math.max(-1.0, Math.min(1.0, power));

            testMotor.setPower(power);
        }

        TelemetryServer.getInstance().setVariable("pidTarget", targetAngle);
        TelemetryServer.getInstance().setVariable("pidActual", getCurrentAngle());

        TelemetryServer.getInstance().setMotorState(
                HardwareNames.TEST_MOTOR,
                testMotor.getCurrentPosition(),
                testMotor.getPower()
        );

        TelemetryServer.getInstance().setSubsystemTelemetry(
                "testMotor",
                "testMotorTargetAngle",
                targetAngle
        );

        TelemetryServer.getInstance().setSubsystemTelemetry(
                "testMotor",
                "testMotorCurrentAngle",
                getCurrentAngle()
        );

        TelemetryServer.getInstance().setSubsystemTelemetry(
                "testMotor",
                "testMotorTargetTicks",
                targetTicks
        );

        TelemetryServer.getInstance().setSubsystemTelemetry(
                "testMotor",
                "testMotorCurrentTicks",
                currentTicks
        );
    }

    public void stop() {
        running = false;
        testMotor.setPower(0.0);
        motorPID.reset();
    }

    public boolean isRunning() {
        return running;
    }

    public int getCurrentPosition() {
        return testMotor.getCurrentPosition();
    }

    public double getGearRatio() {
        return TOTAL_GEAR_RATIO;
    }

    public double getMechanismTicksPerRev() {
        return MECHANISM_TICKS_PER_REV;
    }
}
