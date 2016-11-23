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

import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.IMachineStatusEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.MachineStatusEvent;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLActiveStates;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLCommands;
import sourcerabbit.gcode.sender.Core.Threading.ManualResetEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Position.Position2D;
import sourcerabbit.gcode.sender.Core.Settings.CenterHoleFinderSettings;
import sourcerabbit.gcode.sender.UI.UITools.UITools;
import sourcerabbit.gcode.sender.UI.frmControl;

/**
 *
 * @author Nikos Siatras
 */
public class frmEdgeFinder extends javax.swing.JDialog
{

    private final frmControl fMyMain;
    private boolean fFormIsOpened = true;

    // Procedure Thread
    private Thread fProcedureThread;
    private boolean fProcedureThreadIsRunning = false;

    // Variables...
    private final int fMaxDistance = 50000;
    private final int fSlowFeedRate = 40;

    private boolean fShowWarningIfNecessary = true;
    private boolean fTouchProbeTouchedTheEdge = false;
    private final ManualResetEvent fWaitForTouchProbeToTouchTheEdge = new ManualResetEvent(false);
    private final ManualResetEvent fWaitForMachineToBeIdle = new ManualResetEvent(false);

    private IMachineStatusEventListener fIMachineStatusEventListener;

    public frmEdgeFinder(frmControl parent, boolean modal)
    {
        super(parent, modal);
        fMyMain = parent;
        initComponents();

        // Set form in middle of frmControl
        Position2D pos = UITools.getPositionForDialogToOpenInMiddleOfParentForm(parent, this);
        this.setLocation((int) pos.getX(), (int) pos.getY());

        // Fix jSpinnerTouchProbeDiameter to work with system decimal point
        jSpinnerFeedRate.setEditor(new JSpinner.NumberEditor(jSpinnerFeedRate, "##.##"));
        UITools.FixSpinnerToWorkWithSystemDecimalPoint(jSpinnerFeedRate);

        InitEvents();

        // Set the last used touch probe feed rate value to jSpinnerFeedRate
        jSpinnerFeedRate.setValue(CenterHoleFinderSettings.getTouchProbeFeedRate());
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jButtonTouch = new javax.swing.JButton();
        jLabelWarning = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel1 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jSpinnerFeedRate = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Hole Center Finder");
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
        jLabelWarning.setText("The machine's status must be Idle to use the \"Hole Center Finder\" operation.");

        jLabel10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sourcerabbit/gcode/sender/UI/Images/HoleCenterFinder/HoleCenterFinder.png"))); // NOI18N
        jLabel10.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel14.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel14.setText("Lower feed rate = Better Result");

        jLabel15.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(0, 75, 127));
        jLabel15.setText("mm/min");

        jSpinnerFeedRate.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jSpinnerFeedRate.setModel(new javax.swing.SpinnerNumberModel(80.0d, 10.0d, 55000.0d, 10.0d));

