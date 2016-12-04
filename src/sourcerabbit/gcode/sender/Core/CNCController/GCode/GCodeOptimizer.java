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
package sourcerabbit.gcode.sender.Core.CNCController.GCode;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;

/**
 *
 * @author Nikos Siatras
 */
public class GCodeOptimizer
{

    // The maximum number of decimals a GCode command can have
    private final static int fMaxDecimalsLength;

    private final static DecimalFormatSymbols fDecimalSeparator = DecimalFormatSymbols.getInstance();
    private final static DecimalFormat fDecimalFormatter;
    private final static Pattern fDecimalPattern;

    static
    {
        // Set the maximum number of decimals a GCode command can have
        switch (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getCNCControlFramework())
        {
            case GRBL:
                fMaxDecimalsLength = 3;
                break;

            default:
                fMaxDecimalsLength = 4;
                break;
        }

        // GCode decimal separator is always the '.' character.
        fDecimalSeparator.setDecimalSeparator('.');

        // Initialize the fDecimalFormatter
        String format = "#.";
        for (int i = 0; i < fMaxDecimalsLength; i++)
        {
            format += "#";
        }
        fDecimalFormatter = new DecimalFormat(format, fDecimalSeparator);

        // Initialize the Regular Expression that "detects" the decimals
        format = "\\d+\\.\\d";
        for (int i = 0; i < fMaxDecimalsLength; i++)
        {
            format += "\\d";
        }

        format += "+";
        fDecimalPattern = Pattern.compile(format);
    }

    public static String OptimizeGCodeCommand(GCodeCommand command)
    {
        return TruncateDecimals(command.getCommand().replace(" ", "").replace(command.getComment(), "").trim());
    }

    private static String TruncateDecimals(final String command)
    {
        Matcher matcher = fDecimalPattern.matcher(command);
        Float d;
        StringBuffer sb = new StringBuffer();
        while (matcher.find())
        {
            d = Float.parseFloat(matcher.group());
            matcher.appendReplacement(sb, fDecimalFormatter.format(d));
        }
        matcher.appendTail(sb);

        /*System.out.println("Original: " + command);
        System.out.println("Optimized: " + sb.toString());
        System.out.println("-------------------------------------------------");*/

        // Return new command.
        return sb.toString();
    }
}
