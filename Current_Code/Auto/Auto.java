package org.firstinspires.ftc.teamcode;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.hardware.dfrobot.HuskyLens;

@Autonomous(name = "Auto")                      // name for the program
public class Auto extends LinearOpMode{
        // Define variables for the program
        private DcMotor motorL;                 // left movement motor object
        private DcMotor motorR;                 // right movement motor object
        private DcMotor motorLaunch;            // launch power motor object
        private DcMotor motorIntake;            // intake power motor object
        private Servo ServoHusky;               // huskylens titling sevro object
        private Servo ServoBall;                // ball pusher servo object
        private HuskyLens husky;                // huskylens object
        private HuskyLens.Block[] blocks;       // huskylens detected objects
        private HuskyLens.Block tag;            // huskylens detected tag
        private double goal_x = 90;             // auto correct huskylens callibration
        private double goal_y = 100;            // auto correct huskylens callibration
        private int GoalLossCount;              // counts amount of times the robot lost count of the goal tag
        private int lastPos = 0;                // tracks last position of motorLaunch, used by getLaunchRPM()
        private long lastTime = 0;              // tracks time the position of motorLaunch was updated, used by getLaunchRPM()
        private long startTime;                 // tracks the starting time from the moment the flywheel strarts speeding up
        private double launchSpeed;             // tracks the last know speed of the flywheel
        private double angle = 0.7;             // tracks the current angle of the ServoHusky
        static final int TICKS_PER_REV = 28;    // amount of ticks for one full rotation of motorLaunch
        static final int LaunchMode = 1;        // the mode being used to launch, aka launch speed       
            
    // get the launch rotation speed of the launch
    public double getLaunchRPM() {
        int currentPos = motorLaunch.getCurrentPosition();          // update the current position of motorLaunch
        long currentTime = System.currentTimeMillis();              // update the current running time
    
        int deltaPos = currentPos - lastPos;                        // get the position change since the last update
        long deltaTime = currentTime - lastTime;                    // get the time change since the last update 
        
        lastPos = currentPos;                                       // reset the last know position to the new position
        lastTime = currentTime;                                     // reset the last time to the new time
    
        if (deltaTime <= 1) return 50;                              // if the last time since update is to low for a acurite reading, just return 50 to be save
        
        // calculate and return the RPM
        double ticksPerSecond = (deltaPos * 1000.0) / deltaTime;
        double rps = ticksPerSecond / TICKS_PER_REV;
        return rps * 60.0;
    }
    
    // detect a shooting error
    public boolean detectShootError(int MinSpeed, int MaxSpeed, double Speed) {
        sleep(50);                                                                      // wait a moment 
        // if the flywheel is not spinning fast enough in a fixed time
        if ((System.currentTimeMillis() > startTime + (LaunchMode == 0 ? 2500 : 500) && Speed == 0) || (System.currentTimeMillis() > startTime + (LaunchMode >= 2 ? 50000 : 5000))) {
            motorLaunch.setPower(0);                                                    // abourt the launch
            telemetry.addData("Status", "Failed launch!");                              // add text to the screen
            if ((System.currentTimeMillis() > startTime + 5000)) {                      // if the flywheel didn't get to speed in a fixed time; 5 second for normal launch, 50 seconds for overdrive/max launch
                // add more information
                telemetry.addData("Main reason", "Time-out");
                telemetry.addData("Possible cause", "Battery low, please charge");
            } else {
                // add more information
                telemetry.addData("Main reason", "Flywheel is not rotating");
                telemetry.addData("Possible cause", "Ball is stuck, please unstuck");
            }
            // add even more infortmation
            telemetry.addData("More info", "\n  Speed:                   %s RPM  \n  Start time:            %s ms   \n  Current time:         %s ms (%s)  \n  Min/Max:              %s, %s RPM", launchSpeed, startTime, System.currentTimeMillis(), (startTime - System.currentTimeMillis()), MinSpeed, MaxSpeed);
            telemetry.update();                                             // update the screen so the text appears
            while (!(gamepad1.ps || gamepad2.ps) && opModeIsActive()) {     // wait until human confirm to continue
                sleep(100);                                                 // small wait to avoid update to much
            }
            return true;                                                    // return true if it should stop the shoot sequence
        }
        return false;                                                       // return false if it shouldn't stop the shoot sequence
    }

