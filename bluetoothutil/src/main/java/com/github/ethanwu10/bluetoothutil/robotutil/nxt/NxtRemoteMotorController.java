package com.github.ethanwu10.bluetoothutil.robotutil.nxt;

import com.github.ethanwu10.bluetoothutil.BluetoothSppClient;
import com.github.ethanwu10.bluetoothutil.robotutil.MotorController;
import com.github.ethanwu10.bluetoothutil.robotutil.MotorState;

/**
 * Controls motors on an NXT connected via Bluetooth (using a {@link com.github.ethanwu10.bluetoothutil.BluetoothSppClient})
 * @author Ethan
 */
public class NxtRemoteMotorController implements MotorController {
    private final BluetoothSppClient mBtSppClient;

    public static interface Motor {
        public static final byte A = 0;
        public static final byte B = 1;
        public static final byte C = 2;
        public static final byte ALL = (byte) 0xff;
    }
    public static interface MotorMode {
        public static final byte MOTORON   = 0x01;
        public static final byte BRAKE     = 0x02;
        public static final byte REGULATED = 0x04;
    }
    public static interface MotorRegMode {
        public static final byte IDLE        = 0x00;
        public static final byte MOTOR_SPEED = 0x01;
        public static final byte MOTOR_SYNC  = 0x02;
    }
    public static interface MotorRunState {
        public static final byte IDLE     = 0x00;
        public static final byte RAMPUP   = 0x10;
        public static final byte RUNNING  = 0x20;
        public static final byte RAMPDOWN = 0x40;
    }

    public NxtRemoteMotorController(BluetoothSppClient sppClient) {
        mBtSppClient = sppClient;
    }

    /**
     * Sets the state of the specified motors
     * @param motorStates states of motors to update
     */
    public void setMotorStates(MotorState[] motorStates) {
        byte[][] messages = new byte[motorStates.length][];
        int messageNum = 0;
        for (MotorState motorState : motorStates) {
            /*               0     1            2     3     4     5     6     7     8     9     10    11    12    13*/
            /*                                             port  power mode  reg   turn  runst tacholimit-------------*/
            byte[] tmpMsg = {0x0c, 0x00, (byte) 0x80, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            byte motorMode = 0;
            byte motorRunState = 0;
            byte motorRegMode = 0;
            if (motorState.motor < 0 || motorState.motor > 2) {
                throw new IllegalArgumentException("NxtRemoteMotorController::setMotorStates: motor state has invalid motor number (" + motorState.motor + ")");
            }
            if (motorState.power < -100 || motorState.power > 100) {
                throw new IllegalArgumentException("NxtRemoteMotorController::setMotorStates: motor state has invalid power (" + motorState.power + ")");
            }
            if (motorState.power != 0) {
                motorMode |= MotorMode.MOTORON;
                motorRunState |= MotorRunState.RUNNING;
                if (motorState.sync || motorState.speed_regulation) {
                    motorMode |= MotorMode.REGULATED;
                    if (motorState.speed_regulation) {
                        motorRegMode |= MotorRegMode.MOTOR_SPEED;
                    }
                    if (motorState.sync) {
                        motorRegMode |= MotorRegMode.MOTOR_SYNC;
                    }
                }
            }
            else {
                motorRegMode = MotorRegMode.IDLE;
                motorRunState = MotorRunState.IDLE;
            }
            if (motorState.brake) {
                motorMode |= MotorMode.BRAKE;
                motorMode |= MotorMode.MOTORON;
                motorRunState |= MotorRunState.RUNNING;
            }

            tmpMsg[4] = motorState.motor;
            tmpMsg[5] = motorState.power;
            tmpMsg[6] = motorMode;
            tmpMsg[7] = motorRegMode;
            tmpMsg[9] = motorRunState;
            for (int i = 0; i < 4; i++) {
                tmpMsg[i+10] = (byte)(motorState.tachometer_limit & (0xff << i));
            }
            messages[messageNum++] = tmpMsg;
        }
        byte[] writeBuf = new byte[motorStates.length * 14];
        int i = 0;
        for (byte[] tmp : messages) {
            System.arraycopy(tmp, 0, writeBuf, i, 14);
            i += 14;
        }
        write(writeBuf);
    }

    /**
     * Wrapper for {@link #setMotorStates(com.github.ethanwu10.bluetoothutil.robotutil.MotorState[])}
     * @param motorState motor state to update
     * @see #setMotorStates(com.github.ethanwu10.bluetoothutil.robotutil.MotorState[])
     */
    public void setMotorState(MotorState motorState) {
        MotorState[] motorStates = new MotorState[1];
        motorStates[0] = motorState;
        setMotorStates(motorStates);
    }

    protected void write(byte[] buf) {
        mBtSppClient.write(buf);
    }
    protected void write(byte buf) {
        mBtSppClient.write(buf);
    }
}
