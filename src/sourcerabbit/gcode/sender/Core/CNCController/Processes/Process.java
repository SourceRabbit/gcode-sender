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
package sourcerabbit.gcode.sender.Core.CNCController.Processes;

import javax.swing.JDialog;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.IMachineStatusEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.MachineStatusEvent;

/**
 *
 * @author Nikos Siatras
 */
public class Process
{

    protected final IMachineStatusEventListener fIMachineStatusEventListener;
    protected final JDialog fMyParentForm;

    public Process(JDialog parentForm)
    {
        fMyParentForm = parentForm;

        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().AddListener(fIMachineStatusEventListener = new IMachineStatusEventListener()
        {
            @Override
            public void MachineStatusChanged(MachineStatusEvent evt)
            {
                final int activeState = evt.getMachineStatus();
                MachineStatusHasChange(activeState);
            }

            @Override
            public void MachineStatusReceived(MachineStatusEvent evt)
            {
                // DO NOTHING !!!!
            }

        });
    }

    public void MachineStatusHasChange(int state)
    {

    }

    public void Execute()
    {

    }

    public void ExecuteInNewThread()
    {
        Thread th = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Execute();
            }
        });
        th.start();
    }

    public void KillImmediately()
    {

    }

    public void ProcessFailed(String errorMessage)
    {

    }

    public void Dispose()
    {
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().RemoveListener(fIMachineStatusEventListener);
    }

}
