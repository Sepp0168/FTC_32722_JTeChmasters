package org.firstinspires.ftc.teamcode;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.hardware.dfrobot.HuskyLens;

@Autonomous(name = "Auto")
public class Auto extends LinearOpMode{
        private DcMotor motorL;
        private DcMotor motorR;
        private DcMotor motorLaunch;
        private DcMotor motorIntake;
        private Servo ServoHusky;
        private Servo ServoBall;
        private HuskyLens husky;
        private HuskyLens.Block[] blocks;
        private HuskyLens.Block tag;
        private int lastPos = 0;
        private long lastTime = 0;
        private long startTime;
        private double launchSpeed;
        static final int TICKS_PER_REV = 28; // REV HD Hex Motor
        int LaunchMode = 1;
            
                
        public HuskyLens.Block getLargest(HuskyLens.Block[] blocks) {
                if (blocks == null || blocks.length == 0) {
                    return null; // geen blocks gezien
                }
        
                HuskyLens.Block largest = blocks[0];
                int largestSize = largest.width * largest.height;
        
                for (int i = 1; i < blocks.length; i++) {
                    int size = blocks[i].width * blocks[i].height;
        
                    if (size > largestSize) {
                        largest = blocks[i];
                        largestSize = size;
                    }
                }
        
                return largest;
            }
            
    public double getLaunchRPM() {
        int currentPos = motorLaunch.getCurrentPosition();
        long currentTime = System.currentTimeMillis();
    
        int deltaPos = currentPos - lastPos;
        long deltaTime = currentTime - lastTime;
        
        lastPos = currentPos;
        lastTime = currentTime;
    
        if (deltaTime <= 1) return 50;
    
        double ticksPerSecond = (deltaPos * 1000.0) / deltaTime;
        double rps = ticksPerSecond / TICKS_PER_REV;
        return rps * 60.0;
    }
    
    public boolean detectShootError(int MinSpeed, int MaxSpeed, double Speed) {
        sleep(50);
        if ((System.currentTimeMillis() > startTime + (LaunchMode == 0 ? 2500 : 500) && Speed == 0) || (System.currentTimeMillis() > startTime + (LaunchMode >= 2 ? 50000 : 5000))) {
            motorLaunch.setPower(0);
            telemetry.addData("Status", "Failed launch!");
            if ((System.currentTimeMillis() > startTime + 5000)) {
                telemetry.addData("Main reason", "Time-out");
                telemetry.addData("Possible cause", "Battery low, please charge");
            } else {
                telemetry.addData("Main reason", "Flywheel is not rotating");
                telemetry.addData("Possible cause", "Ball is stuck, please unstuck");
            }
            telemetry.addData("More info", "\n  Speed:                   %s RPM  \n  Start time:            %s ms   \n  Current time:         %s ms (%s)  \n  Min/Max:              %s, %s RPM", launchSpeed, startTime, System.currentTimeMillis(), (startTime - System.currentTimeMillis()), MinSpeed, MaxSpeed);
            telemetry.update();
            while (!(gamepad1.ps || gamepad2.ps) && opModeIsActive()) {
                sleep(100);
            }
            return true;
        }
        return false;
    }

