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

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Toolkit;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import jssc.SerialPortList;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHandler;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.ISerialConnectionEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.SerialConnectionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLConnectionHandler;
import sourcerabbit.gcode.sender.Core.Threading.ManualResetEvent;
import sourcerabbit.gcode.sender.Core.CNCController.Position.Position2D;
import sourcerabbit.gcode.sender.Core.Settings.SettingsManager;
import sourcerabbit.gcode.sender.UI.UITools.UITools;

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

        // Fix decoration for FlatLaf
        dispose();
        setUndecorated(true);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
        setVisible(true);
        JFrame.setDefaultLookAndFeelDecorated(false);

        InitUI();

        // Set form in middle of screen
        Position2D pos = UITools.getPositionForFormToOpenInMiddleOfScreen(this.getSize().width, this.getSize().height);
        setLocation((int) pos.getX(), (int) pos.getY());

        setTitle("SourceRabbit GCode Sender (Version " + SettingsManager.getAppVersion() + ")");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("Images/SourceRabbitIcon.png")));

    }

    private void InitUI()
    {
        String[] serialPorts = SerialPortList.getPortNames();

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
            jComboBoxFramework.addItem("GRBL 0.9 and later");

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

    private final ISerialConnectionEventListener fConnectionEstablishedEventListener = new ISerialConnectionEventListener()
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

        jLabel5 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jComboBoxPort = new javax.swing.JComboBox();
        jComboBoxBaud = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jComboBoxFramework = new javax.swing.JComboBox();
        jButtonConnect = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SourceRabbit GCODE Sender ver 1.0");
        setResizable(false);

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sourcerabbit/gcode/sender/UI/Images/SourceRabbit.png"))); // NOI18N

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Connect to your 3-Axis CNC", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Tahoma", 1, 18))); // NOI18N
        jPanel2.addAncestorListener(new javax.swing.event.AncestorListener()
        {
            public void ancestorMoved(javax.swing.event.AncestorEvent evt)
            {
            }
            public void ancestorAdded(javax.swing.event.AncestorEvent evt)
            {
                jPanel2AncestorAdded(evt);
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt)
            {
            }
        });

        jLabel2.setText("Port:");

        jLabel4.setText("Baud:");

        jLabel3.setText("Framework:");

        jComboBoxFramework.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jComboBoxFramework.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jComboBoxFrameworkActionPerformed(evt);
            }
        });

        jButtonConnect.setText("Connect");
        jButtonConnect.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonConnectActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButtonConnect, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addGap(10, 10, 10)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jComboBoxBaud, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jComboBoxPort, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jComboBoxFramework, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(25, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jComboBoxPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jComboBoxBaud, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jComboBoxFramework, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jButtonConnect, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(77, 77, 77)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(74, 74, 74))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addGap(26, 26, 26))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel5)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(32, Short.MAX_VALUE))
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
                if(ConnectionHelper.ACTIVE_CONNECTION_HANDLER.isConnectionEstablished())
                {
                ConnectionHelper.ACTIVE_CONNECTION_HANDLER.CloseConnection();
                }
            }
            catch (Exception ex)
            {

            }
        }
         

        String baud = jComboBoxBaud.getSelectedItem().toString();
        String port = jComboBoxPort.getSelectedItem().toString();

        final ConnectionHandler handler = new GRBLConnectionHandler();
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER = handler;
        

        // Add connection established listener
        handler.getSerialConnectionEventManager().RemoveListener(fConnectionEstablishedEventListener);
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

                        if (!ConnectionHelper.ACTIVE_CONNECTION_HANDLER.isConnectionEstablished())
                        {
                            try
                            {
                                ConnectionHelper.ACTIVE_CONNECTION_HANDLER.CloseConnection();
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

    private void jComboBoxFrameworkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jComboBoxFrameworkActionPerformed
    {//GEN-HEADEREND:event_jComboBoxFrameworkActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBoxFrameworkActionPerformed

    private void jPanel2AncestorAdded(javax.swing.event.AncestorEvent evt)//GEN-FIRST:event_jPanel2AncestorAdded
    {//GEN-HEADEREND:event_jPanel2AncestorAdded
        // TODO add your handling code here:
    }//GEN-LAST:event_jPanel2AncestorAdded

    public static void main(String args[])
    {
        FlatLightLaf.install();

        try
        {

            UIManager.setLookAndFeel(new FlatDarkLaf());
        }
        catch (Exception ex)
        {

        }

        /* try
        {
            
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex)
        {

        }*/
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
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel2;
    // End of variables declaration//GEN-END:variables
}
