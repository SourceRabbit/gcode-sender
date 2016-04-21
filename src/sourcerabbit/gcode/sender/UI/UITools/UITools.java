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
package sourcerabbit.gcode.sender.UI.UITools;

import sourcerabbit.gcode.sender.Core.CNCController.Tools.Position2D;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 *
 * @author Nikos Siatras
 */
public class UITools
{

    public static Position2D getPositionForFormToOpenInMiddleOfScreen(int formWidth, int formHeight)
    {
        // Get the size of the screen
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        // Determine the new location of the window
        final int w = formWidth;
        final int h = formHeight;
        final int x = (dim.width - w) / 2;
        final int y = (dim.height - h) / 2;

        // Move the window
        return new Position2D(x, y);
    }

    /**
     * Returns the system decimal separator (',' or '.')
     *
     * @return
     */
    public static String getSystemDecimalSeparator()
    {
        // Get system Decimal separator
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance();
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return String.valueOf(symbols.getDecimalSeparator());
    }
}