        jLabel12.setFont(new java.awt.Font("Tahoma", 1, 20)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(0, 75, 127));
        jLabel12.setText("Feed Rate");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel14)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jSpinnerFeedRate, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel15)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(43, 43, 43)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinnerFeedRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(10, 10, 10)
                    .addComponent(jLabel12)
                    .addContainerGap(67, Short.MAX_VALUE)))
        );

        jLabel16.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel16.setText("1. Place touch probe inside the hole");

        jLabel17.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel17.setText("2. Make sure touch probe is not in contact with the edges");

        jLabel20.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel20.setText("3. Click the \"Find Center\" button.");

        jLabel11.setFont(new java.awt.Font("Tahoma", 1, 20)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(0, 75, 127));
        jLabel11.setText("How to Use");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel20)
                .addGap(19, 19, 19))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelWarning, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 262, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButtonTouch, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(233, 233, 233))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(25, 25, 25)
                .addComponent(jLabelWarning)
                .addGap(18, 18, 18)
                .addComponent(jButtonTouch, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void InitEvents()
    {
        // Call UpdateUIOnMachineStatusChange with the current machine status
        UpdateUIOnMachineStatusChange(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getActiveState(), "");

        // Machine status events
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().AddListener(fIMachineStatusEventListener = new IMachineStatusEventListener()
        {
            @Override
            public void MachineStatusChanged(MachineStatusEvent evt)
            {
                final int activeState = evt.getMachineStatus();
                UpdateUIOnMachineStatusChange(activeState, evt.getMessage());
            }
        });
    }

    private void UpdateUIOnMachineStatusChange(int machineStatus, String message)
    {
        switch (machineStatus)
        {
            case GRBLActiveStates.IDLE:
                fWaitForMachineToBeIdle.Set();
                jButtonTouch.setEnabled(true);
                jLabelWarning.setVisible(false);
                break;

            case GRBLActiveStates.RUN:
                //jButtonTouch.setEnabled(false);
                if (fShowWarningIfNecessary)
                {
                    jLabelWarning.setVisible(true);
                }
                break;
            case GRBLActiveStates.HOLD:
            case GRBLActiveStates.ALARM:
            case GRBLActiveStates.RESET_TO_CONTINUE:
                jButtonTouch.setEnabled(false);
                jLabelWarning.setVisible(true);
                fWaitForTouchProbeToTouchTheEdge.Set();

                if (fProcedureThreadIsRunning)
                {
                    ProbeFailedToTouchTheEdgeOfTheHole("Failed to find the center of the hole!\n\nMake sure your touch probe is not in contact with the\nhole edges before you click the 'Find Center' button.");
                }

                break;

            case GRBLActiveStates.MACHINE_TOUCHED_PROBE:
                fTouchProbeTouchedTheEdge = true;
                fWaitForTouchProbeToTouchTheEdge.Set();
                break;
        }
    }

    private void jButtonTouchActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonTouchActionPerformed
    {//GEN-HEADEREND:event_jButtonTouchActionPerformed

        if (jButtonTouch.getText().equals("Click to Stop!"))
        {
            try
            {
                ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendDataImmediately_WithoutMessageCollector(GRBLCommands.COMMAND_SOFT_RESET);
            }
            catch (Exception ex)
            {
            }
            jButtonTouch.setText("Find Center");
            return;
        }
        else
        {
            jButtonTouch.setText("Click to Stop!");
        }

        fProcedureThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                fProcedureThreadIsRunning = true;
                final int distance = 50000; // Travel 50000mm MAX!
                Double dd = Double.parseDouble(jSpinnerFeedRate.getValue().toString());
                final int feedRate = dd.intValue();

                // Set last settings
                CenterHoleFinderSettings.setTouchProbeFeedRate(feedRate);

                try
                {
                    fShowWarningIfNecessary = false;
                    FindAxisCenter("X", distance, feedRate);
                    FindAxisCenter("Y", distance, feedRate);

                    OperationCompletedSuccessfully();
                    SetWorkPosition("X", 0);
                    SetWorkPosition("Y", 0);
                }
                catch (Exception ex)
                {
                    ProbeFailedToTouchTheEdgeOfTheHole(ex.getMessage());
                }

                fProcedureThreadIsRunning = false;
            }
        });
        fProcedureThread.start();
    }//GEN-LAST:event_jButtonTouchActionPerformed

    private void FindAxisCenter(final String axis, final int distance, final int feedRate) throws Exception
    {
        String response = "";
        double startWorkPosition = 0, workPosition1 = 0, workPosition2 = 0;

        // Before everything get the start Work position (The current work position).
        AskForMachineStatus();
        switch (axis)
        {
            case "X":
                startWorkPosition = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getX();
                break;

            case "Y":
                startWorkPosition = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY();
                break;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 1
        // Move the touchprobe towards the "axis -"  until it touches the edge of the hole.
        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        response = SendTouchProbeToTouchTheEdge(axis + "-", feedRate);
        if (!response.equals("ok") || !fTouchProbeTouchedTheEdge)
        {
            throw new Exception("Make sure the touch probe does not touch the edges of the hole!");
        }

        // Move touchprobe 0.5mm back and repeat the process with the slower feed rate
        MoveMachine(axis, 0.5, fSlowFeedRate);
        response = SendTouchProbeToTouchTheEdge(axis + "-", fSlowFeedRate);
        if (!response.equals("ok") || !fTouchProbeTouchedTheEdge)
        {
            throw new Exception("Make sure the touch probe does not touch the edges of the hole!");
        }

        // Ask for machine status to get the current Work Position
        AskForMachineStatus();
        switch (axis)
        {
            case "X":
                workPosition1 = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getX();
                break;

            case "Y":
                workPosition1 = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY();
                break;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 2
        // At the moment the touch probe touches the edge of the hole on "axis -" .
        // Return the touchprobe to the startWorkPosition and then towards the "axis +" until it touches the edge of the hole.
        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        MoveMachineTo(axis, startWorkPosition, feedRate);
        response = SendTouchProbeToTouchTheEdge(axis, feedRate);
        if (!response.equals("ok") || !fTouchProbeTouchedTheEdge)
        {
            throw new Exception("Make sure the touch probe does not touch the edges of the hole!");
        }

        // Move touchprobe 0.5mm back and repeat the process with the slower feed rate
        MoveMachine(axis + "-", 0.5, fSlowFeedRate);
        response = SendTouchProbeToTouchTheEdge(axis, fSlowFeedRate);
        if (!response.equals("ok") || !fTouchProbeTouchedTheEdge)
        {
            throw new Exception("Make sure the touch probe does not touch the edges of the hole!");
        }

        // Ask for machine status to get the new Work Position
        AskForMachineStatus();
        switch (axis)
        {
            case "X":
                workPosition2 = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getX();
                break;

            case "Y":
                workPosition2 = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY();
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 3 
        // Move touch probe in the middle of xDiff
        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        SetWorkPosition(axis, 0);
        final double xDiff = Math.max(workPosition1, workPosition2) - Math.min(workPosition1, workPosition2);
        String moveXToMiddleStr = "G21 G1" + axis + "-" + String.valueOf(xDiff / 2) + "F" + feedRate;
        GCodeCommand moveXToMiddleCommand = new GCodeCommand(moveXToMiddleStr);
        response = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(moveXToMiddleCommand);
        if (!response.equals("ok"))
        {
            throw new Exception("");
        }
        AskForMachineStatus();
        SetWorkPosition(axis, 0);
    }

    /**
     * Send the touch probe to touch the edge of an axis.
     *
     * @param axis the axis to touch the edge (X,X-,Y,Y-)
     * @param feedRate the feed rate to use
     * @return
     */
    private String SendTouchProbeToTouchTheEdge(String axis, int feedRate)
    {
        fWaitForTouchProbeToTouchTheEdge.Reset();
        fTouchProbeTouchedTheEdge = false;
        final GCodeCommand command = new GCodeCommand("G38.2 " + axis + fMaxDistance + "F" + feedRate);
        String response = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(command);
        fWaitForTouchProbeToTouchTheEdge.WaitOne();
        return response;
    }

    /**
     * Move machine to specified position.
     *
     * @param axis the axis to move
     * @param position the position to go
     * @param feedRate is the feed rate to use
     */
    private void MoveMachineTo(String axis, double position, int feedRate)
    {
        String str = "G21 G90 G1" + axis + position + "F" + feedRate;
        GCodeCommand moveZCommand = new GCodeCommand(str);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(moveZCommand);
    }

    /**
     * Move the machine.
     *
     * @param axis the axis to move
     * @param value how much to move
     * @param feedRate is the feed rate to use
     */
    private void MoveMachine(String axis, double value, int feedRate)
    {
        String str = "G21 G91 G1" + axis + value + "F" + feedRate;
        GCodeCommand moveZCommand = new GCodeCommand(str);
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(moveZCommand);
    }

    /**
     * Set work position
     *
     * @param axis
     * @param value
     */
    private void SetWorkPosition(String axis, double value)
    {
        try
        {
            String commandStr = "G21 G92 " + axis + value;
            GCodeCommand command = new GCodeCommand(commandStr);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
        }
        catch (Exception ex)
        {

        }
    }

    /**
     * Ask machine for its status.
     */
    private void AskForMachineStatus()
    {
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(new GCodeCommand(GRBLCommands.COMMAND_GET_STATUS));
    }

    private void ProbeFailedToTouchTheEdgeOfTheHole(String message)
    {
        if (fFormIsOpened)
        {
            fShowWarningIfNecessary = true;
            if (message.equals(""))
            {
                JOptionPane.showMessageDialog(this, "Probe failed to touch the edge!", "Error", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
            }
            jButtonTouch.setText("Find Center");
        }
    }

    private void OperationCompletedSuccessfully()
    {
        fWaitForMachineToBeIdle.Reset();
        fShowWarningIfNecessary = false;
        fWaitForMachineToBeIdle.WaitOne();
        JOptionPane.showMessageDialog(this, "Machine position is now in the center of the hole!", "Center Found", JOptionPane.INFORMATION_MESSAGE);
        jButtonTouch.setText("Find Center");

        SetWorkPosition("X", 0);
        SetWorkPosition("Y", 0);
    }


    private void formWindowClosed(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosed
    {//GEN-HEADEREND:event_formWindowClosed
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().RemoveListener(fIMachineStatusEventListener);
        fFormIsOpened = false;
    }//GEN-LAST:event_formWindowClosed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonTouch;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabelWarning;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSpinner jSpinnerFeedRate;
    // End of variables declaration//GEN-END:variables
}
