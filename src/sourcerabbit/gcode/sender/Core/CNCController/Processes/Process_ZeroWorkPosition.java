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

import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.UI.frmControl;

/**
 *
 * @author Nikos Siatras
 */
public class Process_ZeroWorkPosition extends Process
{

    private String[] fAxisToZero;

    public Process_ZeroWorkPosition(String[] axis)
    {
        super(null);
        fAxisToZero = axis;
    }

    @Override
    public void Execute()
    {
        String commandStr = "G92 ";
        for (String axes : fAxisToZero)
        {
            commandStr += axes + "0 ";
        }

        GCodeCommand command = new GCodeCommand(commandStr.trim());
        String response = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(command);
        if (response.equals("ok"))
        {
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.AskForMachineStatus();

            if (fAxisToZero.length > 1)
            {
                frmControl.fInstance.WriteToConsole("Work Positions " + String.join(",", fAxisToZero) + " Zeroed");
            }
            else
            {
                frmControl.fInstance.WriteToConsole("Work Position " + fAxisToZero[0] + " Zeroed");
            }
        }

    }

}
