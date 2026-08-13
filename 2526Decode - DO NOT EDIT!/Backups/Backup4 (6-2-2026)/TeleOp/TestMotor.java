import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.hardware.dfrobot.HuskyLens;

@TeleOp(name="Control", group="Main")
public class TestMotor extends LinearOpMode {
    private DcMotor motorL;
    private DcMotor motorR;
    private DcMotor motorLaunch;
    private DcMotor motorIntake;
    private Servo Servo1;
    private Servo Servo2;
    private Servo ServoBall;
    private Servo ServoHusky;
    private int DriveMode = 1;
    double SpeedMult = 0.2;
    boolean IntakeOn = false;
    private Gamepad DriverGamepad;
    private HuskyLens husky;
    private HuskyLens.Block ball;
    private int[] codeSequence;
    private int lastPos = 0;
    private long lastTime = 0;
    private long startTime;
    private double launchSpeed;
    static final int TICKS_PER_REV = 28; // REV HD Hex Motor
    int LaunchMode = 1; // 0 = Quiet, 1 = Normal, 2 = Overdrive1, 3 = Overdrive2, 4 = Max

    
    public void HandleMovement() {
        double powerL = 0;
        double powerR = 0;

        if (DriveMode == 1) {
            powerL = (-DriverGamepad.left_stick_y + DriverGamepad.left_stick_x) * SpeedMult;
            powerR = (-DriverGamepad.left_stick_y - DriverGamepad.left_stick_x) * SpeedMult;
        } else {
            powerL = -DriverGamepad.left_stick_y * SpeedMult;
            powerR = -DriverGamepad.right_stick_y * SpeedMult;
        }
        motorL.setPower(powerL);
        motorR.setPower(powerR);

        if (DriverGamepad.rightBumperWasPressed()) {
            SpeedMult += 0.05;
            SpeedMult = Math.min(SpeedMult, 1);
        } else if (DriverGamepad.leftBumperWasPressed()) {
            SpeedMult -= 0.05;
            SpeedMult = Math.max(SpeedMult, 0);
        }
        
        if (DriverGamepad.circleWasPressed()) {
            DriveMode = 1 - DriveMode;
            DriverGamepad.rumble(500);
        }

    }

    // Non-blocking wait (gebruik in TeleOp)
    public void Wait(int timeMs) {
        long endTime = System.currentTimeMillis() + (long) timeMs;
        while (opModeIsActive() && System.currentTimeMillis() < endTime && !(gamepad1.start || gamepad2.start)) {
            HandleMovement();
        }
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
        Wait(50);
        if ((System.currentTimeMillis() > startTime + 500 && Speed == 0) || (System.currentTimeMillis() > startTime + 5000)) {
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
                Wait(100);
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
            motorIntake.setPower(-1);
            Wait(200);
            if (i == 2) {
                ServoBall.setPosition(0);
                Wait(700);
            }
            ServoBall.setPosition(1);
            motorIntake.setPower(0);
            sleep(500);
        }
        launchSpeed = 150;
        motorLaunch.setPower(-0.05);
        while (launchSpeed > 100 && opModeIsActive()) {
                launchSpeed = getLaunchRPM();
                Wait(100);
                telemetry.addData("Status", "Slowing");
                telemetry.addData("RMP", launchSpeed);
                telemetry.update();
            }
        motorLaunch.setPower(0);
        if (launchSpeed == 0) {
                motorLaunch.setPower(0);
                telemetry.addData("Status", "Failed launch!");
                telemetry.update();
                Wait(3000);
                return;
        }
    }

    public void empty() {
        motorIntake.setPower(0.5);
        Wait(500);
        motorIntake.setPower(0);
    }

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

    public void goToBall() {
        motorL.setPower(0);
        motorR.setPower(0);

        // blijf updaten totdat we dichtbij genoeg zijn of geen object meer zien
        while (opModeIsActive()) {
            HuskyLens.Block[] blocks = husky.blocks();
            ball = getLargest(blocks);

            if (ball == null) {
                telemetry.addData("Husky", "Geen object gevonden");
                telemetry.update();
                motorL.setPower(0);
                motorR.setPower(0);
                return;
            }

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
            Wait(50);

            // stopconditie: dichterbij (voorbeeld: y > threshold)
            if (ball.x < 20) { // pas aan naar jouw camera/calibratie
                IntakeOn = false;
                ballInput();
                break;
            }
        }

        // korte finale vooruitstoot en stop
        motorL.setPower(1 * SpeedMult);
        motorR.setPower(1 * SpeedMult);
        Wait((int)(500 / SpeedMult)); // cast naar int
        motorL.setPower(0);
        motorR.setPower(0);
        ballInput();
    }

