public boolean detectShootError(int MinSpeed, int MaxSpeed, double Speed) {
        Wait(50);
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
            telemetry.addData("More info:", "")
            telemetry.addData("", "\n  
                Speed:              %s RPM  \n
                Start time:         %s ms   \n
                Current time:       %s ms (%s)  \n  
                Min/Max:            %s, %s RPM",
                launchSpeed, startTime, System.currentTimeMillis(), (startTime - System.currentTimeMillis()), MinSpeed, MaxSpeed);
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