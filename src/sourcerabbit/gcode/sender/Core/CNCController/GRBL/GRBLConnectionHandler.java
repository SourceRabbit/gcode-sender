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

            //System.out.println("Data received:" + receivedStr);
            if (receivedStr.startsWith("<"))
            {
                // Machine status received !
                // System.out.println(receivedStr);
                receivedStr = receivedStr.toLowerCase().replace("mpos", "").replace("wpos", "").replace(":", "").replace("<", "").replace(">", "");
                String[] parts = receivedStr.split(",");

                //////////////////////////////////////////////////////////////////////////////////////////////////////
                // Check if the machine status changed
                //////////////////////////////////////////////////////////////////////////////////////////////////////
                final int newActiveState = GRBLActiveStates.getGRBLActiveStateFromString(parts[0]);
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

                // Get Machine Position
                fMachinePosition.setX(Float.parseFloat(parts[1]));
                fMachinePosition.setY(Float.parseFloat(parts[2]));
                fMachinePosition.setZ(Float.parseFloat(parts[3]));

                // Get Work Position
                fWorkPosition.setX(Float.parseFloat(parts[4]));
                fWorkPosition.setY(Float.parseFloat(parts[5]));
                fWorkPosition.setZ(Float.parseFloat(parts[6]));

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
