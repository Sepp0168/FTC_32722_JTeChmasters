package org.firstinspires.ftc.teamcode.robot.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.DcMotor;
import org.firstinspires.ftc.teamcode.config.HardwareNames;
import org.firstinspires.ftc.teamcode.robot.debug.TelemetryServer;
import org.firstinspires.ftc.teamcode.util.MotorUtils;
import org.firstinspires.ftc.teamcode.util.PIDController;

/**
 * TestMotor subsystem.
 *
 * Owns the hardware for this mechanism and exposes high-level control
 * methods. Call update() once per loop if this subsystem needs
 * continuous control (e.g. a PID loop).
 */
public class TestMotor {

    public static final double DEFAULT_TARGET_VELOCITY = 2000.0; // Ticks per second

    private final DcMotor testMotor;
    private final PIDController motorPID = new PIDController(0.01, 0.5, 0.001, 0.05);
    private final MotorUtils motorUtils = new MotorUtils();

    private double targetVelocity = 0.0;
    private boolean running = false;

    public TestMotor(HardwareMap hardwareMap) {
        testMotor = hardwareMap.get(DcMotor.class, HardwareNames.TEST_MOTOR);
        testMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        testMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // Register PID listener once during initialization
        TelemetryServer.getInstance().setPidListener((p, i, d, f) -> motorPID.setPIDF(p, i, d, f));
    }

    public void setTargetVelocity(double ticksPerSec) {
        this.targetVelocity = ticksPerSec;
        this.running = (ticksPerSec != 0.0);
        if (!running) {
            stop();
        }
    }

    public void forward() {
        setTargetVelocity(DEFAULT_TARGET_VELOCITY);
    }

    public void stop() {
        this.targetVelocity = 0.0;
        this.running = false;
        testMotor.setPower(0.0);
        motorPID.reset();
    }

    public double getTargetVelocity() {
        return targetVelocity;
    }

    public boolean isRunning() {
        return running;
    }

    /** Call this once per loop from the OpMode to update PID and telemetry. */
    public void update() {
        float currentVelocity = motorUtils.getMotorVelocityTicksPerSec(testMotor);

        if (running) {
            double power = motorPID.calculate(targetVelocity, currentVelocity);
            testMotor.setPower(power);
        }

        // Stream telemetry state to TelemetryServer for debugging / tuning
        TelemetryServer.getInstance().setMotorState(HardwareNames.TEST_MOTOR, testMotor.getCurrentPosition(), testMotor.getPower());
        TelemetryServer.getInstance().setVariable("testMotorTargetVel", targetVelocity);
        TelemetryServer.getInstance().setVariable("testMotorCurrentVel", currentVelocity);
    }
}
