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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.Queue;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeCycleEvents.GCodeCycleEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeCycleEvents.GCodeCycleEventManager;

/**
 *
 * @author Nikos Siatras
 */
public class GCodeSender
{

    // Serial Connection
    protected final ConnectionHandler fMyConnectionHandler;

    // GCode 
    protected File fGCodeFile = null;
    protected Queue<String> fGCodeQueue = new ArrayDeque<>();
    protected int fRowsSent = 0, fRowsInFile = 0;
    protected long fGCodeCycleStartedTimestamp = -1;

    // Event Managers
    protected final GCodeCycleEventManager fGCodeCycleEventManager = new GCodeCycleEventManager();

    public GCodeSender(ConnectionHandler myHandler)
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
        fGCodeFile = gcodeFile;

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

    public void StartSendingGCode()
    {
        fRowsSent = 0;
        // Fire GCodeCycleStartedEvent
        fGCodeCycleEventManager.FireGCodeCycleStartedEvent(new GCodeCycleEvent("Started"));
    }

    /**
     * Cancel sending GCode (Cancel GCode Cycle)
     * @param alarmHappened If an alarm happened then alarmHappened must be true 
     */
    public void CancelSendingGCode(boolean alarmHappened)
    {

    }
    
    public void KillGCodeCycle()
    {
        
    }

    public void PauseSendingGCode()
    {

    }

    public void ResumeSendingGCode()
    {
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

    /**
     * Returns the loaded GCodeFile
     *
     * @return the loaded GCodeFile
     */
    public File getGCodeFile()
    {
        return fGCodeFile;
    }

    /**
     * Returns true if the GCode sender is sending a GCode file to the
     * controller (CYCLE)
     */
    public boolean IsCyclingGCode()
    {
        return false;
    }
}
