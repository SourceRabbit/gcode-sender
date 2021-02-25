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
package sourcerabbit.gcode.sender.UI.Renderers;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Nikos Siatras
 */
public class JTableRenderer extends DefaultTableCellRenderer
{

    public JTableRenderer()
    {

    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
    {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

        if (isSelected)
        {
            c.setBackground(new Color(221, 223, 225));
            c.setForeground(Color.BLACK);
        }
        else
        {
            if (row % 2 == 1)
            {
                c.setBackground(new Color(235, 238, 243));
            }
            else
            {
                c.setBackground(Color.WHITE);
            }
        }

        return c;
    }
}
