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
package sourcerabbit.gcode.sender.UI;

import java.awt.Toolkit;
import java.util.ArrayDeque;
import java.util.Queue;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Position.Position2D;
import sourcerabbit.gcode.sender.UI.UITools.UITools;

/**
 *
 * @author nsiatras
 */
public class frmGCodeViewer extends javax.swing.JFrame
{

    private final Queue<String> fGCodeQueue;

    /**
     * Creates new form frmGCodeViewer
     */
    public frmGCodeViewer()
    {
        initComponents();

        // Initialize the GCodeQueue
        fGCodeQueue = new ArrayDeque(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getGCodeQueue());

        // Set form in middle of screen
        Position2D pos = UITools.getPositionForFormToOpenInMiddleOfScreen(this.getSize().width, this.getSize().height);
        this.setLocation((int) pos.getX(), (int) pos.getY());

        this.setTitle(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getGCodeFile().getName());
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("Images/SourceRabbitIcon.png")));
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(500, 400));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 500, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
