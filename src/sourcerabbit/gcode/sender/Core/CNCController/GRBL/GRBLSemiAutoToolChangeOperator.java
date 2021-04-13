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
public class GRBLSemiAutoToolChangeOperator
{

    private boolean fAlarmHappened = false;
    private final GRBLGCodeSender fMyGCodeSender;

    private final int fFastFeedrateToTouchToolSetter = 550;
    private final int fSlowFeedrateToTouchToolSetter = 60;

    // Tool Setter Variables
    private boolean fIsTheFirstToolChangeInTheGCodeCycle = true;
    private double fTravelBetweenWorkZeroAndToolSetterTop = 0;
    private final ManualResetEvent fWaitToTouchTheProbe = new ManualResetEvent(false);
    protected IMachineStatusEventListener fIMachineStatusEventListener = null;
    private ManualResetEvent fWaitForUserToClickResumeLock = new ManualResetEvent(false);

    // Ask for machine status
    private long fLastTimeAskedForMachineStatus = System.currentTimeMillis();
    private final Object fAskForMachineStatusLock = new Object();
    private ManualResetEvent fWaitForMachineStatusToArrive = new ManualResetEvent(false);
    private int fCurrentMachineStatus = 0;

    // Wair for machine to stop moving variables
    private final Object fMachineWaitToStopMovingLock = new Object();

    public GRBLSemiAutoToolChangeOperator(GRBLGCodeSender myGCodeSender)
    {
        fMyGCodeSender = myGCodeSender;
    }

