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
import java.util.Properties;

/**
 *
 * @author Nikos Siatras
 */
public class SettingsManager
{

    private static final Properties fAppSettings = new Properties();
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
    private static void SaveSettings()
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
     * Returns the file path of the SourceRabbitGCodeSender.cfg file
     *
     * @return
     */
    public static String getAppSettingsFilePath()
    {
        return System.getProperty("user.home") + File.separator + "SourceRabbitGCodeSender.cfg";
    }

}
