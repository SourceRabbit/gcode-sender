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

import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.SerialConnectionEvents.SerialConnectionEvent;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLActiveStates;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLConnectionHandler;

/**
 *
 * @author Nikos Siatras
 */
public class GRBL_0_9_StatusReportParser extends GRBLStatusReportParser
{

    public GRBL_0_9_StatusReportParser(GRBLConnectionHandler myConnectionHandler)
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
        
        statusReportMessage = statusReportMessage.toLowerCase().replace("mpos", "").replace("wpos", "").replace("wco", "").replace(":", "").replace("<", "").replace(">", "");
        // Machine status received !
        String[] statusParts = statusReportMessage.split(",");
        int newActiveState = GRBLActiveStates.getGRBLActiveStateFromString(statusParts[0]);

        // Set Machine Position
        fMyConnectionHandler.getMachinePosition().setX(Float.parseFloat(statusParts[1]));
        fMyConnectionHandler.getMachinePosition().setY(Float.parseFloat(statusParts[2]));
        fMyConnectionHandler.getMachinePosition().setZ(Float.parseFloat(statusParts[3]));

        // Set Work Position
        fMyConnectionHandler.getWorkPosition().setX(Float.parseFloat(statusParts[4]));
        fMyConnectionHandler.getWorkPosition().setY(Float.parseFloat(statusParts[5]));
        fMyConnectionHandler.getWorkPosition().setZ(Float.parseFloat(statusParts[6]));

        return newActiveState;
    }

}
