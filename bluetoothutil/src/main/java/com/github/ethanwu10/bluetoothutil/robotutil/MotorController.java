package com.github.ethanwu10.bluetoothutil.robotutil;

/**
 * Controls motors on a remote robot
 * @author Ethan
 */
public interface MotorController {

    /**
     * Sets the state of the specified motors
     * @param motorStates states of motors to update
     */
    public void setMotorStates(MotorState[] motorStates);

    /**
     * Wrapper for {@link #setMotorStates(com.github.ethanwu10.bluetoothutil.robotutil.MotorState[])}
     * @param motorState motor state to update
     * @see #setMotorStates(com.github.ethanwu10.bluetoothutil.robotutil.MotorState[])
     */
    public void setMotorState(MotorState motorState);

}
