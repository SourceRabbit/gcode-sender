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

    private long fLastTimeAskedForMachineStatus = System.currentTimeMillis();

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
                    fIsTheFirstToolChangeInTheGCodeCycle = true;
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

        // WAIT FOR MACHINE TO STOP MOVING !!!!
        WaitForMachineToStopMoving();

        try
        {
            // Step 1 - Get current Work and Machine positions
            final double workPositionXBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX();
            final double workPositionYBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY();
            final double workPositionZBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getZ();

            final double machinePositionXBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getX();
            final double machinePositionYBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY();
            final double machinePositionZBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ();

            frmControl.fInstance.WriteToConsole("Semi auto tool change process started!");

            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Step 2 - Raise endmill to safe distance and go to Tool Setters X And Y (Machine Position)
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            RaiseEndmillToMachineZMax();
            Step_2_GoToToolSetterXAndY_MachinePosition_G53();
            // Inform user to turn off the spindle if it is the first Tool Change or to Change Tool
            frmControl.fInstance.WriteToConsole(fIsTheFirstToolChangeInTheGCodeCycle ? "Turn off the Spindle and press the resume button." : "Change Tool " + command.getCommand() + " and press the 'Resume' button.");
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().PauseSendingGCode();
            AskForMachineStatus();

            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Step 3 - Touch the tool setter
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            Step_3_TouchTheToolSetter();
            AskForMachineStatus();

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
                frmControl.fInstance.WriteToConsole("Distance between Work Z0 and Tool Setter is " + fTravelBetweenWorkZeroAndToolSetterTop);
            }

            if (fTravelBetweenWorkZeroAndToolSetterTop > 0)
            {
                // Tool setter is LOWER than Work ZERO!
                // Move endmill back to fTravelBetweenWorkZeroAndToolSetterTop
                MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE("Z", fTravelBetweenWorkZeroAndToolSetterTop);
                AskForMachineStatus();
                /////////////////////////////////////////////////////////////////////////////////
                // Move Endmill to Z ZERO POINT !!!!!!!!!!!!
                /////////////////////////////////////////////////////////////////////////////////
                ChangeWorkPositionWithValue("Z", 0);
                AskForMachineStatus();
                /////////////////////////////////////////////////////////////////////////////////
                RaiseEndmillToMachineZMax();
            }
            else
            {
                // <=0
                // TODO
            }

            AskForMachineStatus();

            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Step 4 - Go Back to Work X,Y before tool change
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            frmControl.fInstance.WriteToConsole("Turn on the Spindle and press the 'Resume' button.");
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().PauseSendingGCode();
            AskForMachineStatus();
            Step_4_GoBackToMachineX_Y_BeforeToolChange_G53(machinePositionXBeforeToolChange, machinePositionYBeforeToolChange);

        }
        catch (Exception ex)
        {

        }

        // Go back to work zero position before tool change
        //MoveFromPositionToPosition_ABSOLUTE("Z", workPositionZBeforeToolChange);
    }

    private void RaiseEndmillToMachineZMax() throws InterruptedException
    {
        // Raise endmill to safe distance
        String raiseEndmillCommand = "G53 G0 Z-2.000";
        String response = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(raiseEndmillCommand));
        System.out.println("RaiseEndmillToMachineZMax Response:" + response);
        // WAIT FOR MACHINE TO STOP MOVING
        WaitForMachineToStopMoving();
    }

    private void Step_2_GoToToolSetterXAndY_MachinePosition_G53() throws InterruptedException
    {
        // Move to tool setter's X and Y
        String goToToolSetterCommand = "G53 G0" + " X" + String.valueOf(SemiAutoToolChangeSettings.getToolSetterX()) + " Y" + String.valueOf(SemiAutoToolChangeSettings.getToolSetterY());
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(goToToolSetterCommand));
        // WAIT FOR MACHINE TO STOP MOVING
        WaitForMachineToStopMoving();
    }

    private void Step_3_TouchTheToolSetter() throws Exception
    {
        frmControl.fInstance.WriteToConsole("Touching the tool setter...");
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StartUsingTouchProbe();
        MoveEndmillToToolSetter(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.fZMaxTravel - 6, 700);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(new GCodeCommand("G0G90"));
        WaitForMachineToStopMoving();

        // Move end mill to tool setter slower this time
        MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE("Z", 6);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StartUsingTouchProbe();
        MoveEndmillToToolSetter(6, 60);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(new GCodeCommand("G0G90"));
        WaitForMachineToStopMoving();
    }

    private void Step_4_GoBackToMachineX_Y_BeforeToolChange_G53(double machinePositionXBeforeToolChange, double machinePositionYBeforeToolChange) throws InterruptedException
    {
        String goBackToOriginalMachinePosition = "G53";
        goBackToOriginalMachinePosition += " G0";
        goBackToOriginalMachinePosition += " X" + String.valueOf(machinePositionXBeforeToolChange);
        goBackToOriginalMachinePosition += " Y" + String.valueOf(machinePositionYBeforeToolChange);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(goBackToOriginalMachinePosition));

        // WAIT FOR MACHINE TO STOP MOVING
        WaitForMachineToStopMoving();
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

    private void WaitForMachineToStopMoving()
    {
        frmControl.fInstance.WriteToConsole("Waiting for machine to stop moving...");
        double tempX = 0, tempY = 0, tempZ = 0;
        boolean machineIsMoving = false;
        do
        {
            machineIsMoving = false;
            if (tempX != ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX())
            {
                tempX = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX();
                machineIsMoving = true;
            }

            if (tempY != ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY())
            {
                tempY = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY();
                machineIsMoving = true;
            }

            if (tempZ != ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getZ())
            {
                tempZ = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getZ();
                machineIsMoving = true;
            }
            AskForMachineStatus();
        }
        while (machineIsMoving);
    }

    /**
     * Send the COMMAND_GET_STATUS (?) to the controller
     */
    private void AskForMachineStatus()
    {
        // Wait 800 milliseconds between AskForMachineStatus method calls
        long timeNow = System.currentTimeMillis();
        if (timeNow - fLastTimeAskedForMachineStatus < 800)
        {
            try
            {
                Thread.sleep(800);
            }
            catch (Exception ex)
            {

            }
        }

        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(GRBLCommands.COMMAND_GET_STATUS));
        fLastTimeAskedForMachineStatus = System.currentTimeMillis();
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
