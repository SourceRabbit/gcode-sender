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
public class GRBLCommands
{

    // Real time commands
    public static final String COMMAND_PAUSE = "!";
    public static final String COMMAND_START_CYCLE = "~";
    public static final String COMMAND_GET_STATUS = "?";
    public static final String COMMAND_FEED_HOLD = "!";
    public static final String COMMAND_SOFT_RESET = String.valueOf((char) 24);
    public static final byte JOG_CANCEL_COMMAND = (byte) 0x85;

    // Non real time commands
    public static final String COMMAND_KILL_ALARM_LOCK = "$X";
    public static final String COMMAND_TOGGLE_CHECK_MODE = "$C";
    public static final String COMMAND_VIEW_PARSER_STATE = "$G";

    // GCode Commands
    //public static final String GCODE_RESET_COORDINATES_TO_ZERO = "G10 P0 L20 X0 Y0 Z0";
    public static final String GCODE_RESET_COORDINATES_TO_ZERO = "G21 G92 X0.000 Y0.000 Z0.000";

}
