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

import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.traces.Trace2DSimple;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Queue;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeExecutionEvents.GCodeExecutionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeExecutionEvents.IGCodeExecutionEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.SerialConnectionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.ISerialConnectionEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeCycleEvents.GCodeCycleEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeCycleEvents.IGCodeCycleListener;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLActiveStates;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLCommands;
import sourcerabbit.gcode.sender.Core.CNCController.Tools.ManualResetEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Tools.Position2D;
import sourcerabbit.gcode.sender.Core.CNCController.Tools.Position4D;
import sourcerabbit.gcode.sender.Core.Settings.SettingsManager;
import sourcerabbit.gcode.sender.UI.Tools.UITools;

/**
 *
 * @author Nikos Siatras
 */
public class frmControl extends javax.swing.JFrame
{

    private final frmControl fInstance;
    private ManualResetEvent fMachineStatusThreadWait;
    private boolean fKeepMachineStatusThreadRunning;
    private Thread fMachineStatusThread;
    private String fInchesOrMillimetersGCode = "G21";
    private static final Object fAddRemoveLogTableLines = new Object();
    private int fLastActiveState = -1;

    public frmControl()
    {
        fInstance = this;
        initComponents();

        // Set form in middle of screen
        Position2D pos = UITools.getPositionForFormToOpenInMiddleOfScreen(this.getSize().width, this.getSize().height);
        this.setLocation((int) pos.getX(), (int) pos.getY());

        InitEvents();
        InitUIThreads();

        this.setTitle("SourceRabbit GCode Sender (Version " + SettingsManager.getAppVersion() + ")");
    }

