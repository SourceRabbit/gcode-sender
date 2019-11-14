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
package sourcerabbit.gcode.sender.Core.Settings;

/**
 *
 * @author Nikos Siatras
 */
public class CenterHoleFinderSettings
{

    private final static String TOUCH_PROBE_FEEDRATE = "TOUCH_PROBE_FEEDRATE";
    private final static String HOLE_CENTER_FINDER_DIAMETER = "HOLE_CENTER_FINDER_DIAMETER";

    static
    {

    }

    public static void setTouchProbeFeedRate(int value)
    {
        SettingsManager.fAppSettings.setProperty(TOUCH_PROBE_FEEDRATE, String.valueOf(value));
        SettingsManager.SaveSettings();
    }

    public static int getTouchProbeFeedRate()
    {
        try
        {
            String value = SettingsManager.fAppSettings.getProperty(TOUCH_PROBE_FEEDRATE);
            if (value == null || value.equals(""))
            {
                return 100;
            }
            else
            {
                return Integer.parseInt(value);
            }
        }
        catch (Exception ex)
        {
            return 100;
        }
    }

    public static void setHoleCenterFinderDiameter(int value)
    {
        SettingsManager.fAppSettings.setProperty(HOLE_CENTER_FINDER_DIAMETER, String.valueOf(value));
        SettingsManager.SaveSettings();
    }

    public static int getHoleCenterFinderDiameter()
    {
        try
        {
            String value = SettingsManager.fAppSettings.getProperty(HOLE_CENTER_FINDER_DIAMETER);
            if (value == null || value.equals(""))
            {
                return 100;
            }
            else
            {
                return Integer.parseInt(value);
            }
        }
        catch (Exception ex)
        {
            return 100;
        }
    }

}
