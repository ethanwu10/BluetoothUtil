package com.github.ethanwu10.robotutil;

/**
 * Controls motors on a remote robot
 *
 * @see com.github.ethanwu10.robotutil.MotorState
 *
 * @author Ethan
 */
public interface MotorController {

    /**
     * Sets the state of the specified motors
     *
     * @param motorStates states of motors to update
     *
     * @see #setMotorState(MotorState)
     */
    public void setMotorStates(MotorState[] motorStates);

    /**
     * Sets the state of a specified motor
     *
     * @param motorState motor state to update
     *
     * @see #setMotorStates(com.github.ethanwu10.robotutil.MotorState[])
     */
    public void setMotorState(MotorState motorState);

}
