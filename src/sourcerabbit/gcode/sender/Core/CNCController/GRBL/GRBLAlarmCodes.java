package sourcerabbit.gcode.sender.Core.CNCController.GRBL;

import java.util.HashMap;

/**
 *
 * @author Nikos Siatras
 */
public class GRBLAlarmCodes
{

    private static HashMap<Integer, String> fAlarmsByID = new HashMap<Integer, String>();

    static
    {
        fAlarmsByID.put(1, "Hard limit triggered. Machine position is likely lost due to sudden and immediate halt. Re-homing is highly recommended.");
        fAlarmsByID.put(2, "G-code motion target exceeds machine travel. Machine position safely retained. Alarm may be unlocked.");
        fAlarmsByID.put(3, "Reset while in motion. Grbl cannot guarantee position. Lost steps are likely. Re-homing is highly recommended.");
        fAlarmsByID.put(4, "Probe fail. The probe is not in the expected initial state before starting probe cycle, where G38.2 and G38.3 is not triggered and G38.4 and G38.5 is triggered.");
        fAlarmsByID.put(5, "Probe fail. Probe did not contact the workpiece within the programmed travel for G38.2 and G38.4.");
        fAlarmsByID.put(6, "Homing fail. Reset during active homing cycle.");
        fAlarmsByID.put(7, "Homing fail. Safety door was opened during active homing cycle.");
        fAlarmsByID.put(8, "Homing fail. Cycle failed to clear limit switch when pulling off. Try increasing pull-off setting or check wiring.");
        fAlarmsByID.put(9, "Homing fail. Could not find limit switch within search distance. Defined as 1.5 * max_travel on search and 5 * pulloff on locate phases.");
    }

    public static String getAlarmMessageByID(int alarmID)
    {
        if (fAlarmsByID.containsKey(alarmID))
        {
            return fAlarmsByID.get(alarmID);
        }
        else
        {
            return "Alarm ID " + String.valueOf(alarmID);
        }
    }
}
