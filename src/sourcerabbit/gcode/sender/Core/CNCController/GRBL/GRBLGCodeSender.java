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

import java.util.ArrayDeque;
import java.util.Queue;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHandler;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeCycleEvents.GCodeCycleEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.GCodeSender;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.Settings.SemiAutoToolChangeSettings;

/**
 *
 * @author Nikos Siatras
 */
public class GRBLGCodeSender extends GCodeSender
{

    // GRBL GCode Cycle
    private boolean fKeepGCodeCycle = false;
    private Thread fGCodeCycleThread;
    private boolean fIsCyclingGCode = false;

    // GRBL Tool Change
    public final GRBLSemiAutoToolChangeOperator fSemiAutoToolChangeOperator;
    
    public GRBLGCodeSender(ConnectionHandler myHandler)
    {
        super(myHandler);
        fSemiAutoToolChangeOperator = new GRBLSemiAutoToolChangeOperator(this);
    }

    /**
     * Start the GCode cycle
     */
    @Override
    public void StartSendingGCode()
    {
        if (fGCodeQueue.isEmpty())
        {
            return;
        }

        ////////////////////////////////////////////////////////////
        // Call the parent StartSendingGCode method!!!!!!
        super.StartSendingGCode();
        ////////////////////////////////////////////////////////////

        // Start sending gcodes
        fGCodeCycleThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // Create a new Queue and start sending gcode from that
                final Queue<String> gcodes = new ArrayDeque<>(fGCodeQueue);
                fGCodeCycleStartedTimestamp = System.currentTimeMillis();
                
                fIsCyclingGCode = true;
                
                try
                {
                    // Send cycle start command and ask for the new machine status
                    fMyConnectionHandler.SendDataImmediately_WithoutMessageCollector(GRBLCommands.COMMAND_START_CYCLE + GRBLCommands.COMMAND_GET_STATUS);
                    
                    long lineNumber = 1;
                    while (fKeepGCodeCycle && gcodes.size() > 0)
                    {
                        try
                        {
                            // Create a GCode Command to send
                            final GCodeCommand command = new GCodeCommand(gcodes.remove());
                            command.setLineNumber(lineNumber);
                            lineNumber += 1;

                            ///////////////////////////////////////////////////////////////////////////////////
                            // COMMAND WITH TOOL CHANGE
                            // Check if the command has T (for tool change)
                            // and ask the GRBLSemiAutoToolChangeOperator to do the tool change
                            ///////////////////////////////////////////////////////////////////////////////////
                            if (SemiAutoToolChangeSettings.isSemiAutoToolChangeEnabled() && (command.getCommand().contains("M6") || command.getCommand().contains("M06")) && command.getCommand().contains("T"))
                            {
                                fSemiAutoToolChangeOperator.DoSemiAutoToolChangeSequence(command);
                            }
                            else
                            {
                                if (command.getCommand().equals("M998"))
                                {
                                    // DO NOTHING
                                    // This command tells the machine to move to the tool change height
                                }
                                else
                                {
                                    ///////////////////////////////////////////////////////////////////////////////////
                                    // Send the command
                                    ///////////////////////////////////////////////////////////////////////////////////
                                    fMyConnectionHandler.SendGCodeCommand(command);
                                    ///////////////////////////////////////////////////////////////////////////////////
                                }
                            }

                            // Ask for machine status every MILLISECONDS_TO_ASK_FOR_MACHINE_STATUS_DURING_CYCLING
                            if (System.currentTimeMillis() - fMyConnectionHandler.getLastMachineStatusReceivedTimestamp() > GRBLConstants.MILLISECONDS_TO_ASK_FOR_MACHINE_STATUS_DURING_CYCLING)
                            {
                                fMyConnectionHandler.AskForMachineStatus();
                            }
                            
                            fRowsSent += 1;
                        }
                        catch (Exception ex)
                        {
                            System.err.println(ex.getMessage());
                        }
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
                
                fIsCyclingGCode = false;
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
    @Override
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
            
            fIsCyclingGCode = false;
        }
    }

    /**
     * Immediately pause GCode cycle.
     */
    @Override
    public void PauseSendingGCode()
    {
        try
        {
            fMyConnectionHandler.SendDataImmediately_WithoutMessageCollector("! ?");
        }
        catch (Exception ex)
        {
        }
        fGCodeCycleEventManager.FireGCodeCyclePausedEvent(new GCodeCycleEvent("Paused"));
    }

    /**
     * Resume the GCode cycle.
     */
    @Override
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
    
    @Override
    public boolean IsCyclingGCode()
    {
        return fIsCyclingGCode;
    }

    /**
     * Return's the Semi Auto Tool Change Operator
     *
     * @return
     */
    public GRBLSemiAutoToolChangeOperator getMySemiAutoToolChangeOperator()
    {
        return fSemiAutoToolChangeOperator;
    }
    
}
