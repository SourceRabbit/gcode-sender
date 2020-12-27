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
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.Units.EUnits;

/**
 *
 * @author Nikos Siatras
 */
public class Process_Jogging extends Process
{

    private final String fAxis;
    private final double fStepValue;
    private final EUnits fUnits;

    public Process_Jogging(JDialog parentForm, String axis, double stepValue, EUnits units)
    {
        super(parentForm);
        fAxis = axis;
        fStepValue = stepValue;
        fUnits = units;
    }

    @Override
    public void Execute()
    {
        // Get appropriate GCode for Metric or Imperial Units
        final String inchesOrMillimetersGCode;
        switch (fUnits)
        {
            case Imperial:
                inchesOrMillimetersGCode = "G20";
                break;

            case Metric:
                inchesOrMillimetersGCode = "G21";
                break;

            default:
                inchesOrMillimetersGCode = "";
                break;
        }

        switch (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getCNCControlFrameworkVersion())
        {
            case GRBL0_9:
                try
                {
                    GCodeCommand command = new GCodeCommand(inchesOrMillimetersGCode + "G91G0" + fAxis + fStepValue);
                    ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
                }
                catch (Exception ex)
                {

                }
                break;

            case GRBL1_1:
                try
                {
                    GCodeCommand command = new GCodeCommand("$J=G91 " + inchesOrMillimetersGCode + fAxis + fStepValue + "F30000");
                    ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
                }
                catch (Exception ex)
                {

                }

                break;
        }
    }

}
