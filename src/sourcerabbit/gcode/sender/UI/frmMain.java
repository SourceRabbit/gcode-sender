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

import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import jssc.SerialPortList;
import sourcerabbit.gcode.sender.Core.CNCController.CNCControllFrameworks.CNCControlFramework;
import sourcerabbit.gcode.sender.Core.CNCController.CNCControllFrameworks.CNCControlFrameworkManager;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHandler;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.ISerialConnectionEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.SerialConnectionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Tools.ManualResetEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Tools.Position2D;
import sourcerabbit.gcode.sender.Core.Settings.SettingsManager;
import sourcerabbit.gcode.sender.UI.Tools.UITools;

/**
 *
 * @author Nikos Siatras
 */
public class frmMain extends javax.swing.JFrame
{

    private ManualResetEvent fWaitToEstablishConnectionResetEvent = new ManualResetEvent(false);
    private final frmMain fInstance;

    public frmMain()
    {
        fInstance = this;
        initComponents();

        InitUI();

        // Set form in middle of screen
        Position2D pos = UITools.getPositionForFormToOpenInMiddleOfScreen(this.getSize().width, this.getSize().height);
        this.setLocation((int) pos.getX(), (int) pos.getY());

        this.setTitle("SourceRabbit GCode Sender (Version " + SettingsManager.getAppVersion() + ")");
    }

    private void InitUI()
    {
        String[] serialPorts = SerialPortList.getPortNames();
        ArrayList<CNCControlFramework> cncframeworks = CNCControlFrameworkManager.getCNCControlFrameworks();

        if (serialPorts.length < 1)
        {
        }
        else
        {
            String preselectedSerialPort = SettingsManager.getPreselectedSerialPort();

            // Add serial ports to jComboBoxPort
            int index = 0;
            int selectedIndex = 0;
            for (String port : serialPorts)
            {
                jComboBoxPort.addItem(port);
                if (port.equals(preselectedSerialPort))
                {
                    selectedIndex = index;
                }
                index += 1;
            }
            jComboBoxPort.setSelectedIndex(selectedIndex);

            // Add Baud rates
            jComboBoxBaud.addItem("115200");

            // Add Frameworks
            for (CNCControlFramework framework : cncframeworks)
            {
                jComboBoxFramework.addItem(framework.getName());
            }
            jComboBoxFramework.setSelectedIndex(0);
        }
    }

    /**
     * The SourceRabbit GCode Sender managed to connect successfully to the CNC
     * controller. Now the Control Form must be opened!
     */
    private void OpenControlForm()
    {
        // Remove the fConnectionEstablishedEventListener (We dont need it any more)
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getSerialConnectionEventManager().RemoveListener(fConnectionEstablishedEventListener);

        // Set the selected port as the preselected port for later use
        String port = jComboBoxPort.getSelectedItem().toString();
        SettingsManager.setPreselectedSerialPort(port);

        // Show to control form
        frmControl frm = new frmControl();
        frm.setVisible(true);
        this.setVisible(false);
        this.dispose();
    }

    private ISerialConnectionEventListener fConnectionEstablishedEventListener = new ISerialConnectionEventListener()
    {
        @Override
        public void ConnectionEstablished(SerialConnectionEvent evt)
        {
            fWaitToEstablishConnectionResetEvent.Set();
            OpenControlForm();
        }

        @Override
        public void ConnectionClosed(SerialConnectionEvent evt)
        {

        }

        @Override
        public void DataReceivedFromSerialConnection(SerialConnectionEvent evt)
        {

        }

    };

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jLabel1 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jComboBoxPort = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jComboBoxFramework = new javax.swing.JComboBox();
        jButtonConnect = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jComboBoxBaud = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SourceRabbit GCODE Sender ver 1.0");
        setResizable(false);

        jLabel1.setFont(new java.awt.Font("Arial", 0, 24)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Connect to your CNC");

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sourcerabbit/gcode/sender/UI/Images/SourceRabbit.png"))); // NOI18N

        jLabel2.setText("Port:");

        jLabel3.setText("Framework:");

        jButtonConnect.setText("Connect");
        jButtonConnect.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonConnectActionPerformed(evt);
            }
        });

        jLabel4.setText("Baud:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBoxFramework, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel4))
                        .addGap(39, 39, 39)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBoxBaud, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBoxPort, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jButtonConnect, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jComboBoxPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jComboBoxBaud, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jComboBoxFramework, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonConnect, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(112, 112, 112))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getAccessibleContext().setAccessibleName("frmMain");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonConnectActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonConnectActionPerformed
    {//GEN-HEADEREND:event_jButtonConnectActionPerformed

        if (ConnectionHelper.ACTIVE_CONNECTION_HANDLER != null)
        {
            try
            {
                ConnectionHelper.ACTIVE_CONNECTION_HANDLER.CloseConnection();
            }
            catch (Exception ex)
            {

            }
        }

        String frameworkName = jComboBoxFramework.getSelectedItem().toString();
        String baud = jComboBoxBaud.getSelectedItem().toString();
        String port = jComboBoxPort.getSelectedItem().toString();

        CNCControlFramework framework = CNCControlFrameworkManager.getFrameworkByName(frameworkName);
        final ConnectionHandler handler = framework.getHandler();
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER = handler;

        // Add connection established listener
        handler.getSerialConnectionEventManager().RemoveAllListeners();
        handler.getSerialConnectionEventManager().AddListener(fConnectionEstablishedEventListener);

        try
        {
            jButtonConnect.setText("Connecting...");
            jButtonConnect.setEnabled(false);
            fWaitToEstablishConnectionResetEvent.Reset();
            if (handler.OpenConnection(port, Integer.parseInt(baud)))
            {
                // Wait for 3 seconds to establish connection
                // Otherwise disconnect (Close Connection)
                Thread th = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        fWaitToEstablishConnectionResetEvent.WaitOne(3000);

                        if (!handler.isConnectionEstablished())
                        {
                            try
                            {
                                handler.CloseConnection();
                            }
                            catch (Exception ex)
                            {

                            }

                            jButtonConnect.setText("Connect");
                            jButtonConnect.setEnabled(true);
                            JOptionPane.showMessageDialog(fInstance, "Unable to establish connection with the CNC Controller!\nPlease check if you are using the correct port.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                th.start();
            }
            else
            {
                handler.CloseConnection();
            }
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(fInstance, ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            jButtonConnect.setText("Connect");
            jButtonConnect.setEnabled(true);
        }

    }//GEN-LAST:event_jButtonConnectActionPerformed

    public static void main(String args[])
    {

        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex)
        {

        }

        java.awt.EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                new frmMain().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonConnect;
    private javax.swing.JComboBox jComboBoxBaud;
    private javax.swing.JComboBox jComboBoxFramework;
    private javax.swing.JComboBox jComboBoxPort;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
}
