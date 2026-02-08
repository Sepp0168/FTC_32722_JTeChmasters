package org.firstinspires.ftc.teamcode;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.hardware.dfrobot.HuskyLens;

@Autonomous(name = "Auto_leave")
public class Auto_Copy extends LinearOpMode{
        private DcMotor motorL;
        private DcMotor motorR;
        private DcMotor motorLaunch;
        private DcMotor motorIntake;
        private Servo ServoHusky;
        private Servo ServoBall;
        private HuskyLens husky;
        private HuskyLens.Block[] blocks;
        private HuskyLens.Block tag;
        private double goal_x = 90;
        private double goal_y = 100;
        private int GoalLossCount;
        private int lastPos = 0;
        private long lastTime = 0;
        private long startTime;
        private double launchSpeed;
        private double angle = 0.7;
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
            launchSpeed = getLaunchRPM();
            while (launchSpeed > (MaxSpeed) && opModeIsActive()) { // wait until: to speed
                launchSpeed = getLaunchRPM();
                motorLaunch.setPower(-0.05);
                telemetry.addData("Status", "Slowing");
                telemetry.addData("RMP", launchSpeed);
                telemetry.update();
                if (detectShootError(MinSpeed, MaxSpeed, launchSpeed)) {
                    return;
                }
            }
            motorLaunch.setPower(LaunchMode == 0 ? 0.1 : 1);
            while (launchSpeed < (MinSpeed) && opModeIsActive()) { // wait until: to speed
                launchSpeed = getLaunchRPM();
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
            
            motorLaunch.setPower(LaunchMode == 0 ? 0.075 : 1);
            telemetry.addData("Status", "ToSpeed");
            telemetry.update();
            motorIntake.setPower(-1);
            sleep(200);
            if (i == 2) {
                ServoBall.setPosition(0);
                sleep(700);
            }
            ServoBall.setPosition(1);
            motorIntake.setPower(0);
            sleep(500);
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
        motorLaunch.setPower(0);
        if (launchSpeed == 0) {
                motorLaunch.setPower(0);
                telemetry.addData("Status", "Failed launch!");
                telemetry.update();
                sleep(3000);
                return;
        }
    }
    public HuskyLens.Block getLargest(HuskyLens.Block[] blocks, boolean elemination) {
                if (blocks == null || blocks.length == 0) {
                    return null; // geen blocks gezien
                }
        
                HuskyLens.Block largest = null;
                int largestSize = 0;
        
                for (int i = 0; i < blocks.length; i++) {
                    int size = blocks[i].width * blocks[i].height;
                    float proportion = blocks[i].width / (float)(blocks[i].height);
                    //telemetry.addData("width", blocks[i].width);
                    //telemetry.addData("height", blocks[i].height);
                    //telemetry.addData("proportion", proportion);
                    //telemetry.update();
                    if (elemination ? (proportion < 1.4 && proportion > 0.6) : true) {
                        if (size > largestSize && size > 20) {
                            largest = blocks[i];
                            largestSize = size;
                        }
                    }
                }
        
                return largest;
            }
    
