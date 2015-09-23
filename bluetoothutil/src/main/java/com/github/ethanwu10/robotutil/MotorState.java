package com.github.ethanwu10.robotutil;

/**
 * A class that represents the state of a motor.
 *
 * @author Ethan
 */
public class MotorState {

    /**
     * Motor ID to be set.
     */
    public byte motor;

    /**
     * Motor power to be set.
     * <br><br>
     * Range varies depending on robot type. (MAY CHANGE IN FUTURE)
     */
    public byte power;

    /**
     * Controls whether or not the motor should brake.
     */
    public boolean brake;

    /**
     * Controls whether or not motor should be synced to other motors.
     *
     * @see #synced_motors
     */
    public boolean sync;

    /**
     * An array of other motor IDs to be synced to. <br>
     * Does not include this motor (specified in {@link #motor}).
     * <br><br>
     * Only relevant if {@link #sync} is <code>true</code>.
     */
    public byte[] synced_motors;

    /**
     * Turn ratio to use while synced.
     * <br><br>
     * Only relevant if {@link #sync} is <code>true</code>
     *   and {@link #synced_motors} has 1 element.
     */
    public byte synced_turn_ratio;

    /**
     * Controls whether or not the motor should regulate its speed.
     */
    public boolean speed_regulation;

    /**
     * Sets a tachometer limit for the motor.
     * <br><br>
     * Sets the number of on-board encoder counts (if equipped) read
     *   before stopping the motor,
     */
    public int tachometer_limit;

    //TODO: ramp up/down

}
