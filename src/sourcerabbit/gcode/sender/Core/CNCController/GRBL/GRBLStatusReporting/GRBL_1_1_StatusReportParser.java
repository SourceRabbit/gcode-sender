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
package sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLStatusReporting;

import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLActiveStates;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLConnectionHandler;
import sourcerabbit.gcode.sender.Core.Machine.MachineInformation;

/**
 *
 * @author Nikos Siatras
 */
public class GRBL_1_1_StatusReportParser extends GRBLStatusReportParser
{

    // Work Coordinate Offset (WCO):
    public boolean fControllerSendsWorkPosition = false;
    public Float fXOffset = 0.0f, fYOffset = 0.0f, fZOffset = 0.0f;

    public GRBL_1_1_StatusReportParser(GRBLConnectionHandler myConnectionHandler)
    {
        super(myConnectionHandler);
    }

    @Override
    public int ParseStatusReportMessageAndReturnActiveState(String statusReportMessage)
    {
        int currentActiveState = 0;
        // Example: <Idle|MPos:0.000,0.000,0.000|FS:0,0|WCO:0.000,0.000,0.000>
        statusReportMessage = statusReportMessage.toLowerCase().replace("<", "").replace(">", "");
        //System.out.println(statusReportMessage);

        // Get the Active State
        String[] statusParts = statusReportMessage.split("\\|");
        currentActiveState = GRBLActiveStates.getGRBLActiveStateFromString(statusParts[0]);

        // Parse status message parts
        for (String part : statusParts)
        {
            if (part.startsWith("mpos:"))
            {
                // Get Machine Position
                fControllerSendsWorkPosition = false;
                String[] machinePosition = part.replace("mpos:", "").split(",");

                // Set Machine Position
                fMyConnectionHandler.getMachinePosition().setX(Float.parseFloat(machinePosition[0]));
                fMyConnectionHandler.getMachinePosition().setY(Float.parseFloat(machinePosition[1]));
                fMyConnectionHandler.getMachinePosition().setZ(Float.parseFloat(machinePosition[2]));

                // Call the CalculateWorkCoordinateOffset method!
                CalculateCoordinateOffset();
            }
            else if (part.startsWith("wpos:"))
            {
                // Get Work Position
                fControllerSendsWorkPosition = true;
                String[] workPosition = part.replace("wpos:", "").split(",");

                // Set Machine Position
                fMyConnectionHandler.getWorkPosition().setX(Float.parseFloat(workPosition[0]));
                fMyConnectionHandler.getWorkPosition().setY(Float.parseFloat(workPosition[1]));
                fMyConnectionHandler.getWorkPosition().setZ(Float.parseFloat(workPosition[2]));

                // Call the CalculateWorkCoordinateOffset method!
                CalculateCoordinateOffset();
            }
            else if (part.startsWith("wco:"))
            {
                String[] wco = part.replace("wco:", "").split(",");

                fXOffset = Float.parseFloat(wco[0]);
                fYOffset = Float.parseFloat(wco[1]);
                fZOffset = Float.parseFloat(wco[2]);

                // Call the CalculateWorkCoordinateOffset method!
                CalculateCoordinateOffset();
            }
            else if (part.startsWith("f:"))
            {
                try
                {
                    // F:500 contains real-time feed rate data as the value
                    String[] parts = part.replace("f:", "").split(",");
                    int liveFeedRate = Integer.parseInt(parts[0]);
                    MachineInformation.LiveFeedRate().set(liveFeedRate);
                }
                catch (Exception ex)
                {

                }
            }
            else if (part.startsWith("fs:"))
            {
                try
                {
                    // FS:500,8000 contains real-time feed rate, followed by spindle speed, data as the values. 
                    // Note the FS:, rather than F:, data type name indicates spindle speed data is included.
                    String[] parts = part.replace("fs:", "").split(",");
                    int liveFeedRate = Integer.parseInt(parts[0]);
                    MachineInformation.LiveFeedRate().set(liveFeedRate);

                    int liveSpindleRPM = Integer.parseInt(parts[1]);
                    MachineInformation.LiveSpindleRPM().set(liveSpindleRPM);
                }
                catch (Exception ex)
                {

                }
            }
        }

        return currentActiveState;
    }

    private void CalculateCoordinateOffset()
    {
        if (fControllerSendsWorkPosition)
        {
            // MPos = WPos + WCO
            fMyConnectionHandler.getMachinePosition().setX(fMyConnectionHandler.getWorkPosition().getX() + fXOffset);
            fMyConnectionHandler.getMachinePosition().setY(fMyConnectionHandler.getWorkPosition().getY() + fYOffset);
            fMyConnectionHandler.getMachinePosition().setZ(fMyConnectionHandler.getWorkPosition().getZ() + fZOffset);
        }
        else
        {
            // WPos = MPos - WCO
            fMyConnectionHandler.getWorkPosition().setX(fMyConnectionHandler.getMachinePosition().getX() - fXOffset);
            fMyConnectionHandler.getWorkPosition().setY(fMyConnectionHandler.getMachinePosition().getY() - fYOffset);
            fMyConnectionHandler.getWorkPosition().setZ(fMyConnectionHandler.getMachinePosition().getZ() - fZOffset);
        }
    }
}
