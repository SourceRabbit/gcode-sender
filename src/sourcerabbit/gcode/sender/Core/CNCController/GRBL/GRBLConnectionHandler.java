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
import sourcerabbit.gcode.sender.Core.CNCController.CNCControllFrameworks.ECNCControlFrameworkID;
import sourcerabbit.gcode.sender.Core.CNCController.CNCControllFrameworks.ECNCControlFrameworkVersion;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeExecutionEvents.GCodeExecutionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.SerialConnectionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.ISerialConnectionEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHandler;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.MachineStatusEvent;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLStatusReporting.GRBLStatusReportParser;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLStatusReporting.GRBL_0_9_StatusReportParser;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLStatusReporting.GRBL_1_1_StatusReportParser;
import sourcerabbit.gcode.sender.Core.Settings.GCodeSenderSettings;
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
    private ManualResetEvent fWaitForGetStatusCommandReply;

    // Status thread and Parser
    private GRBLStatusReportParser fMyStatusReportParser;
    private long fLastMachinePositionReceivedTimestamp;
    private boolean fKeepStatusThread = false;
    private Thread fStatusThread;

    // This holds the active command that has been send to GRBL controller for execution
    private GCodeCommand fLastCommandSentToController = null;
    private String fGCodeCommandResponse = "";

    private boolean fMachineSettingsAsked = false;

    public GRBLConnectionHandler()
    {
        super.fMessageSplitter = "\n";
        super.fMessageSplitterLength = fMessageSplitter.length();
        super.fMessageSplitterBytes = String.valueOf(fMessageSplitter).getBytes();

        // Set my control framework ID
        super.fMyControlFrameworkID = ECNCControlFrameworkID.GRBL;

        fWaitForCommandToBeExecuted = new ManualResetEvent(false);

        InitEvents();
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

            if (receivedStr.equals(""))
            {
                return;
            }
            //System.out.println("Data received:" + receivedStr);

            if (receivedStr.startsWith("<"))
            {
                if (isShowVerboseOutputEnabled())
                {
                    fSerialConnectionEventManager.FireDataReceivedFromSerialConnectionEvent(new SerialConnectionEvent(receivedStr));
                }

                // Ask the appropriate GRBL Status Parser to parse the new Status Message
                // and get the current Active State of the machine.
                int currentActiveState = fMyStatusReportParser.ParseStatusReportMessageAndReturnActiveState(receivedStr);

                //System.out.println(receivedStr);
                //////////////////////////////////////////////////////////////////////////////////////////////////////
                // Check if the machine status changed
                //////////////////////////////////////////////////////////////////////////////////////////////////////
                if (currentActiveState != fActiveState)
                {
                    fActiveState = currentActiveState;

                    // Fire the MachineStatusChangedEvent
                    fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(fActiveState, ""));
                }

                if (fActiveState == GRBLActiveStates.IDLE)
                {
                    if (!fMachineSettingsAsked)
                    {
                        // The first time the machine goes to Idle ask the controller settings 
                        // in order to get the max X,Y and Z travels.
                        SendData("$$");
                        fMachineSettingsAsked = true;
                    }
                }
                else
                {
                    if (fActiveState == GRBLActiveStates.ALARM || fActiveState == GRBLActiveStates.MACHINE_IS_LOCKED || fActiveState == GRBLActiveStates.RESET_TO_CONTINUE)
                    {
                        fMachineSettingsAsked = false;
                    }
                }

                // Set the fLastMachinePositionReceivedTimestamp value and the 
                // WaitForGetStatusCommandReply manual reset event.
                fLastMachinePositionReceivedTimestamp = System.currentTimeMillis();
                fWaitForGetStatusCommandReply.Set();

            }
            else
            {
                //System.out.println("Data received:" + receivedStr);
                fGCodeCommandResponse = receivedStr;
                if (receivedStr.equals("ok"))
                {
                    if (fLastCommandSentToController != null)
                    {
                        fGCodeExecutionEventsManager.FireGCodeExecutedSuccessfully(new GCodeExecutionEvent(fLastCommandSentToController));
                        fLastCommandSentToController = null;
                        fWaitForCommandToBeExecuted.Set();
                    }
                }
                else
                {
                    if (receivedStr.startsWith("error"))
                    {
                        String errorMessage = "";

                        // Get the error message
                        switch (fMyControlFrameworkVersion)
                        {
                            case GRBL0_9:
                                errorMessage = receivedStr;
                                break;

                            case GRBL1_1:
                                String errorID = receivedStr.toLowerCase().replace("error", "").replace(":", "").trim();
                                int errorIDInt = Integer.parseInt(errorID);
                                errorMessage = GRBLErrorCodes.getErrorMessageFromCode(errorIDInt);
                                fGCodeCommandResponse = "error: " + errorMessage;
                                break;
                        }

                        System.err.println("GRBLConnectionHander Error: " + errorMessage + "---> Last Command: " + fLastCommandSentToController.getCommand());
                        frmControl.fInstance.WriteToConsole("Error: " + errorMessage + "---> Last Command: " + fLastCommandSentToController.getCommand());

                        fLastCommandSentToController.setError(errorMessage);
                        fGCodeExecutionEventsManager.FireGCodeExecutedWithError(new GCodeExecutionEvent(fLastCommandSentToController));
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
                    }
                    else if (receivedStr.startsWith("ALARM"))
                    {
                        // ALARM is ON!
                        // Machine propably needs to be unlocked
                        fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.ALARM, ""));
                        fMyGCodeSender.CancelSendingGCode();
                        fWaitForCommandToBeExecuted.Set();
                    }
                    else if (receivedStr.equals("[MSG:'$H'|'$X' to unlock]") || receivedStr.equals("['$H'|'$X' to unlock]"))
                    {
                        // If the machine is in an Alarm state and the user choose to do a "soft reset"
                        // then the GRBL controller lockes and needs to be unlocked.
                        fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.MACHINE_IS_LOCKED, ""));
                        fMyGCodeSender.CancelSendingGCode();
                        fWaitForCommandToBeExecuted.Set();
                    }
                    else if (receivedStr.startsWith("[PRB:"))
                    {
                        //System.out.println("Endmill touched the Touch Probe!");
                        fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.MACHINE_TOUCHED_PROBE, receivedStr));
                        /////////////////////////////////////////////////////
                        // DO NOT SET THE fWaitForCommandToBeExecuted !!!
                        // GRBL sends an "OK" back.
                        // fWaitForCommandToBeExecuted.Set();
                        ////////////////////////////////////////////////////
                    }
                    else if (receivedStr.toLowerCase().startsWith("grbl"))
                    {
                        // Parse the GRBL "Welcome Message" and find out which GRBL version 
                        // is running on the controller.                    
                        // From the GRBL version set the appropriate CNCControlFrameworkVersion
                        // and StatusReportParser.
                        if (receivedStr.toLowerCase().contains("grbl 1."))
                        {
                            fMyStatusReportParser = new GRBL_1_1_StatusReportParser(this);
                            this.setCNCControlFrameworkVersion(ECNCControlFrameworkVersion.GRBL1_1);
                        }
                        else
                        {
                            fMyStatusReportParser = new GRBL_0_9_StatusReportParser(this);
                            this.setCNCControlFrameworkVersion(ECNCControlFrameworkVersion.GRBL0_9);
                        }

                        // To Inform UI that connection with the controller is sucessful fire the ConnectionEstablishedEvent
                        fConnectionEstablished = true;
                        fConnectionEstablishedManualResetEvent.Set();
                        fSerialConnectionEventManager.FireConnectionEstablishedEvent(new SerialConnectionEvent(receivedStr));
                        fSerialConnectionEventManager.FireDataReceivedFromSerialConnectionEvent(new SerialConnectionEvent(receivedStr));
                        fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.IDLE, ""));
                    }
                    else
                    {
                        fSerialConnectionEventManager.FireDataReceivedFromSerialConnectionEvent(new SerialConnectionEvent(receivedStr));
                        fLastCommandSentToController = null;
                        fWaitForCommandToBeExecuted.Set();
                    }
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
            final String optimizedCommand = command.getOptimizedCommand();
            if (optimizedCommand.equals(""))
            {
                return true;
            }

            //System.out.println("Data Sent: " + optimizedCommand);
            fLastCommandSentToController = command;

            if (!fLastCommandSentToController.getComment().equals(""))
            {
                this.getSerialConnectionEventManager().FireDataReceivedFromSerialConnectionEvent(new SerialConnectionEvent("Last Comment: " + fLastCommandSentToController.getComment()));
            }

            // Fire GCodeCommandSentToController
            this.getGCodeExecutionEventsManager().FireGCodeCommandSentToController(new GCodeExecutionEvent(command));

            // Command has comment !
            if (!command.getComment().equals(""))
            {
                this.getGCodeExecutionEventsManager().FireGCodeCommandHasComment(new GCodeExecutionEvent(command));
            }

            // Reset fWaitForCommandToBeExecuted manual reset event
            fWaitForCommandToBeExecuted.Reset();

            // Send data !!!!
            if (super.SendData(optimizedCommand))
            {
                // Wait for "ok" or "error:" to come back
                fWaitForCommandToBeExecuted.WaitOne();
            }
            else
            {
                try
                {
                    CloseConnection();
                }
                catch (Exception ex)
                {
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Send GCodeCommand and wait for response
     *
     * @param command
     * @return
     */
    @Override
    public String SendGCodeCommandAndGetResponse(GCodeCommand command
    )
    {
        fGCodeCommandResponse = "";
        try
        {
            SendGCodeCommand(command);
        }
        catch (Exception ex)
        {
        }

        return fGCodeCommandResponse;
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
                        if ((System.currentTimeMillis() - fLastMachinePositionReceivedTimestamp) > GCodeSenderSettings.getStatusPollRate())
                        {
                            try
                            {
                                // If a Touch Probe operation is currently active 
                                // do not ask for the machine status. Also do not ask for machine status
                                // if the GCodeSender is cycling GCode.
                                if (fAProcessIsUsingTouchProbe || fMyGCodeSender.IsCyclingGCode())
                                {
                                    Thread.sleep(100);
                                    continue;
                                }

                                // Ask for status report           
                                fWaitForGetStatusCommandReply = new ManualResetEvent(false);
                                //fWaitForGetStatusCommandReply.Reset();

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
    private boolean AskForMachineStatus()
    {
        synchronized (fSendDataLock)
        {
            try
            {
                return SendData(GRBLCommands.COMMAND_GET_STATUS);
            }
            catch (Exception ex)
            {
                return false;
            }
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
            // Wait 2 seconds to establish connection.
            // If we dont get a reply from the board then we send the Soft Reset command.
            Thread th = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    // Wait for 2 seconds max.
                    fConnectionEstablishedManualResetEvent.WaitOne(2000);

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
        super.CloseConnection();
        fWaitForCommandToBeExecuted.Set();
    }
}
