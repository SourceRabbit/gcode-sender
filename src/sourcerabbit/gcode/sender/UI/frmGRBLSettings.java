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
import java.util.HashMap;
import javax.swing.JDialog;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.ISerialConnectionEventListener;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.SerialConnectionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLSettingsDescription;
import sourcerabbit.gcode.sender.Core.CNCController.Position.Position2D;
import sourcerabbit.gcode.sender.UI.Renderers.JTableRenderer;
import sourcerabbit.gcode.sender.UI.UITools.UITools;

/**
 *
 * @author Nikos Siatras
 */
public class frmGRBLSettings extends JDialog
{

    private HashMap<String, String> fOldValues = new HashMap<String, String>();

    private final GRBLSettingsDescription fSettingsDescription = new GRBLSettingsDescription();

    /**
     * Creates new form frmGRBLSettings
     */
    public frmGRBLSettings()
    {
        initComponents();

        // Set form in middle of screen
        Position2D pos = UITools.getPositionForFormToOpenInMiddleOfScreen(this.getSize().width, this.getSize().height);
        this.setLocation((int) pos.getX(), (int) pos.getY());

        // Set Form Icon
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("Images/SourceRabbitIcon.png")));

        jTableSettings.setDefaultRenderer(Object.class, new JTableRenderer());

        // Add event for incoming data
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getSerialConnectionEventManager().AddListener(fSerialConnectionEvents);

        Init();
    }

    ISerialConnectionEventListener fSerialConnectionEvents = new ISerialConnectionEventListener()
    {

        @Override
        public void ConnectionEstablished(SerialConnectionEvent evt)
        {

        }

        @Override
        public void ConnectionClosed(SerialConnectionEvent evt)
        {

        }

        @Override
        public void DataReceivedFromSerialConnection(SerialConnectionEvent evt)
        {
            String data = evt.getSource().toString();

            String[] parts = data.split(" ");
            String[] idAndValueParts = parts[0].split("=");
            String description = data.replace(parts[0], "").trim();

            String item[] = new String[1];
            switch (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getCNCControlFrameworkVersion())
            {
                case GRBL0_9:
                    item = new String[4];
                    item[0] = idAndValueParts[0];
                    item[1] = idAndValueParts[1];
                    item[2] = "";
                    item[3] = description;
                    break;

                case GRBL1_1:
                    item = new String[4];
                    item[0] = idAndValueParts[0];
                    item[1] = idAndValueParts[1];
                    item[2] = fSettingsDescription.getSettingType(Integer.parseInt(item[0].replace("$", "")));
                    item[3] = fSettingsDescription.getSettingDescription(Integer.parseInt(item[0].replace("$", "")));
                    break;
            }

            fOldValues.put(item[0], item[1]);

            DefaultTableModel model = (DefaultTableModel) jTableSettings.getModel();
            model.addRow(item);
        }
    };

    private void Init()
    {
        // Add the apropriate columns for each GRBL version
        switch (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getCNCControlFrameworkVersion())
        {
            case GRBL0_9:
                // Remove column "Type"
                TableColumn col = jTableSettings.getColumnModel().getColumn(2);
                jTableSettings.removeColumn(col);
                jTableSettings.revalidate();

                break;

            case GRBL1_1:
                // Do nothing !
                break;
        }

        try
        {
            // Send "$$" command to GRBL Controller
            GCodeCommand command = new GCodeCommand("$$");
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommand(command);
        }
        catch (Exception ex)
        {

        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTableSettings = new javax.swing.JTable();
        jButtonSave = new javax.swing.JButton();
        jButtonClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("GRBL Controller Settings");

        jTableSettings.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jTableSettings.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {

            },
            new String []
            {
                "ID", "Value", "Type", "Description"
            }
        )
        {
            boolean[] canEdit = new boolean []
            {
                false, true, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return canEdit [columnIndex];
            }
        });
        jTableSettings.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTableSettings.setShowHorizontalLines(false);
        jTableSettings.setShowVerticalLines(false);
        jTableSettings.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jTableSettings);
        if (jTableSettings.getColumnModel().getColumnCount() > 0)
        {
            jTableSettings.getColumnModel().getColumn(0).setMinWidth(60);
            jTableSettings.getColumnModel().getColumn(0).setPreferredWidth(60);
            jTableSettings.getColumnModel().getColumn(0).setMaxWidth(60);
            jTableSettings.getColumnModel().getColumn(1).setMinWidth(100);
            jTableSettings.getColumnModel().getColumn(1).setPreferredWidth(100);
            jTableSettings.getColumnModel().getColumn(1).setMaxWidth(100);
            jTableSettings.getColumnModel().getColumn(2).setMinWidth(120);
            jTableSettings.getColumnModel().getColumn(2).setPreferredWidth(120);
            jTableSettings.getColumnModel().getColumn(2).setMaxWidth(120);
        }

        jButtonSave.setText("Save Settings");
        jButtonSave.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonSaveActionPerformed(evt);
            }
        });

        jButtonClose.setText("Close");
        jButtonClose.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonCloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonSave)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonClose)
                .addContainerGap(277, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 669, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonSave)
                    .addComponent(jButtonClose))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonCloseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonCloseActionPerformed
    {//GEN-HEADEREND:event_jButtonCloseActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButtonCloseActionPerformed

    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonSaveActionPerformed
    {//GEN-HEADEREND:event_jButtonSaveActionPerformed
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getSerialConnectionEventManager().RemoveListener(fSerialConnectionEvents);
        jButtonSave.requestFocus();

        int tableRows = jTableSettings.getRowCount();
        for (int i = 0; i < tableRows; i++)
        {
            try
            {
                String id = jTableSettings.getValueAt(i, 0).toString();
                String value = jTableSettings.getValueAt(i, 1).toString().trim();

                if (!fOldValues.get(id).equals(value))
                {
                    String commandStr = id + "=" + value;
                    GCodeCommand command = new GCodeCommand(commandStr);
                    String result = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(command);
                    System.out.println(result);
                }
            }
            catch (Exception ex)
            {
            }
        }
        this.dispose();
    }//GEN-LAST:event_jButtonSaveActionPerformed

    @Override
    public void dispose()
    {
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getSerialConnectionEventManager().RemoveListener(fSerialConnectionEvents);
        super.dispose(); //To change body of generated methods, choose Tools | Templates.
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableSettings;
    // End of variables declaration//GEN-END:variables
}
