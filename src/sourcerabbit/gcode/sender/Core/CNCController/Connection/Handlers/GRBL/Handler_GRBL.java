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
package sourcerabbit.gcode.sender.Core.CNCController.Connection.Handlers.GRBL;

import jssc.SerialPortException;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.SerialConnectionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.ISerialConnectionEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Handlers.ConnectionHandler;
import sourcerabbit.gcode.sender.Core.CNCController.Tools.ManualResetEvent;

/**
 *
 * @author Nikos Siatras
 */
public class Handler_GRBL extends ConnectionHandler
{

    // Commands
    private final Object fSendDataLock = new Object();
    private ManualResetEvent fWaitForCommandToBeExecuted;

    // Status thread
    private long fLastMachinePositionReceivedTimestamp;
    private final long fMillisecondsToGetMachineTimestamp = 400;
    private boolean fKeepStatusThread = false;
    private Thread fStatusThread;

    public Handler_GRBL()
    {
        super.fMessageSplitter = "\n";
        super.fMessageSplitterLength = fMessageSplitter.length();
        super.fMessageSplitterBytes = String.valueOf(fMessageSplitter).getBytes();

        fWaitForCommandToBeExecuted = new ManualResetEvent(false);

        InitEvents();
    }

    /**
     * Initialize events needed for this connection handler
     */
    private void InitEvents()
    {
        // Connection Closed Event
        fSerialConnectionEventManager.AddListener(new ISerialConnectionEventListener()
        {
            @Override
            public void ConnectionEstablished(SerialConnectionEvent evt)
            {
                StartStatusReportThread();
            }

            @Override
            public void ConnectionClosed(SerialConnectionEvent evt)
            {
                fKeepStatusThread = false;
                try
                {
                    fStatusThread.interrupt();
                }
                catch (Exception ex)
                {

                }
            }
        });
    }

    @Override
    public void OnDataReceived(byte[] data)
    {
        String receivedStr = new String(data);
        receivedStr = receivedStr.replace("\r", "");
        System.out.println("Data received:" + receivedStr);

        if (receivedStr.toLowerCase().startsWith("grbl"))
        {
            // Fire the ConnectionEstablishedEvent
            fConnectionEstablished = true;
            fSerialConnectionEventManager.FireConnectionEstablishedEvent(new SerialConnectionEvent(receivedStr));
        }
        else if (receivedStr.equals("ok"))
        {
            fWaitForCommandToBeExecuted.Set();
        }
        else if (receivedStr.startsWith("error"))
        {
            fWaitForCommandToBeExecuted.Set();
        }
        else
        {
            if (receivedStr.startsWith("<") && receivedStr.endsWith(">"))
            {
                // Machine status received !
                fLastMachinePositionReceivedTimestamp = System.currentTimeMillis();

                receivedStr = receivedStr.toLowerCase();
                receivedStr = receivedStr.replace("mpos", "").replace("wpos", "").replace(":", "").replace("<", "").replace(">", "");
                String[] parts = receivedStr.split(",");

                fActiveState = parts[0];

                fMachinePosition.setX(Double.parseDouble(parts[1]));
                fMachinePosition.setY(Double.parseDouble(parts[2]));
                fMachinePosition.setZ(Double.parseDouble(parts[3]));

                fWorkPosition.setX(Double.parseDouble(parts[4]));
                fWorkPosition.setY(Double.parseDouble(parts[5]));
                fWorkPosition.setZ(Double.parseDouble(parts[6]));
            }
        }
    }

    @Override
    public boolean SendData(String data) throws SerialPortException
    {
        synchronized (fSendDataLock)
        {
            fWaitForCommandToBeExecuted.Reset();

            // Send data !!!!
            if (super.SendData(data))
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

    @Override
    public boolean SendDataImmediately(String data) throws SerialPortException
    {
        if (super.SendData(data))
        {
            return true;
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

    /**
     * Send "?" command to the GRBL Controller
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
                        if ((System.currentTimeMillis() - fLastMachinePositionReceivedTimestamp) > fMillisecondsToGetMachineTimestamp)
                        {
                            try
                            {
                                // Ask for status report
                                SendDataImmediately(GRBLCommands.COMMAND_GET_STATUS);
                            }
                            catch (Exception ex)
                            {
                            }
                        }

                        try
                        {
                            Thread.sleep(fMillisecondsToGetMachineTimestamp + 1);
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
}