    public void ballInput() {
        if (!IntakeOn) {
            ServoHusky.setPosition(0.25);
            motorIntake.setPower(-1);
        } else {
            ServoHusky.setPosition(0.4);
            motorIntake.setPower(0);
        }
        IntakeOn = !IntakeOn;
    }

    // Scan tags en zet codeSequence volgens jouw mapping
    public void scanCode() {
        husky.selectAlgorithm(HuskyLens.Algorithm.TAG_RECOGNITION);
        Wait(100); // laat HuskyLens tijd om over te schakelen

        HuskyLens.Block[] blocks = husky.blocks();

        if (blocks == null || blocks.length == 0) {
            telemetry.addData("ScanCode", "Geen tags gevonden");
            telemetry.update();
            codeSequence = null;
            return;
        }

        int tagId = blocks[0].id;
        if (tagId == 1) {
            codeSequence = new int[]{1, 2, 2};
        } else if (tagId == 2) {
            codeSequence = new int[]{2, 1, 2};
        } else if (tagId == 3) {
            codeSequence = new int[]{2, 2, 1};
        } else {
            codeSequence = null; // onbekend
        }

        telemetry.addData("TagId", tagId);
        telemetry.addData("CodeSeq", codeSequence == null ? "null" : java.util.Arrays.toString(codeSequence));
        telemetry.update();
    }

    @Override
    public void runOpMode() {
        motorL = hardwareMap.get(DcMotor.class, "Left_Drive_Motor");
        motorL.setDirection(DcMotor.Direction.FORWARD);
        motorL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorL.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        motorR = hardwareMap.get(DcMotor.class, "Right_Drive_Motor");
        motorR.setDirection(DcMotor.Direction.REVERSE);
        motorR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorR.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        
        motorLaunch = hardwareMap.get(DcMotor.class, "LaunchMotor");
        motorLaunch.setDirection(DcMotor.Direction.FORWARD);
        motorLaunch.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorLaunch.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        
        motorIntake = hardwareMap.get(DcMotor.class, "IntakeMotor");
        motorIntake.setDirection(DcMotor.Direction.FORWARD);
        motorIntake.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorIntake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        Servo1 = hardwareMap.get(Servo.class, "Servo1");
        Servo2 = hardwareMap.get(Servo.class, "Servo2");
        ServoHusky = hardwareMap.get(Servo.class, "ServoHusky");
        ServoBall = hardwareMap.get(Servo.class, "ServoBall");

        husky = hardwareMap.get(HuskyLens.class, "huskylens");

        husky.selectAlgorithm(HuskyLens.Algorithm.COLOR_RECOGNITION);

        int DriveMode = 1;
        DriverGamepad = gamepad1;

        Servo1.setPosition(0.35);
        Servo2.setPosition(0.5);
        ServoBall.setPosition(1);

        waitForStart();
        


        while (opModeIsActive()) {

            
            HandleMovement();
            
            if (gamepad1.triangleWasPressed() || gamepad2.triangleWasPressed()) {
                if (LaunchMode == 0) {
                    shoot(500, 750);
                } else if (LaunchMode == 1) {
                    shoot(750, 1000);
                } else if (LaunchMode == 2) {
                    shoot(1000, 1250);
                } else if (LaunchMode == 3) {
                    shoot(1250, 1500);
                } else if (LaunchMode == 4) {
                    shoot(2000, 2250);
                } 
            }
            if (gamepad1.backWasReleased() || gamepad2.backWasReleased()) {
                empty();
            }
            if (gamepad1.squareWasPressed()) {
                DriverGamepad = gamepad1;
            }
            if (gamepad2.squareWasPressed()) {
                DriverGamepad = gamepad2;
            }
            if (gamepad1.crossWasPressed() || gamepad2.crossWasPressed()) {
                ballInput();
            }
            if (DriverGamepad.dpadDownWasPressed()) {
                goToBall();
            }
            if (DriverGamepad.dpadRightWasPressed()) {
                scanCode();
            }

            try {
                telemetry.addData("Speedmult", SpeedMult);
                telemetry.addData("DriverMode", DriveMode);
                telemetry.addData("DriverGamepad", DriverGamepad == null ? "null" : DriverGamepad.toString());
                telemetry.addData("Servo1", Servo1.getPosition());
                HuskyLens.Block[] blocks = husky.blocks();

                if (blocks != null && blocks.length > 0) {
                    telemetry.addData("ID", blocks[0].id);
                    telemetry.addData("X", blocks[0].x);
                    telemetry.addData("Y", blocks[0].y);
                }

                telemetry.update();
            } catch (Exception e) {
                telemetry.addData("Errors", "telemetry error: " + e.getMessage());
                telemetry.update();
            }

            // loop delay kleine pauze zodat we niet te hard poll'en
            Wait(20);
        }

        gamepad1.rumble(500);
        gamepad2.rumble(500);
        
        motorLaunch.setPower(0);
    }
}
