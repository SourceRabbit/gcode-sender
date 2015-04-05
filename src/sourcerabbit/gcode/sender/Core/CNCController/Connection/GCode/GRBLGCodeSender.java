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
package sourcerabbit.gcode.sender.Core.CNCController.Connection.GCode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.Queue;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Handlers.ConnectionHandler;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.GCode.GCodeCycleEvents.GCodeCycleEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.GCode.GCodeCycleEvents.GCodeCycleEventManager;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Handlers.GRBL.GRBLCommands;

/**
 *
 * @author Nikos Siatras
 */
public class GRBLGCodeSender
{

    private final ConnectionHandler fMyConnectionHandler;
    private Queue<String> fGCodeQueue = new ArrayDeque<>();
    private int fRowsSent = 0, fRowsInFile = 0;

    private GCodeCycleEventManager fGCodeCycleEventManager = new GCodeCycleEventManager();

    private boolean fKeepGCodeCycle = false;
    private Thread fGCodeCycleThread;

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
        // Fire GCodeCycleStartedEvent
        fGCodeCycleEventManager.FireGCodeCycleStartedEvent(new GCodeCycleEvent("Started"));

        // Start sending gcodes
        fRowsSent = 0;
        fGCodeCycleThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // Create a new Queue and starting sending gcode from that
                final Queue<String> gcodes = new ArrayDeque<>(fGCodeQueue);

                try
                {
                    fMyConnectionHandler.SendData(GRBLCommands.COMMAND_START_CYCLE);
                    while (fKeepGCodeCycle && gcodes.size() > 0)
                    {
                        String line = gcodes.remove();
                        // Remove comments from line
                        if (line.contains(";"))
                        {
                            String tmpLine = line.substring(0, line.indexOf(";"));
                            tmpLine = tmpLine.trim();
                            fMyConnectionHandler.SendData(tmpLine.replace(" ", ""));
                            fRowsSent += 1;
                        }
                        else if (line.startsWith("("))
                        {
                            fRowsSent += 1;
                        }
                        else
                        {
                            fMyConnectionHandler.SendData(line.trim());
                            fRowsSent += 1;
                        }
                    }
                }
                catch (Exception ex)
                {

                }

                // Fire GCodeCycleFinishedEvent
                fGCodeCycleEventManager.FireGCodeCycleFinishedEvent(new GCodeCycleEvent("Finished"));
            }
        });
        fGCodeCycleThread.setPriority(Thread.NORM_PRIORITY);
        fKeepGCodeCycle = true;
        fGCodeCycleThread.start();
    }

    public void CancelSendingGCode()
    {
        if (fKeepGCodeCycle)
        {
            fKeepGCodeCycle = false;
            PauseSendingGCode();
            try
            {
                fMyConnectionHandler.SendDataImmediately(GRBLCommands.COMMAND_SOFT_RESET);
            }
            catch (Exception ex)
            {

            }
            fGCodeCycleEventManager.FireGCodeCycleCanceledEvent(new GCodeCycleEvent("Canceled"));
        }
    }

    public void PauseSendingGCode()
    {
        try
        {
            fMyConnectionHandler.SendDataImmediately("!");
        }
        catch (Exception ex)
        {

        }
        fGCodeCycleEventManager.FireGCodeCyclePausedEvent(new GCodeCycleEvent("Paused"));
    }

    public void ResumeSendingGCode()
    {
        try
        {
            fMyConnectionHandler.SendDataImmediately("~");
        }
        catch (Exception ex)
        {

        }

        fGCodeCycleEventManager.FireGCodeCycleResumedEvent(new GCodeCycleEvent("Resumed"));
    }

    public Queue<String> getGCodeQueue()
    {
        return fGCodeQueue;
    }

    public int getRowsInFile()
    {
        return fRowsInFile;
    }

    public int getRowsSent()
    {
        return fRowsSent;
    }

    public int getRowsRemaining()
    {
        return fRowsInFile - fRowsSent;
    }

    public GCodeCycleEventManager getCycleEventManager()
    {
        return fGCodeCycleEventManager;
    }
}
