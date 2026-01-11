public void correct(speed) {
        boolean CorrectPos = false;
        while (!CorrectPos && opModeIsActive()) {
                HuskyLens.Block[] blocks = husky.blocks();
                HuskyLens.Block tag = getLargest(blocks);
                telemetry.addData("tag", tag);
                telemetry.update();
                if (tag != null && (tag.id == 5 || tag.id == 4)) {
                    DiaginalDistance = (TAG_WIDTH_METERS * FOCAL_LENGTH_PIXELS) / tag.width;
                    angleRad = Math.toRadians(angle * ANGLEMULT);
                    distance = DiaginalDistance * Math.cos(angleRad);   
                    if (tag.y < goal_y - 20) {
                        motorR.setPower(0.1 * speed);
                        motorL.setPower(-0.1 * speed);
                    } else if (tag.y > goal_y + 20) {
                        motorR.setPower(-0.1 * speed);
                        motorL.setPower(0.1 * speed);
                    } else {
                        if (distance > goal_distance + 0.1) {
                            motorR.setPower(-0.2 * speed);
                            motorL.setPower(-0.2 * speed);
                        } else if (distance < goal_distance - 0.1) {
                            motorR.setPower(0.5 * speed);
                            motorL.setPower(0.5 * speed);
                        } else {
                            motorR.setPower(0);
                            motorL.setPower(0);
                            CorrectPos = true;
                        }
                    }
                } else {
                    motorR.setPower(0);
                    motorL.setPower(0);
                    if (angle >= 0.7) {
                        angle = 0.3;
                        motorR.setPower(-1);
                        motorL.setPower(1);
                        sleep(500);
                        motorR.setPower(0);
                        motorL.setPower(0);
                    }
                    ServoHusky.setPosition(angle);
                    angle += 0.15;
                    sleep(300);
                }
        } 
    }