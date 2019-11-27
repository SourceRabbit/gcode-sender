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
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeCycleEvents.GCodeCycleEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeCycleEvents.IGCodeCycleListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.IMachineStatusEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.MachineStatusEvent;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.Settings.SemiAutoToolChangeSettings;
import sourcerabbit.gcode.sender.Core.Threading.ManualResetEvent;
import sourcerabbit.gcode.sender.UI.frmControl;

/**
 *
 * @author Nikos Siatras
 */
public class GRBLToolChangeOperator
{

    private final GRBLGCodeSender fMyGCodeSender;

    // Tool Setter Variables
    private boolean fIsTheFirstToolChangeInTheGCodeCycle = true;
    private double fTravelBetweenWorkZeroAndToolSetterTop = 0;
    private final ManualResetEvent fWaitToTouchTheProbe = new ManualResetEvent(false);
    private final ManualResetEvent fWaitForMachineToBeIdle = new ManualResetEvent(false);
    protected IMachineStatusEventListener fIMachineStatusEventListener = null;

    public GRBLToolChangeOperator(GRBLGCodeSender myGCodeSender)
    {
        fMyGCodeSender = myGCodeSender;
    }

    private void MachineStatusHasChange(int state)
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
                fWaitToTouchTheProbe.Set();
                break;
            case GRBLActiveStates.MACHINE_TOUCHED_PROBE:
                fWaitToTouchTheProbe.Set();
                break;
        }
    }

    private void InitializeMachineStatusEventListener()
    {
        if (ConnectionHelper.ACTIVE_CONNECTION_HANDLER != null && fIMachineStatusEventListener == null)
        {
            // Add a listener for the machine status changes
            // This is required for the tool setter process
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().AddListener(fIMachineStatusEventListener = (MachineStatusEvent evt) ->
            {
                final int activeState = evt.getMachineStatus();
                MachineStatusHasChange(activeState);
            });

            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getCycleEventManager().AddListener(new IGCodeCycleListener()
            {
                @Override
                public void GCodeCycleStarted(GCodeCycleEvent evt)
                {
                }

                @Override
                public void GCodeCycleFinished(GCodeCycleEvent evt)
                {
                    fIsTheFirstToolChangeInTheGCodeCycle = true;
                }

                @Override
                public void GCodeCycleCanceled(GCodeCycleEvent evt)
                {
                    fIsTheFirstToolChangeInTheGCodeCycle = true;
                }

                @Override
                public void GCodeCyclePaused(GCodeCycleEvent evt)
                {
                }

                @Override
                public void GCodeCycleResumed(GCodeCycleEvent evt)
                {
                }
            });
        }
    }

    public void DoSemiAutoToolChangeSequence(GCodeCommand command)
    {
        InitializeMachineStatusEventListener();

        // Step 1 - Get current work position status
        try
        {
            Thread.sleep(1000);
        }
        catch (Exception ex)
        {

        }
        AskForMachineStatus();

        final double workPositionXBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX();
        final double workPositionYBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY();
        final double workPositionZBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getZ();

        final double machinePositionXBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getX();
        final double machinePositionYBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY();
        final double machinePositionZBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ();

        // Step 2 - Go to Work Z4
        frmControl.fInstance.WriteToConsole("Semi auto tool change process started.");
        MoveFromPositionToPosition_ABSOLUTE("Z", 4);
        //MoveFromPositionToPosition_2Axis_ABSOLUTE("X", 0, "Y", 0);
        SendPauseCommand(0.2);

        // Step 3 - Change work position with Machine Position to all axes
        try
        {
            ChangeWorkPositionWithMachinePosition("X");
            ChangeWorkPositionWithMachinePosition("Y");
            ChangeWorkPositionWithMachinePosition("Z");
            AskForMachineStatus();
        }
        catch (Exception ex)
        {
            System.err.println("GRBLToolChangeOperator.DoSemiAutoToolChangeSequence Step 3 Failed:" + ex.getMessage());
        }

        // Step 4 - Move Z to top and then X and Y to the Tool Setter's Location and pause
        // until the user changes the tool
        try
        {
            MoveFromPositionToPosition_ABSOLUTE("Z", -2);
            MoveFromPositionToPosition_2Axis_ABSOLUTE("X", SemiAutoToolChangeSettings.getToolSetterX(), "Y", SemiAutoToolChangeSettings.getToolSetterY());

            // Wait until Y moves to Tool Setter Y position
            while (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY() != SemiAutoToolChangeSettings.getToolSetterY())
            {
                Thread.sleep(800);
                AskForMachineStatus();
            }

            if (fIsTheFirstToolChangeInTheGCodeCycle)
            {
                frmControl.fInstance.WriteToConsole("Turn off the Spindle and press the resume button.");
            }
            else
            {
                frmControl.fInstance.WriteToConsole("Change Tool " + command.getCommand() + " and press the 'Resume' button.");
            }

            Thread.sleep(500);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().PauseSendingGCode();
            AskForMachineStatus();
        }
        catch (Exception ex)
        {
            System.err.println("GRBLToolChangeOperator.DoSemiAutoToolChangeSequence Step 4 Failed:" + ex.getMessage());
        }

        // Step 5 - Touch the tool setter
        try
        {
            //////////////////////////////////////////////////////////////////////////////
            // Move Endmill to the tool setter
            //////////////////////////////////////////////////////////////////////////////
            frmControl.fInstance.WriteToConsole("Touching the tool setter...");
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StartUsingTouchProbe();
            MoveEndmillToToolSetter(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.fZMaxTravel, 800);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(new GCodeCommand("G0G90"));

            // Move end mill to tool setter slower this time
            MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE("Z", 6);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StartUsingTouchProbe();
            MoveEndmillToToolSetter(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.fZMaxTravel, 180);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(new GCodeCommand("G0G90"));
            AskForMachineStatus();
            //SendPauseCommand(1);
            //////////////////////////////////////////////////////////////////////////////

            // At this point we know that the endmill is exactly at SemiAutoToolChangeSettings.getToolSetterHeight()
            // but... we do not know the distance between the work Z 0 (before the tool change) and the tool setter.
            // So if it is the FIRST TOOL CHANGE IN THE GCODE CYCLE we have to make the calculation
            // of the travel between the work Z 0 point to the Tool setter top
            if (fIsTheFirstToolChangeInTheGCodeCycle)
            {
                fTravelBetweenWorkZeroAndToolSetterTop = machinePositionZBeforeToolChange - ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ();
                fIsTheFirstToolChangeInTheGCodeCycle = false;
            }

            if (fTravelBetweenWorkZeroAndToolSetterTop < 0)
            {
                // That means the original work zero is below the tool setter height
                // So move the Z 10mm up
                MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE("Z", 10);
            }
            else
            {
                // Move endmill back to  fTravelBetweenWorkZeroAndToolSetterTop
                MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE("Z", fTravelBetweenWorkZeroAndToolSetterTop);
                SendPauseCommand(0.5);
            }

            AskForMachineStatus();

            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // At this point the Endmill is at the original Work Z 0 point
            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if (fTravelBetweenWorkZeroAndToolSetterTop < 0)
            {
                ChangeWorkPositionWithValue("Z", Math.abs(fTravelBetweenWorkZeroAndToolSetterTop) + 10);
            }
            else
            {
                ChangeWorkPositionWithValue("Z", 0);
            }

            // RAISE THE ENDMILL TO THE TOP IN ORDER TO AVOID WORKHOLDING
            AskForMachineStatus();
            double maxDistance = Math.abs(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ() + 2);
            MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE("Z", maxDistance);
            AskForMachineStatus();
            SendPauseCommand(0.5);

        }
        catch (Exception ex)
        {

        }

        // Step 6 - Return to X and Y before Tool Change
        try
        {
            frmControl.fInstance.WriteToConsole("Turn on the Spindle and press the 'Resume' button.");
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().PauseSendingGCode();
            AskForMachineStatus();
            MoveFromPositionToPosition_2Axis_ABSOLUTE("X", machinePositionXBeforeToolChange, "Y", machinePositionYBeforeToolChange);

            // Wait until Y moves to Tool Setter Y position
            while (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY() != machinePositionYBeforeToolChange)
            {
                Thread.sleep(800);
                AskForMachineStatus();
            }

            // Move endmill back to ZERO
            MoveFromPositionToPosition_ABSOLUTE("Z", workPositionZBeforeToolChange);
            ChangeWorkPositionWithValue("X", workPositionXBeforeToolChange);
            ChangeWorkPositionWithValue("Y", workPositionYBeforeToolChange);
            ChangeWorkPositionWithValue("Z", workPositionZBeforeToolChange);

            AskForMachineStatus();
        }
        catch (Exception ex)
        {
            System.err.println("GRBLToolChangeOperator.DoSemiAutoToolChangeSequence Step 6 Failed:" + ex.getMessage());
        }

    }

    private void MoveFromPositionToPosition_ABSOLUTE(String axis, double to)
    {
        String command = "G90 " + axis + String.valueOf(to) + "F3000";
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(command));
    }

    private void MoveFromPositionToPosition_2Axis_ABSOLUTE(String axis1, double to1, String axis2, double to2)
    {
        String command = "G90 " + axis1 + String.valueOf(to1) + axis2 + String.valueOf(to2) + "F9000";
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(command));
    }

    private void MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE(String axis, double to)
    {
        String command = "G91 " + axis + String.valueOf(to) + "F3000";
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(command));
        command = "G90";
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

    private String MoveEndmillToToolSetter(int distance, int feedRate)
    {
        String gCodeStr = "G38.2Z-" + distance + "F" + feedRate;
        final GCodeCommand command = new GCodeCommand(gCodeStr);
        return ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(command);
    }

    private void ChangeWorkPositionWithMachinePosition(String axis) throws Exception
    {
        String value = "";
        AskForMachineStatus();
        axis = axis.toLowerCase();

        switch (axis)
        {
            case "x":
                value = String.format("%.3f", ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getX());
                break;

            case "y":
                value = String.format("%.3f", ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY());
                break;

            case "z":
                value = String.format("%.3f", ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ());
                break;
        }

        String commandStr = "G92 " + axis.toUpperCase() + value;
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(commandStr));
        AskForMachineStatus();
    }

    private void ChangeWorkPositionWithValue(String axis, double value) throws Exception
    {
        String commandStr = "G92 " + axis.toUpperCase() + value;
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(commandStr));
        AskForMachineStatus();
    }
}
