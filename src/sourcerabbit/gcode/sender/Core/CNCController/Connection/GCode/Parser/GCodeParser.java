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
package sourcerabbit.gcode.sender.Core.CNCController.Connection.GCode.Parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sourcerabbit.gcode.sender.Core.CNCController.Tools.Position3D;

/**
 *
 * @author Nikos Siatras
 */
public class GCodeParser
{

    public static GCodeCommand CreateCommand(String command)
    {
        command = command.trim();

        // Check if the command is GCode 
        if (!command.startsWith("(") && !command.equals(""))
        {
            String comment = getCommandComment(command);
            String clearCommand = command.replace(comment, "");
            Position3D coordinates = getCommandCordinates(clearCommand);

            if (coordinates.getX() != null || coordinates.getY() != null | coordinates.getZ() != null)
            {
                return new GCodeCommand(coordinates);
            }
        }

        return null;
    }

    private static String getCommandComment(String command)
    {
        if (command.contains(";"))
        {
            String tmp = command.substring(command.indexOf(";"));
            return tmp.trim();
        }
        return "";
    }

    private static Position3D getCommandCordinates(String command)
    {
        command = command.toLowerCase();
        Double x = null, y = null, z = null;
        Pattern pattern = Pattern.compile("[ngxyzf][+-]?[0-9]*\\.?[0-9]­*");
        Matcher m = pattern.matcher(command);
        while (m.find())
        {
            String groupStr = m.group();
            if (groupStr.contains("x"))
            {
                x = Double.parseDouble(groupStr.replace("x", "").trim());
            }
            else if (groupStr.contains("y"))
            {
                y = Double.parseDouble(groupStr.replace("y", "").trim());
            }
            else if (groupStr.contains("z"))
            {
                z = Double.parseDouble(groupStr.replace("z", "").trim());
            }
        }

        Position3D result = new Position3D(x, y, z);

        return result;
    }

}
