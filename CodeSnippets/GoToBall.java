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
                Wait(50);

                // stopconditie: dichterbij (voorbeeld: y > threshold)
                if (ball.x < 20) { // pas aan naar jouw camera/calibratie
                    IntakeOn = false;
                    ballInput();
                    break;
                }
            }
        }