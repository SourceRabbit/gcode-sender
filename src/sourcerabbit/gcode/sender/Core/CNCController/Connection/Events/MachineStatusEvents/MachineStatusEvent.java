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

import java.util.EventObject;

/**
 *
 * @author Nikos Siatras
 */
public class MachineStatusEvent extends EventObject
{

    private final int fMachineStatus;
    private final String fMessage;

    public MachineStatusEvent(int machineStatus, String message)
    {
        super(machineStatus);
        fMachineStatus = machineStatus;
        fMessage = message;
    }

    public int getMachineStatus()
    {
        return fMachineStatus;
    }

    public String getMessage()
    {
        return fMessage;
    }

}
