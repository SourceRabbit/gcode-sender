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
public class SemiAutoToolChangeSettings
{

    private final static String ENABLE_SEMI_AUTO_TOOL_CHANGE = "ENABLE_SEMI_AUTO_TOOL_CHANGE";
    private final static String TOOL_SETTER_X = "TOOL_SETTER_X";
    private final static String TOOL_SETTER_Y = "TOOL_SETTER_Y";
    private final static String TOOL_SETTER_HEIGHT = "TOOL_SETTER_HEIGHT";

    static
    {

    }

    public static void setEnableSemiAutoToolChange(boolean value)
    {
        SettingsManager.fAppSettings.setProperty(ENABLE_SEMI_AUTO_TOOL_CHANGE, String.valueOf(value));
        SettingsManager.SaveSettings();
    }

    public static boolean isSemiAutoToolChangeEnabled()
    {
        try
        {
            String value = SettingsManager.fAppSettings.getProperty(ENABLE_SEMI_AUTO_TOOL_CHANGE);
            if (value == null || value.equals("") || value.toLowerCase().equals("false"))
            {
                return false;
            }
            else
            {
                return true;
            }
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    public static void setToolSetterX(int value)
    {
        SettingsManager.fAppSettings.setProperty(TOOL_SETTER_X, String.valueOf(value));
        SettingsManager.SaveSettings();
        
    
    }

    public static int getToolSetterX()
    {
        try
        {
            String value = SettingsManager.fAppSettings.getProperty(TOOL_SETTER_X);
            if (value == null || value.equals(""))
            {
                return 0;
            }
            else
            {
                return Integer.parseInt(value);
            }
        }
        catch (Exception ex)
        {
            return 0;
        }
    }

    public static void setToolSetterY(int value)
    {
        SettingsManager.fAppSettings.setProperty(TOOL_SETTER_Y, String.valueOf(value));
        SettingsManager.SaveSettings();
    }

    public static int getToolSetterY()
    {
        try
        {
            String value = SettingsManager.fAppSettings.getProperty(TOOL_SETTER_Y);
            if (value == null || value.equals(""))
            {
                return 0;
            }
            else
            {
                return Integer.parseInt(value);
            }
        }
        catch (Exception ex)
        {
            return 0;
        }
    }
    

}
