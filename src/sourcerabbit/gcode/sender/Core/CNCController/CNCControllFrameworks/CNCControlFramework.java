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
package sourcerabbit.gcode.sender.Core.CNCController.CNCControllFrameworks;

import sourcerabbit.gcode.sender.Core.CNCController.Connection.Handlers.ConnectionHandler;

/**
 *
 * @author Nikos Siatras
 */
public class CNCControlFramework
{

    private final int fID;
    private final String fName;
    private final ConnectionHandler fHandler;

    public CNCControlFramework(int id, String name, ConnectionHandler handler)
    {
        fID = id;
        fName = name;
        fHandler = handler;
    }

    public int getID()
    {
        return fID;
    }

    public String getName()
    {
        return fName;
    }

    public ConnectionHandler getHandler()
    {
        return fHandler;
    }
}
