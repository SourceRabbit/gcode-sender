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
package sourcerabbit.gcode.sender.Core.CNCController.Connection;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeExecutionEvents.GCodeExecutionEventsManager;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.MachineStatusEventsManager;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.SerialConnectionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.SerialConnectionEventManager;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLGCodeSender;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.Arrays.ByteArrayBuilder;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.EGRBLVersion;
import sourcerabbit.gcode.sender.Core.Threading.ManualResetEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Position.Position4D;
import sourcerabbit.gcode.sender.UI.frmControl;

/**
 *
 * @author Nikos Siatras
 */
public class ConnectionHandler implements SerialPortEventListener
{

    // Serial Communication
    protected SerialPort fSerialPort;
    protected String fPortName;
    protected int fBaudRate;
    protected ByteArrayBuilder fIncomingDataBuffer = null;
    protected boolean fConnectionEstablished = false;
    protected ManualResetEvent fConnectionEstablishedManualResetEvent = new ManualResetEvent(false);

    //Check if connection is active
    protected Thread fCheckConnectionActivityThread;
    protected final Object fCheckConnectionLock;
    protected long fNanosecondsToWaitToWriteCommandBytes = Long.parseLong("4000000000");
    protected long fTimeStartedSendingBytes = 0;
    protected long fTimeFinishedSendingBytes = 0;

    // Incoming Data
    protected String fMessageSplitter;
    protected byte[] fMessageSplitterBytes;
    protected int fMessageSplitterLength, fIndexOfMessageSplitter;
    protected final Object fLockSerialEvent = new Object();

    // CNC position and status
    protected int fActiveState = 1;
    protected Position4D fMachinePosition;
    protected Position4D fWorkPosition;

    // Event Managers
    protected SerialConnectionEventManager fSerialConnectionEventManager = new SerialConnectionEventManager();
    protected GCodeExecutionEventsManager fGCodeExecutionEventsManager = new GCodeExecutionEventsManager();
    protected MachineStatusEventsManager fMachineStatusEventsManager = new MachineStatusEventsManager();

    // Byte count Thread
    private Thread fKeepCountingBytesThread;
    private boolean fKeepCountingBytes = false;
    private long fBytesIn = 0, fBytesOut = 0;
    private long fBytesInPerSec = 0, fBytesOutPerSec = 0;

    // GCode
    protected final GCodeSender fMyGCodeSender;

    // CNC Control Framework
    protected EGRBLVersion fControllerGRBLVersion;

    // Max Travels
    public static int fXMaxTravel = 00;
    public static int fYMaxTravel = 00;
    public static int fZMaxTravel = 00;
    
     // Home Position
    public double fXHomePosition = -255;
    public double fYHomePosition = -255;
    public double fZHomePosition = -255;

    protected long fLastMachineStatusReceivedTimestamp = 0;

    // For UI purposes
    protected boolean fShowVerboseOutput = false;