    public void correct(double speed, double distanceMult) {
        double speedSt = 0.1;
        husky.selectAlgorithm(HuskyLens.Algorithm.TAG_RECOGNITION);
    
        HuskyLens.Block[] blocks;
        HuskyLens.Block tag = null;
        HuskyLens.Block lastTag = null;
    
        boolean CorrectPos = false;
    
        while (!CorrectPos && opModeIsActive()) {
            blocks = husky.blocks();
            tag = getLargest(blocks, false);
    
            if (tag != null) {
                lastTag = tag;
            }
    
            telemetry.addData("tag", tag);
            telemetry.update();
    
            if (tag != null && (tag.id == 4 || tag.id == 5)) {
    
                GoalLossCount = 0;
    
                /* CAMERA CORRECTION PHASE */
                if (angle < 0.7) {
    
                    motorR.setPower(0.5);
                    motorL.setPower(0.5);
                    sleep(450);
                    motorR.setPower(0);
                    motorL.setPower(0);
    
                    angle = Math.min(angle + 0.15, 0.7);
                    ServoHusky.setPosition(angle);
    
                    sleep(50);
    
                    blocks = husky.blocks();
                    tag = getLargest(blocks, false);
    
                    while (tag.x > 155 && tag.x < 165 &&
                           angle < 0.7 &&
                           opModeIsActive()) {
                        blocks = husky.blocks();
                        tag = getLargest(blocks, false);
                        if (tag.x < 155) {
                            angle += 0.05;
                        } else if (tag.x > 165) {
                            angle -= 0.05;
                        }
                        motorR.setPower(0);
                        motorL.setPower(0);
    
                        angle = Math.max(0.4, Math.min(angle, 0.7));
                        ServoHusky.setPosition(angle);
                        
                        if (tag != null){
                            tag = getLargest(husky.blocks(), false);
                            motorR.setPower(0);
                            motorL.setPower(0);
                        } else {
                            if (lastTag.x > 200) {
                                motorR.setPower(-0.25 * speed);
                                motorL.setPower(-0.25 * speed); 
                            } else if (lastTag.x < 40) {
                                motorR.setPower(0.25 * speed);
                                motorL.setPower(0.25 * speed);
                            } else if (lastTag.y < 40) {
                                motorR.setPower(0.5 * speed);
                                motorL.setPower(-0.5 * speed);
                            } else if (lastTag.y > 180) {
                                motorR.setPower(-0.5 * speed);
                                motorL.setPower(0.5 * speed);
                            } 
                        }
                    }
    
                }
                /* POSITION CORRECTION PHASE */
                else {
    
                    if (tag.x > goal_x + 5) {
                        motorR.setPower(-0.5 * speed);
                        motorL.setPower(-0.5 * speed);
                    } 
                    else if (tag.x < goal_x - 5) {
                        motorR.setPower(0.5 * speed);
                        motorL.setPower(0.5 * speed);
                    } 
                    else {
                        if (tag.y > goal_y + 20) {
                            motorR.setPower(-0.1 * speed);
                            motorL.setPower(0.1 * speed);
                        } 
                        else if (tag.y < goal_y - 20) {
                            motorR.setPower(0.1 * speed);
                            motorL.setPower(-0.1 * speed);
                        } 
                        else {
                            motorR.setPower(0);
                            motorL.setPower(0);
                            CorrectPos = true;
                        }
                    }
                }
            }
            /* TAG LOST */
            else {
    
                GoalLossCount++;
                telemetry.addData("GoalLossCount", GoalLossCount);
                telemetry.addData("status", "Searching");
                telemetry.addData("last", lastTag);
                telemetry.update();
                
                if (lastTag == null) {
                    if (angle >= 0.7) {
                        motorR.setPower(speedSt * speed);
                        motorL.setPower(-speedSt * speed);
                        sleep(400);
                        motorR.setPower(0);
                        motorL.setPower(0);
                    }
                    ServoHusky.setPosition(angle);
                    angle = Math.min(angle + 0.15, 0.7);
                    sleep(450);
                }
                if (GoalLossCount > 20) {
                    speedSt = 0.5;
                } else {
                    speedSt = 0.25;
                }
    
                if (GoalLossCount >= 10 && lastTag != null) {
    
                    motorR.setPower(0);
                    motorL.setPower(0);
                    if (angle >= 0.7) {
                        angle = 0.3;
                        if (lastTag.x > 200) {
                            motorR.setPower(-speedSt * speed);
                            motorL.setPower(-speedSt * speed); 
                        } else if (lastTag.y < 40) {
                            motorR.setPower(speedSt * speed);
                            motorL.setPower(-speedSt * speed);
                        } else {
                            motorR.setPower(-speedSt * speed);
                            motorL.setPower(speedSt * speed);
                        } 
                        sleep(300);
                        motorR.setPower(0);
                        motorL.setPower(0);
                    }
    
                    ServoHusky.setPosition(angle);
                    angle = Math.min(angle + 0.15, 0.7);
                    sleep(450);
                }
    
                sleep(50);
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
                
                motorR.setPower(0.5);
                motorL.setPower(0.5);
                sleep(500);
                motorR.setPower(0);
                motorL.setPower(0);
        }
}
