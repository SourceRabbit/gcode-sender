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
package sourcerabbit.gcode.sender.Core.CNCController.Processes;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import sourcerabbit.gcode.sender.Core.CNCController.Connection.ConnectionHelper;
import sourcerabbit.gcode.sender.Core.CNCController.GCode.GCodeCommand;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLActiveStates;
import sourcerabbit.gcode.sender.Core.CNCController.GRBL.GRBLCommands;
import sourcerabbit.gcode.sender.Core.Settings.TouchProbeSettings;
import sourcerabbit.gcode.sender.Core.Threading.ManualResetEvent;

/**
 *
 * @author Nikos Siatras
 */
public class Process_ZAxisTouchProbe extends Process
{

    // Variables
    private int fMaxDistance = 20;
    private final int fFeedRate = 80;
    private final int fSlowFeedRate = 30;

    private boolean fMachineTouchedTheProbe = false;
    private final ManualResetEvent fWaitToTouchTheProbe = new ManualResetEvent(false);
    private final ManualResetEvent fWaitForMachineToBeIdle = new ManualResetEvent(false);

    public Process_ZAxisTouchProbe(JDialog parentForm)
    {
        super(parentForm);
    }

    @Override
    public void MachineStatusHasChange(int state)
    {
        switch (state)
        {
            case GRBLActiveStates.IDLE:
                fWaitForMachineToBeIdle.Set();
                break;
            case GRBLActiveStates.RUN:
                break;
            case GRBLActiveStates.HOLD:
            case GRBLActiveStates.ALARM:
            case GRBLActiveStates.RESET_TO_CONTINUE:
                fWaitToTouchTheProbe.Set();
                break;
            case GRBLActiveStates.MACHINE_TOUCHED_PROBE:
                fMachineTouchedTheProbe = true;
                fWaitToTouchTheProbe.Set();
                break;
        }
    }

    @Override
    public void Execute()
    {
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StartUsingTouchProbe();

        // Step 1
        // Move the endmill towards the probe until they touch each other.
        fWaitToTouchTheProbe.Reset();
        fMachineTouchedTheProbe = false;
        String response = MoveEndmillToProbe(fMaxDistance, fFeedRate);
        fWaitToTouchTheProbe.WaitOne();
        if (!response.equals("ok") || !fMachineTouchedTheProbe)
        {
            MachineFailedToTouchTheProbe();
            return;
        }

        // Step 2
        // Move the endmill 0.5 mm back
        String moveZStr = "G21G91G1Z0.5F" + fSlowFeedRate;
        GCodeCommand moveZCommand = new GCodeCommand(moveZStr);
        response = ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(moveZCommand);
        System.out.println(response);
        if (!response.equals("ok"))
        {
            MachineFailedToTouchTheProbe();
            return;
        }

        // Step 3
        // Touch the probe with slow feedrate
        fWaitToTouchTheProbe.Reset();
        fMachineTouchedTheProbe = false;
        response = MoveEndmillToProbe(1, fSlowFeedRate);
        fWaitToTouchTheProbe.WaitOne();
        if (!response.equals("ok") || !fMachineTouchedTheProbe)
        {
            MachineFailedToTouchTheProbe();
            return;
        }

        ///////////////////////////////////////////////////////////////////////////////
        // Success !!!
        // The endmill touched the touch probe twice !
        ///////////////////////////////////////////////////////////////////////////////
        // Stop Using Touch Probe
        ConnectionHelper.ACTIVE_CONNECTION_HANDLER.StopUsingTouchProbe();
        ///////////////////////////////////////////////////////////////////////////////
        String response2 = SetZAxisPosition(TouchProbeSettings.getHeightOfProbe());
        if (response2.equals("ok"))
        {
            // All good!
            // Move the Z 0.5mm above the probe
            moveZStr = "G21G91G1Z0.5F" + fSlowFeedRate;
            moveZCommand = new GCodeCommand(moveZStr);
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(moveZCommand);

            MachineTouchedTheProbeSucessfully();
        }
        else
        {
            MachineFailedToTouchTheProbe();
        }
    }

    private String MoveEndmillToProbe(int distance, int feedRate)
    {
        fMachineTouchedTheProbe = false;
        String gCodeStr = "G38.2Z-" + distance + "F" + feedRate;
        final GCodeCommand command = new GCodeCommand(gCodeStr);
        return ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(command);
    }

    private void MachineFailedToTouchTheProbe()
    {
        JOptionPane.showMessageDialog(fMyParentForm, "Machine failed to touch the probe!", "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void MachineTouchedTheProbeSucessfully()
    {
        fWaitForMachineToBeIdle.Reset();
        fWaitForMachineToBeIdle.WaitOne();
        JOptionPane.showMessageDialog(fMyParentForm, "Machine touched the probe sucessfully!");
    }

    private String SetZAxisPosition(double value)
    {
        String commandStr = "G92 Z" + String.valueOf(value);
        GCodeCommand commandSetZ = new GCodeCommand(commandStr);
        return ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendGCodeCommandAndGetResponse(commandSetZ);
    }

    @Override
    public void KillImmediately()
    {
        try
        {
            ConnectionHelper.ACTIVE_CONNECTION_HANDLER.SendDataImmediately_WithoutMessageCollector(GRBLCommands.COMMAND_SOFT_RESET);
        }
        catch (Exception ex)
        {
        }
    }
}
