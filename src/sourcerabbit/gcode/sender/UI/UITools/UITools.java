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

import sourcerabbit.gcode.sender.Core.CNCController.Position.Position2D;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JSpinner;

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

    public static Position2D getPositionForDialogToOpenInMiddleOfParentForm(JFrame parent, JDialog form)
    {
        // Set form in middle of frmControl
        final int frmControlWidth = parent.getSize().width;
        final int frmControlHeight = parent.getSize().height;
        final int w = form.getSize().width;
        final int h = form.getSize().height;
        final int x = ((frmControlWidth - w) / 2) + parent.getX();
        final int y = (frmControlHeight - h) / 2 + parent.getY();

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

    public static void FixSpinnerToWorkWithSystemDecimalPoint(JSpinner spinner)
    {
        // Get system decimal separator
        DecimalFormat decFormat = new DecimalFormat();
        DecimalFormatSymbols decSymbols = decFormat.getDecimalFormatSymbols();
        final String decimalSeparator = String.valueOf(decSymbols.getDecimalSeparator());

        // Get the char to replace with the decimal separator
        final String replaceWithDecimalSeparator = decimalSeparator.equals(",") ? "." : ",";

        JFormattedTextField jSpinnerTF = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().addKeyListener(new KeyListener()
        {

            @Override
            public void keyPressed(KeyEvent e)
            {
            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                final String spinnerText = jSpinnerTF.getText().replace(replaceWithDecimalSeparator, decimalSeparator);
                jSpinnerTF.setText(spinnerText);
            }

            @Override
            public void keyTyped(KeyEvent e)
            {
            }
        });
    }
}