    private void InitEvents()
    {
        // Serial Connection Events
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getSerialConnectionEventManager().AddListener(new ISerialConnectionEventListener()
        {
            @Override
            public void ConnectionEstablished(SerialConnectionEvent evt)
            {
                jLabelConnectionStatus.setForeground(new Color(0, 153, 51));
                jLabelConnectionStatus.setText("Connected");
                jButtonConnectDisconnect.setText("Disconnect");
                jButtonConnectDisconnect.setEnabled(true);
                jButtonSoftReset.setEnabled(true);
                jButtonResetZero.setEnabled(true);

                // Enable Machine Control Components
                SetMachineControlsEnabled(true);

                jButtonGCodeBrowse.setEnabled(true);
                jButtonGCodeSend.setEnabled(true);
                jButtonGCodePause.setEnabled(false);
                jButtonGCodeCancel.setEnabled(false);
                jButtonGCodePause.setText("Pause");
                SetMachineControlsEnabled(true);
            }

            @Override
            public void ConnectionClosed(SerialConnectionEvent evt)
            {
                ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().CancelSendingGCode();

                jLabelConnectionStatus.setForeground(Color.red);
                jLabelConnectionStatus.setText("Disconnected");
                jButtonConnectDisconnect.setText("Connect");
                jButtonConnectDisconnect.setEnabled(true);
                jButtonSoftReset.setEnabled(false);
                jButtonResetZero.setEnabled(false);

                // Disable Machine Control Components
                SetMachineControlsEnabled(false);
            }
        });

        // Gcode Cycle Events
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getCycleEventManager().AddListener(new IGCodeCycleListener()
        {
            @Override
            public void GCodeCycleStarted(GCodeCycleEvent evt)
            {
                jButtonGCodeBrowse.setEnabled(false);
                jButtonGCodeSend.setEnabled(false);
                jButtonGCodePause.setEnabled(true);
                jButtonGCodeCancel.setEnabled(true);
                jButtonGCodePause.setText("Pause");
                SetMachineControlsEnabled(false);
                jButtonResetZero.setEnabled(false);
                jButtonSoftReset.setEnabled(false);

                jTextFieldGCodeFile.setEnabled(false);
            }

            @Override
            public void GCodeCycleFinished(GCodeCycleEvent evt)
            {
                jButtonGCodeBrowse.setEnabled(true);
                jButtonGCodeSend.setEnabled(true);
                jButtonGCodePause.setEnabled(false);
                jButtonGCodeCancel.setEnabled(false);
                jButtonGCodePause.setText("Pause");
                SetMachineControlsEnabled(true);
                jButtonResetZero.setEnabled(true);
                jButtonSoftReset.setEnabled(true);

                jTextFieldGCodeFile.setEnabled(true);

                JOptionPane.showMessageDialog(fInstance, evt.getSource().toString(), "Finished", JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            public void GCodeCycleCanceled(GCodeCycleEvent evt)
            {
                jButtonGCodeBrowse.setEnabled(true);
                jButtonGCodeSend.setEnabled(true);
                jButtonGCodePause.setEnabled(false);
                jButtonGCodeCancel.setEnabled(false);
                jButtonGCodePause.setText("Pause");
                SetMachineControlsEnabled(true);
                jButtonResetZero.setEnabled(true);
                jButtonSoftReset.setEnabled(true);

                jTextFieldGCodeFile.setEnabled(true);
            }

            @Override
            public void GCodeCyclePaused(GCodeCycleEvent evt)
            {
                jButtonGCodePause.setText("Resume");
            }

            @Override
            public void GCodeCycleResumed(GCodeCycleEvent evt)
            {
                jButtonGCodePause.setText("Pause");
            }
        });

        // GCode Execution Events
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getGCodeExecutionEventsManager().AddListener(new IGCodeExecutionEventListener()
        {
            @Override
            public void GCodeCommandSentToController(GCodeExecutionEvent evt)
            {
                try
                {
                    synchronized (fAddRemoveLogTableLines)
                    {
                        DefaultTableModel model = (DefaultTableModel) jTableGCodeLog.getModel();
                        model.addRow(new Object[]
                        {
                            evt.getCommand().getCommand(), true, false
                        });
                        //jTableGCodeLog.scrollRectToVisible(jTableGCodeLog.getCellRect(jTableGCodeLog.getRowCount(), 0, true));
                    }
                }
                catch (Exception ex)
                {
                }
            }

            @Override
            public void GCodeExecutedSuccessfully(GCodeExecutionEvent evt)
            {
                try
                {
                    synchronized (fAddRemoveLogTableLines)
                    {
                        DefaultTableModel model = (DefaultTableModel) jTableGCodeLog.getModel();
                        if (!evt.getCommand().getCommand().equals(""))
                        {
                            int lastRow = model.getRowCount() - 1;
                            model.setValueAt(true, lastRow, 2);
                        }
                    }
                }
                catch (Exception ex)
                {
                }
            }

            @Override
            public void GCodeExecutedWithError(GCodeExecutionEvent evt)
            {
                try
                {
                    synchronized (fAddRemoveLogTableLines)
                    {
                        DefaultTableModel model = (DefaultTableModel) jTableGCodeLog.getModel();
                        if (!evt.getCommand().equals(""))
                        {
                            int lastRow = model.getRowCount() - 1;
                            model.setValueAt(true, lastRow, 2);
                            model.setValueAt(evt.getCommand().getError(), lastRow, 3);
                        }
                    }
                }
                catch (Exception ex)
                {
                }
            }

            @Override
            public void GCodeCommandHasComment(GCodeExecutionEvent evt)
            {
                System.out.println(evt.getCommand().getCommandComment());
            }
        }
        );
    }

    private void SetMachineControlsEnabled(boolean state)
    {
        // Enable Machine Control Components
        for (Component c : jPanelMachineControl.getComponents())
        {
            c.setEnabled(state);
        }
    }

    private void InitUIThreads()
    {
        fMachineStatusThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (fKeepMachineStatusThreadRunning)
                {
                    fMachineStatusThreadWait.Reset();

                    // Update Active State
                    int activeState = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getActiveState();
                    if (fLastActiveState != activeState)
                    {
                        switch (activeState)
                        {
                            case GRBLActiveStates.IDLE:
                                jLabelActiveState.setForeground(new Color(0, 153, 51));
                                jLabelActiveState.setText("Idle");
                                break;

                            case GRBLActiveStates.RUN:
                                jLabelActiveState.setForeground(Color.blue);
                                jLabelActiveState.setText("Run");
                                break;

                            case GRBLActiveStates.HOLD:
                                jLabelActiveState.setForeground(Color.red);
                                jLabelActiveState.setText("Hold");
                                break;

                            case GRBLActiveStates.ALARM:
                                jLabelActiveState.setForeground(Color.red);
                                jLabelActiveState.setText("Alarm!");
                                break;
                        }

                        jButtonKillAlarm.setVisible(activeState == GRBLActiveStates.ALARM);
                        jButtonResetZero.setEnabled(activeState == GRBLActiveStates.IDLE);
                        jButtonReturnToZero.setEnabled(activeState == GRBLActiveStates.IDLE);
                        jButtonGCodeSend.setEnabled(activeState == GRBLActiveStates.IDLE);
                    }

                    // Update Machine Position
                    jLabelMachineX.setText("X: " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getX());
                    jLabelMachineY.setText("Y: " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY());
                    jLabelMachineZ.setText("Z: " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ());

                    // Update Work position
                    jLabelWorkX.setText("X: " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX());
                    jLabelWorkY.setText("Y: " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY());
                    jLabelWorkZ.setText("Z: " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getZ());

                    // Update remaining rows & rows sent
                    jLabelSentRows.setText(String.valueOf(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getRowsSent()));
                    jLabelRemainingRows.setText(String.valueOf(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getRowsRemaining()));

                    // Time elapsed
                    if (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getGCodeCycleStartedTimestamp() > 0)
                    {
                        long millis = System.currentTimeMillis() - ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getGCodeCycleStartedTimestamp();
                        long second = (millis / 1000) % 60;
                        long minute = (millis / (1000 * 60)) % 60;
                        long hour = (millis / (1000 * 60 * 60)) % 24;

                        String time = String.format("%02d:%02d:%02d", hour, minute, second);
                        jLabelTimeElapsed.setText(time);
                    }

                    fMachineStatusThreadWait.WaitOne(200);
                }
            }
        });
        fMachineStatusThread.setPriority(Thread.MIN_PRIORITY);
        fKeepMachineStatusThreadRunning = true;
        fMachineStatusThreadWait = new ManualResetEvent(false);
        fMachineStatusThread.start();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jPanel2 = new javax.swing.JPanel();
        jLabelWorkX = new javax.swing.JLabel();
        jLabelWorkY = new javax.swing.JLabel();
        jLabelWorkZ = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabelMachineX = new javax.swing.JLabel();
        jLabelMachineY = new javax.swing.JLabel();
        jLabelMachineZ = new javax.swing.JLabel();
        jButtonResetZero = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabelActiveState = new javax.swing.JLabel();
        jButtonKillAlarm = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabelConnectionStatus = new javax.swing.JLabel();
        jButtonConnectDisconnect = new javax.swing.JButton();
        jButtonSoftReset = new javax.swing.JButton();
        jPanelMachineControl = new javax.swing.JPanel();
        jRadioButtonInches = new javax.swing.JRadioButton();
        jRadioButtonMillimeters = new javax.swing.JRadioButton();
        jLabel4 = new javax.swing.JLabel();
        jSpinnerStep = new javax.swing.JSpinner();
        jButtonXPlus = new javax.swing.JButton();
        jButtonYMinus = new javax.swing.JButton();
        jButtonYPlus = new javax.swing.JButton();
        jButtonXMinus = new javax.swing.JButton();
        jButtonZMinus = new javax.swing.JButton();
        jButtonZPlus = new javax.swing.JButton();
        jButtonReturnToZero = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldGCodeFile = new javax.swing.JTextField();
        jButtonGCodeBrowse = new javax.swing.JButton();
        jLabelRowsInFile = new javax.swing.JLabel();
        jLabelRowsInFile1 = new javax.swing.JLabel();
        jLabelRowsInFile2 = new javax.swing.JLabel();
        jLabelRowsInFile3 = new javax.swing.JLabel();
        jLabelSentRows = new javax.swing.JLabel();
        jLabelRemainingRows = new javax.swing.JLabel();
        jButtonGCodeSend = new javax.swing.JButton();
        jButtonGCodePause = new javax.swing.JButton();
        jButtonGCodeCancel = new javax.swing.JButton();
        jButtonGCodeVisualize = new javax.swing.JButton();
        jLabelRowsInFile4 = new javax.swing.JLabel();
        jLabelTimeElapsed = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableGCodeLog = new javax.swing.JTable();
        jButtonClearLog = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SourceRabbit GCODE Sender");
        setResizable(false);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Machine Status"));

