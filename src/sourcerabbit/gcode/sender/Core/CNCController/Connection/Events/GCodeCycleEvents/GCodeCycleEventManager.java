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
package sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.GCodeCycleEvents;

import sourcerabbit.gcode.sender.Core.CNCController.Connection.Events.EventManager;

/**
 *
 * @author Nikos Siatras
 */
public class GCodeCycleEventManager extends EventManager
{

    public GCodeCycleEventManager()
    {

    }

    public void FireGCodeCycleStartedEvent(GCodeCycleEvent evt)
    {
        for (Object obj : fEventListeners)
        {
            IGCodeCycleListener listener = (IGCodeCycleListener) obj;
            listener.GCodeCycleStarted(evt);
        }
    }

    public void FireGCodeCycleFinishedEvent(GCodeCycleEvent evt)
    {
        for (Object obj : fEventListeners)
        {
            IGCodeCycleListener listener = (IGCodeCycleListener) obj;
            listener.GCodeCycleFinished(evt);
        }
    }

    public void FireGCodeCycleCanceledEvent(GCodeCycleEvent evt)
    {
        for (Object obj : fEventListeners)
        {
            IGCodeCycleListener listener = (IGCodeCycleListener) obj;
            listener.GCodeCycleCanceled(evt);
        }
    }

    public void FireGCodeCyclePausedEvent(GCodeCycleEvent evt)
    {
        for (Object obj : fEventListeners)
        {
            IGCodeCycleListener listener = (IGCodeCycleListener) obj;
            listener.GCodeCyclePaused(evt);
        }
    }

    public void FireGCodeCycleResumedEvent(GCodeCycleEvent evt)
    {
        for (Object obj : fEventListeners)
        {
            IGCodeCycleListener listener = (IGCodeCycleListener) obj;
            listener.GCodeCycleResumed(evt);
        }
    }
}
