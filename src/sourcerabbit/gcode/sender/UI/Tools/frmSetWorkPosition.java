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
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLActiveStates;
import sourcerabbit.gcode.sender.Core.CNCController.Position.Position2D;
import sourcerabbit.gcode.sender.Core.CNCController.Processes.Process_SetWorkPosition;
import sourcerabbit.gcode.sender.UI.UITools.UITools;
import sourcerabbit.gcode.sender.UI.frmControl;

/**
 *
 * @author Nikos Siatras
 */
public class frmSetWorkPosition extends javax.swing.JDialog
{

    private final frmControl fMyMain;
    private IMachineStatusEventListener fIMachineStatusEventListener;
    private final frmSetWorkPosition fThisForm = this;

    /**
     * Creates new form frmSetWorkPosition
     *
     * @param parent
     * @param modal
     */
    public frmSetWorkPosition(frmControl parent, boolean modal)
    {
        super(parent, modal);
        fMyMain = parent;
        initComponents();

        // Set form in middle of frmControl
        Position2D pos = UITools.getPositionForDialogToOpenInMiddleOfParentForm(parent, this);
        this.setLocation((int) pos.getX(), (int) pos.getY());

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
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().AddListener(fIMachineStatusEventListener = new IMachineStatusEventListener()
        {
            @Override
            public void MachineStatusChanged(MachineStatusEvent evt)
            {
                MachineStatusHasChange(evt);
            }

            @Override
            public void MachineStatusReceived(MachineStatusEvent evt)
            {
                // DO NOTHING
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
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Set Work Position");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosed(java.awt.event.WindowEvent evt)
            {
                formWindowClosed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel1.setText("Set Work Position");

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel2.setText("X:");

        jTextFieldX.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jTextFieldX.setText("0");

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel3.setText("Y:");

        jTextFieldY.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jTextFieldY.setText("0");

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel4.setText("Z:");

        jTextFieldZ.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jTextFieldZ.setText("0");

        jButtonSet.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jButtonSet.setText("Set Position");
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

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sourcerabbit/gcode/sender/UI/Images/WorkArea.png"))); // NOI18N
        jLabel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel6.setText("This is going to set/override the work position.");

        jLabel7.setText("It is not going to move the machine.");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelNotice, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6)
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
                                            .addComponent(jTextFieldZ, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addComponent(jLabel7))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                        .addComponent(jLabel5)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButtonSet, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(199, 199, 199))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
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
                        .addGap(18, 18, 18)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7))
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelNotice, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonSet, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
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

            Process_SetWorkPosition process = new Process_SetWorkPosition(this, x, y, z);
            process.Execute();
            process.Dispose();
        }
        catch (Exception ex)
        {

        }
    }//GEN-LAST:event_jButtonSetActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosed
    {//GEN-HEADEREND:event_formWindowClosed
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().RemoveListener(fIMachineStatusEventListener);
    }//GEN-LAST:event_formWindowClosed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonSet;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabelNotice;
    private javax.swing.JTextField jTextFieldX;
    private javax.swing.JTextField jTextFieldY;
    private javax.swing.JTextField jTextFieldZ;
    // End of variables declaration//GEN-END:variables
}
