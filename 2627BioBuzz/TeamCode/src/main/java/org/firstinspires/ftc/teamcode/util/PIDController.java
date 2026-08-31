package org.firstinspires.ftc.teamcode.util;

/**
 * PIDF Controller for motor velocity and position control.
 */
public class PIDController {
    private double kP, kI, kD, kF;
    private double integralSum = 0;
    private double lastError = 0;
    private long lastTimeNs = 0;
    private double integralLimit = Double.MAX_VALUE; // anti-windup clamp
    private double minOutput = -1.0;
    private double maxOutput = 1.0;

    public PIDController(double kP, double kI, double kD, double kF) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        this.kF = kF;
    }

    public void setPIDF(double kP, double kI, double kD, double kF) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        this.kF = kF;
    }

    public void setIntegralLimit(double limit) {
        this.integralLimit = Math.abs(limit);
    }

    public void setOutputBounds(double min, double max) {
        this.minOutput = min;
        this.maxOutput = max;
    }

    public double calculate(double target, double current) {
        double error = target - current;
        long now = System.nanoTime();

        if (lastTimeNs == 0) {
            lastTimeNs = now;
            lastError = error;
            double ff = kF * Math.signum(target);
            double output = (kP * error) + ff;
            return Math.max(minOutput, Math.min(maxOutput, output));
        }

        double dt = (now - lastTimeNs) / 1e9;
        if (dt <= 0) {
            double ff = kF * Math.signum(target);
            double output = (kP * error) + (kI * integralSum) + ff;
            return Math.max(minOutput, Math.min(maxOutput, output));
        }

        lastTimeNs = now;

        integralSum += error * dt;
        integralSum = Math.max(-integralLimit, Math.min(integralLimit, integralSum));

        double derivative = (error - lastError) / dt;
        lastError = error;

        double feedforward = kF * Math.signum(target);
        double output = (kP * error) + (kI * integralSum) + (kD * derivative) + feedforward;

        return Math.max(minOutput, Math.min(maxOutput, output));
    }

    public void reset() {
        integralSum = 0;
        lastError = 0;
        lastTimeNs = 0;
    }
}