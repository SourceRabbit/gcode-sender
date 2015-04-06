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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.Queue;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHandler;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeCycleEvents.GCodeCycleEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeCycleEvents.GCodeCycleEventManager;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;

/**
 *
 * @author Nikos Siatras
 */
public class GRBLGCodeSender
{

    // Serial Connection
    private final ConnectionHandler fMyConnectionHandler;

    // GCode 
    private Queue<String> fGCodeQueue = new ArrayDeque<>();

    // GCode Cycle
    private int fRowsSent = 0, fRowsInFile = 0;
    private boolean fKeepGCodeCycle = false;
    private Thread fGCodeCycleThread;
    private long fGCodeCycleStartedTimestamp = -1;

    // Event Managers
    private GCodeCycleEventManager fGCodeCycleEventManager = new GCodeCycleEventManager();

    public GRBLGCodeSender(ConnectionHandler myHandler)
    {
        fMyConnectionHandler = myHandler;
    }

    /**
     * Try to load the GCode file
     *
     * @param gcodeFile
     * @return
     */
    public boolean LoadGCodeFile(File gcodeFile)
    {
        fGCodeQueue = new ArrayDeque<>();

        try (BufferedReader br = new BufferedReader(new FileReader(gcodeFile)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                fGCodeQueue.add(line);
            }

            fRowsInFile = fGCodeQueue.size();
            fRowsSent = 0;
            return true;
        }
        catch (Exception ex)
        {
        }

        return false;
    }

    /**
     * Start the GCode cycle
     */
    public void StartSendingGCode()
    {
        if (fGCodeQueue.isEmpty())
        {
            return;
        }

        // Fire GCodeCycleStartedEvent
        fGCodeCycleEventManager.FireGCodeCycleStartedEvent(new GCodeCycleEvent("Started"));

        // Start sending gcodes
        fRowsSent = 0;
        fGCodeCycleThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // Create a new Queue and start sending gcode from that
                final Queue<String> gcodes = new ArrayDeque<>(fGCodeQueue);
                fGCodeCycleStartedTimestamp = System.currentTimeMillis();

                try
                {
                    // Send cycle start command
                    fMyConnectionHandler.SendDataImmediately_WithoutMessageCollector(GRBLCommands.COMMAND_START_CYCLE);

                    while (fKeepGCodeCycle && gcodes.size() > 0)
                    {
                        // Create a GCode Command to send
                        final GCodeCommand command = new GCodeCommand(gcodes.remove());
                        fMyConnectionHandler.SendGCodeCommand(command);

                        fRowsSent += 1;
                    }
                }
                catch (Exception ex)
                {
                }

                // Fire GCodeCycleFinishedEvent
                if (gcodes.isEmpty())
                {
                    final long millis = System.currentTimeMillis() - fGCodeCycleStartedTimestamp;
                    final long second = (millis / 1000) % 60;
                    final long minute = (millis / (1000 * 60)) % 60;
                    final long hour = (millis / (1000 * 60 * 60)) % 24;

                    final String time = String.format("%02d:%02d:%02d", hour, minute, second);
                    fGCodeCycleStartedTimestamp = - 1;
                    fGCodeCycleEventManager.FireGCodeCycleFinishedEvent(new GCodeCycleEvent("Finished!\nTime: " + time));
                }
            }
        });
        fGCodeCycleThread.setPriority(Thread.NORM_PRIORITY);
        fKeepGCodeCycle = true;
        fGCodeCycleThread.start();
    }

    /**
     * Cancel Sending GCode to GRBL Controller! This method stops immediately
     * the GCode cycle and the CNC machine stops.
     */
    public void CancelSendingGCode()
    {
        if (fKeepGCodeCycle)
        {
            fKeepGCodeCycle = false;

            // Pause the GCode cycle
            PauseSendingGCode();

            // Soft reset the machine
            try
            {
                fMyConnectionHandler.SendDataImmediately_WithoutMessageCollector(GRBLCommands.COMMAND_SOFT_RESET);
            }
            catch (Exception ex)
            {
            }

            // Fire GCodeCycleCanceledEvent
            fGCodeCycleEventManager.FireGCodeCycleCanceledEvent(new GCodeCycleEvent("Canceled"));
            fGCodeCycleStartedTimestamp = -1;
        }
    }

    /**
     * Immediately pause GCode cycle.
     */
    public void PauseSendingGCode()
    {
        try
        {
            fMyConnectionHandler.SendDataImmediately_WithoutMessageCollector("!");
        }
        catch (Exception ex)
        {
        }
        fGCodeCycleEventManager.FireGCodeCyclePausedEvent(new GCodeCycleEvent("Paused"));
    }

    /**
     * Resume the GCode cycle.
     */
    public void ResumeSendingGCode()
    {
        try
        {
            fMyConnectionHandler.SendDataImmediately_WithoutMessageCollector("~");
        }
        catch (Exception ex)
        {
        }
        fGCodeCycleEventManager.FireGCodeCycleResumedEvent(new GCodeCycleEvent("Resumed"));
    }

    /**
     * Return a FIFO queue with all the GCode commands
     *
     * @return FIFO queue with all the GCode commands
     */
    public Queue<String> getGCodeQueue()
    {
        return fGCodeQueue;
    }

    /**
     * Returns the number of GCode lines in the GCode Queue (GCode Commands
     * Queue).
     *
     * @return the number of GCode lines in the GCode Queue
     */
    public int getRowsInFile()
    {
        return fRowsInFile;
    }

    /**
     * Returns the number of GCode commands sent to the GRBL Controller
     *
     * @return the number of GCode commands sent to the GRBL Controller
     */
    public int getRowsSent()
    {
        return fRowsSent;
    }

    /**
     * Returns the number of GCode commands left in fGCodeQueue
     *
     * @return the number of GCode commands left in fGCodeQueue
     */
    public int getRowsRemaining()
    {
        return fRowsInFile - fRowsSent;
    }

    /**
     * Returns the GCode Cycle events manager
     *
     * @return
     */
    public GCodeCycleEventManager getCycleEventManager()
    {
        return fGCodeCycleEventManager;
    }

    /**
     * Returns the timestamp when the GCode Cycle started
     *
     * @return the timestamp when the GCode Cycle started.
     */
    public long getGCodeCycleStartedTimestamp()
    {
        return fGCodeCycleStartedTimestamp;
    }
}
