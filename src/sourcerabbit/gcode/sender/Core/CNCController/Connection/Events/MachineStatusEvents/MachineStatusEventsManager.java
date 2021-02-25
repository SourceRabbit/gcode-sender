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
package sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents;

import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.EventManager;

/**
 *
 * @author Nikos Siatras
 */
public class MachineStatusEventsManager extends EventManager
{

    private MachineStatusEvent fCurrentEvent;
    private final Object fLock;

    public MachineStatusEventsManager()
    {
        fLock = new Object();
    }

    public void FireMachineStatusChangedEvent(MachineStatusEvent evt)
    {
        synchronized (fLock)
        {
            fCurrentEvent = evt;
            for (Object obj : fEventListeners)
            {
                IMachineStatusEventListener listener = (IMachineStatusEventListener) obj;
                listener.MachineStatusChanged(evt);
            }
        }
    }

    public void FireMachineStatusReceived(MachineStatusEvent evt)
    {
        synchronized (fLock)
        {
            fCurrentEvent = evt;
            for (Object obj : fEventListeners)
            {
                IMachineStatusEventListener listener = (IMachineStatusEventListener) obj;
                listener.MachineStatusReceived(evt);
            }
        }
    }

    public MachineStatusEvent getCurrentStatus()
    {
        return fCurrentEvent;
    }
}
