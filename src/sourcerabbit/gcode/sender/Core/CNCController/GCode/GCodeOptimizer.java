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



/**
 *
 * @author Nikos Siatras
 */
public class GCodeOptimizer
{

    //private final static DecimalFormatSymbols fDecimalSeparator = DecimalFormatSymbols.getInstance();
    //private static DecimalFormat fDecimalFormatter;
    //private static Pattern fDecimalPattern;

    static
    {
        Initialize();
    }

    public static void Initialize()
    {
        // GCode decimal separator is always the '.' character.
        //fDecimalSeparator.setDecimalSeparator('.');

        /*
        // Initialize the fDecimalFormatter
        String format = "#.";
        for (int i = 0; i < GCodeSenderSettings.getTruncateDecimalDigits(); i++)
        {
            format += "#";
        }
        fDecimalFormatter = new DecimalFormat(format, fDecimalSeparator);

        // Initialize the Regular Expression that "detects" the decimals
        format = "\\d+\\.\\d";
        for (int i = 0; i < GCodeSenderSettings.getTruncateDecimalDigits(); i++)
        {
            format += "\\d";
        }

        format += "+";
        fDecimalPattern = Pattern.compile(format);*/
    }

    public static String OptimizeGCodeCommand(GCodeCommand command)
    {
        return command.getCommand().replace(" ", "").replace(command.getComment(), "").trim();
        //return TruncateDecimals(command.getCommand().replace(" ", "").replace(command.getComment(), "").trim());
    }

    /*private static String TruncateDecimals(final String command)
    {
        Matcher matcher = fDecimalPattern.matcher(command);
        Double d;
        StringBuffer sb = new StringBuffer();
        while (matcher.find())
        {
            d = Double.parseDouble(matcher.group());
            matcher.appendReplacement(sb, fDecimalFormatter.format(d));
        }
        matcher.appendTail(sb);


        return sb.toString();
    }*/
}
