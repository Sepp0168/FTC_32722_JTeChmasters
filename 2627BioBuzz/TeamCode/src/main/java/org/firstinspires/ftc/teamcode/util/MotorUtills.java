package org.firstinspires.ftc.teamcode.util;

import com.qualcomm.robotcore.hardware.DcMotor;

public class MotorUtills {

    private int lastPosition = 0;
    private long lastTimeNs = 0;

    public float getMotorVelocityTicksPerSec(DcMotor motor) {
        int currentPosition = motor.getCurrentPosition();
        long now = System.nanoTime();

        double dt = lastTimeNs == 0 ? 0 : (now - lastTimeNs) / 1e9;
        double velocity = dt > 0 ? (currentPosition - lastPosition) / dt : 0;

        lastPosition = currentPosition;
        lastTimeNs = now;

        return (float) velocity; // explicit cast fixes the lossy-conversion error too
    }
}