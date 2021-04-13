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

import jssc.SerialPortException;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeExecutionEvents.GCodeExecutionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.*;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.*;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.*;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLStatusReporting.*;
import sourcerabbit.gcode.sender.Core.CNCController.Processes.Process_Homing;
import sourcerabbit.gcode.sender.Core.Threading.ManualResetEvent;
import sourcerabbit.gcode.sender.UI.frmControl;

/**
 *
 * @author Nikos Siatras
 */
public class GRBLConnectionHandler extends ConnectionHandler
{

    // Commands
    private final Object fSendDataLock = new Object();
    private final ManualResetEvent fWaitForCommandToBeExecuted;

    // Ask for machine status
    private long fLastTimeAskedForMachineStatus = System.currentTimeMillis();
    private ManualResetEvent fWaitForGetStatusCommandReply;

    // Status thread and Parser
    private GRBLStatusReportParser fMyStatusReportParser;

    private boolean fKeepStatusThread = false;
    private Thread fStatusThread;

    // This holds the active command that has been send to GRBL controller for execution
    private GCodeCommand fLastCommandSentToController = null;
    private String fResponseOfTheLastCommandSendToController = "";

    private boolean fMachineSettingsAsked = false;

    public GRBLConnectionHandler()
    {
        super.fMessageSplitter = "\n";
        super.fMessageSplitterLength = fMessageSplitter.length();
        super.fMessageSplitterBytes = String.valueOf(fMessageSplitter).getBytes();

        fWaitForCommandToBeExecuted = new ManualResetEvent(false);

        InitEvents();

        GRBLErrorCodes.Initialize();
    }

    /**
     * Initialize events needed for this connection handler
     */
    private void InitEvents()
    {

        // Connection Closed Event
        super.fSerialConnectionEventManager.AddListener(new ISerialConnectionEventListener()
        {
            @Override
            public void ConnectionEstablished(SerialConnectionEvent evt)
            {
                StartStatusReportThread();
            }

            @Override
            public void ConnectionClosed(SerialConnectionEvent evt)
            {
                StopStatusReportThread();
            }

            @Override
            public void DataReceivedFromSerialConnection(SerialConnectionEvent evt)
            {
                // Do nothing!
            }
        });
    }

