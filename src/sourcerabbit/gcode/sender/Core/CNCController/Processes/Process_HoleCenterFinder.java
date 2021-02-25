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
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLActiveStates;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLCommands;
import sourcerabbit.gcode.sender.Core.Settings.CenterHoleFinderSettings;
import sourcerabbit.gcode.sender.Core.Threading.ManualResetEvent;

/**
 *
 * @author Nikos Siatras
 */
public class Process_HoleCenterFinder extends Process
{

    // Variables...
    private boolean fTouchProbeTouchedTheEdge = false;
    private final ManualResetEvent fWaitForTouchProbeToTouchTheEdge = new ManualResetEvent(false);
    private final ManualResetEvent fWaitForMachineToBeIdle = new ManualResetEvent(false);
    private boolean fProcessIsKilled = false;

    public Process_HoleCenterFinder(JDialog parentForm)
    {
        super(parentForm);
    }

    @Override
    public void MachineStatusHasChange(int state)
    {
        switch (state)
        {
            case GRBLActiveStates.IDLE:
                fWaitForMachineToBeIdle.Set();
                break;
            case GRBLActiveStates.RUN:
                break;
            case GRBLActiveStates.HOLD:
            case GRBLActiveStates.ALARM:
            case GRBLActiveStates.RESET_TO_CONTINUE:
                fWaitForTouchProbeToTouchTheEdge.Set();
                if (!fProcessIsKilled)
                {
                    ProbeFailedToTouchTheEdgeOfTheHole("Failed to find the center of the hole!\n\nMake sure your touch probe is not in contact with the\nhole edges before you click the 'Find Center' button.");
                }
                break;
            case GRBLActiveStates.MACHINE_TOUCHED_PROBE:
                fTouchProbeTouchedTheEdge = true;
                fWaitForTouchProbeToTouchTheEdge.Set();
                break;
        }
    }

    @Override
    public void Execute()
    {
        fProcessIsKilled = false;
        int feedRate = CenterHoleFinderSettings.getTouchProbeFeedRate();

        try
        {
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StartUsingTouchProbe();

            SetWorkPosition("X", 0);
            SetWorkPosition("Y", 0);

            FindAxisCenter("X", feedRate);
            FindAxisCenter("Y", feedRate);

            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
            OperationCompletedSuccessfully();

            SetWorkPosition("X", 0);
            SetWorkPosition("Y", 0);
        }
        catch (Exception ex)
        {
            ProbeFailedToTouchTheEdgeOfTheHole(ex.getMessage());
        }

        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
    }

    private void FindAxisCenter(final String axis, final int feedRate) throws Exception
    {
        String response;
        double startWorkPosition = 0, workPosition1 = 0, workPosition2 = 0;

        // Before everything get the start Work position (The current work position).
        AskForMachineStatus();
        startWorkPosition = axis.equals("X") ? ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX() : ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY();

        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 1
        // Move the touch probe towards the "axis -"  until it touches the edge of the hole.
        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        response = SendTouchProbeToTouchTheEdge(axis + "-", feedRate);
        if (!response.equals("ok") || !fTouchProbeTouchedTheEdge)
        {
            throw new Exception("Make sure the touch probe does not touch the edges of the hole!");
        }

        // Ask for machine status to get the current Work Position
        AskForMachineStatus();
        workPosition1 = axis.equals("X") ? ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX() : ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY();

        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 2
        // At the moment the touch probe touches the edge of the hole on "axis -" .
        // Return the touchprobe to the startWorkPosition and then towards the "axis +" until it touches the edge of the hole.
        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        MoveMachineAbsolute(axis, startWorkPosition, feedRate);
        response = SendTouchProbeToTouchTheEdge(axis, feedRate);
        if (!response.equals("ok") || !fTouchProbeTouchedTheEdge)
        {
            throw new Exception("Make sure the touch probe does not touch the edges of the hole!");
        }

        // Ask for machine status to get the new Work Position
        AskForMachineStatus();
        workPosition2 = axis.equals("X") ? ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX() : ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY();

        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 3 
        // Move touch probe in the middle of xDiff
        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        SetWorkPosition(axis, 0);
        final double xDiff = Math.max(workPosition1, workPosition2) - Math.min(workPosition1, workPosition2);
        MoveMachineAbsolute(axis + "-", xDiff / 2, feedRate);

        /////////////////////////////////////////////////////////////////////////////////
        // Finished !!!
        // Ask for machine status and set the X & Y axes position to 0
        /////////////////////////////////////////////////////////////////////////////////
        AskForMachineStatus();
        SetWorkPosition(axis, 0);
    }

    /**
     * Send the touch probe to touch the edge of an axis.
     *
     * @param axis the axis to touch the edge (X,X-,Y,Y-)
     * @param feedRate the feed rate to use
     * @return
     */
    private String SendTouchProbeToTouchTheEdge(String axis, int feedRate)
    {
        fWaitForTouchProbeToTouchTheEdge.Reset();
        fTouchProbeTouchedTheEdge = false;
        final GCodeCommand command = new GCodeCommand("G38.2 " + axis + (CenterHoleFinderSettings.getHoleCenterFinderDiameter() / 2) + "F" + feedRate);
        String response = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(command);
        fWaitForTouchProbeToTouchTheEdge.WaitOne();
        return response;
    }

    /**
     * Set work position
     *
     * @param axis
     * @param value
     */
    private void SetWorkPosition(String axis, double value)
    {
        try
        {
            String commandStr = "G21 G92 " + axis + value;
            GCodeCommand command = new GCodeCommand(commandStr);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
        }
        catch (Exception ex)
        {

        }
    }

    /**
     * Move machine to specified position.
     *
     * @param axis the axis to move
     * @param position the position to go
     * @param feedRate is the feed rate to use
     */
    private void MoveMachineAbsolute(String axis, double position, int feedRate)
    {
        String str = "G21 G90 G1" + axis + position + "F" + feedRate;
        GCodeCommand moveZCommand = new GCodeCommand(str);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(moveZCommand);
    }

    private void ProbeFailedToTouchTheEdgeOfTheHole(String message)
    {
        if (message.equals(""))
        {
            JOptionPane.showMessageDialog(fMyParentForm, "Probe failed to touch the edge!", "Error", JOptionPane.ERROR_MESSAGE);
        }
        else
        {
            JOptionPane.showMessageDialog(fMyParentForm, message, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void OperationCompletedSuccessfully()
    {
        fWaitForMachineToBeIdle.Reset();
        fWaitForMachineToBeIdle.WaitOne();
        JOptionPane.showMessageDialog(fMyParentForm, "Work position is now in the center of the hole!", "Center Found", JOptionPane.INFORMATION_MESSAGE);

        SetWorkPosition("X", 0);
        SetWorkPosition("Y", 0);
    }

    /**
     * Ask machine for the machine status.
     */
    private void AskForMachineStatus()
    {
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.AskForMachineStatus();
    }

    @Override
    public void KillImmediately()
    {
        try
        {
            fProcessIsKilled = true;
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendDataImmediately_WithoutMessageCollector(GRBLCommands.COMMAND_SOFT_RESET);
        }
        catch (Exception ex)
        {
        }
    }
}