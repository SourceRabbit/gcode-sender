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
package sourcerabbit.gcode.sender.Core.CNCController.Processes;

import javax.swing.JDialog;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.MachineStatusEvent;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLActiveStates;

/**
 *
 * @author Nikos Siatras
 */
public class Process_Homing extends Process
{

    public Process_Homing(JDialog parentForm)
    {
        super(parentForm);
    }

    @Override
    public void Execute()
    {
        try
        {
            // Before sending the "$H" command to the controller
            // ask one last time for the Machine Status
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.AskForMachineStatus();

            // Use the StartUsingTouchProbe in order to PAUSE 
            // the Status Report Thread inside the GRBLConnection Handler
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StartUsingTouchProbe();

            // Send the "$H" homing command to GRBL Controller and change the active state to GRBLActiveStates.HOME
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendData("$H");
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().FireMachineStatusReceived(new MachineStatusEvent(GRBLActiveStates.HOME, ""));
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.HOME, ""));

            // Use the StopUsingTouchProbe in order to RESUME 
            // the Status Report Thread inside the GRBLConnection Handler
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
        }
        catch (Exception ex)
        {

        }
    }

}
