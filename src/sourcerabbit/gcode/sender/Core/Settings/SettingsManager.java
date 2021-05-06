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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

/**
 *
 * @author Nikos Siatras
 */
public class SettingsManager
{

    private static final String fAppVersion = "1.4.6";
    public static final Properties fAppSettings = new Properties();
    private static String fSettingsFilePath = "";

    static
    {
        fSettingsFilePath = getAppSettingsFilePath();
        LoadSettings();
    }

    /**
     * Load the App settings
     */
    private static void LoadSettings()
    {
        try
        {
            File configFile = new File(fSettingsFilePath);
            try (FileReader reader = new FileReader(configFile))
            {
                fAppSettings.load(reader);
            }
        }
        catch (Exception ex)
        {
            // file does not exist
        }
    }

    /**
     * Save the App settings
     */
    public static void SaveSettings()
    {
        try
        {
            File configFile = new File(fSettingsFilePath);
            try (FileWriter writer = new FileWriter(configFile))
            {
                fAppSettings.store(writer, "SGSSettings");
            }
        }
        catch (Exception ex)
        {
            // file does not exist
        }
    }

    public static ArrayList<String> getMacros()
    {
        ArrayList<String> macros = new ArrayList<>();

        String macrosStr = fAppSettings.getProperty("Macros");
        if (macrosStr != null && !macrosStr.equals(""))
        {
            String tmp[] = macrosStr.split("!-----!");
            macros.addAll(Arrays.asList(tmp));
        }

        while (macros.size() < 10)
        {
            macros.add("");
        }

        return macros;
    }

    public static void setMacros(ArrayList<String> values)
    {
        String macrosStr = "";

        for (String s : values)
        {
            macrosStr += s + "!-----!";
        }
        fAppSettings.setProperty("Macros", macrosStr);

        SaveSettings();
    }

    /**
     * Returns the name of the last port the user used to connect to his CNC
     *
     * @return the name of the port (ex. Com3)
     */
    public static String getPreselectedSerialPort()
    {
        return fAppSettings.getProperty("SerialPort");
    }

    /**
     * Set the last port the user used to connect to his CNC
     *
     * @param port is the name of the port (ex. Com3)
     */
    public static void setPreselectedSerialPort(String port)
    {
        fAppSettings.setProperty("SerialPort", port);
        SaveSettings();
    }

    /**
     * Returns the last GCodeBrowsedDirectory
     *
     * @return
     */
    public static String getLastGCodeBrowsedDirectory()
    {
        String value = fAppSettings.getProperty("LastGCodeBrowsedDirectory");
        if (value == null || value.equals(""))
        {
            return System.getProperty("user.home") + File.separator;
        }
        else
        {
            return value;
        }
    }

    /**
     * Set the last GCodeBrowsedDirectory
     *
     * @param dir
     */
    public static void setLastGCodeBrowsedDirectory(String dir)
    {
        fAppSettings.setProperty("LastGCodeBrowsedDirectory", dir);
        SaveSettings();
    }

    /**
     * Returns the file path of the SourceRabbitGCodeSender.cfg file
     *
     * @return
     */
    public static String getAppSettingsFilePath()
    {
        return System.getProperty("user.home") + File.separator + "SourceRabbitGCodeSender1_1.cfg";
    }

    /**
     * Returns the last GCodeBrowsedDirectory
     *
     * @return
     */
    public static boolean getIsGCodeLogEnabled()
    {
        String value = fAppSettings.getProperty("IsGCodeLogEnabled");
        if (value == null || value.equals(""))
        {
            return false;
        }
        else
        {
            return Boolean.parseBoolean(value);
        }
    }

    /**
     * Set the last IsGCodeLogEnabled
     *
     * @param value
     */
    public static void setIsGCodeLogEnabled(boolean value)
    {
        fAppSettings.setProperty("IsGCodeLogEnabled", String.valueOf(value));
        SaveSettings();
    }

    /////////////////////////////////////////////////////////////////////////
    public static String getAppVersion()
    {
        return fAppVersion;
    }

    /////////////////////////////////////////////////////////////////////////
    /**
     * Set the IsKeyboardJoggingEnabled
     *
     * @param value
     */
    public static void setIsKeyboardJoggingEnabled(boolean value)
    {
        fAppSettings.setProperty("IsKeyboardJoggingEnabled", String.valueOf(value));
        SaveSettings();
    }

    /**
     * Returns the last GCodeBrowsedDirectory
     *
     * @return
     */
    public static boolean getIsKeyboardJoggingEnabled()
    {
        String value = fAppSettings.getProperty("IsKeyboardJoggingEnabled");
        if (value == null || value.equals(""))
        {
            return false;
        }
        else
        {
            return Boolean.parseBoolean(value);
        }
    }

    /////////////////////////////////////////////////////////////////////////
}
