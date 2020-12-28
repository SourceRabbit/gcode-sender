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
public class GRBLSettingsDescription
{

    private final HashMap<Integer, String> fSettingsDescriptions = new HashMap<Integer, String>();
    private final HashMap<Integer, String> fSettingsTypes = new HashMap<Integer, String>();

    public GRBLSettingsDescription()
    {
        final String v1_1 = "\"0\",\"Step pulse time\",\"microseconds\",\"Sets time length per step. Minimum 3usec.\"\n"
                + "\"1\",\"Step idle delay\",\"milliseconds\",\"Sets a short hold delay when stopping to let dynamics settle before disabling steppers. Value 255 keeps motors enabled with no delay.\"\n"
                + "\"2\",\"Step pulse invert\",\"mask\",\"Inverts the step signal. Set axis bit to invert (00000ZYX).\"\n"
                + "\"3\",\"Step direction invert\",\"mask\",\"Inverts the direction signal. Set axis bit to invert (00000ZYX).\"\n"
                + "\"4\",\"Invert step enable pin\",\"boolean\",\"Inverts the stepper driver enable pin signal.\"\n"
                + "\"5\",\"Invert limit pins\",\"boolean\",\"Inverts the all of the limit input pins.\"\n"
                + "\"6\",\"Invert probe pin\",\"boolean\",\"Inverts the probe input pin signal.\"\n"
                + "\"10\",\"Status report options\",\"mask\",\"Alters data included in status reports.\"\n"
                + "\"11\",\"Junction deviation\",\"millimeters\",\"Sets how fast Grbl travels through consecutive motions. Lower value slows it down.\"\n"
                + "\"12\",\"Arc tolerance\",\"millimeters\",\"Sets the G2 and G3 arc tracing accuracy based on radial error. Beware: A very small value may effect performance.\"\n"
                + "\"13\",\"Report in inches\",\"boolean\",\"Enables inch units when returning any position and rate value that is not a settings value.\"\n"
                + "\"20\",\"Soft limits enable\",\"boolean\",\"Enables soft limits checks within machine travel and sets alarm when exceeded. Requires homing.\"\n"
                + "\"21\",\"Hard limits enable\",\"boolean\",\"Enables hard limits. Immediately halts motion and throws an alarm when switch is triggered.\"\n"
                + "\"22\",\"Homing cycle enable\",\"boolean\",\"Enables homing cycle. Requires limit switches on all axes.\"\n"
                + "\"23\",\"Homing direction invert\",\"mask\",\"Homing searches for a switch in the positive direction. Set axis bit (00000ZYX) to search in negative direction.\"\n"
                + "\"24\",\"Homing locate feed rate\",\"mm/min\",\"Feed rate to slowly engage limit switch to determine its location accurately.\"\n"
                + "\"25\",\"Homing search seek rate\",\"mm/min\",\"Seek rate to quickly find the limit switch before the slower locating phase.\"\n"
                + "\"26\",\"Homing switch debounce delay\",\"milliseconds\",\"Sets a short delay between phases of homing cycle to let a switch debounce.\"\n"
                + "\"27\",\"Homing switch pull-off distance\",\"millimeters\",\"Retract distance after triggering switch to disengage it. Homing will fail if switch isn't cleared.\"\n"
                + "\"30\",\"Maximum spindle speed\",\"RPM\",\"Maximum spindle speed. Sets PWM to 100% duty cycle.\"\n"
                + "\"31\",\"Minimum spindle speed\",\"RPM\",\"Minimum spindle speed. Sets PWM to 0.4% or lowest duty cycle.\"\n"
                + "\"32\",\"Laser-mode enable\",\"boolean\",\"Enables laser mode. Consecutive G1/2/3 commands will not halt when spindle speed is changed.\"\n"
                + "\"100\",\"X-axis travel resolution\",\"step/mm\",\"X-axis travel resolution in steps per millimeter.\"\n"
                + "\"101\",\"Y-axis travel resolution\",\"step/mm\",\"Y-axis travel resolution in steps per millimeter.\"\n"
                + "\"102\",\"Z-axis travel resolution\",\"step/mm\",\"Z-axis travel resolution in steps per millimeter.\"\n"
                + "\"103\",\"A-axis travel resolution\",\"step/mm\",\"A-axis travel resolution in steps per millimeter.\"\n"
                + "\"104\",\"B-axis travel resolution\",\"step/mm\",\"B-axis travel resolution in steps per millimeter.\"\n"
                + "\"105\",\"C-axis travel resolution\",\"step/mm\",\"C-axis travel resolution in steps per millimeter.\"\n"
                + "\"110\",\"X-axis maximum rate\",\"mm/min\",\"X-axis maximum rate. Used as G0 rapid rate.\"\n"
                + "\"111\",\"Y-axis maximum rate\",\"mm/min\",\"Y-axis maximum rate. Used as G0 rapid rate.\"\n"
                + "\"112\",\"Z-axis maximum rate\",\"mm/min\",\"Z-axis maximum rate. Used as G0 rapid rate.\"\n"
                + "\"113\",\"A-axis maximum rate\",\"mm/min\",\"A-axis maximum rate. Used as G0 rapid rate.\"\n"
                + "\"114\",\"B-axis maximum rate\",\"mm/min\",\"B-axis maximum rate. Used as G0 rapid rate.\"\n"
                + "\"115\",\"C-axis maximum rate\",\"mm/min\",\"C-axis maximum rate. Used as G0 rapid rate.\"\n"
                + "\"120\",\"X-axis acceleration\",\"mm/sec^2\",\"X-axis acceleration. Used for motion planning to not exceed motor torque and lose steps.\"\n"
                + "\"121\",\"Y-axis acceleration\",\"mm/sec^2\",\"Y-axis acceleration. Used for motion planning to not exceed motor torque and lose steps.\"\n"
                + "\"122\",\"Z-axis acceleration\",\"mm/sec^2\",\"Z-axis acceleration. Used for motion planning to not exceed motor torque and lose steps.\"\n"
                + "\"123\",\"A-axis acceleration\",\"mm/sec^2\",\"A-axis acceleration. Used for motion planning to not exceed motor torque and lose steps.\"\n"
                + "\"124\",\"B-axis acceleration\",\"mm/sec^2\",\"B-axis acceleration. Used for motion planning to not exceed motor torque and lose steps.\"\n"
                + "\"125\",\"C-axis acceleration\",\"mm/sec^2\",\"C-axis acceleration. Used for motion planning to not exceed motor torque and lose steps.\"\n"
                + "\"130\",\"X-axis maximum travel\",\"millimeters\",\"Maximum X-axis travel distance from homing switch. Determines valid machine space for soft-limits and homing search distances.\"\n"
                + "\"131\",\"Y-axis maximum travel\",\"millimeters\",\"Maximum Y-axis travel distance from homing switch. Determines valid machine space for soft-limits and homing search distances.\"\n"
                + "\"132\",\"Z-axis maximum travel\",\"millimeters\",\"Maximum Z-axis travel distance from homing switch. Determines valid machine space for soft-limits and homing search distances.\"\n"
                + "\"133\",\"A-axis maximum travel\",\"millimeters\",\"Maximum A-axis travel distance from homing switch. Determines valid machine space for soft-limits and homing search distances.\"\n"
                + "\"134\",\"B-axis maximum travel\",\"millimeters\",\"Maximum B-axis travel distance from homing switch. Determines valid machine space for soft-limits and homing search distances.\"\n"
                + "\"135\",\"C-axis maximum travel\",\"millimeters\",\"Maximum C-axis travel distance from homing switch. Determines valid machine space for soft-limits and homing search distances.\"\n";

        CSVReader reader = new CSVReader();
        reader.Parse(v1_1);

        ArrayList<ArrayList<String>> csvData = reader.getDataInArrayFormat();

        for (ArrayList<String> array : csvData)
        {
            int id = Integer.parseInt(array.get(0));
            fSettingsDescriptions.put(id, array.get(1));
            fSettingsTypes.put(id, array.get(2));
        }
    }

    public String getSettingDescription(int id)
    {
        return fSettingsDescriptions.containsKey(id) ? fSettingsDescriptions.get(id) : "";
    }

    public String getSettingType(int id)
    {
        return fSettingsTypes.containsKey(id) ? fSettingsTypes.get(id) : "";
    }
}
