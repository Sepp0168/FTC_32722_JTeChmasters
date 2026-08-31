package org.firstinspires.ftc.teamcode.util;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

public class MotorUtils {

    private int lastPosition = 0;
    private long lastTimeNs = 0;

    public float getMotorVelocityTicksPerSec(DcMotor motor) {
        if (motor == null) return 0f;

        if (motor instanceof DcMotorEx) {
            return (float) ((DcMotorEx) motor).getVelocity();
        }

        int currentPosition = motor.getCurrentPosition();
        long now = System.nanoTime();

        if (lastTimeNs == 0) {
            lastPosition = currentPosition;
            lastTimeNs = now;
            return 0f;
        }

        double dt = (now - lastTimeNs) / 1e9;
        if (dt <= 0) {
            return 0f;
        }

        double velocity = (currentPosition - lastPosition) / dt;

        lastPosition = currentPosition;
        lastTimeNs = now;

        return (float) velocity;
    }
}