    // the main shoot sequence
    public void shoot(int MinSpeed, int MaxSpeed) {
        // for 3 balls, repeat
        for (int i = 0; i <3; i++) {
            startTime = System.currentTimeMillis();                                         // update the starting time
            motorLaunch.setPower(LaunchMode == 0 ? 0.1 : 1);                                // speed up acourding to launch mode; 1% power if quiet mode, 100% power if not quiet mode
            launchSpeed = getLaunchRPM();                                                   // update/get current launch speed
            while (launchSpeed > (MaxSpeed) && opModeIsActive()) {                          // check if the current launch speed exceeds the maximum speed, if so, wait until it is under
                launchSpeed = getLaunchRPM();                                               // update the launch speed
                motorLaunch.setPower(-0.05);                                                // make sure the motor is slowing the flywheel down
                telemetry.addData("Status", "Slowing");                                     // add text to the screen
                telemetry.addData("RMP", launchSpeed);                                      // add text to the screen
                telemetry.update();                                                         // update the screen so the text appears
                if (detectShootError(MinSpeed, MaxSpeed, launchSpeed)) {                    // check for a shooting error, if so, stop the sequence
                    return;                                                                 // stop the sequence
                }  
            }
            motorLaunch.setPower(LaunchMode == 0 ? 0.1 : 1);                                // make sure the motor is speeding the flywheel up
            while (launchSpeed < (MinSpeed) && opModeIsActive()) {                          // check if the current launch speed meets the minumum speed, if so, wait until it is above
                launchSpeed = getLaunchRPM();                                               // update/get current launch speed
                telemetry.addData("Status", "Speeding");                                    // add text to the screen
                telemetry.addData("RMP", launchSpeed);                                      // add text to the screen
                telemetry.update();                                                         // update the screen so the text appears
                if (detectShootError(MinSpeed, MaxSpeed, launchSpeed)) {                    // check for a shooting error, if so, stop the sequence
                    return;                                                                 // stop the sequence
                }
            }
            
            if (detectShootError(MinSpeed, MaxSpeed, launchSpeed)) {                        // check for a shooting error, if so, stop the sequence
                return;                                                                     // stop the sequence
            }
            
            motorLaunch.setPower(LaunchMode == 0 ? 0.075 : 1);                              // make sure the motor is speeding the flywheel up       
            telemetry.addData("Status", "ToSpeed");                                         // add text to the screen
            telemetry.update();                                                             // update the screen so the text appears
            motorIntake.setPower(-1);                                                       // start rotation the intake motor so a ball is pushed into the flywheel
            sleep(200);                                                                     // wait a moment
            if (i == 2) {                                                                   // if it is the last ball in the robot,
                ServoBall.setPosition(0);                                                   // push the ball into the flywheel using ServoBall lever
                sleep(700);                                                                 // wait a moment
            }
            ServoBall.setPosition(1);                                                       // return ServoBall lever to neutral position
            motorIntake.setPower(0);                                                        // stop the pushing ball into flywheel
            sleep(500);                                                                     // wait a moment
        }
        launchSpeed = 150;                                                                  // set basic launch speed to ensure the following code gets run
        motorLaunch.setPower(-0.05);                                                        // start braking for a faster slowdown
        while (launchSpeed > 100 && opModeIsActive()) {                                     // wait until the speed is below 100 RPM
                launchSpeed = getLaunchRPM();                                               // update the speed
                sleep(100);                                                                 // wait a moment
                telemetry.addData("Status", "Slowing");                                     // add text
                telemetry.addData("RMP", launchSpeed);                                      // add text
                telemetry.update();                                                         // update the screen so the text appears
            }
        motorLaunch.setPower(0);                                                            // stop powering the motor
        startTime = System.currentTimeMillis() - 600;                                       // manipulate startTime for better detectShootError
        if (detectShootError(MinSpeed, MaxSpeed, launchSpeed)) {                            // check for a shooting error, if so, stop the sequence
            return;                                                                         // stop the sequence
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
                
                motorR.setPower(-0.5);
                motorL.setPower(-0.5);
                motorLaunch.setPower(0.1);
                ServoHusky.setPosition(0.7);
                sleep(1500);
                motorR.setPower(0);
                motorL.setPower(0);
                correct(1, 0);
                shoot(800, 1050);
        }
}