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
package sourcerabbit.gcode.sender.UI.Tools;

import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.IMachineStatusEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.MachineStatusEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.ISerialConnectionEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.SerialConnectionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLActiveStates;
import sourcerabbit.gcode.sender.UI.frmControl;

/**
 *
 * @author Nikos Siatras
 */
public class frmSetWorkPosition extends javax.swing.JDialog
{

    private final frmControl fMyMain;
    private final frmSetWorkPosition fThisForm = this;

    /**
     * Creates new form frmSetWorkPosition
     */
    public frmSetWorkPosition(frmControl parent, boolean modal)
    {
        super(parent, modal);
        fMyMain = parent;
        initComponents();

        // Set form in middle of frmControl
        final int frmControlWidth = parent.getSize().width;
        final int frmControlHeight = parent.getSize().height;
        final int w = this.getSize().width;
        final int h = this.getSize().height;
        final int x = ((frmControlWidth - w) / 2) + parent.getX();
        final int y = (frmControlHeight - h) / 2 + parent.getY();
        this.setLocation(x, y);

        InitEvents();

        // Set default values
        jTextFieldX.setText(String.valueOf(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX()));
        jTextFieldY.setText(String.valueOf(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY()));
        jTextFieldZ.setText(String.valueOf(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getZ()));
    }

    private void InitEvents()
    {
        jLabelNotice.setText("");
        final MachineStatusEvent lastEvent = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().getCurrentStatus();
        MachineStatusHasChange(lastEvent);

        // Machine status events
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().AddListener(new IMachineStatusEventListener()
        {
            @Override
            public void MachineStatusChanged(MachineStatusEvent evt)
            {
                MachineStatusHasChange(evt);
            }
        });

        // Serial Connection Events
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getSerialConnectionEventManager().AddListener(new ISerialConnectionEventListener()
        {

            @Override
            public void ConnectionEstablished(SerialConnectionEvent evt)
            {

            }

            @Override
            public void ConnectionClosed(SerialConnectionEvent evt)
            {
                fThisForm.dispose();
            }

            @Override
            public void DataReceivedFromSerialConnection(SerialConnectionEvent evt)
            {

            }
        });
    }

    private void MachineStatusHasChange(MachineStatusEvent evt)
    {
        final int activeState = evt.getMachineStatus();
        switch (activeState)
        {
            case GRBLActiveStates.IDLE:
                jLabelNotice.setText("");
                jButtonSet.setEnabled(true);
                break;

            case GRBLActiveStates.RUN:
                jLabelNotice.setText("The machine's status must be Idle!");
                jButtonSet.setEnabled(false);
                break;
            case GRBLActiveStates.HOLD:
                jLabelNotice.setText("Your machine is on hold!");
                jButtonSet.setEnabled(false);
                break;
            case GRBLActiveStates.ALARM:
                jLabelNotice.setText("Your machine has an alarm on!");
                jButtonSet.setEnabled(false);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldX = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldY = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldZ = new javax.swing.JTextField();
        jButtonSet = new javax.swing.JButton();
        jLabelNotice = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Set Work Position");
        setResizable(false);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("This is going to set/override the Work position");

        jLabel2.setText("X:");

        jTextFieldX.setText("0");

        jLabel3.setText("Y:");

        jTextFieldY.setText("0");

        jLabel4.setText("Z:");

        jTextFieldZ.setText("0");

        jButtonSet.setText("Set");
        jButtonSet.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonSetActionPerformed(evt);
            }
        });

        jLabelNotice.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabelNotice.setForeground(new java.awt.Color(204, 0, 0));
        jLabelNotice.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelNotice.setText("NOTICE");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(108, 108, 108)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextFieldX))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextFieldY, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextFieldZ)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addComponent(jButtonSet, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(27, 27, 27)))))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jLabelNotice, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextFieldX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextFieldY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextFieldZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelNotice)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonSet)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonSetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonSetActionPerformed
    {//GEN-HEADEREND:event_jButtonSetActionPerformed
        try
        {
            double x = Double.parseDouble(jTextFieldX.getText());
            double y = Double.parseDouble(jTextFieldY.getText());
            double z = Double.parseDouble(jTextFieldZ.getText());
            String commandStr = "G92 X" + x + " Y" + y + " Z" + z;

            GCodeCommand command = new GCodeCommand(commandStr);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
        }
        catch (Exception ex)
        {

        }
    }//GEN-LAST:event_jButtonSetActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonSet;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabelNotice;
    private javax.swing.JTextField jTextFieldX;
    private javax.swing.JTextField jTextFieldY;
    private javax.swing.JTextField jTextFieldZ;
    // End of variables declaration//GEN-END:variables
}
