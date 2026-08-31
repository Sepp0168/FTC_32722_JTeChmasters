package org.firstinspires.ftc.teamcode.util;

public class PIDController {
    private double kP, kI, kD, kF;
    private double integralSum = 0;
    private double lastError = 0;
    private long lastTimeNs = 0;
    private double integralLimit = Double.MAX_VALUE; // anti-windup clamp

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
        this.integralLimit = limit;
    }

    public double calculate(double target, double current) {
        double error = target - current;

        long now = System.nanoTime();
        double dt = lastTimeNs == 0 ? 0 : (now - lastTimeNs) / 1e9;
        lastTimeNs = now;

        integralSum += error * dt;
        integralSum = Math.max(-integralLimit, Math.min(integralLimit, integralSum));

        double derivative = dt > 0 ? (error - lastError) / dt : 0;
        lastError = error;

        return (kP * error) + (kI * integralSum) + (kD * derivative) + kF;
    }

    public void reset() {
        integralSum = 0;
        lastError = 0;
        lastTimeNs = 0;
    }
}