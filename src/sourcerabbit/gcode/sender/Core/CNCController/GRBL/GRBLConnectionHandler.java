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
import sourcerabbit.gcode.sender.Core.Threading.ManualResetEvent;

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

    // Status thread
    private long fLastMachinePositionReceivedTimestamp;
    private int fMillisecondsToGetMachineStatus = 300;
    private boolean fKeepStatusThread = false;
    private Thread fStatusThread;

    // This holds the active command that has been send to GRBL controller for execution
    private GCodeCommand fLastCommandSentToController = null;
    private String fGCodeCommandResponse = "";

    // Work Coordinate Offset (WCO):
    private boolean fControllerSendsWorkPosition = false;
    private Float fXOffset = 0.0f, fYOffset = 0.0f, fZOffset = 0.0f;

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
        try
        {
            String receivedStr = new String(data).replace("\r", "").trim();

            if (receivedStr.equals(""))
            {
                return;
            }

            System.out.println("Data received:" + receivedStr);
            if (receivedStr.startsWith("<"))
            {
                int newActiveState = -1;
                final String[] statusParts;

                switch (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getCNCControlFrameworkVersion())
                {
                    case GRBL0_9:
                        receivedStr = receivedStr.toLowerCase().replace("mpos", "").replace("wpos", "").replace("wco", "").replace(":", "").replace("<", "").replace(">", "");
                        // Machine status received !
                        statusParts = receivedStr.split(",");
                        newActiveState = GRBLActiveStates.getGRBLActiveStateFromString(statusParts[0]);

                        // Set Machine Position
                        fMachinePosition.setX(Float.parseFloat(statusParts[1]));
                        fMachinePosition.setY(Float.parseFloat(statusParts[2]));
                        fMachinePosition.setZ(Float.parseFloat(statusParts[3]));

                        // Set Work Position
                        fWorkPosition.setX(Float.parseFloat(statusParts[4]));
                        fWorkPosition.setY(Float.parseFloat(statusParts[5]));
                        fWorkPosition.setZ(Float.parseFloat(statusParts[6]));

                        break;

                    case GRBL1_1:
                        // Example: <Idle|MPos:0.000,0.000,0.000|FS:0,0|WCO:0.000,0.000,0.000>
                        receivedStr = receivedStr.toLowerCase().replace("<", "").replace(">", "");

                        // Get the Active State
                        statusParts = receivedStr.split("\\|");
                        newActiveState = GRBLActiveStates.getGRBLActiveStateFromString(statusParts[0]);

                        // Parse status message parts
                        for (String part : statusParts)
                        {
                            if (part.startsWith("mpos:"))
                            {
                                // Get Machine Position
                                fControllerSendsWorkPosition = false;
                                String[] machinePosition = part.replace("mpos:", "").split(",");

                                // Set Machine Position
                                fMachinePosition.setX(Float.parseFloat(machinePosition[0]));
                                fMachinePosition.setY(Float.parseFloat(machinePosition[1]));
                                fMachinePosition.setZ(Float.parseFloat(machinePosition[2]));

                                // Call the CalculateWorkCoordinateOffset method!
                                CalculateWorkCoordinateOffset();
                            }
                            else if (part.startsWith("wpos:"))
                            {
                                // Get Work Position
                                fControllerSendsWorkPosition = true;
                                String[] workPosition = part.replace("wpos:", "").split(",");

                                // Set Machine Position
                                fWorkPosition.setX(Float.parseFloat(workPosition[0]));
                                fWorkPosition.setY(Float.parseFloat(workPosition[1]));
                                fWorkPosition.setZ(Float.parseFloat(workPosition[2]));

                                // Call the CalculateWorkCoordinateOffset method!
                                CalculateWorkCoordinateOffset();
                            }
                            else if (part.startsWith("wco:"))
                            {
                                // Depending on $10 status report mask settings, position may be sent as either:
                                // MPos:0.000,-10.000,5.000 machine position or
                                // WPos:-2.500,0.000,11.000 work position
                                String[] wco = part.replace("wco:", "").split(",");

                                fXOffset = Float.parseFloat(wco[0]);
                                fYOffset = Float.parseFloat(wco[1]);
                                fZOffset = Float.parseFloat(wco[2]);

                                // Call the CalculateWorkCoordinateOffset method!
                                CalculateWorkCoordinateOffset();
                            }
                        }

                        break;
                }

                //////////////////////////////////////////////////////////////////////////////////////////////////////
                // Check if the machine status changed
                //////////////////////////////////////////////////////////////////////////////////////////////////////
                if (newActiveState != fActiveState)
                {
                    fActiveState = newActiveState;

                    // 1500ms when machine is in RUN status otherwise 300ms
                    fMillisecondsToGetMachineStatus = (fActiveState == GRBLActiveStates.RUN) ? 1500 : 300;

                    // Fire the MachineStatusChangedEvent
                    fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(fActiveState, ""));
                }
                //////////////////////////////////////////////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////
                // Debug
                ///////////////////////////////////////////////////////////////
                //System.out.println("Last status received " + (System.currentTimeMillis() - fLastMachinePositionReceivedTimestamp) + "ms ago");
                ///////////////////////////////////////////////////////////////
                fLastMachinePositionReceivedTimestamp = System.currentTimeMillis();

                //////////////////////////////////////////////////////////////////
                // Set the WaitForGetStatusCommandReply manual reset event
                //////////////////////////////////////////////////////////////////
                fWaitForGetStatusCommandReply.Set();
                //System.out.println("Machine status received");
                //////////////////////////////////////////////////////////////////
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
                else if (receivedStr.startsWith("error"))
                {
                    fLastCommandSentToController.setError(receivedStr);
                    fGCodeExecutionEventsManager.FireGCodeExecutedWithError(new GCodeExecutionEvent(fLastCommandSentToController));
                    fLastCommandSentToController = null;
                    fWaitForCommandToBeExecuted.Set();

                    if (receivedStr.equals("error: Alarm lock"))
                    {
                        fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.ALARM, ""));
                    }
                }
                else if (receivedStr.toLowerCase().startsWith("grbl"))
                {
                    // Get the GRBL Version of the controller
                    String[] parts = receivedStr.split(" ");
                    String versionStr = parts[1];

                    // Set the GRBL version 
                    if (versionStr.contains("1."))
                    {
                        this.setCNCControlFrameworkVersion(ECNCControlFrameworkVersion.GRBL1_1);
                    }
                    else
                    {
                        this.setCNCControlFrameworkVersion(ECNCControlFrameworkVersion.GRBL0_9);
                    }

                    // Fire the ConnectionEstablishedEvent
                    fConnectionEstablished = true;
                    fConnectionEstablishedManualResetEvent.Set();
                    fSerialConnectionEventManager.FireConnectionEstablishedEvent(new SerialConnectionEvent(receivedStr));
                    fSerialConnectionEventManager.FireDataReceivedFromSerialConnectionEvent(new SerialConnectionEvent(receivedStr));
                    fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.IDLE, ""));
                }
                else if (receivedStr.toLowerCase().contains("[reset to continue]"))
                {
                    fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.RESET_TO_CONTINUE, ""));
                    fMyGCodeSender.CancelSendingGCode();
                    fWaitForCommandToBeExecuted.Set();
                }
                else if (receivedStr.startsWith("[PRB:"))
                {
                    // Example of incoming message [PRB:0.000,0.000,-0.910:1]
                    //System.out.println("Endmill touched the Touch Probe!");
                    fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.MACHINE_TOUCHED_PROBE, receivedStr));
                    /////////////////////////////////////////////////////
                    // DONT !!!!!!!!!!!!!!!!!!! GRBL sends an "OK" back
                    // fWaitForCommandToBeExecuted.Set();
                    ////////////////////////////////////////////////////
                }
                else if (receivedStr.equals("ALARM: Probe fail") || receivedStr.equals("['$H'|'$X' to unlock]"))
                {
                    // MACHINE NEEDS UNLOCK !
                    fMachineStatusEventsManager.FireMachineStatusChangedEvent(new MachineStatusEvent(GRBLActiveStates.ALARM, ""));
                }
                else
                {
                    fSerialConnectionEventManager.FireDataReceivedFromSerialConnectionEvent(new SerialConnectionEvent(receivedStr));
                    fLastCommandSentToController = null;
                    fWaitForCommandToBeExecuted.Set();
                }
            }
        }
        catch (Exception ex)
        {
            System.err.println("GRBLConnectionHandler.OnDataReceived Error: " + ex.getMessage());
        }
    }

    private void CalculateWorkCoordinateOffset()
    {
        if (fControllerSendsWorkPosition)
        {
            // MPos = WPos + WCO
            fMachinePosition.setX(fWorkPosition.getX() + fXOffset);
            fMachinePosition.setY(fWorkPosition.getY() + fYOffset);
            fMachinePosition.setZ(fWorkPosition.getZ() + fZOffset);
        }
        else
        {
            // WPos = MPos - WCO
            fWorkPosition.setX(fMachinePosition.getX() - fXOffset);
            fWorkPosition.setY(fMachinePosition.getY() - fYOffset);
            fWorkPosition.setZ(fMachinePosition.getZ() - fZOffset);
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
    public String SendGCodeCommandAndGetResponse(GCodeCommand command)
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
                        if ((System.currentTimeMillis() - fLastMachinePositionReceivedTimestamp) > fMillisecondsToGetMachineStatus)
                        {
                            try
                            {
                                // Ask for status report           
                                fWaitForGetStatusCommandReply = new ManualResetEvent(false);
                                fWaitForGetStatusCommandReply.Reset();

                                // If a Touch Probe operation is currently active then
                                // do not ask for the machine status.
                                if (fAnOperationIsUsingTouchProbe)
                                {
                                    Thread.sleep(100);
                                    continue;
                                }

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
