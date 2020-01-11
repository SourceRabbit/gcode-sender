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
public class GRBLConstants
{

    // This is the interval the GRBLGCodeSender is allowed to ask the controller for its status
    // during a gcode cycle.
    public static final int MILLISECONDS_TO_ASK_FOR_MACHINE_STATUS_DURING_CYCLING = 4000;

    // This is the interval the GRBLConnectionHandler Status Report Thread is allowed to 
    // ask the controller for its status when the controller is Idle
    public static final int MILLISECONDS_TO_ASK_FOR_MACHINE_STATUS_WHEN_CONTROLLER_IS_IDLE = 800;

}
