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
public class GCodeSenderSettings
{

    ///////////////////////////////////////////////////////////////////////////////////
    private static final String TRUNCATE_DECIMAL_DIGITS = "TRUNCATE_DECIMAL_DIGITS";
    private static int fTruncateDecimalDigits_Temp = -1;

    public static void setTruncateDecimalDigits(int value)
    {
        SettingsManager.fAppSettings.setProperty(TRUNCATE_DECIMAL_DIGITS, String.valueOf(value));
        SettingsManager.SaveSettings();
    }

    public static int getTruncateDecimalDigits()
    {
        if (fTruncateDecimalDigits_Temp == -1)
        {
            try
            {
                String value = SettingsManager.fAppSettings.getProperty(TRUNCATE_DECIMAL_DIGITS);
                if (value == null || value.equals(""))
                {
                    fTruncateDecimalDigits_Temp = 3;
                }
                else
                {
                    fTruncateDecimalDigits_Temp = Integer.parseInt(value);
                }
            }
            catch (Exception ex)
            {
                fTruncateDecimalDigits_Temp = 3;
            }
        }

        return fTruncateDecimalDigits_Temp;
    }
}
