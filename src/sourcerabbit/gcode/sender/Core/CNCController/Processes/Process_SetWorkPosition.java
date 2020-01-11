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
import javax.swing.JOptionPane;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;

/**
 *
 * @author Nikos Siatras
 */
public class Process_SetWorkPosition extends Process
{

    private final double fX, fY, fZ;

    public Process_SetWorkPosition(JDialog parentForm, double x, double y, double z)
    {
        super(parentForm);
        fX = x;
        fY = y;
        fZ = z;
    }

    @Override
    public void Execute()
    {
        String commandStr = "G92 X" + fX + " Y" + fY + " Z" + fZ;
        GCodeCommand command = new GCodeCommand(commandStr);
        String response = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(command);
        if (response.toLowerCase().equals("ok"))
        {
            JOptionPane.showMessageDialog(fMyParentForm, "Work position changed!");
        }
        else
        {
            JOptionPane.showMessageDialog(fMyParentForm, "Something went wrong.Reset the GRBL controller and try again.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
