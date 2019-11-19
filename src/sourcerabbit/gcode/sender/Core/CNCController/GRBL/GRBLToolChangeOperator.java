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

import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.Settings.SemiAutoToolChangeSettings;
import sourcerabbit.gcode.sender.UI.frmControl;

/**
 *
 * @author Nikos Siatras
 */
public class GRBLToolChangeOperator
{

    private final GRBLGCodeSender fMyGCodeSender;

    public GRBLToolChangeOperator(GRBLGCodeSender myGCodeSender)
    {
        fMyGCodeSender = myGCodeSender;
    }

    public void DoSemiAutoToolChangeSequence(GCodeCommand command)
    {
        double currentWorkX, currentWorkY, currentWorkZ;
        double currentMachineX, currentMachineY, currentMachineZ;
        String commandStr = "";

        // Step 1 - Go to Work X0,Y0,Z4
        MoveFromPositionToPosition_ABSOLUTE("Z", 4);
        MoveFromPositionToPosition_ABSOLUTE("X", 0);
        MoveFromPositionToPosition_ABSOLUTE("Y", 0);
        SendPauseCommand(0.2);

        // Step 2 - Get current work position status
        AskForMachineStatus();
        final double workPositionXBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX();
        final double workPositionYBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY();
        final double workPositionZBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getZ();

        final double machinePositionXBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getX();
        final double machinePositionYBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY();
        final double machinePositionZBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ();

        // Step 3 - Change work position with Machine Position to all axes
        try
        {
            String valuesX = String.format("%.3f", ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getX());
            String valuesY = String.format("%.3f", ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY());
            String valuesZ = String.format("%.3f", ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ());

            commandStr = "G92 X" + valuesX + " Y" + valuesY + " Z" + valuesZ;
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(new GCodeCommand(commandStr));
            AskForMachineStatus();
        }
        catch (Exception ex)
        {
            System.err.println("GRBLToolChangeOperator.DoSemiAutoToolChangeSequence Step 3 Failed:" + ex.getMessage());
        }

        // Step 4 - Move to Tool Setter Location and pause
        // until the user changes the tool
        try
        {
            MoveFromPositionToPosition_ABSOLUTE("Z", -2);
            MoveFromPositionToPosition_ABSOLUTE("X", SemiAutoToolChangeSettings.getToolSetterX());
            MoveFromPositionToPosition_ABSOLUTE("Y", SemiAutoToolChangeSettings.getToolSetterY());

            // Wait until Y move to Tool Setter Y position
            while (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY() != SemiAutoToolChangeSettings.getToolSetterY())
            {
                Thread.sleep(800);
                AskForMachineStatus();
            }

            frmControl.fInstance.WriteToConsole("Change Tool " + command.getCommand());
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().PauseSendingGCode();
            AskForMachineStatus();
        }
        catch (Exception ex)
        {
            System.err.println("GRBLToolChangeOperator.DoSemiAutoToolChangeSequence Step 4 Failed:" + ex.getMessage());
        }

        // Step 5 - Touch the tool setter
        MoveFromPositionToPosition_ABSOLUTE("Z", -40);
        SendPauseCommand(1);

        // Step 6 - Return
        MoveFromPositionToPosition_ABSOLUTE("Z", -2);
        MoveFromPositionToPosition_ABSOLUTE("X", machinePositionXBeforeToolChange);
        MoveFromPositionToPosition_ABSOLUTE("Y", machinePositionYBeforeToolChange);
        MoveFromPositionToPosition_ABSOLUTE("Z", machinePositionZBeforeToolChange);
        AskForMachineStatus();

        // Step 7 - Change work position with the original work position
        // before the tool change
        try
        {
            commandStr = "G92 X" + workPositionXBeforeToolChange + " Y" + workPositionYBeforeToolChange + " Z" + workPositionZBeforeToolChange;
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(new GCodeCommand(commandStr));
            AskForMachineStatus();
        }
        catch (Exception ex)
        {
            System.err.println("GRBLToolChangeOperator.DoSemiAutoToolChangeSequence Step 7 Failed:" + ex.getMessage());
        }
    }

    private void MoveFromPositionToPosition_ABSOLUTE(String axis, double to)
    {
        String command = "G90 " + axis + String.valueOf(to) + "F9000";
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(command));
    }

    /**
     * Send the COMMAND_GET_STATUS (?) to the controller
     */
    private void AskForMachineStatus()
    {
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(GRBLCommands.COMMAND_GET_STATUS));
    }

    /**
     * Send a G4 P(Seconds) command to the controller
     *
     * @param seconds how many seconds to pause
     */
    private void SendPauseCommand(double seconds)
    {
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand("G4 P" + String.valueOf(seconds)));
    }

}
