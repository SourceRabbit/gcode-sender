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

import java.math.BigDecimal;
import java.math.MathContext;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.SerialConnectionEvent;
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
    public Double fXOffset = 0.000, fYOffset = 0.000, fZOffset = 0.000;

    public GRBL_1_1_StatusReportParser(GRBLConnectionHandler myConnectionHandler)
    {
        super(myConnectionHandler);
    }

    @Override
    public int ParseStatusReportMessageAndReturnActiveState(String statusReportMessage)
    {
        // Show verbose output
        if (ConnectionHelper.ACTIVE_CONNECTION_HANDLER.isShowVerboseOutputEnabled())
        {
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.getSerialConnectionEventManager().FireDataReceivedFromSerialConnectionEvent(new SerialConnectionEvent(statusReportMessage));
        }

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
                BigDecimal machineXBigDecimal = parseMachineOrWorkPositionToBigDecimal64(machinePosition[0]);
                BigDecimal machineYBigDecimal = parseMachineOrWorkPositionToBigDecimal64(machinePosition[1]);
                BigDecimal machineZBigDecimal = parseMachineOrWorkPositionToBigDecimal64(machinePosition[2]);

                fMyConnectionHandler.getMachinePosition().setX(machineXBigDecimal.doubleValue());
                fMyConnectionHandler.getMachinePosition().setY(machineYBigDecimal.doubleValue());
                fMyConnectionHandler.getMachinePosition().setZ(machineZBigDecimal.doubleValue());

                // Call the CalculateWorkCoordinateOffset method!
                CalculateCoordinateOffset();
            }
            else
            {
                if (part.startsWith("wpos:"))
                {
                    // Get Work Position
                    fControllerSendsWorkPosition = true;
                    String[] workPosition = part.replace("wpos:", "").split(",");

                    // Set work Position
                    BigDecimal workXBigDecimal = parseMachineOrWorkPositionToBigDecimal64(workPosition[0]);
                    BigDecimal workYBigDecimal = parseMachineOrWorkPositionToBigDecimal64(workPosition[1]);
                    BigDecimal workZBigDecimal = parseMachineOrWorkPositionToBigDecimal64(workPosition[2]);

                    fMyConnectionHandler.getWorkPosition().setX(workXBigDecimal.doubleValue());
                    fMyConnectionHandler.getWorkPosition().setY(workYBigDecimal.doubleValue());
                    fMyConnectionHandler.getWorkPosition().setZ(workZBigDecimal.doubleValue());

                    // Call the CalculateWorkCoordinateOffset method!
                    CalculateCoordinateOffset();
                }
                else
                {
                    if (part.startsWith("wco:"))
                    {
                        String[] wco = part.replace("wco:", "").split(",");

                        BigDecimal decimalX = new BigDecimal(wco[0]);
                        BigDecimal decimalY = new BigDecimal(wco[1]);
                        BigDecimal decimalZ = new BigDecimal(wco[2]);

                        fXOffset = decimalX.doubleValue();
                        fYOffset = decimalY.doubleValue();
                        fZOffset = decimalZ.doubleValue();

                        // Call the CalculateWorkCoordinateOffset method!
                        CalculateCoordinateOffset();
                    }
                    else
                    {
                        if (part.startsWith("f:"))
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
                        else
                        {
                            if (part.startsWith("fs:"))
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
                    }
                }
            }
        }

        return currentActiveState;
    }

    private void CalculateCoordinateOffset()
    {
        if (fControllerSendsWorkPosition)
        {
            // Controller sends Work Position
            // Calculate the Machine position !!
            // MPos = WPos + WCO
            fMyConnectionHandler.getMachinePosition().setX(fMyConnectionHandler.getWorkPosition().getX() + fXOffset);
            fMyConnectionHandler.getMachinePosition().setY(fMyConnectionHandler.getWorkPosition().getY() + fYOffset);
            fMyConnectionHandler.getMachinePosition().setZ(fMyConnectionHandler.getWorkPosition().getZ() + fZOffset);
        }
        else
        {
            // Controller sends Machine Position
            // Calculate the work position !!
            // WPos = MPos - WCO
            // X Axis
            BigDecimal workPositionX = getWorkPositionFromMachinePositionAndOffset(fMyConnectionHandler.getMachinePosition().getX(), fXOffset);
            fMyConnectionHandler.getWorkPosition().setX(workPositionX.doubleValue());

            // Y Axis
            BigDecimal workPositionY = getWorkPositionFromMachinePositionAndOffset(fMyConnectionHandler.getMachinePosition().getY(), fYOffset);
            fMyConnectionHandler.getWorkPosition().setY(workPositionY.doubleValue());

            // Z Axis
            BigDecimal workPositionZ = getWorkPositionFromMachinePositionAndOffset(fMyConnectionHandler.getMachinePosition().getZ(), fZOffset);
            fMyConnectionHandler.getWorkPosition().setZ(workPositionZ.doubleValue());
        }
    }

    private BigDecimal parseMachineOrWorkPositionToBigDecimal64(String value)
    {
        double valueToDouble = Double.parseDouble(value);
        BigDecimal bigDecimalValue = new BigDecimal(0, MathContext.DECIMAL64);
        bigDecimalValue = bigDecimalValue.setScale(2);
        bigDecimalValue = bigDecimalValue.add(new BigDecimal(valueToDouble, MathContext.DECIMAL64));
        return bigDecimalValue;
    }

    private BigDecimal getWorkPositionFromMachinePositionAndOffset(double machinePosition, double offset)
    {
        // Calculate the work position !!
        // WPos = MPos - WCO (offset)

        BigDecimal result = new BigDecimal(0, MathContext.DECIMAL64);
        result.setScale(0);

        BigDecimal machinePositionBigDecimal = new BigDecimal(0, MathContext.DECIMAL64);
        machinePositionBigDecimal.setScale(0);
        machinePositionBigDecimal = machinePositionBigDecimal.add(new BigDecimal(machinePosition, MathContext.DECIMAL64));

        BigDecimal offsetBigDecimal = new BigDecimal(0, MathContext.DECIMAL64);
        offsetBigDecimal.setScale(0);
        offsetBigDecimal = offsetBigDecimal.add(new BigDecimal(offset, MathContext.DECIMAL64));

        result = result.add(machinePositionBigDecimal).subtract(offsetBigDecimal);

        return result;
    }

}
