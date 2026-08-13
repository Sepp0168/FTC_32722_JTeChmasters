package org.firstinspires.ftc.teamcode;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.hardware.dfrobot.HuskyLens;

@Autonomous(name = "🚨 !DEMO! 🚨 ||| NOT FOR COMP USE")
public class Demo extends LinearOpMode{
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
        private double SpeedMult = 0.5;
        private HuskyLens.Block ball;
        boolean IntakeOn = false;
        double angle = 0.7;
        static final int goal_distance = 1;
        static final int goal_y = 100;
        static final int goal_x = 125;
        private int GoalLossCount = 0;
                
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
                motorLaunch.setPower(0);
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
    
    public void correct(double speed) {
        husky.selectAlgorithm(HuskyLens.Algorithm.TAG_RECOGNITION);
        boolean CorrectPos = false;
        while (!CorrectPos && opModeIsActive()) {
                HuskyLens.Block[] blocks = husky.blocks();
                HuskyLens.Block tag = getLargest(blocks);
                telemetry.addData("tag", tag);
                telemetry.update();
                if (tag != null && (tag.id == 5 || tag.id == 4)) {
                    GoalLossCount = 0;
                    if (angle < 0.7) {
                        motorR.setPower(0.5);
                        motorL.setPower(0.5);
                        sleep(450);
                        motorR.setPower(0);
                        motorL.setPower(0);
                        angle += 0.15;
                        ServoHusky.setPosition(angle);
                    } else {
                        if (tag.y < goal_y - 20) {
                            motorR.setPower(0.1 * speed);
                            motorL.setPower(-0.1 * speed);
                        } else if (tag.y > goal_y + 20) {
                            motorR.setPower(-0.1 * speed);
                            motorL.setPower(0.1 * speed);
                        } else {
                            if (tag.x > goal_x + 5) {
                                motorR.setPower(-0.2 * speed);
                                motorL.setPower(-0.2 * speed);
                            } else if (tag.x < goal_x - 5) {
                                motorR.setPower(0.5 * speed);
                                motorL.setPower(0.5 * speed);
                            } else {
                                motorR.setPower(0);
                                motorL.setPower(0);
                                CorrectPos = true;
                            }
                        }
                    }
                } else {
                    GoalLossCount++;
                    telemetry.addData("GoalLossCount", GoalLossCount);
                    telemetry.update();
                    if (GoalLossCount >= 10) {
                        motorR.setPower(0);
                        motorL.setPower(0);
                        if (angle >= 0.7) {
                            angle = 0.3;
                            motorR.setPower(-0.3);
                            motorL.setPower(0.3);
                            sleep(300);
                            motorR.setPower(0);
                            motorL.setPower(0);
                        }
                        ServoHusky.setPosition(angle);
                        angle += 0.15;
                        sleep(450);
                    }
                    sleep(50);
                }
        } 
    }
    
    public void goToBall() {
        husky.selectAlgorithm(HuskyLens.Algorithm.COLOR_RECOGNITION);
        motorL.setPower(0);
        motorR.setPower(0);

        // blijf updaten totdat we dichtbij genoeg zijn of geen object meer zien
        while (opModeIsActive()) {
            HuskyLens.Block[] blocks = husky.blocks();
            ball = getLargest(blocks);

            if (ball == null) {
                telemetry.addData("Husky", "Geen object gevonden");
                telemetry.update();
                motorL.setPower(0.1);
                motorR.setPower(-0.1);
            } else {

                // als de y (hoogte) criterium een stopvoorwaarde is, kun je die hier gebruiken.
                // hier gebruiken we x om te sturen (horizontale offset)
                telemetry.addData("Ball X", ball.x);
                telemetry.addData("Ball Y", ball.y);
                telemetry.update();
    
                // stuurcorrectie op basis van y
                if (ball.y < 80) { // links
                    motorL.setPower(0.5 * SpeedMult);
                    motorR.setPower(0.7 * SpeedMult);
                } else if (ball.y > 120) { // rechts
                    motorL.setPower(0.7 * SpeedMult);
                    motorR.setPower(0.5 * SpeedMult);
                } else { // recht vooruit
                    motorL.setPower(0.7 * SpeedMult);
                    motorR.setPower(0.7 * SpeedMult);
                }
    
                // update frequentie klein houden
                sleep(50);
    
                // stopconditie: dichterbij (voorbeeld: y > threshold)
                if (ball.x < 20) { // pas aan naar jouw camera/calibratie
                    IntakeOn = false;
                    ballInput();
                    break;
                }
            }
        }

        // korte finale vooruitstoot en stop
        motorL.setPower(1);
        motorR.setPower(-1);
        sleep(200);
        motorL.setPower(1 * SpeedMult);
        motorR.setPower(0.25 * SpeedMult);
        sleep((int)(500 / SpeedMult)); // cast naar int
        motorL.setPower(-0.5);
        motorR.setPower(-0.7);
        sleep(500);
        motorL.setPower(1);
        motorR.setPower(0.75);
        sleep(250);
        motorL.setPower(0);
        motorR.setPower(0);
        sleep(1000);
    }

    public void ballInput() {
        if (!IntakeOn) {
            ServoHusky.setPosition(0.25);
            motorIntake.setPower(-0.5);
        } else {
            ServoHusky.setPosition(0.4);
            motorIntake.setPower(0);
        }
        IntakeOn = !IntakeOn;
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
                
                ServoHusky.setPosition(0.5);
                
                waitForStart();
                
                motorR.setPower(-0.5);
                motorL.setPower(-0.5);
                
                while (opModeIsActive()) {
                    motorLaunch.setPower(0);
                    ServoHusky.setPosition(0.7);
                    sleep(1500);
                    motorR.setPower(0);
                    motorL.setPower(0);
                    correct(1.5);
                    ballInput();
                    shoot(750, 1000);
                    ServoHusky.setPosition(0.4);
                    motorR.setPower(-0.5);
                    motorL.setPower(0.5);
                    motorLaunch.setPower(0);
                    sleep(400);
                    goToBall();
                    motorR.setPower(-0.5);
                    motorL.setPower(0.5);
                    sleep(200);
                }
        }
}