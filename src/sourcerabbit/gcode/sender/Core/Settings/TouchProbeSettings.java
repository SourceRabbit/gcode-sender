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
public class TouchProbeSettings
{

    private final static String TOUCH_PROBE_DISTANCE_FROM_PROBE = "TOUCH_PROBE_DISTANCE_FROM_PROBE";
    private final static String TOUCH_PROBE_FEEDRATE_TO_PROBE = "TOUCH_PROBE_FEEDRATE_TO_PROBE";
    private final static String TOUCH_PROBE_HEIGH_OF_PROBE = "TOUCH_PROBE_HEIGH_OF_PROBE";

    static
    {

    }

    public static void setDistanceFromProbe(int value)
    {
        SettingsManager.fAppSettings.setProperty(TOUCH_PROBE_DISTANCE_FROM_PROBE, String.valueOf(value));
        SettingsManager.SaveSettings();
    }

    public static int getDistanceFromProbe()
    {
        try
        {
            String value = SettingsManager.fAppSettings.getProperty(TOUCH_PROBE_DISTANCE_FROM_PROBE);
            if (value == null || value.equals(""))
            {
                return 10;
            }
            else
            {
                return Integer.parseInt(value);
            }
        }
        catch (Exception ex)
        {
            return 10;
        }
    }

    public static void setFeedRateToProbe(int value)
    {
        SettingsManager.fAppSettings.setProperty(TOUCH_PROBE_FEEDRATE_TO_PROBE, String.valueOf(value));
        SettingsManager.SaveSettings();
    }

    public static int getFeedRateToProbe()
    {
        try
        {
            String value = SettingsManager.fAppSettings.getProperty(TOUCH_PROBE_FEEDRATE_TO_PROBE);
            if (value == null || value.equals(""))
            {
                return 40;
            }
            else
            {
                return Integer.parseInt(value);
            }
        }
        catch (Exception ex)
        {
            return 40;
        }
    }

    public static void setHeightOfProbe(double value)
    {
        SettingsManager.fAppSettings.setProperty(TOUCH_PROBE_HEIGH_OF_PROBE, String.valueOf(value));
        SettingsManager.SaveSettings();
    }

    public static double getHeightOfProbe()
    {
        try
        {
            String value = SettingsManager.fAppSettings.getProperty(TOUCH_PROBE_HEIGH_OF_PROBE);
            if (value == null || value.equals(""))
            {
                return 1;
            }
            else
            {
                return Double.parseDouble(value);
            }
        }
        catch (Exception ex)
        {
            return 1;
        }
    }

}