    @Override
    public void OnDataReceived(byte[] data)
    {
        String receivedStr = "";
        try
        {
            receivedStr = new String(data).replace("\r", "").trim();
            fResponseOfTheLastCommandSendToController = receivedStr;

            if (receivedStr.equals(""))
            {
                // Do nothing
                return;
            }

            if (receivedStr.startsWith("<"))
            {

                // Ask the appropriate GRBL Status Parser to parse the new Status Message
                // and get the current Active State of the machine.
                int newActiveState = fMyStatusReportParser.ParseStatusReportMessageAndReturnActiveState(receivedStr);

                 //////////////////////////////////////////////////////////////////////////////////////////////////////
                // Check if the machine status changed from homing to Idle
                //////////////////////////////////////////////////////////////////////////////////////////////////////
                if (fActiveState == GRBLActiveStates.HOME && newActiveState == GRBLActiveStates.IDLE)
                {
                    // If the status changed from homing to idle
                    // get the home position
                    fXHomePosition = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getX();
                    fYHomePosition = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY();
                    fZHomePosition = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ();
 
                    frmControl.fInstance.WriteToConsole("Home Coordinates: X:" + String.valueOf(fXHomePosition) + ", Y:" + String.valueOf(fYHomePosition) + ", Z:" + String.valueOf(fZHomePosition));
                }   


                //////////////////////////////////////////////////////////////////////////////////////////////////////
                // Check if the machine status changed
                //////////////////////////////////////////////////////////////////////////////////////////////////////
                if (newActiveState != fActiveState)
                {
                    fActiveState = newActiveState;

                    // Fire the MachineStatusChangedEvent
                    fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(newActiveState, ""));
                }

                ///////////////////////////////////////////////////////////////////////////////////////////////////
                // FIRE FireMachineStatusReceived event all the time!!!!!!!!
                ///////////////////////////////////////////////////////////////////////////////////////////////////
                fMachineStatusEventsManager.FireMachineStatusReceived(new MachineStatusEvent(newActiveState, ""));
                ///////////////////////////////////////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////////////////////////////////////////////
                // The first Time the machine is in IDLE status
                // Ask for the controller Settings ($)
                ///////////////////////////////////////////////////////////////////////////////////////////////////////
                if (fActiveState == GRBLActiveStates.IDLE && !fMachineSettingsAsked)
                {
                    // The first time the machine goes to Idle ask the controller settings 
                    // in order to get the max X,Y and Z travels.
                    SendData("$$");
                    fMachineSettingsAsked = true;
                }
                else if (fActiveState == GRBLActiveStates.ALARM || fActiveState == GRBLActiveStates.MACHINE_IS_LOCKED || fActiveState == GRBLActiveStates.RESET_TO_CONTINUE)
                {
                    fMachineSettingsAsked = false;
                }

                ///////////////////////////////////////////////////////////////////////////////////////////////////////
                ///////////////////////////////////////////////////////////////////////////////////////////////////////
                // Set the fLastMachinePositionReceivedTimestamp value and the 
                // WaitForGetStatusCommandReply manual reset event.
                fLastMachineStatusReceivedTimestamp = System.currentTimeMillis();
                fWaitForGetStatusCommandReply.Set();
            }
            else
            {
                if (receivedStr.equals("ok"))
                {
                    if (fLastCommandSentToController != null)
                    {
                        fGCodeExecutionEventsManager.FireGCodeExecutedSuccessfully(new GCodeExecutionEvent(fLastCommandSentToController));
                        fLastCommandSentToController = null;
                        fWaitForCommandToBeExecuted.Set();
                    }
                }
                else if (receivedStr.startsWith("error"))
                {
                    String errorMessage = "";

                    // Get the error message
                    switch (fControllerGRBLVersion)
                    {
                        case GRBL0_9:
                            errorMessage = receivedStr;
                            fResponseOfTheLastCommandSendToController = "Error: " + receivedStr;
                            break;

                        case GRBL1_1:
                            String errorID = receivedStr.toLowerCase().replace("error", "").replace(":", "").trim();
                            int errorIDInt = Integer.parseInt(errorID);
                            errorMessage = GRBLErrorCodes.getErrorMessageFromCode(errorIDInt);
                            fResponseOfTheLastCommandSendToController = "Error: " + errorMessage;
                            break;
                    }

                    if (fLastCommandSentToController != null)
                    {
                        fLastCommandSentToController.setError(fResponseOfTheLastCommandSendToController);
                        fGCodeExecutionEventsManager.FireGCodeExecutedWithError(new GCodeExecutionEvent(fLastCommandSentToController));

                        if (fLastCommandSentToController.getCommand() == null)
                        {
                            // In case the command text is EMPTY
                            frmControl.fInstance.WriteToConsole("Error-->" + errorMessage + "\n");
                        }
                        else
                        {
                            frmControl.fInstance.WriteToConsole("Error-->Line:" + String.valueOf(fLastCommandSentToController.getLineNumber()) + " | Command:" + fLastCommandSentToController.getCommand() + "\n  " + errorMessage + "\n");
                        }
                    }

                    fLastCommandSentToController = null;
                    fWaitForCommandToBeExecuted.Set();
                }
                else if (receivedStr.startsWith("$"))
                {
                    // READ CONTROLLER SETTINGS HERE !!!!
                    if (receivedStr.startsWith("$130"))
                    {
                        fXMaxTravel = (int) Double.parseDouble(receivedStr.replace("$130=", ""));
                    }
                    else if (receivedStr.startsWith("$131"))
                    {
                        fYMaxTravel = (int) Double.parseDouble(receivedStr.replace("$131=", ""));
                    }
                    else if (receivedStr.startsWith("$132"))
                    {
                        fZMaxTravel = (int) Double.parseDouble(receivedStr.replace("$132=", ""));
                    }

                    fSerialConnectionEventManager.FireDataReceivedFromSerialConnectionEvent(new SerialConnectionEvent(receivedStr));
                    fLastCommandSentToController = null;
                    fWaitForCommandToBeExecuted.Set();
                }
                else if (receivedStr.startsWith("ALARM"))
                {
                    // ALARM is ON!
                    // Machine propably needs to be unlocked
                    fMyGCodeSender.CancelSendingGCode(true);
                    fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.ALARM, ""));
                    fMachineStatusEventsManager.FireMachineStatusReceived(new MachineStatusEvent(GRBLActiveStates.ALARM, ""));
                    fLastCommandSentToController = null;
                    fWaitForCommandToBeExecuted.Set();

                    // Get alarm ID
                    try
                    {
                        String[] parts = receivedStr.split(":");
                        int alarmID = Integer.parseInt(parts[1]);
                        frmControl.fInstance.WriteToConsole("ALARM:" + GRBLAlarmCodes.getAlarmMessageByID(alarmID));
                    }
                    catch (Exception ex)
                    {
                        frmControl.fInstance.WriteToConsole("ALARM: Unidentified alarm!");
                    }

                }
                else if (receivedStr.equals("[MSG:'$H'|'$X' to unlock]") || receivedStr.equals("['$H'|'$X' to unlock]"))
                {
                    // If the machine is in an Alarm state and the user choose to do a "soft reset"
                    // then the GRBL controller lockes and needs to be unlocked.
                    fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.MACHINE_IS_LOCKED, ""));
                    fMyGCodeSender.CancelSendingGCode(false);
                    fLastCommandSentToController = null;
                    fWaitForCommandToBeExecuted.Set();
                }
                else if (receivedStr.equals("[MSG:Reset to continue]"))
                {
                    // MACHINE HAS TO RESET TO CONTINUE!!!!!!!!!!!!!!!!!!!
                    fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.RESET_TO_CONTINUE, ""));
                    fMachineStatusEventsManager.FireMachineStatusReceived(new MachineStatusEvent(GRBLActiveStates.RESET_TO_CONTINUE, ""));
                    fMyGCodeSender.CancelSendingGCode(false);
                    fLastCommandSentToController = null;
                    fWaitForCommandToBeExecuted.Set();

                    //////////////////////////////////////////////////////////////////////////////
                    // ALSO SET THE fWaitForGetStatusCommandReply 
                    fWaitForGetStatusCommandReply.Set();
                    //////////////////////////////////////////////////////////////////////////////
                }
                else if (receivedStr.startsWith("[PRB:"))
                {
                    //////////////////////////////////////////////////////////////////////////////
                    // Endmill just touched the probe!
                    // Fire the Machine Status event with GRBLActiveStates.MACHINE_TOUCHED_PROBE
                    fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.MACHINE_TOUCHED_PROBE, receivedStr));
                    fMachineStatusEventsManager.FireMachineStatusReceived(new MachineStatusEvent(GRBLActiveStates.MACHINE_TOUCHED_PROBE, receivedStr));
                    //////////////////////////////////////////////////////////////////////////////
                    //////////////////////////////////////////////////////////////////////////////

                    /////////////////////////////////////////////////////
                    // DO NOT SET THE fWaitForCommandToBeExecuted !!!
                    // GRBL sends an "OK" back.
                    // (fWaitForCommandToBeExecuted.Set();)
                    ////////////////////////////////////////////////////
                }
                else if (receivedStr.toLowerCase().startsWith("grbl"))
                {
                    ConnectionHelper.AUTO_TOOL_CHANGE_OPERATION_IS_ACTIVE = false;
                    ConnectionHelper.A_PROCESS_IS_USING_TOUCH_PROBE = false;

                    // Parse the GRBL "Welcome Message" and find out which GRBL version is running on the controller.                    
                    // From the GRBL version set the appropriate CNCControlFrameworkVersion
                    // and the appropriate StatusReportParser
                    if (receivedStr.toLowerCase().contains("grbl 1."))
                    {
                        this.setCNCControlFrameworkVersion(EGRBLVersion.GRBL1_1);
                        fMyStatusReportParser = new GRBL_1_1_StatusReportParser(this);
                    }
                    else
                    {
                        this.setCNCControlFrameworkVersion(EGRBLVersion.GRBL0_9);
                        fMyStatusReportParser = new GRBL_0_9_StatusReportParser(this);
                    }

                    // To Inform UI that connection with the controller is successful and fire the ConnectionEstablishedEvent
                    fConnectionEstablished = true;
                    fConnectionEstablishedManualResetEvent.Set();
                    fSerialConnectionEventManager.FireConnectionEstablishedEvent(new SerialConnectionEvent(receivedStr));
                    fSerialConnectionEventManager.FireDataReceivedFromSerialConnectionEvent(new SerialConnectionEvent(receivedStr));
                    fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.IDLE, ""));
                }
                else if (receivedStr.toLowerCase().startsWith("[msg"))
                {
                    //String message = receivedStr.substring(5, receivedStr.length() - 1);
                    //fSerialConnectionEventManager.FireDataReceivedFromSerialConnectionEvent(new SerialConnectionEvent(message));
                    fLastCommandSentToController = null;
                    fWaitForCommandToBeExecuted.Set();
                }
                else
                {
                    //fSerialConnectionEventManager.FireDataReceivedFromSerialConnectionEvent(new SerialConnectionEvent(receivedStr));
                    fLastCommandSentToController = null;
                    fWaitForCommandToBeExecuted.Set();
                }
            }
        }
        catch (Exception ex)
        {
            System.err.println("GRBLConnectionHandler.OnDataReceived Error: " + ex.getMessage() + " Data: " + receivedStr);
        }
    }

    @Override
    public boolean SendGCodeCommand(GCodeCommand command) throws SerialPortException
    {
        synchronized (fSendDataLock)
        {
            //Step 1 - Optimize the command String
            final String optimizedCommand = command.getOptimizedCommand();

            // Step 2 - Check Special commands
            switch (optimizedCommand)
            {
                case "":
                    // In case the command is empty then return True
                    return true;

                case "$H":
                    // HOMING COMMAND
                    // Start a new Process_Homing in order to not pause the UI
                    Process_Homing homingProcess = new Process_Homing(null);
                    homingProcess.ExecuteInNewThread();
                    return true;
            }

            // Step 3 - Reset fWaitForCommandToBeExecuted manual reset event
            fResponseOfTheLastCommandSendToController = "";
            fLastCommandSentToController = command;
            fWaitForCommandToBeExecuted.Reset();

            // Step 4 - TRY to SEND the optimizedCommand to the control board.
            boolean commandSentToControllerWithSuccess = false;
            try
            {
                commandSentToControllerWithSuccess = super.SendData(optimizedCommand);

                if (commandSentToControllerWithSuccess)
                {
                    // Fire GCodeCommandSentToController event
                    this.getGCodeExecutionEventsManager().FireGCodeCommandSentToController(new GCodeExecutionEvent(command));
                    fWaitForCommandToBeExecuted.WaitOne();
                }
                else
                {
                    CommandWasNotSentToControlBoardWithSuccess(command);
                }
            }
            catch (SerialPortException ex)
            {
                CommandWasNotSentToControlBoardWithSuccess(command);
                throw ex;
            }

            // Since the command has been sucessfully sent to the controller
            // dispose it!
            command.Dispose();

            return commandSentToControllerWithSuccess;
        }
    }

    /**
     * This is called internally from the SendGCodeCommand method when the
     * command is not able to reach the control board
     *
     * @param command
     */
    private void CommandWasNotSentToControlBoardWithSuccess(GCodeCommand command)
    {
        try
        {
            if (command.getLineNumber() > -1)
            {
                frmControl.fInstance.WriteToConsole("Error: Unable to send command " + command.getCommand() + "(Line: " + command.getLineNumber() + ") to controller! Connection is closed!");
                fWaitForCommandToBeExecuted.Set();
            }
            else
            {
                frmControl.fInstance.WriteToConsole("Error: Unable to send command " + command.getCommand() + " to controller! Connection is closed!");
                fWaitForCommandToBeExecuted.Set();
            }
        }
        catch (Exception ex)
        {

        }
        try
        {
            CloseConnection();
        }
        catch (Exception ex)
        {
        }
    }

    /**
     * Send GCodeCommand and wait for response
     *
     * @param command
     * @return
     *
     */
    @Override
    public String SendGCodeCommandAndGetResponse(GCodeCommand command)
    {
        synchronized (fSendDataLock)
        {
            try
            {
                SendGCodeCommand(command);
                return fResponseOfTheLastCommandSendToController;
            }
            catch (SerialPortException ex)
            {
                return fResponseOfTheLastCommandSendToController;
            }
        }
    }

    /**
     * Start a thread to ask and receive the GRBL controller's status.
     */
    private void StartStatusReportThread()
    {
        if (fConnectionEstablished)
        {
            // Start the status thread
            fStatusThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    while (fKeepStatusThread)
                    {
                        if ((System.currentTimeMillis() - fLastMachineStatusReceivedTimestamp) > GRBLConstants.MILLISECONDS_TO_ASK_FOR_MACHINE_STATUS_WHEN_CONTROLLER_IS_IDLE)
                        {
                            try
                            {
                                ///////////////////////////////////////////////////////////////////////////////////////////
                                // If a Touch Probe operation is currently active 
                                // or the controller is Cycling GCode 
                                // DO NOT ask for the machine status 
                                if (ConnectionHelper.A_PROCESS_IS_USING_TOUCH_PROBE || fMyGCodeSender.IsCyclingGCode())
                                {
                                    Thread.sleep(50);
                                    continue;
                                }
                                ///////////////////////////////////////////////////////////////////////////////////////////
                                ///////////////////////////////////////////////////////////////////////////////////////////

                                // Ask for status report           
                                fWaitForGetStatusCommandReply = new ManualResetEvent(false);

                                if (AskForMachineStatus())
                                {
                                    // Wait for Get Status Command Reply
                                    fWaitForGetStatusCommandReply.WaitOne();
                                }
                                else
                                {
                                    CloseConnection();
                                }
                            }
                            catch (Exception ex)
                            {
                                System.err.println("StartStatusReportThread Error:" + ex.getMessage());
                                fWaitForGetStatusCommandReply.Set();
                            }
                        }

                        try
                        {
                            Thread.sleep(100);
                        }
                        catch (Exception ex)
                        {
                        }
                    }
                }
            });
            fKeepStatusThread = true;
            fStatusThread.setPriority(Thread.NORM_PRIORITY);
            fStatusThread.start();
        }
    }

    /**
     * Send the '?' command to GRBL and ask for the machine status
     *
     * @return true if the '?' can be sent
     */
    @Override
    public boolean AskForMachineStatus()
    {
        try
        {
            //////////////////////////////////////////////////////////////////////////////////////////
            // Wait MILLISECONDS_TO_ASK_FOR_MACHINE_STATUS_WHEN_CONTROLLER_IS_IDLE between asking for machine status
            //////////////////////////////////////////////////////////////////////////////////////////
            long timeNow = System.currentTimeMillis();
            if ((timeNow - fLastTimeAskedForMachineStatus) < GRBLConstants.MILLISECONDS_TO_ASK_FOR_MACHINE_STATUS_WHEN_CONTROLLER_IS_IDLE)
            {
                long waitFor = GRBLConstants.MILLISECONDS_TO_ASK_FOR_MACHINE_STATUS_WHEN_CONTROLLER_IS_IDLE - (timeNow - fLastTimeAskedForMachineStatus);
                Thread.sleep(waitFor);
            }

            fLastTimeAskedForMachineStatus = System.currentTimeMillis();
            return SendGCodeCommand(new GCodeCommand(GRBLCommands.COMMAND_GET_STATUS));
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    /**
     * Stop the Status Report Thread
     */
    private void StopStatusReportThread()
    {
        fKeepStatusThread = false;
        fWaitForGetStatusCommandReply.Set();
        try
        {
            fStatusThread.interrupt();
        }
        catch (Exception ex)
        {
        }
    }

    @Override
    public boolean OpenConnection(String serialPort, int baudRate) throws Exception
    {
        ////////////////////////////////////////////////////////////////////////        
        // Re-initialize the fConnectionEstablishedManualResetEvent
        ////////////////////////////////////////////////////////////////////////
        fConnectionEstablishedManualResetEvent = new ManualResetEvent(false);
        ////////////////////////////////////////////////////////////////////////

        if (super.OpenConnection(serialPort, baudRate))
        {
            // Wait 2.5 seconds to establish connection.
            // If we dont get a reply from the board then we send the Soft Reset command.
            Thread th = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    // Wait for 2.5 seconds max.
                    fConnectionEstablishedManualResetEvent.WaitOne(2500);

                    if (!fConnectionEstablished)
                    {
                        // We are not connected to the board. Try to send a reset signal to wake it up.
                        try
                        {
                            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendDataImmediately_WithoutMessageCollector(GRBLCommands.COMMAND_SOFT_RESET);
                        }
                        catch (Exception ex)
                        {

                        }
                    }

                }
            });
            th.start();

            return true;
        }

        return false;
    }

    @Override
    public void CloseConnection() throws Exception
    {
        System.err.println("Connection Closed");
        //fMyGCodeSender.CancelSendingGCode();
        fMyGCodeSender.KillGCodeCycle();
        super.CloseConnection();
        fWaitForCommandToBeExecuted.Set();
    }
}