    public ConnectionHandler()
    {
        fMyGCodeSender = new GRBLGCodeSender(this);

        fCheckConnectionLock = new Object();

        fCheckConnectionActivityThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    if (fSerialPort != null)
                    {
                        synchronized (fCheckConnectionLock)
                        {
                            if (fTimeStartedSendingBytes > 0)
                            {
                                long dif = System.nanoTime() - fTimeStartedSendingBytes;

                                if (dif > fNanosecondsToWaitToWriteCommandBytes)
                                {
                                    System.err.println("Connection droped!");
                                    fTimeStartedSendingBytes = 0;

                                    try
                                    {
                                        frmControl.fInstance.WriteToConsole("ERROR: Serial Connection droped! This might happen from external electromagnetic interference. Check USB wire or bluetooth communication!");
                                    }
                                    catch (Exception ex)
                                    {

                                    }

                                    try
                                    {
                                        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.CloseConnection();
                                    }
                                    catch (Exception ex)
                                    {

                                    }
                                }
                            }
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

        fCheckConnectionActivityThread.start();
    }

    /**
     * Try to establish a connection with the CNC Controller
     *
     * @param serialPort
     * @param baudRate
     * @return
     * @throws Exception
     */
    public boolean OpenConnection(String serialPort, int baudRate) throws Exception
    {
        fIncomingDataBuffer = new ByteArrayBuilder();
        fPortName = serialPort;
        fBaudRate = baudRate;

        if (fMyGCodeSender instanceof GRBLGCodeSender)
        {
            fSerialPort = new SerialPort(serialPort);
            fSerialPort.openPort();
            fSerialPort.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE, true, true);
            fSerialPort.addEventListener(this);
        }
        else
        {
            throw new Exception("Unknown framework!");
        }

        if (fSerialPort == null)
        {
            throw new Exception("Serial port not found!");
        }

        fMachinePosition = new Position4D((double) 0.000, (double) 0.000, (double) 0.000, null);
        fWorkPosition = new Position4D((double) 0.000, (double) 0.000, (double) 0.000, null);

        StartByteCountThread();

        return true;
    }

    /**
     * Close the serial connection!
     *
     * @throws Exception
     */
    public void CloseConnection() throws Exception
    {
        if (fSerialPort != null)
        {
            try
            {
                fSerialPort.removeEventListener();

                if (fSerialPort.isOpened())
                {
                    fSerialPort.closePort();
                }
            }
            catch (Exception ex)
            {

            }
            finally
            {
                fIncomingDataBuffer = null;
                fSerialPort = null;
            }
        }

        fConnectionEstablished = false;

        // Fire connection closed event
        if (fSerialConnectionEventManager != null)
        {
            fSerialConnectionEventManager.FireConnectionClosedEvent(new SerialConnectionEvent("Connection Closed"));
        }

        StopByteCountThread();
    }

    private void StartByteCountThread()
    {
        fKeepCountingBytes = true;
        fKeepCountingBytesThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (fKeepCountingBytes)
                {
                    fBytesInPerSec = fBytesIn;
                    fBytesOutPerSec = fBytesOut;
                    fBytesIn = fBytesOut = 0;

                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (Exception ex)
                    {

                    }
                }
            }
        });
        fKeepCountingBytesThread.start();
    }

    private void StopByteCountThread()
    {
        fKeepCountingBytes = false;
        fKeepCountingBytesThread.interrupt();
    }

    @Override
    public void serialEvent(SerialPortEvent spe)
    {
        synchronized (fLockSerialEvent)
        {
            try
            {
                byte[] incomingBytes = this.fSerialPort.readBytes();
                if (incomingBytes != null && incomingBytes.length > 0)
                {
                    // Data received
                    AppendDataToIncomingBuffer(incomingBytes);
                }
            }
            catch (Exception ex)
            {
            }
        }
    }

    private void AppendDataToIncomingBuffer(byte[] bytes)
    {
        try
        {
            fBytesIn += bytes.length;
            fIncomingDataBuffer.Append(bytes);
            fIndexOfMessageSplitter = fIncomingDataBuffer.IndexOf(fMessageSplitterBytes);
            while (fIndexOfMessageSplitter > -1)
            {
                OnDataReceived(fIncomingDataBuffer.SubList(0, fIndexOfMessageSplitter));
                fIncomingDataBuffer.Delete(0, fIndexOfMessageSplitter + fMessageSplitterLength);
                fIndexOfMessageSplitter = fIncomingDataBuffer.IndexOf(fMessageSplitterBytes);
            }
        }
        catch (Exception ex)
        {
            System.err.println("<ConnectionHandler.AppendDataToIncomingBuffer> Error:" + ex.getMessage());
        }
    }

    public void OnDataReceived(byte[] data)
    {

    }

    public boolean SendData(String data) throws SerialPortException
    {
        boolean result = false;

        try
        {

            fBytesOut += data.getBytes().length + 1;

            synchronized (fCheckConnectionLock)
            {
                fTimeStartedSendingBytes = System.nanoTime();
            }

            // Write Bytes
            result = fSerialPort.writeString(data + fMessageSplitter);

            synchronized (fCheckConnectionLock)
            {
                // ALWAYS SET TO ZERO AFTER BYTES ARE WRITTEN!
                fTimeStartedSendingBytes = 0;
            }

            //System.out.println ("Dif: " + String.valueOf(System.nanoTime() - fTimeStartedSendingBytes));
        }
        catch (Exception ex)
        {
            try
            {
                CloseConnection();
            }
            catch (Exception ex1)
            {
            }
            throw ex;
        }

        return result;
    }

    public boolean SendGCodeCommand(GCodeCommand command) throws Exception
    {
        throw new Exception("Not implemented yet!");
    }

    public String SendGCodeCommandAndGetResponse(GCodeCommand command)
    {
        return "";
    }

    public boolean AskForMachineStatus()
    {
        return false;
    }

    public boolean SendDataImmediately_WithoutMessageCollector(String data) throws SerialPortException
    {
        try
        {
            return fSerialPort.writeString(data);
        }
        catch (Exception ex)
        {
            try
            {
                CloseConnection();
            }
            catch (Exception ex1)
            {
            }
            throw ex;
        }
    }

    public boolean SendDataImmediately_WithoutMessageCollector(byte data) throws SerialPortException
    {
        try
        {
            return fSerialPort.writeByte(data);
        }
        catch (Exception ex)
        {
            try
            {
                CloseConnection();
            }
            catch (Exception ex1)
            {
            }
            throw ex;
        }
    }

    /**
     * Call the "StartUsingTouchProbe" at the start of each operation that
     * requires the use of touch probe. After the operation finishes then call
     * the "StopUsingTouchProbe" method.
     */
    public void StartUsingTouchProbe()
    {
        ConnectionHelper.A_PROCESS_IS_USING_TOUCH_PROBE = true;
    }

    /**
     * Call the "StopUsingTouchProbe" when an operation that requires touch
     * probe finishes.
     */
    public void StopUsingTouchProbe()
    {
        ConnectionHelper.A_PROCESS_IS_USING_TOUCH_PROBE = false;
    }

    /**
     * Returns the connection's port name
     *
     * @return
     */
    public String getSerialPortName()
    {
        return fPortName;
    }

    /**
     * Returns the connection's baud rate
     *
     * @return
     */
    public int getBaud()
    {
        return fBaudRate;
    }

    /**
     * Return the number of bytes received during the last second.
     *
     * @return
     */
    public long getBytesInPerSec()
    {
        return fBytesInPerSec;
    }

    /**
     * Return the number of bytes sent during the last second.
     *
     * @return
     */
    public long getBytesOutPerSec()
    {
        return fBytesOutPerSec;
    }

    /**
     * Returns true if the connection is established
     *
     * @return
     */
    public boolean isConnectionEstablished()
    {
        return fConnectionEstablished;
    }

    public Position4D getMachinePosition()
    {
        return fMachinePosition;
    }

    /**
     * Returns the active state of the controller
     *
     * @return the active state of the controller
     */
    public int getActiveState()
    {
        return fActiveState;
    }

    public Position4D getWorkPosition()
    {
        return fWorkPosition;
    }

    /**
     * Returns the Serial Connection Event Manager
     *
     * @return ConnectionClosedEventManager
     */
    public SerialConnectionEventManager getSerialConnectionEventManager()
    {
        return fSerialConnectionEventManager;
    }

    /**
     * Returns the GCodeExecutionEventsManager
     *
     * @return GCodeExecutionEventsManager
     */
    public GCodeExecutionEventsManager getGCodeExecutionEventsManager()
    {
        return fGCodeExecutionEventsManager;
    }

    /**
     * Returns the MachineStatusEventsManager
     *
     * @return fMachineStatusEventsManager
     */
    public MachineStatusEventsManager getMachineStatusEventsManager()
    {
        return fMachineStatusEventsManager;
    }

    /**
     * Returns the handler's GCode sender
     *
     * @return
     */
    public GCodeSender getMyGCodeSender()
    {
        return fMyGCodeSender;
    }

    /**
     * Set the version for the connected controller.
     *
     * @param version is the version
     */
    public void setCNCControlFrameworkVersion(EGRBLVersion version)
    {
        fControllerGRBLVersion = version;
    }

    /**
     * Get the version of framework that the connected controller uses.
     *
     * @return ECNCControlFrameworkVersion
     */
    public EGRBLVersion getCNCControlFrameworkVersion()
    {
        return fControllerGRBLVersion;
    }

    /**
     * Set the Show Verbose output boolean
     *
     * @param value
     */
    public void setShowVerboseOutput(boolean value)
    {
        fShowVerboseOutput = value;
    }

    public boolean isShowVerboseOutputEnabled()
    {
        return fShowVerboseOutput;
    }

    /**
     * Returns the last time that machine status has been received
     *
     * @return
     */
    public long getLastMachineStatusReceivedTimestamp()
    {
        return fLastMachineStatusReceivedTimestamp;
    }
}
