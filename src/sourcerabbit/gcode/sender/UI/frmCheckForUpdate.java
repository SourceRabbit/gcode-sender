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

import java.awt.Desktop;
import java.io.StringReader;
import java.net.URI;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import sourcerabbit.gcode.sender.Core.CNCController.Position.Position2D;
import sourcerabbit.gcode.sender.Core.HTTPRequest.HTTPRequestData;
import sourcerabbit.gcode.sender.UI.UITools.UITools;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import sourcerabbit.gcode.sender.Core.Settings.SettingsManager;

/**
 *
 * @author Nikos Siatras
 */
public class frmCheckForUpdate extends javax.swing.JDialog
{

    private Thread fUpdateThread;
    private final String fUpdatesXMLURL = "https://www.sourcerabbit.com/GCode-Sender/downloads/updates.xml";

    public frmCheckForUpdate(frmControl parent, boolean modal)
    {
        super(parent, modal);
        initComponents();

        // Set form in middle of parent
        Position2D pos = UITools.getPositionForDialogToOpenInMiddleOfParentForm(parent, this);
        setLocation((int) pos.getX(), (int) pos.getY());

        CheckForUpdate();
    }

    private void CheckForUpdate()
    {
        fUpdateThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Step1_DownloadUpdatesXML();
            }
        });
        fUpdateThread.start();
    }

    private void Step1_DownloadUpdatesXML()
    {
        jLabelStatus.setText("Contacting Server...");
        String xmlData = HTTPRequestData.GetHTML(fUpdatesXMLURL);

        Step2_ParseXML(xmlData);
    }

    private void Step2_ParseXML(final String xmldata)
    {
        jLabelStatus.setText("Checking Version...");

        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = (Document) dBuilder.parse(new InputSource(new StringReader(xmldata)));

            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("entry");

            if (nList.getLength() > 0)
            {
                Node nNode = nList.item(0);
                Element eElement = (Element) nNode;
                String newVersion = eElement.getAttribute("newVersion");
                Step3_CheckNewVersion(newVersion);
            }
        }
        catch (Exception ex)
        {
            String a = "asasa";
        }
    }

    private void Step3_CheckNewVersion(final String newVersion)
    {
        jLabelStatus.setText("Finished!");

        if (SettingsManager.getAppVersion().equals(newVersion))
        {
            // New version found
            JOptionPane.showMessageDialog(this, "Your version is up to date!", "Finished", JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
        }
        else
        {
            //JOptionPane.showMessageDialog(this, "To download the new version go to https://www.sourcerabbit.com/GCode-Sender \nor click the \"Yes\" button", "Newer version found", JOptionPane.QUESTION_MESSAGE);
            Object[] options =
            {
                "Download Now",
                "Later"
            };
            int result = JOptionPane.showOptionDialog(this, "Latest version is " + newVersion + " !\nDo you want to download now ?\n\nhttps://www.sourcerabbit.com/GCode-Sender",
                    "New Version Found!",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null, options, options[1]);

            switch (result)
            {
                case 0:
                    // Download
                    try
                    {
                        URI uri = new URI("https://www.sourcerabbit.com/GCode-Sender/#DownloadsSection");
                        Desktop.getDesktop().browse(uri);
                    }
                    catch (Exception ex)
                    {

                    }
                    break;

                case 1:
                    // Later
                    break;
            }

            this.dispose();
        }
    }

    private void ErrorOccured(String error)
    {
        jLabelStatus.setText("Error! Please try again later!");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jLabelStatus = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Check for Update");
        setResizable(false);

        jLabelStatus.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabelStatus.setForeground(new java.awt.Color(0, 75, 127));
        jLabelStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelStatus.setText("Status....");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 67, Short.MAX_VALUE)
        );

        getAccessibleContext().setAccessibleName("Checking for Update...");

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabelStatus;
    // End of variables declaration//GEN-END:variables
}