    private void MachineStatusHasChange(int state)
    {
        switch (state)
        {
            case GRBLActiveStates.IDLE:
                break;
            case GRBLActiveStates.RUN:
                break;
            case GRBLActiveStates.HOLD:
            case GRBLActiveStates.RESET_TO_CONTINUE:
                fWaitToTouchTheProbe.Set();
                break;

            case GRBLActiveStates.ALARM:
                fAlarmHappened = true;
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
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().AddListener(new IMachineStatusEventListener()
            {
                @Override
                public void MachineStatusChanged(MachineStatusEvent evt)
                {
                    final int activeState = evt.getMachineStatus();
                    MachineStatusHasChange(activeState);
                }

                @Override
                public void MachineStatusReceived(MachineStatusEvent evt)
                {
                    // MACHINE STATUS RECEIVED!!!!
                    fWaitForMachineStatusToArrive.Set();
                    fCurrentMachineStatus = evt.getMachineStatus();
                }
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
                    fWaitForUserToClickResumeLock.Set();
                }
            });
        }
    }

    public void DoSemiAutoToolChangeSequence(GCodeCommand command)
    {
        fAlarmHappened = false;

        // SET AUTO_TOOL_CHANGE_OPERATION_IS_ACTIVE to True
        ConnectionHelper.AUTO_TOOL_CHANGE_OPERATION_IS_ACTIVE = true;

        ////////////////////////////////////////////////////
        // FIRST THING ALL THE TIME
        ////////////////////////////////////////////////////
        InitializeMachineStatusEventListener();
        ////////////////////////////////////////////////////

        // WAIT FOR MACHINE TO STOP MOVING !!!!
        WaitForMachineToStopMoving();

        try
        {
            // Step 1 - Get current Work and Machine positions
            //final double workPositionXBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX();
            //final double workPositionYBeforeToolChange = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY();
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
            frmControl.fInstance.WriteToConsole(fIsTheFirstToolChangeInTheGCodeCycle ? "Turn off the Spindle and press the resume button." : "Change Tool " + command.getCommand() + " (" + command.getComment() + ")" + " and press the 'Resume' button.");
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().PauseSendingGCode();
            WaitForUserToClickResume();

            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Step 3 - Touch the tool setter
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if (fAlarmHappened)
            {
                return;
            }
            Step_3_TouchTheToolSetter();

            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
                //frmControl.fInstance.WriteToConsole("Distance between Work Z0 and Tool Setter is " + fTravelBetweenWorkZeroAndToolSetterTop);
            }

            if (fTravelBetweenWorkZeroAndToolSetterTop > 0)
            {
                // Tool setter is LOWER than Work ZERO!
                // Move endmill back to fTravelBetweenWorkZeroAndToolSetterTop
                MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE("Z", fTravelBetweenWorkZeroAndToolSetterTop);

                /////////////////////////////////////////////////////////////////////////////////
                // At this moment the endmill is at Z zero position!!!!!!!!!!!!!
                // Set the work position to zero !!!!!!!!!
                ChangeWorkPositionWithValue("Z", 0);

                /////////////////////////////////////////////////////////////////////////////////
                // Finaly raise endmill to Z max
                RaiseEndmillToMachineZMax();
            }
            else
            {
                // Tool setter is HIGHER than Work ZERO!
                /////////////////////////////////////////////////////////////////////////////////
                // At this moment the endmill is at (+)fTravelBetweenWorkZeroAndToolSetterTop
                // Set the work position to (+)fTravelBetweenWorkZeroAndToolSetterTop
                ChangeWorkPositionWithValue("Z", Math.abs(fTravelBetweenWorkZeroAndToolSetterTop));

                /////////////////////////////////////////////////////////////////////////////////
                // Finaly raise endmill to Z max
                RaiseEndmillToMachineZMax();
            }

            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Step 4 - Go Back to Work X,Y before tool change
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            frmControl.fInstance.WriteToConsole("Turn on the Spindle and press the 'Resume' button.");
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().PauseSendingGCode();
            WaitForUserToClickResume();

            // SET AUTO_TOOL_CHANGE_OPERATION_IS_ACTIVE to FALSE
            ConnectionHelper.AUTO_TOOL_CHANGE_OPERATION_IS_ACTIVE = false;

            Step_4_GoBackToMachineX_Y_BeforeToolChange_G53(machinePositionXBeforeToolChange, machinePositionYBeforeToolChange);

            // GO back to Z work position before tool change
            MoveFromPositionToPosition_ABSOLUTE("Z", workPositionZBeforeToolChange);
        }
        catch (Exception ex)
        {
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().CancelSendingGCode(false);
            frmControl.fInstance.WriteToConsole("GRBLToolChangeOperator Error: " + ex.getMessage());
        }

        ConnectionHelper.AUTO_TOOL_CHANGE_OPERATION_IS_ACTIVE = false;
    }

    private void RaiseEndmillToMachineZMax() throws InterruptedException
    {
        if (fAlarmHappened)
        {
            return;
        }
        // Raise endmill to safe distance
        String raiseEndmillCommand = "G53 G0 Z" + String.valueOf(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.fZHomePosition);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(raiseEndmillCommand));

        // WAIT FOR MACHINE TO STOP MOVING
        WaitForMachineToStopMoving();
    }

    private void Step_2_GoToToolSetterXAndY_MachinePosition_G53() throws InterruptedException
    {
        if (fAlarmHappened)
        {
            return;
        }

        // Move to tool setter's X and Y
        String goToToolSetterCommand = "G53 G0" + " X" + String.valueOf(SemiAutoToolChangeSettings.getToolSetterX()) + " Y" + String.valueOf(SemiAutoToolChangeSettings.getToolSetterY());
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(goToToolSetterCommand));

        // WAIT FOR MACHINE TO STOP MOVING
        WaitForMachineToStopMoving();
    }

    private void Step_3_TouchTheToolSetter() throws Exception
    {
        if (fAlarmHappened)
        {
            return;
        }

        frmControl.fInstance.WriteToConsole("Touching the tool setter...");
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StartUsingTouchProbe();
        MoveEndmillToToolSetter(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.fZMaxTravel - 6, fFastFeedrateToTouchToolSetter);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(new GCodeCommand("G0G90"));

        // WAIT FOR MACHINE TO STOP MOVING
        WaitForMachineToStopMoving();

        // Move end mill to tool setter slower this time
        MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE("Z", 4);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StartUsingTouchProbe();
        MoveEndmillToToolSetter(4, fSlowFeedrateToTouchToolSetter);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(new GCodeCommand("G0G90"));

        // WAIT FOR MACHINE TO STOP MOVING
        WaitForMachineToStopMoving();
    }

    private void Step_4_GoBackToMachineX_Y_BeforeToolChange_G53(double machinePositionXBeforeToolChange, double machinePositionYBeforeToolChange) throws InterruptedException
    {
        if (fAlarmHappened)
        {
            return;
        }

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
        if (fAlarmHappened)
        {
            return;
        }

        String command = "G90 G0 " + axis + String.valueOf(to);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(command));

        // WAIT FOR MACHINE TO STOP MOVING
        WaitForMachineToStopMoving();
    }

    private void MoveFromPositionToPosition_INCREMENTAL_AND_THEN_CHANGE_TO_ABSOLUTE(String axis, double to)
    {
        if (fAlarmHappened)
        {
            return;
        }

        String command = "G91 G0 " + axis + String.valueOf(to);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(command));
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand("G90"));

        // WAIT FOR MACHINE TO STOP MOVING
        WaitForMachineToStopMoving();
    }

    private void WaitForMachineToStopMoving()
    {
        synchronized (fMachineWaitToStopMovingLock)
        {
            boolean machineIsMoving = false;
            do
            {
                AskForMachineStatus();
                if (fCurrentMachineStatus != GRBLActiveStates.IDLE)
                {
                    machineIsMoving = true;
                }
                else
                {
                    machineIsMoving = false;
                }
            }
            while (machineIsMoving && !fAlarmHappened);
        }
    }

    /**
     * Send the COMMAND_GET_STATUS (?) to the controller
     */
    private void AskForMachineStatus()
    {
        synchronized (fAskForMachineStatusLock)
        {
            fWaitForMachineStatusToArrive = new ManualResetEvent(false);

            // Wait 800 milliseconds between AskForMachineStatus method calls
            int millisecondsToWait = 900;
            long timeNow = System.currentTimeMillis();
            if (timeNow - fLastTimeAskedForMachineStatus < (millisecondsToWait))
            {
                try
                {
                    long waitFor = millisecondsToWait - (timeNow - fLastTimeAskedForMachineStatus);
                    Thread.sleep(waitFor);
                }
                catch (Exception ex)
                {

                }
            }

            fLastTimeAskedForMachineStatus = System.currentTimeMillis();
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.AskForMachineStatus();

            // Wait to receive machine status
            fWaitForMachineStatusToArrive.WaitOne();
        }
    }

    private void WaitForUserToClickResume()
    {
        fWaitForUserToClickResumeLock = new ManualResetEvent(false);
        fWaitForUserToClickResumeLock.WaitOne();
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
        String gCodeStr = "G91 G38.2 Z-" + distance + "F" + feedRate;
        final GCodeCommand command = new GCodeCommand(gCodeStr);
        return ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(command);
    }

    private void ChangeWorkPositionWithValue(String axis, double value) throws Exception
    {
        String commandStr = "G92 " + axis.toUpperCase() + value;
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(commandStr));
    }
}