    public void shoot(int MinSpeed, int MaxSpeed) {
        startTime = System.currentTimeMillis();
        for (int i = 0; i <3; i++) {
            motorLaunch.setPower(LaunchMode == 0 ? 0.1 : 1); // speed up
            launchSpeed = 20; 
            while (launchSpeed < (MinSpeed) && opModeIsActive()) { // wait until: to speed
                launchSpeed = getLaunchRPM();
                telemetry.addData("Status", "Speeding");
                telemetry.addData("RMP", launchSpeed);
                telemetry.update();
                if (detectShootError(MinSpeed, MaxSpeed, launchSpeed)) {
                    return;
                }
            }
            while (launchSpeed > (MaxSpeed) && opModeIsActive()) { // wait until: to speed
                launchSpeed = getLaunchRPM();
                motorLaunch.setPower(-0.05);
                telemetry.addData("Status", "Speeding");
                telemetry.addData("RMP", launchSpeed);
                telemetry.update();
                if (detectShootError(MinSpeed, MaxSpeed, launchSpeed)) {
                    return;
                }
            }
            
            if (detectShootError(MinSpeed, MaxSpeed, launchSpeed)) {
                    return;
            }
            
            motorLaunch.setPower(LaunchMode == 0 ? 0.075 : 0.5);
            telemetry.addData("Status", "ToSpeed");
            telemetry.update();
            motorIntake.setPower(-0.2);
            sleep(200);
            if (i == 2) {
                ServoBall.setPosition(0);
                sleep(700);
            }
            ServoBall.setPosition(1);
            motorIntake.setPower(0);
        }
        launchSpeed = 150;
        motorLaunch.setPower(-0.05);
        while (launchSpeed > 100 && opModeIsActive()) {
                launchSpeed = getLaunchRPM();
                sleep(100);
                telemetry.addData("Status", "Slowing");
                telemetry.addData("RMP", launchSpeed);
                telemetry.update();
            }
        startTime = System.currentTimeMillis() - 500;
        if (detectShootError(MinSpeed, MaxSpeed, launchSpeed)) {
            return;
        }
    }
    
    public void correct() {
        boolean CorrectPos = false;
        while (!CorrectPos && opModeIsActive()) {
                HuskyLens.Block[] blocks = husky.blocks();
                HuskyLens.Block tag = getLargest(blocks);
                telemetry.addData("tag", tag);
                telemetry.update();
                if (tag != null) {
                    if (tag.x > 130) {
                        motorR.setPower(-0.2);
                        motorL.setPower(-0.2);
                    } else if (tag.x < 120) {
                        motorR.setPower(0.5);
                        motorL.setPower(0.5);
                    } else {
                        if (tag.y < 80) {
                            motorR.setPower(0.1);
                            motorL.setPower(-0.1);
                        } else if (tag.y > 120) {
                            motorR.setPower(-0.1);
                            motorL.setPower(0.1);
                        } else {
                            motorR.setPower(0);
                            motorL.setPower(0);
                            CorrectPos = true;
                        }
                    }
                } else {
                    motorR.setPower(0);
                    motorL.setPower(0);
                }
        } 
    }
        
        public void runOpMode() {
                motorL = hardwareMap.get(DcMotor.class, "Left_Drive_Motor");
                motorL.setDirection(DcMotor.Direction.FORWARD);
                motorL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                motorL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        
                motorR = hardwareMap.get(DcMotor.class, "Right_Drive_Motor");
                motorR.setDirection(DcMotor.Direction.REVERSE);
                motorR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                motorR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            
                motorLaunch = hardwareMap.get(DcMotor.class, "LaunchMotor");
                motorLaunch.setDirection(DcMotor.Direction.FORWARD);
                motorLaunch.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                motorLaunch.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                
                motorIntake = hardwareMap.get(DcMotor.class, "IntakeMotor");
                motorIntake.setDirection(DcMotor.Direction.FORWARD);
                motorIntake.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                motorIntake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                
                ServoHusky = hardwareMap.get(Servo.class, "ServoHusky");
                husky = hardwareMap.get(HuskyLens.class, "huskylens");
                husky.selectAlgorithm(HuskyLens.Algorithm.TAG_RECOGNITION);
                ServoBall = hardwareMap.get(Servo.class, "ServoBall");
                
                ServoHusky.setPosition(0.7);
                
                waitForStart();
                
                motorR.setPower(-0.5);
                motorL.setPower(-0.5);
                motorLaunch.setPower(0.1);
                ServoHusky.setPosition(0.7);
                sleep(1000);
                motorR.setPower(0);
                motorL.setPower(0);
                correct();
                shoot(1000, 1250);
        }
}