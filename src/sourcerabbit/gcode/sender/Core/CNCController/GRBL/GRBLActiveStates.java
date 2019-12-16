/*
 Copyright (C) 2015  Nikos Siatras

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 SourceRabbit GCode Sender is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package sourcerabbit.gcode.sender.Core.CNCController.GRBL;

/**
 *
 * @author Nikos Siatras
 */
public class GRBLActiveStates
{

    // All systems are go, no motions queued, and it's ready for anything.
    public static final int IDLE = 1;

    // Indicates a cycle is running.
    public static final int RUN = 2;

    // A feed hold is in process of executing, or slowing down to a stop. 
    // After the hold is complete, Grbl will remain in Hold and wait for a cycle start to resume the program.
    public static final int HOLD = 3;

    // (New in v0.9i) This compile-option causes Grbl to feed hold, shut-down the spindle and coolant, 
    // and wait until the door switch has been closed and the user has issued a cycle start. 
    // Useful for OEM that need safety doors.
    public static final int DOOR = 4;

    // In the middle of a homing cycle. NOTE: Positions are not updated live during the homing cycle, but they'll be set to the home position once done.
    public static final int HOME = 5;

    // This indicates something has gone wrong or Grbl doesn't know its position. 
    // This state locks out all G-code commands, but allows you to interact with Grbl's settings if you need to. '$X' 
    // kill alarm lock releases this state and puts Grbl in the Idle state, which will let you move things again. 
    // As said before, be cautious of what you are doing after an alarm.
    public static final int ALARM = 6;

    // Grbl is in check G-code mode. 
    // It will process and respond to all G-code commands, but not motion or turn on anything. 
    // Once toggled off with another '$C' command, Grbl will reset itself.
    public static final int CHECK = 7;

    // GRBL controller is jogging
    public static final int JOG = 8;

    // GRBL sent the "[MSG:Reset to continue]" string.
    // This can happen if the hard_limits is on and the machine hits on the limit switches.
    public static final int RESET_TO_CONTINUE = 100001;

    // GRBL sent the "['$H'|'$X' to unlock]" string.
    public static final int MACHINE_IS_LOCKED = 100002;

    // Machine touched the probe.
    public static final int MACHINE_TOUCHED_PROBE = 100003;

    public static int getGRBLActiveStateFromString(String state)
    {
        state = state.toLowerCase();
        switch (state)
        {
            case "idle":
                return GRBLActiveStates.IDLE;

            case "run":
                return GRBLActiveStates.RUN;

            case "hold":
            case "hold:0":
            case "hold:1":
                return GRBLActiveStates.HOLD;

            case "door":
            case "door:0":
            case "door:1":
            case "door:2":
            case "door:3":
                return GRBLActiveStates.DOOR;

            case "home":
                return GRBLActiveStates.HOME;

            case "alarm":
                return GRBLActiveStates.ALARM;

            case "check":
                return GRBLActiveStates.CHECK;

            case "jog":
                return GRBLActiveStates.JOG;

            default:
                System.err.println("GRBLActiveStates.getGRBLActiveStateFromString: Unknown state " + state);
                return -1;

        }
    }
}
