package com.github.ethanwu10.bluetoothutil.robotutil;

/**
 * A class that represents the state of a motor
 * @author Ethan
 */
public class MotorState {

    public byte motor;
    public byte power;
    public boolean brake;
    public boolean sync;
    public byte[] synced_motors;
    public byte synced_turn_ratio;
    public boolean speed_regulation;
    public int tachometer_limit;

    //TODO: ramp up/down

}
