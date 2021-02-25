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

import javax.swing.JSpinner;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.IMachineStatusEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.MachineStatusEvent;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLActiveStates;
import sourcerabbit.gcode.sender.Core.CNCController.Position.Position2D;
import sourcerabbit.gcode.sender.Core.CNCController.Processes.Process_HoleCenterFinder;
import sourcerabbit.gcode.sender.Core.Settings.CenterHoleFinderSettings;
import sourcerabbit.gcode.sender.UI.UITools.UITools;
import sourcerabbit.gcode.sender.UI.frmControl;

/**
 *
 * @author Nikos Siatras
 */
public class frmHoleCenterFinder extends javax.swing.JDialog
{

    private final frmControl fMyMain;
    private final Process_HoleCenterFinder fMyProcess;
    private IMachineStatusEventListener fIMachineStatusEventListener;

    public frmHoleCenterFinder(frmControl parent, boolean modal)
    {
        super(parent, modal);
        fMyMain = parent;
        fMyProcess = new Process_HoleCenterFinder(this);
        initComponents();

        // Set form in middle of frmControl
        Position2D pos = UITools.getPositionForDialogToOpenInMiddleOfParentForm(parent, this);
        this.setLocation((int) pos.getX(), (int) pos.getY());

        // Fix jSpinnerTouchProbeDiameter to work with system decimal point
        jSpinnerFeedRate.setEditor(new JSpinner.NumberEditor(jSpinnerFeedRate, "##.##"));
        UITools.FixSpinnerToWorkWithSystemDecimalPoint(jSpinnerFeedRate);

        // Fix jSpinnerDiameter to work with system decimal point
        jSpinnerDiameter.setEditor(new JSpinner.NumberEditor(jSpinnerDiameter, "##.##"));
        UITools.FixSpinnerToWorkWithSystemDecimalPoint(jSpinnerDiameter);

        InitEvents();

        // Set the last used touch probe feed rate value to jSpinnerFeedRate
        // and hole diameter
        jSpinnerFeedRate.setValue(CenterHoleFinderSettings.getTouchProbeFeedRate());
        jSpinnerDiameter.setValue(CenterHoleFinderSettings.getHoleCenterFinderDiameter());
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jButtonTouch = new javax.swing.JButton();
        jLabelWarning = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jSpinnerFeedRate = new javax.swing.JSpinner();
        jLabel15 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jSpinnerDiameter = new javax.swing.JSpinner();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Hole Center Finder");
        setPreferredSize(new java.awt.Dimension(660, 347));
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosed(java.awt.event.WindowEvent evt)
            {
                formWindowClosed(evt);
            }
        });

        jButtonTouch.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jButtonTouch.setText("Find Center");
        jButtonTouch.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonTouchActionPerformed(evt);
            }
        });

        jLabelWarning.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabelWarning.setForeground(new java.awt.Color(204, 0, 0));
        jLabelWarning.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelWarning.setText("<html>The machine's status must be <b>Idle</b> to use the \"Hole Center Finder\" operation.</html>");

        jLabel10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sourcerabbit/gcode/sender/UI/Images/HoleCenterFinder/HoleCenterFinder.png"))); // NOI18N
        jLabel10.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jSpinnerFeedRate.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jSpinnerFeedRate.setModel(new javax.swing.SpinnerNumberModel(80.0d, 10.0d, 55000.0d, 10.0d));

        jLabel15.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel15.setText("mm/min");

        jLabel14.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabel14.setText("Lower feed rate = Better Accuracy");

        jLabel19.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel19.setText("Feedrate:");

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel1.setText("Hole Center Finder");

        jLabel16.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel16.setText("1. Place the Touch Probe inside the hole");

        jLabel17.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel17.setText("2. Make sure the Touch Probe is not in contact with the edges");

        jLabel18.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel18.setText("3. Click the \"Find Center\" button");

        jLabel20.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel20.setText("Hole Diameter:");

        jSpinnerDiameter.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jSpinnerDiameter.setModel(new javax.swing.SpinnerNumberModel(80.0d, 10.0d, 55000.0d, 10.0d));
        jSpinnerDiameter.setValue(100);

        jLabel21.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel21.setText("mm");

        jLabel22.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabel22.setText("A proximity of the hole's diameter");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelWarning)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel14)
                                    .addComponent(jLabel16)
                                    .addComponent(jLabel17)
                                    .addComponent(jLabel18)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(jLabel22)
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel20)
                                                .addGap(18, 18, 18)
                                                .addComponent(jSpinnerDiameter, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel19)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jSpinnerFeedRate, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel15)
                                            .addComponent(jLabel21))))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 262, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButtonTouch, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(233, 233, 233))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel19)
                            .addComponent(jLabel15)
                            .addComponent(jSpinnerFeedRate, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel20)
                            .addComponent(jLabel21)
                            .addComponent(jSpinnerDiameter, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel10))
                .addGap(18, 18, 18)
                .addComponent(jLabelWarning, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonTouch, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void InitEvents()
    {
        // Call UpdateUIOnMachineStatusChange with the current machine status
        UpdateUIOnMachineStatusChange(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getActiveState());

        // Machine status events
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().AddListener(fIMachineStatusEventListener = new IMachineStatusEventListener()
        {
            @Override
            public void MachineStatusChanged(MachineStatusEvent evt)
            {
                final int activeState = evt.getMachineStatus();
                UpdateUIOnMachineStatusChange(activeState);
            }

            @Override
            public void MachineStatusReceived(MachineStatusEvent evt)
            {
                // DO NOTHING
            }

        });
    }

    private void UpdateUIOnMachineStatusChange(int machineStatus)
    {
        switch (machineStatus)
        {
            case GRBLActiveStates.IDLE:
                jButtonTouch.setEnabled(true);
                jButtonTouch.setText("Find Center");
                jLabelWarning.setText("");
                break;

            case GRBLActiveStates.JOG:
            case GRBLActiveStates.RUN:
            case GRBLActiveStates.MACHINE_TOUCHED_PROBE:
                jButtonTouch.setEnabled(true);
                jButtonTouch.setText("Click to Stop!");
                break;

            case GRBLActiveStates.HOLD:
            case GRBLActiveStates.ALARM:
            case GRBLActiveStates.RESET_TO_CONTINUE:
            case GRBLActiveStates.MACHINE_IS_LOCKED:
                jButtonTouch.setEnabled(false);
                jLabelWarning.setText("<html>The machine's status must be <b>Idle</b> to use the \"Hole Center Finder\" operation.</html>");
                break;

        }
    }

    private void jButtonTouchActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonTouchActionPerformed
    {//GEN-HEADEREND:event_jButtonTouchActionPerformed
        if (jButtonTouch.getText().equals("Click to Stop!"))
        {
            fMyProcess.KillImmediately();
            jButtonTouch.setText("Find Center");
        }
        else
        {
            Double feedRateDouble = Double.parseDouble(jSpinnerFeedRate.getValue().toString());
            final int feedRate = feedRateDouble.intValue();

            Double diameterDouble = Double.parseDouble(jSpinnerDiameter.getValue().toString());
            final int diameter = diameterDouble.intValue();

            // Set last settings
            CenterHoleFinderSettings.setTouchProbeFeedRate(feedRate);
            CenterHoleFinderSettings.setHoleCenterFinderDiameter(diameter);

            fMyProcess.ExecuteInNewThread();
            jButtonTouch.setText("Click to Stop!");
        }
    }//GEN-LAST:event_jButtonTouchActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosed
    {//GEN-HEADEREND:event_formWindowClosed
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().RemoveListener(fIMachineStatusEventListener);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
        fMyProcess.Dispose();
    }//GEN-LAST:event_formWindowClosed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonTouch;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabelWarning;
    private javax.swing.JSpinner jSpinnerDiameter;
    private javax.swing.JSpinner jSpinnerFeedRate;
    // End of variables declaration//GEN-END:variables
}
