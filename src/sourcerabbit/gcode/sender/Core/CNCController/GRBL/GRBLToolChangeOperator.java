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

import java.math.BigDecimal;
import java.math.MathContext;
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

        try
        {
            Thread.sleep(600);
        }
        catch (Exception ex)
        {

        }
        AskForMachineStatus();

        // Step 1 - Get current work and machine positions
        //final double workPositionXBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX();
        //final double workPositionYBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY();
        final double workPositionZBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getZ();

        final double machinePositionXBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getX();
        final double machinePositionYBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY();
        final double machinePositionZBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ();

        frmControl.fInstance.WriteToConsole("Semi auto tool change process started!");
        SendPauseCommand(0.2);

        // Step 2 - Raise endmil to safe distance
        // and Go to Tool Setter X and Y using G53 Command
        // and pause until user changes the tool
        try
        {
            // Raise endmill to safe distance
            String raiseEndmillCommand = "G53 G0 Z-2";
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(raiseEndmillCommand));
            while (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ() < -2)
            {
                Thread.sleep(500);
                AskForMachineStatus();
            }

            // Move to tool setter's X and Y
            String goToToolSetterCommand = "G53 G0" + " X" + String.valueOf(SemiAutoToolChangeSettings.getToolSetterX()) + " Y" + String.valueOf(SemiAutoToolChangeSettings.getToolSetterY());
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(goToToolSetterCommand));
            while (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY() != SemiAutoToolChangeSettings.getToolSetterY())
            {
                Thread.sleep(500);
                AskForMachineStatus();
            }

            // Inform user to turn off the spindle if it is the first Tool Change or to Change Tool
            frmControl.fInstance.WriteToConsole(fIsTheFirstToolChangeInTheGCodeCycle ? "Turn off the Spindle and press the resume button." : "Change Tool " + command.getCommand() + " and press the 'Resume' button.");

            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().PauseSendingGCode();
            AskForMachineStatus();
        }
        catch (Exception ex)
        {
        }

        // Step 3 - Touch the tool setter
        try
        {
            //////////////////////////////////////////////////////////////////////////////
            // Move Endmill to the tool setter
            //////////////////////////////////////////////////////////////////////////////
            frmControl.fInstance.WriteToConsole("Touching the tool setter...");
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StartUsingTouchProbe();
            MoveEndmillToToolSetter(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.fZMaxTravel - 6, 700);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(new GCodeCommand("G0G90"));
            SendPauseCommand(0.2);
            AskForMachineStatus();

            // Move end mill to tool setter slower this time
            MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE("Z", 4);
            SendPauseCommand(0.2);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StartUsingTouchProbe();
            MoveEndmillToToolSetter(4, 60);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(new GCodeCommand("G0G90"));
            AskForMachineStatus();
            SendPauseCommand(0.2);
            //////////////////////////////////////////////////////////////////////////////

            // At this point we know that the endmill is exactly at the top of the tool setter
            // but... we do not know the distance between the work Z 0 (before the tool change) and the tool setter.
            // So if it is the FIRST TOOL CHANGE IN THE GCODE CYCLE we have to make the calculation
            // of the travel between the work Z 0 point to the Tool setter top
            if (fIsTheFirstToolChangeInTheGCodeCycle)
            {
                BigDecimal bigDecimalValue = new BigDecimal(machinePositionZBeforeToolChange, MathContext.DECIMAL64);
                bigDecimalValue = bigDecimalValue.setScale(2);
                bigDecimalValue = bigDecimalValue.subtract(new BigDecimal(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ(), MathContext.DECIMAL64));
                //////////////////////////////////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////////////////////////////////
                fTravelBetweenWorkZeroAndToolSetterTop = bigDecimalValue.doubleValue();
                //////////////////////////////////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////////////////////////////////
                fIsTheFirstToolChangeInTheGCodeCycle = false;
            }

            if (fTravelBetweenWorkZeroAndToolSetterTop < 0)
            {
                // That means the original work zero is below the tool setter height
                // So move the Z 10mm up
                double currentZ = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ();
                MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE("Z", 10);
                while (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ() < (currentZ + 10))
                {
                    Thread.sleep(500);
                    AskForMachineStatus();
                }
            }
            else
            {
                // Move endmill back to fTravelBetweenWorkZeroAndToolSetterTop
                AskForMachineStatus();
                double currentZ = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ();
                MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE("Z", fTravelBetweenWorkZeroAndToolSetterTop);
                while (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ() < (currentZ + fTravelBetweenWorkZeroAndToolSetterTop))
                {
                    Thread.sleep(500);
                    AskForMachineStatus();
                }
            }

            AskForMachineStatus();

            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // At this point the Endmill is at the original Work Z 0 point
            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if (fTravelBetweenWorkZeroAndToolSetterTop < 0)
            {
                ChangeWorkPositionWithValue("Z", Math.abs(fTravelBetweenWorkZeroAndToolSetterTop) + 10);
                SendPauseCommand(0.2);
            }
            else
            {
                ChangeWorkPositionWithValue("Z", workPositionZBeforeToolChange);
                SendPauseCommand(0.2);
            }

            // RAISE THE ENDMILL TO THE TOP IN ORDER TO AVOID WORKHOLDING
            // and inform user to turn on the spindle
            String raiseEndmillCommand = "G53 G0 Z-2";
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(raiseEndmillCommand));
            // Wait until Z moves to -2
            while (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ() < -2)
            {
                try
                {
                    Thread.sleep(800);
                    AskForMachineStatus();
                }
                catch (Exception ex)
                {

                }
            }
        }
        catch (Exception ex)
        {

        }

        // Step 4 - Ask user to turn on the Spindle and
        // --- Go back to MACHINE Position X and Y Before Tool Change
        // --- Go back to WORK work position Z Before Tool Change
        frmControl.fInstance.WriteToConsole("Turn on the Spindle and press the 'Resume' button.");
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().PauseSendingGCode();
        AskForMachineStatus();

        String goBackToOriginalMachinePosition = "G53";
        goBackToOriginalMachinePosition += " G0";
        goBackToOriginalMachinePosition += " X" + String.valueOf(machinePositionXBeforeToolChange);
        goBackToOriginalMachinePosition += " Y" + String.valueOf(machinePositionYBeforeToolChange);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(goBackToOriginalMachinePosition));
        // Wait until Y moves to machinePositionYBeforeToolChange
        while (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY() != SemiAutoToolChangeSettings.getToolSetterY())
        {
            try
            {
                Thread.sleep(800);
                AskForMachineStatus();
            }
            catch (Exception ex)
            {

            }
        }
        
        // Go back to work zero position before tool change
        MoveFromPositionToPosition_ABSOLUTE("Z", workPositionZBeforeToolChange);
    }

    private void MoveFromPositionToPosition_ABSOLUTE(String axis, double to)
    {
        String command = "G90 " + axis + String.valueOf(to) + "F3000";
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
        String gCodeStr = "G91 G38.2Z-" + distance + "F" + feedRate;
        final GCodeCommand command = new GCodeCommand(gCodeStr);
        return ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(command);
    }

    private void ChangeWorkPositionWithValue(String axis, double value) throws Exception
    {
        String commandStr = "G92 " + axis.toUpperCase() + value;
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(commandStr));
        AskForMachineStatus();
    }
}
