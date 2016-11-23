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
import info.monitorenter.gui.chart.rangepolicies.RangePolicyFixedViewport;
import info.monitorenter.gui.chart.traces.Trace2DSimple;
import info.monitorenter.util.Range;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Queue;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeExecutionEvents.GCodeExecutionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeExecutionEvents.IGCodeExecutionEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.SerialConnectionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.ISerialConnectionEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeCycleEvents.GCodeCycleEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeCycleEvents.IGCodeCycleListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.IMachineStatusEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.MachineStatusEvents.MachineStatusEvent;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLActiveStates;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLCommands;
import sourcerabbit.gcode.sender.Core.Threading.ManualResetEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Position.Position2D;
import sourcerabbit.gcode.sender.Core.CNCController.Position.Position4D;
import sourcerabbit.gcode.sender.Core.Settings.SettingsManager;
import sourcerabbit.gcode.sender.UI.Tools.frmEdgeFinder;
import sourcerabbit.gcode.sender.UI.Tools.frmHoleCenterFinder;
import sourcerabbit.gcode.sender.UI.Tools.frmSetWorkPosition;
import sourcerabbit.gcode.sender.UI.Tools.frmZAxisTouchProbe;
import sourcerabbit.gcode.sender.UI.UITools.UITools;

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

    private final Calendar fCalendar = Calendar.getInstance();
    private final DateFormat fDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    // Macros
    private ArrayList<JTextField> fMacroTexts = new ArrayList<>();
    private ArrayList<JButton> fMacroButtons = new ArrayList<>();

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
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("Images/SourceRabbitIcon.png")));

        this.jCheckBoxEnableGCodeLog.setSelected(SettingsManager.getIsGCodeLogEnabled());

        InitMacroButtons();

        // Fix jSpinnerStep to work with system decimal point
        jSpinnerStep.setEditor(new JSpinner.NumberEditor(jSpinnerStep, "##.##"));
        UITools.FixSpinnerToWorkWithSystemDecimalPoint(jSpinnerStep);
    }

    private void InitMacroButtons()
    {
        ArrayList<String> savedMacros = SettingsManager.getMacros();

        int topOffset = 50;
        for (int i = 0; i < 7; i++)
        {
            final JButton button = new JButton();
            int id = i + 1;
            button.setText("C" + String.valueOf(id));
            button.setSize(50, 30);
            button.setLocation(10, topOffset + (i * 35));
            fMacroButtons.add(button);

            final JTextField textField = new JTextField();
            textField.setText(savedMacros.get(i));
            textField.setSize(300, 30);
            textField.setLocation(80, topOffset + (i * 35));
            fMacroTexts.add(textField);

            textField.addKeyListener(new KeyAdapter()
            {
                @Override
                public void keyReleased(KeyEvent e)
                {
                    try
                    {
                        ArrayList<String> macroCommands = new ArrayList<>();
                        for (JTextField text : fMacroTexts)
                        {
                            macroCommands.add(text.getText());
                        }
                        SettingsManager.setMacros(macroCommands);
                    }
                    catch (Exception ex)
                    {

                    }
                }
            });

            button.addActionListener(new java.awt.event.ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    final String commandStr = textField.getText().replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "");

                    // Get commands
                    String commands[] = commandStr.split(";");
                    for (String commandString : commands)
                    {
                        GCodeCommand command = new GCodeCommand(commandString);
                        String response = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(command);
                        WriteToConsole(commandString + "\nResponse:" + response + "\n");
                    }
                }
            });

            // Add button and textfield
            jPanelMacros.add(button);
            jPanelMacros.add(textField);
        }
    }

    private void WriteToConsole(String output)
    {
        String dateTime = (fDateFormat.format(fCalendar.getTime()));
        jTextAreaConsole.append(dateTime + " - " + output + "\n");
        jTextAreaConsole.setCaretPosition(jTextAreaConsole.getDocument().getLength());
    }

    private void InitEvents()
    {
        // Machine status events
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachineStatusEventsManager().AddListener(new IMachineStatusEventListener()
        {
            @Override
            public void MachineStatusChanged(MachineStatusEvent evt)
            {
                final int activeState = evt.getMachineStatus();
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

                    case GRBLActiveStates.RESET_TO_CONTINUE:
                        jLabelActiveState.setForeground(Color.red);
                        jLabelActiveState.setText("Reset to continue!");
                        break;
                }

                jButtonKillAlarm.setVisible(activeState == GRBLActiveStates.ALARM);
                jButtonResetZero.setEnabled(activeState == GRBLActiveStates.IDLE);
                jButtonReturnToZero.setEnabled(activeState == GRBLActiveStates.IDLE);
                jButtonGCodeSend.setEnabled(activeState == GRBLActiveStates.IDLE);
            }
        });

        // Serial Connection Events
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getSerialConnectionEventManager().AddListener(new ISerialConnectionEventListener()
        {
            @Override
            public void ConnectionEstablished(SerialConnectionEvent evt)
            {
                WriteToConsole("Connection Established!");
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

                jMenuItemGRBLSettings.setEnabled(true);
            }

            @Override
            public void ConnectionClosed(SerialConnectionEvent evt)
            {
                WriteToConsole("Connection Closed!");
                ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().CancelSendingGCode();

                jLabelConnectionStatus.setForeground(Color.red);
                jLabelConnectionStatus.setText("Disconnected");
                jButtonConnectDisconnect.setText("Connect");
                jButtonConnectDisconnect.setEnabled(true);
                jButtonSoftReset.setEnabled(false);
                jButtonResetZero.setEnabled(false);

                jLabelActiveState.setForeground(Color.red);
                jLabelActiveState.setText("----");

                // Disable Machine Control Components
                SetMachineControlsEnabled(false);

                jMenuItemGRBLSettings.setEnabled(false);
            }

            @Override
            public void DataReceivedFromSerialConnection(SerialConnectionEvent evt)
            {
                WriteToConsole((String) evt.getSource());
            }
        });

        // Gcode Cycle Events
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getCycleEventManager().AddListener(new IGCodeCycleListener()
        {
            @Override
            public void GCodeCycleStarted(GCodeCycleEvent evt)
            {
                WriteToConsole("Cycle Started!");

                jButtonGCodeBrowse.setEnabled(false);
                jButtonGCodeSend.setEnabled(false);
                jButtonGCodePause.setEnabled(true);
                jButtonGCodeCancel.setEnabled(true);
                jButtonGCodePause.setText("Pause");
                SetMachineControlsEnabled(false);
                jButtonResetZero.setEnabled(false);
                jButtonSoftReset.setEnabled(false);

                jTextFieldGCodeFile.setEnabled(false);

                jTextFieldCommand.setEnabled(false);
                jTextAreaConsole.setEnabled(false);

                jMenuItemGRBLSettings.setEnabled(false);

                jProgressBarGCodeProgress.setValue(0);
                jProgressBarGCodeProgress.setMaximum(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getRowsInFile());
            }

            @Override
            public void GCodeCycleFinished(GCodeCycleEvent evt)
            {
                WriteToConsole("Cycle Finished!");

                jButtonGCodeBrowse.setEnabled(true);
                jButtonGCodeSend.setEnabled(true);
                jButtonGCodePause.setEnabled(false);
                jButtonGCodeCancel.setEnabled(false);
                jButtonGCodePause.setText("Pause");
                SetMachineControlsEnabled(true);
                jButtonResetZero.setEnabled(true);
                jButtonSoftReset.setEnabled(true);

                jTextFieldGCodeFile.setEnabled(true);

                jTextFieldCommand.setEnabled(true);
                jTextAreaConsole.setEnabled(true);

                jMenuItemGRBLSettings.setEnabled(true);

                JOptionPane.showMessageDialog(fInstance, evt.getSource().toString(), "Finished", JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            public void GCodeCycleCanceled(GCodeCycleEvent evt)
            {
                WriteToConsole("Cycle Canceled");
                jButtonGCodeBrowse.setEnabled(true);
                jButtonGCodeSend.setEnabled(true);
                jButtonGCodePause.setEnabled(false);
                jButtonGCodeCancel.setEnabled(false);
                jButtonGCodePause.setText("Pause");
                SetMachineControlsEnabled(true);
                jButtonResetZero.setEnabled(true);
                jButtonSoftReset.setEnabled(true);

                jTextFieldGCodeFile.setEnabled(true);

                jTextFieldCommand.setEnabled(true);
                jTextAreaConsole.setEnabled(true);

                jMenuItemGRBLSettings.setEnabled(true);

                jProgressBarGCodeProgress.setValue(0);
                jProgressBarGCodeProgress.setMaximum(0);
            }

            @Override
            public void GCodeCyclePaused(GCodeCycleEvent evt)
            {
                WriteToConsole("Cycle Paused");
                jLabelActiveState.setText("Hold");
                jLabelActiveState.setForeground(Color.red);

                jButtonGCodePause.setText("Resume");

                jButtonGCodeCancel.setEnabled(false);
            }

            @Override
            public void GCodeCycleResumed(GCodeCycleEvent evt)
            {
                WriteToConsole("Cycle Resumed");

                jLabelActiveState.setText("Run");
                jLabelActiveState.setForeground(Color.blue);

                jButtonGCodePause.setText("Pause");

                jButtonGCodeCancel.setEnabled(true);
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
                    if (jCheckBoxEnableGCodeLog.isSelected())
                    {
                        synchronized (fAddRemoveLogTableLines)
                        {
                            DefaultTableModel model = (DefaultTableModel) jTableGCodeLog.getModel();
                            model.addRow(new Object[]
                            {
                                evt.getCommand().getCommand(), true, false
                            });
                        }
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
                    if (jCheckBoxEnableGCodeLog.isSelected())
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

        // Macros
        try
        {
            for (JButton b : fMacroButtons)
            {
                b.setEnabled(state);
            }
        }
        catch (Exception ex)
        {

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

                    // Update Machine Position
                    jLabelMachineX.setText("X: " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getX());
                    jLabelMachineY.setText("Y: " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getY());
                    jLabelMachineZ.setText("Z: " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMachinePosition().getZ());

                    // Update Work position
                    jLabelWorkX.setText("X: " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getX());
                    jLabelWorkY.setText("Y: " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getY());
                    jLabelWorkZ.setText("Z: " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getWorkPosition().getZ());

                    // Update bytes per second
                    jLabelBytesPerSecond.setText(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getBytesInPerSec() + " / " + ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getBytesOutPerSec());

                    // Update remaining rows & rows sent
                    jLabelSentRows.setText(String.valueOf(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getRowsSent()));
                    jLabelRemainingRows.setText(String.valueOf(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getRowsRemaining()));

                    jProgressBarGCodeProgress.setValue(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getRowsSent());

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
        jLabelRowsInFile5 = new javax.swing.JLabel();
        jProgressBarGCodeProgress = new javax.swing.JProgressBar();
        jLabelRowsInFile6 = new javax.swing.JLabel();
        jLabelBytesPerSecond = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel5 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jTextFieldCommand = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaConsole = new javax.swing.JTextArea();
        jLabel8 = new javax.swing.JLabel();
        jButtonClearConsole = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableGCodeLog = new javax.swing.JTable();
        jButtonClearLog = new javax.swing.JButton();
        jCheckBoxEnableGCodeLog = new javax.swing.JCheckBox();
        jPanelMacros = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItemGRBLSettings = new javax.swing.JMenuItem();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItemHoleCenterFinder = new javax.swing.JMenuItem();
        jMenuItemEdgeFinder = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SourceRabbit GCODE Sender");
        setResizable(false);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Machine Status", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11), new java.awt.Color(0, 75, 127))); // NOI18N

        jLabelWorkX.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabelWorkX.setForeground(new java.awt.Color(0, 75, 127));
        jLabelWorkX.setText("X: 0");

        jLabelWorkY.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabelWorkY.setForeground(new java.awt.Color(0, 75, 127));
        jLabelWorkY.setText("Y: 0");

        jLabelWorkZ.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabelWorkZ.setForeground(new java.awt.Color(0, 75, 127));
        jLabelWorkZ.setText("Z: 0");

        jLabel3.setText("Machine Position:");

        jLabelMachineX.setText("X: 0");

        jLabelMachineY.setText("Y: 0");

        jLabelMachineZ.setText("Z: 0");

        jButtonResetZero.setText("Reset Zero");
        jButtonResetZero.setToolTipText("Reset the Work Position to 0,0,0");
        jButtonResetZero.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonResetZeroActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(0, 75, 127));
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
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelWorkX)
                    .addComponent(jLabelMachineX))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelWorkY)
                    .addComponent(jLabelMachineY))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelWorkZ)
                    .addComponent(jLabelMachineZ))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonResetZero, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(158, 158, 158))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Connection", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11), new java.awt.Color(0, 75, 127))); // NOI18N

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

        jPanelMachineControl.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Machine Control", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11), new java.awt.Color(0, 75, 127))); // NOI18N

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

        jSpinnerStep.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.009999999776482582d, null, 0.009999999776482582d));
        jSpinnerStep.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

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
        jButtonReturnToZero.setToolTipText("Return to initial Work Position (0,0,0)");
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
                        .addComponent(jButtonXMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonYPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonYMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonXPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanelMachineControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonZMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonZPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))))
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

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "GCode File", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11), new java.awt.Color(0, 75, 127))); // NOI18N

        jLabel5.setText("File:");

        jTextFieldGCodeFile.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jTextFieldGCodeFileActionPerformed(evt);
            }
        });

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

        jLabelRowsInFile5.setText("Progress:");

        jLabelRowsInFile6.setText("Bytes In/Out (sec):");

        jLabelBytesPerSecond.setText("0/0");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldGCodeFile)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jButtonGCodeSend, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGCodePause, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGCodeCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonGCodeVisualize)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGCodeBrowse))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel5)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabelRowsInFile3)
                                .addGap(34, 34, 34)
                                .addComponent(jLabelRowsInFile, javax.swing.GroupLayout.DEFAULT_SIZE, 83, Short.MAX_VALUE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabelRowsInFile2)
                                    .addComponent(jLabelRowsInFile1))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabelSentRows, javax.swing.GroupLayout.DEFAULT_SIZE, 83, Short.MAX_VALUE)
                                    .addComponent(jLabelRemainingRows, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabelRowsInFile6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabelBytesPerSecond, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabelRowsInFile4)
                                    .addComponent(jLabelRowsInFile5))
                                .addGap(33, 33, 33)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jProgressBarGCodeProgress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelTimeElapsed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
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
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabelSentRows)
                        .addComponent(jLabelRowsInFile5))
                    .addComponent(jLabelRowsInFile1)
                    .addComponent(jProgressBarGCodeProgress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelRowsInFile6)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabelRowsInFile2)
                        .addComponent(jLabelRemainingRows)
                        .addComponent(jLabelBytesPerSecond)))
                .addContainerGap(22, Short.MAX_VALUE))
        );

        jTabbedPane1.setForeground(new java.awt.Color(0, 75, 127));
        jTabbedPane1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N

        jLabel7.setText("Command:");

        jTextFieldCommand.setToolTipText("Send a command to the controller");
        jTextFieldCommand.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jTextFieldCommandActionPerformed(evt);
            }
        });

        jTextAreaConsole.setColumns(20);
        jTextAreaConsole.setFont(new java.awt.Font("Tahoma", 0, 11)); // NOI18N
        jTextAreaConsole.setRows(5);
        jScrollPane2.setViewportView(jTextAreaConsole);

        jLabel8.setText("Output:");

        jButtonClearConsole.setText("Clear Console");
        jButtonClearConsole.setToolTipText("Clear the GCode Log");
        jButtonClearConsole.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonClearConsoleActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldCommand)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 472, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel8)
                            .addComponent(jButtonClearConsole))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonClearConsole)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Console", jPanel5);

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
        jButtonClearLog.setToolTipText("Clear the GCode Log");
        jButtonClearLog.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonClearLogActionPerformed(evt);
            }
        });

        jCheckBoxEnableGCodeLog.setText("Enable GCode Log");
        jCheckBoxEnableGCodeLog.setToolTipText("You may uncheck it on slower computers");
        jCheckBoxEnableGCodeLog.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jCheckBoxEnableGCodeLogActionPerformed(evt);
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBoxEnableGCodeLog)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonClearLog)
                    .addComponent(jCheckBoxEnableGCodeLog))
                .addContainerGap())
        );

        jTabbedPane1.addTab("GCode Log", jPanel4);

        jLabel9.setText("Each box can contain a series of GCode commands separated by ';'.");

        jLabel10.setText("To execute just click the 'C' button.");

        javax.swing.GroupLayout jPanelMacrosLayout = new javax.swing.GroupLayout(jPanelMacros);
        jPanelMacros.setLayout(jPanelMacrosLayout);
        jPanelMacrosLayout.setHorizontalGroup(
            jPanelMacrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMacrosLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelMacrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10))
                .addContainerGap(157, Short.MAX_VALUE))
        );
        jPanelMacrosLayout.setVerticalGroup(
            jPanelMacrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMacrosLayout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(jLabel9)
                .addGap(3, 3, 3)
                .addComponent(jLabel10)
                .addContainerGap(275, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Macros", jPanelMacros);

        jMenu1.setText("System");

        jMenuItemGRBLSettings.setText("GRBL Settings");
        jMenuItemGRBLSettings.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItemGRBLSettingsActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemGRBLSettings);

        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemExit);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Tools");

        jMenuItem1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sourcerabbit/gcode/sender/UI/Images/WorkArea-16x16.png"))); // NOI18N
        jMenuItem1.setText("Set Work Position");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem1);

        jMenuItem2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sourcerabbit/gcode/sender/UI/Images/ZTouchProbe/ZAxisTouchProbe-16x16.png"))); // NOI18N
        jMenuItem2.setText("Z Axis Touch Probe");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem2);

        jMenuItemHoleCenterFinder.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sourcerabbit/gcode/sender/UI/Images/HoleCenterFinder/HoleCenterFinder-16x16.png"))); // NOI18N
        jMenuItemHoleCenterFinder.setText("Hole Center Finder");
        jMenuItemHoleCenterFinder.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItemHoleCenterFinderActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemHoleCenterFinder);

        jMenuItemEdgeFinder.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sourcerabbit/gcode/sender/UI/Images/EdgeFinder/Edge Finder-16x16.png"))); // NOI18N
        jMenuItemEdgeFinder.setText("Edge Finder");
        jMenuItemEdgeFinder.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItemEdgeFinderActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemEdgeFinder);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("Help");

        jMenuItem3.setText("About");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem3);

        jMenuItem4.setText("Check for Update");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem4);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

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
                        .addGap(5, 5, 5)
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
        final String path = SettingsManager.getLastGCodeBrowsedDirectory();
        JFileChooser fc;
        try
        {
            fc = new JFileChooser(new File(path));
            FileNameExtensionFilter filter = new FileNameExtensionFilter("GCode Files (.nc, .gcode, .tap)", "nc", "gcode", "tap");
            fc.setFileFilter(filter);
        }
        catch (Exception ex)
        {
            fc = new JFileChooser();
        }
        int returnVal = fc.showOpenDialog(this);

        if (fc.getSelectedFile() != null && returnVal == JFileChooser.APPROVE_OPTION)
        {
            File gcodeFile = fc.getSelectedFile();
            String gcodeFilePath = fc.getSelectedFile().getPath();
            jTextFieldGCodeFile.setText(gcodeFilePath);

            SettingsManager.setLastGCodeBrowsedDirectory(gcodeFile.getParent());

            // Ask the GCodeSender of the active connection handler to load the GCode File
            if (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().LoadGCodeFile(gcodeFile))
            {
                jLabelRowsInFile.setText(String.valueOf(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getRowsInFile()));
            }
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

                //final GCodeCommand command2 = new GCodeCommand("G21 G90 G28 X0 Y0");
                final GCodeCommand command2 = new GCodeCommand("G21 G90 X0 Y0");
                ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command2);

                final GCodeCommand command3 = new GCodeCommand("G21 G90 G0 Z0");
                ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command3);

                WriteToConsole("Return to zero");
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

            WriteToConsole("Reset work zero");
        }
        catch (Exception ex)
        {
        }
    }//GEN-LAST:event_jButtonResetZeroActionPerformed

    private void jButtonSoftResetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonSoftResetActionPerformed
    {//GEN-HEADEREND:event_jButtonSoftResetActionPerformed
        try
        {
            WriteToConsole("Restarting...");
            jLabelActiveState.setForeground(Color.MAGENTA);
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
            /*frmGCodeViewer frm = new frmGCodeViewer();
            frm.setVisible(true);*/

            final Chart2D chart = new Chart2D();
            // Create an ITrace: 
            ITrace2D trace = new Trace2DSimple();
            // Add the trace to the chart. This has to be done before adding points (deadlock prevention): 
            chart.addTrace(trace);

            final Queue<String> gcodeQueue = new ArrayDeque(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getGCodeQueue());
            double x = 0, y = 0, z = 0, maxX = 0, maxY = 0;

            while (gcodeQueue.size() > 0)
            {
                final GCodeCommand command = new GCodeCommand(gcodeQueue.remove());
                x = (command.getCoordinates().getX() != null) ? command.getCoordinates().getX() : x;
                y = (command.getCoordinates().getY() != null) ? command.getCoordinates().getY() : y;
                z = (command.getCoordinates().getZ() != null) ? command.getCoordinates().getZ() : z;

                if (z < 0)
                {
                    maxX = Math.max(x, maxX);
                    maxY = Math.max(y, maxY);
                    trace.addPoint(x, y);
                }
            }

            chart.getAxisX().setRangePolicy(new RangePolicyFixedViewport(new Range(0, Math.max(maxY, maxX))));
            chart.getAxisY().setRangePolicy(new RangePolicyFixedViewport(new Range(0, Math.max(maxY, maxX))));

            // Make it visible:
            // Create a frame.
            final JFrame frame = new JFrame(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getMyGCodeSender().getGCodeFile().getName());
            // add the chart to the frame: 
            frame.getContentPane().add(chart);
            frame.setSize(600, 600);
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

    private void jCheckBoxEnableGCodeLogActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jCheckBoxEnableGCodeLogActionPerformed
    {//GEN-HEADEREND:event_jCheckBoxEnableGCodeLogActionPerformed
        try
        {
            SettingsManager.setIsGCodeLogEnabled(jCheckBoxEnableGCodeLog.isSelected());
        }
        catch (Exception ex)
        {

        }
    }//GEN-LAST:event_jCheckBoxEnableGCodeLogActionPerformed

    private void jTextFieldCommandActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jTextFieldCommandActionPerformed
    {//GEN-HEADEREND:event_jTextFieldCommandActionPerformed
        try
        {
            final String str = this.jTextFieldCommand.getText().replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "");
            GCodeCommand command = new GCodeCommand(str);
            String response = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(command);

            WriteToConsole(str + "\nResponse:" + response + "\n");

            jTextFieldCommand.setText("");
        }
        catch (Exception ex)
        {
        }
    }//GEN-LAST:event_jTextFieldCommandActionPerformed

    private void jButtonClearConsoleActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonClearConsoleActionPerformed
    {//GEN-HEADEREND:event_jButtonClearConsoleActionPerformed
        jTextAreaConsole.setText("");
    }//GEN-LAST:event_jButtonClearConsoleActionPerformed

    private void jTextFieldGCodeFileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jTextFieldGCodeFileActionPerformed
    {//GEN-HEADEREND:event_jTextFieldGCodeFileActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldGCodeFileActionPerformed

    private void jMenuItemGRBLSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemGRBLSettingsActionPerformed
    {//GEN-HEADEREND:event_jMenuItemGRBLSettingsActionPerformed
        frmGRBLSettings frm = new frmGRBLSettings();
        frm.setModal(true);
        frm.setVisible(true);
    }//GEN-LAST:event_jMenuItemGRBLSettingsActionPerformed

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemExitActionPerformed
    {//GEN-HEADEREND:event_jMenuItemExitActionPerformed
        System.exit(EXIT_ON_CLOSE);
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem1ActionPerformed
    {//GEN-HEADEREND:event_jMenuItem1ActionPerformed
        frmSetWorkPosition frm = new frmSetWorkPosition(this, true);
        frm.setVisible(true);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem2ActionPerformed
    {//GEN-HEADEREND:event_jMenuItem2ActionPerformed
        frmZAxisTouchProbe frm = new frmZAxisTouchProbe(this, true);
        frm.setVisible(true);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem3ActionPerformed
    {//GEN-HEADEREND:event_jMenuItem3ActionPerformed
        frmAbout frm = new frmAbout(this, true);
        frm.setVisible(true);
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem4ActionPerformed
    {//GEN-HEADEREND:event_jMenuItem4ActionPerformed
        frmCheckForUpdate frm = new frmCheckForUpdate(this, true);
        frm.setVisible(true);
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItemHoleCenterFinderActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemHoleCenterFinderActionPerformed
    {//GEN-HEADEREND:event_jMenuItemHoleCenterFinderActionPerformed
        frmHoleCenterFinder frm = new frmHoleCenterFinder(this, true);
        frm.setVisible(true);
    }//GEN-LAST:event_jMenuItemHoleCenterFinderActionPerformed

    private void jMenuItemEdgeFinderActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemEdgeFinderActionPerformed
    {//GEN-HEADEREND:event_jMenuItemEdgeFinderActionPerformed
        frmEdgeFinder frm = new frmEdgeFinder(this, true);
        frm.setVisible(true);
    }//GEN-LAST:event_jMenuItemEdgeFinderActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClearConsole;
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
    private javax.swing.JCheckBox jCheckBoxEnableGCodeLog;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelActiveState;
    private javax.swing.JLabel jLabelBytesPerSecond;
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
    private javax.swing.JLabel jLabelRowsInFile5;
    private javax.swing.JLabel jLabelRowsInFile6;
    private javax.swing.JLabel jLabelSentRows;
    private javax.swing.JLabel jLabelTimeElapsed;
    private javax.swing.JLabel jLabelWorkX;
    private javax.swing.JLabel jLabelWorkY;
    private javax.swing.JLabel jLabelWorkZ;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItemEdgeFinder;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemGRBLSettings;
    private javax.swing.JMenuItem jMenuItemHoleCenterFinder;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanelMachineControl;
    private javax.swing.JPanel jPanelMacros;
    private javax.swing.JProgressBar jProgressBarGCodeProgress;
    private javax.swing.JRadioButton jRadioButtonInches;
    private javax.swing.JRadioButton jRadioButtonMillimeters;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSpinner jSpinnerStep;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTableGCodeLog;
    private javax.swing.JTextArea jTextAreaConsole;
    private javax.swing.JTextField jTextFieldCommand;
    private javax.swing.JTextField jTextFieldGCodeFile;
    // End of variables declaration//GEN-END:variables
}
