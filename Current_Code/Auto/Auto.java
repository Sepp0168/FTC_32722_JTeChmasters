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
        static final int TICKS_PER_REV = 28; // REV HD Hex Motor
        boolean QuietLaunch = false;
        static final int goal_x = 125;
        static final int goal_y = 100;

            
                
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
    
        if (deltaTime <= 0) return 50;
    
        double ticksPerSecond = (deltaPos * 1000.0) / deltaTime;
        double rps = ticksPerSecond / TICKS_PER_REV;
        return rps * 60.0;
    }


    public void shoot(int MinSpeed, int MaxSpeed) {
        for (int i = 0; i <3; i++) {
            motorLaunch.setPower(QuietLaunch ? 0.1 : 1); // speed up
            double launchSpeed = 0; 
            while (launchSpeed < (QuietLaunch ? 100 : MinSpeed) && opModeIsActive()) { // wait until: to speed
                launchSpeed = getLaunchRPM();
                sleep(100);
                telemetry.addData("Status", "Speeding");
                telemetry.addData("RMP", launchSpeed);
                telemetry.update();
            }
            while (launchSpeed > (QuietLaunch ? 100 : MaxSpeed) && opModeIsActive()) { // wait until: to speed
                motorLaunch.setPower(-0.05);
                launchSpeed = getLaunchRPM();
                sleep(100);
                telemetry.addData("Status", "Speeding");
                telemetry.addData("RMP", launchSpeed);
                telemetry.update();
            }
            motorLaunch.setPower(QuietLaunch ? 0.05 : 0.5);
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
        
        double launchSpeed = 150;
        motorLaunch.setPower(-0.05);
        while (launchSpeed > 100 && opModeIsActive()) {
                launchSpeed = getLaunchRPM();
                sleep(100);
                telemetry.addData("Status", "Slowing");
                telemetry.addData("RMP", launchSpeed);
                telemetry.update();
            }
        motorLaunch.setPower(0);
    }
    
    public void correct(speed) {
        boolean CorrectPos = false;
        while (!CorrectPos && opModeIsActive()) {
                HuskyLens.Block[] blocks = husky.blocks();
                HuskyLens.Block tag = getLargest(blocks);
                telemetry.addData("tag", tag);
                telemetry.update();
                if (tag != null && (tag.id == 5 || tag.id == 4)) {
                    if (tag.x > goal_x + 5) {
                        motorR.setPower(-0.2 * speed);
                        motorL.setPower(-0.2 * speed);
                    } else if (tag.x < goal_x - 5) {
                        motorR.setPower(0.5 * speed);
                        motorL.setPower(0.5 * speed);
                    } else {
                        if (tag.y < goal_y - 20) {
                            motorR.setPower(0.1 * speed);
                            motorL.setPower(-0.1 * speed);
                        } else if (tag.y > goal_y + 20) {
                            motorR.setPower(-0.1 * speed);
                            motorL.setPower(0.1 * speed);
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
                correct(1);
                motorR.setPower(0);
                motorL.setPower(0);
                correct(0.5);
                motorR.setPower(0);
                motorL.setPower(0);
                shoot();
        }
}