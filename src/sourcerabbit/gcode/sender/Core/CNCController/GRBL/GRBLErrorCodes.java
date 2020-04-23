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

import java.util.ArrayList;
import java.util.HashMap;
import sourcerabbit.gcode.sender.Core.CSV.CSVReader;

/**
 *
 * @author Nikos Siatras
 */
public class GRBLErrorCodes
{

    private static HashMap<Integer, String> fErrorsByID = new HashMap<Integer, String>();

    static
    {
        String v1_1Errors = "\"1\",\"Expected command letter\",\"G-code words consist of a letter and a value. Letter was not found.\"\n"
                + "\"2\",\"Bad number format\",\"Missing the expected G-code word value or numeric value format is not valid.\"\n"
                + "\"3\",\"Invalid statement\",\"Grbl '$' system command was not recognized or supported.\"\n"
                + "\"4\",\"Value < 0\",\"Negative value received for an expected positive value.\"\n"
                + "\"5\",\"Setting disabled\",\"Homing cycle failure. Homing is not enabled via settings.\"\n"
                + "\"6\",\"Value < 3 usec\",\"Minimum step pulse time must be greater than 3usec.\"\n"
                + "\"7\",\"EEPROM read fail. Using defaults\",\"An EEPROM read failed. Auto-restoring affected EEPROM to default values.\"\n"
                + "\"8\",\"Not idle\",\"Grbl '$' command cannot be used unless Grbl is IDLE. Ensures smooth operation during a job.\"\n"
                + "\"9\",\"G-code lock\",\"G-code commands are locked out during alarm or jog state.\"\n"
                + "\"10\",\"Homing not enabled\",\"Soft limits cannot be enabled without homing also enabled.\"\n"
                + "\"11\",\"Line overflow\",\"Max characters per line exceeded. Received command line was not executed.\"\n"
                + "\"12\",\"Step rate > 30kHz\",\"Grbl '$' setting value cause the step rate to exceed the maximum supported.\"\n"
                + "\"13\",\"Check Door\",\"Safety door detected as opened and door state initiated.\"\n"
                + "\"14\",\"Line length exceeded\",\"Build info or startup line exceeded EEPROM line length limit. Line not stored.\"\n"
                + "\"15\",\"Travel exceeded\",\"Jog target exceeds machine travel. Jog command has been ignored.\"\n"
                + "\"16\",\"Invalid jog command\",\"Jog command has no '=' or contains prohibited g-code.\"\n"
                + "\"20\",\"Unsupported command\",\"Unsupported or invalid g-code command found in block.\"\n"
                + "\"21\",\"Modal group violation\",\"More than one g-code command from same modal group found in block.\"\n"
                + "\"22\",\"Undefined feed rate\",\"Feed rate has not yet been set or is undefined.\"\n"
                + "\"23\",\"Invalid gcode ID:23\",\"G-code command in block requires an integer value.\"\n"
                + "\"24\",\"Invalid gcode ID:24\",\"More than one g-code command that requires axis words found in block.\"\n"
                + "\"25\",\"Invalid gcode ID:25\",\"Repeated g-code word found in block.\"\n"
                + "\"26\",\"Invalid gcode ID:26\",\"No axis words found in block for g-code command or current modal state which requires them.\"\n"
                + "\"27\",\"Invalid gcode ID:27\",\"Line number value is invalid.\"\n"
                + "\"28\",\"Invalid gcode ID:28\",\"G-code command is missing a required value word.\"\n"
                + "\"29\",\"Invalid gcode ID:29\",\"G59.x work coordinate systems are not supported.\"\n"
                + "\"30\",\"Invalid gcode ID:30\",\"G53 only allowed with G0 and G1 motion modes.\"\n"
                + "\"31\",\"Invalid gcode ID:31\",\"Axis words found in block when no command or current modal state uses them.\"\n"
                + "\"32\",\"Invalid gcode ID:32\",\"G2 and G3 arcs require at least one in-plane axis word.\"\n"
                + "\"33\",\"Invalid gcode ID:33\",\"Motion command target is invalid.\"\n"
                + "\"34\",\"Invalid gcode ID:34\",\"Arc radius value is invalid.\"\n"
                + "\"35\",\"Invalid gcode ID:35\",\"G2 and G3 arcs require at least one in-plane offset word.\"\n"
                + "\"36\",\"Invalid gcode ID:36\",\"Unused value words found in block.\"\n"
                + "\"37\",\"Invalid gcode ID:37\",\"G43.1 dynamic tool length offset is not assigned to configured tool length axis.\"";

        CSVReader reader = new CSVReader();
        reader.Parse(v1_1Errors);

        ArrayList<ArrayList<String>> csvData = reader.getDataInArrayFormat();

        for (ArrayList<String> array : csvData)
        {
            int id = Integer.parseInt(array.get(0));
            fErrorsByID.put(id, array.get(1) + ": " + array.get(2));
        }
    }

    public static void Initialize()
    {

    }

    public static String getErrorMessageFromCode(int id)
    {
        return fErrorsByID.containsKey(id) ? fErrorsByID.get(id) : "";
    }
}