        jLabelWorkX.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabelWorkX.setText("X: 0");

        jLabelWorkY.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabelWorkY.setText("Y: 0");

        jLabelWorkZ.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabelWorkZ.setText("Z: 0");

        jLabel3.setText("Machine Position:");

        jLabelMachineX.setText("X: 0");

        jLabelMachineY.setText("Y: 0");

        jLabelMachineZ.setText("Z: 0");

        jButtonResetZero.setText("Reset Zero");
        jButtonResetZero.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonResetZeroActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel2.setText("Work Position:");

        jLabel6.setText("Active State:");

        jLabelActiveState.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabelActiveState.setText("Restarting...");

        jButtonKillAlarm.setText("Kill Alarm");
        jButtonKillAlarm.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonKillAlarmActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonResetZero, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelWorkZ, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabelWorkY, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabelWorkX, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jLabel2)
                                .addGap(55, 55, 55))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabelActiveState, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(10, 10, 10)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(jLabelMachineX, javax.swing.GroupLayout.DEFAULT_SIZE, 55, Short.MAX_VALUE)
                                            .addComponent(jLabelMachineY, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(jLabelMachineZ, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addGap(8, 8, 8))
                            .addComponent(jButtonKillAlarm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jLabelActiveState)
                    .addComponent(jButtonKillAlarm))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabelWorkX)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelWorkY)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelWorkZ))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabelMachineX)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelMachineY)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelMachineZ)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonResetZero, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(158, 158, 158))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Connection"));

        jLabel1.setText("Status:");

        jLabelConnectionStatus.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabelConnectionStatus.setForeground(new java.awt.Color(0, 153, 51));
        jLabelConnectionStatus.setText("Connected");

        jButtonConnectDisconnect.setText("Disconnect");
        jButtonConnectDisconnect.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonConnectDisconnectActionPerformed(evt);
            }
        });

        jButtonSoftReset.setText("Soft Reset");
        jButtonSoftReset.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonSoftResetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(jLabelConnectionStatus))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jButtonConnectDisconnect)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButtonSoftReset)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabelConnectionStatus))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 14, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonConnectDisconnect)
                    .addComponent(jButtonSoftReset))
                .addContainerGap())
        );

        jPanelMachineControl.setBorder(javax.swing.BorderFactory.createTitledBorder("Machine Control"));

        jRadioButtonInches.setText("inches");
        jRadioButtonInches.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jRadioButtonInchesActionPerformed(evt);
            }
        });

        jRadioButtonMillimeters.setSelected(true);
        jRadioButtonMillimeters.setText("millimeters");
        jRadioButtonMillimeters.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jRadioButtonMillimetersActionPerformed(evt);
            }
        });

        jLabel4.setText("Step Size:");

        jSpinnerStep.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(1.0d), Double.valueOf(0.01d), null, Double.valueOf(0.01d)));

        jButtonXPlus.setText("X+");
        jButtonXPlus.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonXPlusActionPerformed(evt);
            }
        });

        jButtonYMinus.setText("Y-");
        jButtonYMinus.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonYMinusActionPerformed(evt);
            }
        });

        jButtonYPlus.setText("Y+");
        jButtonYPlus.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonYPlusActionPerformed(evt);
            }
        });

        jButtonXMinus.setText("X-");
        jButtonXMinus.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonXMinusActionPerformed(evt);
            }
        });

        jButtonZMinus.setText("Z-");
        jButtonZMinus.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonZMinusActionPerformed(evt);
            }
        });

        jButtonZPlus.setText("Z+");
        jButtonZPlus.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonZPlusActionPerformed(evt);
            }
        });

        jButtonReturnToZero.setText("Return to 0");
        jButtonReturnToZero.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonReturnToZeroActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelMachineControlLayout = new javax.swing.GroupLayout(jPanelMachineControl);
        jPanelMachineControl.setLayout(jPanelMachineControlLayout);
        jPanelMachineControlLayout.setHorizontalGroup(
            jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMachineControlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonReturnToZero, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelMachineControlLayout.createSequentialGroup()
                        .addGroup(jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelMachineControlLayout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jSpinnerStep))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelMachineControlLayout.createSequentialGroup()
                                .addComponent(jRadioButtonInches)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jRadioButtonMillimeters)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanelMachineControlLayout.createSequentialGroup()
                        .addComponent(jButtonXMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonYPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonYMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonXPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonZMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonZPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        jPanelMachineControlLayout.setVerticalGroup(
            jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMachineControlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jSpinnerStep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButtonInches)
                    .addComponent(jRadioButtonMillimeters))
                .addGroup(jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelMachineControlLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanelMachineControlLayout.createSequentialGroup()
                                    .addGap(22, 22, 22)
                                    .addComponent(jButtonXPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanelMachineControlLayout.createSequentialGroup()
                                    .addComponent(jButtonYPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jButtonYMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanelMachineControlLayout.createSequentialGroup()
                                .addComponent(jButtonZPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonZMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanelMachineControlLayout.createSequentialGroup()
                        .addGap(31, 31, 31)
                        .addComponent(jButtonXMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(jButtonReturnToZero, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("GCode"));

        jLabel5.setText("File:");

        jButtonGCodeBrowse.setText("Browse");
        jButtonGCodeBrowse.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonGCodeBrowseActionPerformed(evt);
            }
        });

        jLabelRowsInFile.setText("0");

        jLabelRowsInFile1.setText("Sent Rows:");

        jLabelRowsInFile2.setText("Remaining Rows:");

        jLabelRowsInFile3.setText("Rows in file:");

        jLabelSentRows.setText("0");

        jLabelRemainingRows.setText("0");

        jButtonGCodeSend.setText("Send");
        jButtonGCodeSend.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonGCodeSendActionPerformed(evt);
            }
        });

        jButtonGCodePause.setText("Pause");
        jButtonGCodePause.setEnabled(false);
        jButtonGCodePause.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonGCodePauseActionPerformed(evt);
            }
        });

        jButtonGCodeCancel.setText("Cancel");
        jButtonGCodeCancel.setEnabled(false);
        jButtonGCodeCancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonGCodeCancelActionPerformed(evt);
            }
        });

        jButtonGCodeVisualize.setText("Visualize");
        jButtonGCodeVisualize.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonGCodeVisualizeActionPerformed(evt);
            }
        });

        jLabelRowsInFile4.setText("Time elapsed:");

        jLabelTimeElapsed.setText("00:00:00");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldGCodeFile)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jButtonGCodeSend, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButtonGCodePause, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButtonGCodeCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonGCodeVisualize)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGCodeBrowse))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel5)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabelRowsInFile3)
                                        .addGap(42, 42, 42)
                                        .addComponent(jLabelRowsInFile, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(2, 2, 2)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabelRowsInFile4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabelTimeElapsed, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabelRowsInFile2)
                                    .addComponent(jLabelRowsInFile1))
                                .addGap(18, 18, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabelSentRows, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabelRemainingRows, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(169, 169, 169)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldGCodeFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonGCodeBrowse)
                    .addComponent(jButtonGCodeSend)
                    .addComponent(jButtonGCodePause)
                    .addComponent(jButtonGCodeCancel)
                    .addComponent(jButtonGCodeVisualize))
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelRowsInFile)
                    .addComponent(jLabelRowsInFile3)
                    .addComponent(jLabelRowsInFile4)
                    .addComponent(jLabelTimeElapsed))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelSentRows)
                    .addComponent(jLabelRowsInFile1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelRowsInFile2)
                    .addComponent(jLabelRemainingRows))
                .addContainerGap(22, Short.MAX_VALUE))
        );

        jTableGCodeLog.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {

            },
            new String []
            {
                "Command", "TX", "RX", "Error"
            }
        )
        {
            Class[] types = new Class []
            {
                java.lang.Object.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean []
            {
                true, false, false, false
            };

            public Class getColumnClass(int columnIndex)
            {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTableGCodeLog);
        if (jTableGCodeLog.getColumnModel().getColumnCount() > 0)
        {
            jTableGCodeLog.getColumnModel().getColumn(1).setMinWidth(50);
            jTableGCodeLog.getColumnModel().getColumn(1).setPreferredWidth(50);
            jTableGCodeLog.getColumnModel().getColumn(1).setMaxWidth(50);
            jTableGCodeLog.getColumnModel().getColumn(2).setMinWidth(50);
            jTableGCodeLog.getColumnModel().getColumn(2).setPreferredWidth(50);
            jTableGCodeLog.getColumnModel().getColumn(2).setMaxWidth(50);
        }

        jButtonClearLog.setText("Clear Log");
        jButtonClearLog.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonClearLogActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 472, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jButtonClearLog)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonClearLog)
                .addContainerGap())
        );

        jTabbedPane1.addTab("GCode Commands Log", jPanel4);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelMachineControl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTabbedPane1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanelMachineControl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTabbedPane1)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getAccessibleContext().setAccessibleName("frmControl");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonConnectDisconnectActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonConnectDisconnectActionPerformed
    {//GEN-HEADEREND:event_jButtonConnectDisconnectActionPerformed
        if (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.isConnectionEstablished())
        {
            try
            {
                ConnectionHelper.ACTIVE_CONNECTION_HANDLER.CloseConnection();
            }
            catch (Exception ex)
            {
            }
        }
        else
        {
            try
            {
                jButtonConnectDisconnect.setText("Connecting...");
                jButtonConnectDisconnect.setEnabled(false);
                ConnectionHelper.ACTIVE_CONNECTION_HANDLER.OpenConnection(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getSerialPortName(), ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getBaud());
            }
            catch (Exception ex)
            {
            }
        }
    }//GEN-LAST:event_jButtonConnectDisconnectActionPerformed

    private void jButtonYPlusActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonYPlusActionPerformed
    {//GEN-HEADEREND:event_jButtonYPlusActionPerformed
        double stepValue = (double) jSpinnerStep.getValue();
        try
        {
            GCodeCommand command = new GCodeCommand(fInchesOrMillimetersGCode + "G91G0Y" + stepValue);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
        }
        catch (Exception ex)
        {
        }
    }//GEN-LAST:event_jButtonYPlusActionPerformed

    private void jButtonYMinusActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonYMinusActionPerformed
    {//GEN-HEADEREND:event_jButtonYMinusActionPerformed
        double stepValue = (double) jSpinnerStep.getValue();
        try
        {
            GCodeCommand command = new GCodeCommand(fInchesOrMillimetersGCode + "G91G0Y-" + stepValue);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
        }
        catch (Exception ex)
        {
        }
    }//GEN-LAST:event_jButtonYMinusActionPerformed

    private void jButtonXPlusActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonXPlusActionPerformed
    {//GEN-HEADEREND:event_jButtonXPlusActionPerformed
        double stepValue = (double) jSpinnerStep.getValue();
        try
        {
            GCodeCommand command = new GCodeCommand(fInchesOrMillimetersGCode + "G91G0X" + stepValue);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
        }
        catch (Exception ex)
        {
        }
    }//GEN-LAST:event_jButtonXPlusActionPerformed

    private void jButtonXMinusActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonXMinusActionPerformed
    {//GEN-HEADEREND:event_jButtonXMinusActionPerformed
        double stepValue = (double) jSpinnerStep.getValue();
        try
        {
            GCodeCommand command = new GCodeCommand(fInchesOrMillimetersGCode + "G91G0X-" + stepValue);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
        }
        catch (Exception ex)
        {
        }
    }//GEN-LAST:event_jButtonXMinusActionPerformed

    private void jRadioButtonInchesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jRadioButtonInchesActionPerformed
    {//GEN-HEADEREND:event_jRadioButtonInchesActionPerformed
        // Inches Selected!
        jRadioButtonMillimeters.setSelected(false);
        fInchesOrMillimetersGCode = "G20";
    }//GEN-LAST:event_jRadioButtonInchesActionPerformed

    private void jRadioButtonMillimetersActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jRadioButtonMillimetersActionPerformed
    {//GEN-HEADEREND:event_jRadioButtonMillimetersActionPerformed
        // Millimeters Selected!
        jRadioButtonInches.setSelected(false);
        fInchesOrMillimetersGCode = "G21";
    }//GEN-LAST:event_jRadioButtonMillimetersActionPerformed

    private void jButtonZMinusActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonZMinusActionPerformed
    {//GEN-HEADEREND:event_jButtonZMinusActionPerformed
        double stepValue = (double) jSpinnerStep.getValue();
        try
        {
            GCodeCommand command = new GCodeCommand(fInchesOrMillimetersGCode + "G91G0Z-" + stepValue);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
        }
        catch (Exception ex)
        {
        }
    }//GEN-LAST:event_jButtonZMinusActionPerformed

    private void jButtonZPlusActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonZPlusActionPerformed
    {//GEN-HEADEREND:event_jButtonZPlusActionPerformed
        double stepValue = (double) jSpinnerStep.getValue();
        try
        {
            GCodeCommand command = new GCodeCommand(fInchesOrMillimetersGCode + "G91G0Z" + stepValue);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
        }
        catch (Exception ex)
        {
        }
    }//GEN-LAST:event_jButtonZPlusActionPerformed

    private void jButtonGCodeBrowseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonGCodeBrowseActionPerformed
    {//GEN-HEADEREND:event_jButtonGCodeBrowseActionPerformed
        String path = SettingsManager.getLastGCodeBrowsedDirectory();
        JFileChooser fc;
        try
        {
            fc = new JFileChooser(new File(path));
        }
        catch (Exception ex)
        {
            fc = new JFileChooser();
        }
        fc.showOpenDialog(this);

        File gcodeFile = fc.getSelectedFile();
        String gcodeFilePath = fc.getSelectedFile().getPath();
        jTextFieldGCodeFile.setText(gcodeFilePath);

        SettingsManager.setLastGCodeBrowsedDirectory(gcodeFile.getParent());

        // Ask the GCodeSender of the active connection handler to load the GCode File
        if (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().LoadGCodeFile(gcodeFile))
        {
            jLabelRowsInFile.setText(String.valueOf(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getRowsInFile()));
        }

    }//GEN-LAST:event_jButtonGCodeBrowseActionPerformed

    private void jButtonGCodeSendActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonGCodeSendActionPerformed
    {//GEN-HEADEREND:event_jButtonGCodeSendActionPerformed
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().StartSendingGCode();
    }//GEN-LAST:event_jButtonGCodeSendActionPerformed

    private void jButtonGCodePauseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonGCodePauseActionPerformed
    {//GEN-HEADEREND:event_jButtonGCodePauseActionPerformed
        if (jButtonGCodePause.getText().equals("Pause"))
        {
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().PauseSendingGCode();
        }
        else
        {
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().ResumeSendingGCode();
        }
    }//GEN-LAST:event_jButtonGCodePauseActionPerformed

    private void jButtonGCodeCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonGCodeCancelActionPerformed
    {//GEN-HEADEREND:event_jButtonGCodeCancelActionPerformed
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().CancelSendingGCode();
    }//GEN-LAST:event_jButtonGCodeCancelActionPerformed

    private void jButtonReturnToZeroActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonReturnToZeroActionPerformed
    {//GEN-HEADEREND:event_jButtonReturnToZeroActionPerformed
        try
        {
            final Position4D machinePos = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition();
            if (machinePos.getX() != 0 || machinePos.getY() != 0 || machinePos.getZ() != 0)
            {
                if (machinePos.getZ() < 2)
                {
                    final GCodeCommand command1 = new GCodeCommand("G21 G90 G0 Z2");
                    ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command1);
                }

                final GCodeCommand command2 = new GCodeCommand("G21 G90 G28 X0 Y0");
                ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command2);

                final GCodeCommand command3 = new GCodeCommand("G21 G90 G0 Z0");
                ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command3);
            }
        }
        catch (Exception ex)
        {
        }
    }//GEN-LAST:event_jButtonReturnToZeroActionPerformed

    private void jButtonResetZeroActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonResetZeroActionPerformed
    {//GEN-HEADEREND:event_jButtonResetZeroActionPerformed
        try
        {
            final GCodeCommand command = new GCodeCommand(GRBLCommands.GCODE_RESET_COORDINATES_TO_ZERO);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
        }
        catch (Exception ex)
        {
        }
    }//GEN-LAST:event_jButtonResetZeroActionPerformed

    private void jButtonSoftResetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonSoftResetActionPerformed
    {//GEN-HEADEREND:event_jButtonSoftResetActionPerformed
        try
        {
            jLabelActiveState.setText("Restarting...");
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendDataImmediately_WithoutMessageCollector(GRBLCommands.COMMAND_SOFT_RESET);
        }
        catch (Exception ex)
        {
        }
    }//GEN-LAST:event_jButtonSoftResetActionPerformed

    private void jButtonKillAlarmActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonKillAlarmActionPerformed
    {//GEN-HEADEREND:event_jButtonKillAlarmActionPerformed
        try
        {
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendData(GRBLCommands.COMMAND_KILL_ALARM_LOCK);
        }
        catch (Exception ex)
        {
        }
    }//GEN-LAST:event_jButtonKillAlarmActionPerformed

    private void jButtonGCodeVisualizeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonGCodeVisualizeActionPerformed
    {//GEN-HEADEREND:event_jButtonGCodeVisualizeActionPerformed
        try
        {
            final Chart2D chart = new Chart2D();
            // Create an ITrace: 
            ITrace2D trace = new Trace2DSimple();
            // Add the trace to the chart. This has to be done before adding points (deadlock prevention): 
            chart.addTrace(trace);

            final Queue<String> gcodeQueue = new ArrayDeque(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getGCodeQueue());

            double x = 0;
            double y = 0;
            while (gcodeQueue.size() > 0)
            {
                final GCodeCommand command = new GCodeCommand(gcodeQueue.remove());
                if (command.getCoordinates().getX() != null || command.getCoordinates().getY() != null)
                {
                    if (command.getCoordinates().getX() != null)
                    {
                        x = command.getCoordinates().getX();
                    }

                    if (command.getCoordinates().getY() != null)
                    {
                        y = command.getCoordinates().getY();
                    }
                    trace.addPoint(x, y);
                }
            }

            // Make it visible:
            // Create a frame.
            final JFrame frame = new JFrame("GCode Visualizer");
            // add the chart to the frame: 
            frame.getContentPane().add(chart);
            frame.setSize(800, 600);
            frame.setVisible(true);
        }
        catch (Exception ex)
        {

        }
    }//GEN-LAST:event_jButtonGCodeVisualizeActionPerformed

    private void jButtonClearLogActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonClearLogActionPerformed
    {//GEN-HEADEREND:event_jButtonClearLogActionPerformed
        synchronized (fAddRemoveLogTableLines)
        {
            DefaultTableModel model = (DefaultTableModel) jTableGCodeLog.getModel();
            int rowCount = model.getRowCount();
            for (int i = rowCount - 1; i >= 0; i--)
            {
                model.removeRow(i);
            }
        }
    }//GEN-LAST:event_jButtonClearLogActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClearLog;
    private javax.swing.JButton jButtonConnectDisconnect;
    private javax.swing.JButton jButtonGCodeBrowse;
    private javax.swing.JButton jButtonGCodeCancel;
    private javax.swing.JButton jButtonGCodePause;
    private javax.swing.JButton jButtonGCodeSend;
    private javax.swing.JButton jButtonGCodeVisualize;
    private javax.swing.JButton jButtonKillAlarm;
    private javax.swing.JButton jButtonResetZero;
    private javax.swing.JButton jButtonReturnToZero;
    private javax.swing.JButton jButtonSoftReset;
    private javax.swing.JButton jButtonXMinus;
    private javax.swing.JButton jButtonXPlus;
    private javax.swing.JButton jButtonYMinus;
    private javax.swing.JButton jButtonYPlus;
    private javax.swing.JButton jButtonZMinus;
    private javax.swing.JButton jButtonZPlus;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabelActiveState;
    private javax.swing.JLabel jLabelConnectionStatus;
    private javax.swing.JLabel jLabelMachineX;
    private javax.swing.JLabel jLabelMachineY;
    private javax.swing.JLabel jLabelMachineZ;
    private javax.swing.JLabel jLabelRemainingRows;
    private javax.swing.JLabel jLabelRowsInFile;
    private javax.swing.JLabel jLabelRowsInFile1;
    private javax.swing.JLabel jLabelRowsInFile2;
    private javax.swing.JLabel jLabelRowsInFile3;
    private javax.swing.JLabel jLabelRowsInFile4;
    private javax.swing.JLabel jLabelSentRows;
    private javax.swing.JLabel jLabelTimeElapsed;
    private javax.swing.JLabel jLabelWorkX;
    private javax.swing.JLabel jLabelWorkY;
    private javax.swing.JLabel jLabelWorkZ;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanelMachineControl;
    private javax.swing.JRadioButton jRadioButtonInches;
    private javax.swing.JRadioButton jRadioButtonMillimeters;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSpinner jSpinnerStep;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTableGCodeLog;
    private javax.swing.JTextField jTextFieldGCodeFile;
    // End of variables declaration//GEN-END:variables
}
