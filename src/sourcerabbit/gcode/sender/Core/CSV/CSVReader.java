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
package sourcerabbit.gcode.sender.Core.CSV;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Nikos Siatras
 */
public class CSVReader
{

    private static final Pattern fCSVPattern = Pattern.compile("\"([^\"]*)\"|(?<=,|^)([^,]*)(?:,|$)");
    private ArrayList<ArrayList<String>> fData;

    public CSVReader()
    {

    }

    public void Parse(String str)
    {
        fData = new ArrayList<>();
        Matcher matcher;

        String[] lines = str.split("\n");
        for (String line : lines)
        {
            matcher = fCSVPattern.matcher(line);
            String match;
            ArrayList<String> matches = new ArrayList<String>();
            while (matcher.find())
            {
                match = matcher.group(1);
                if (match != null)
                {
                    matches.add(match);
                }
                else
                {
                    matches.add(matcher.group(2));
                }
            }

            fData.add(matches);
        }
    }

    public ArrayList<ArrayList<String>> getDataInArrayFormat()
    {
        return fData;
    }

}
