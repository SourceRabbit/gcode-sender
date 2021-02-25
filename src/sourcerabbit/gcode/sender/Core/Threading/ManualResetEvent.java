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
package sourcerabbit.gcode.sender.Core.Threading;

/**
 *
 * @author Nikos Siatras
 */
public class ManualResetEvent
{

    private boolean fInitialState;

    public ManualResetEvent(boolean initialState)
    {
        fInitialState = initialState;
    }

    public synchronized void Reset()
    {
        fInitialState = false;
    }

    public synchronized void WaitOne()
    {
        while (!fInitialState)
        {
            try
            {
                wait();
            }
            catch (InterruptedException ex)
            {
            }
        }
    }

    public synchronized void WaitOne(long milliseconds)
    {
        try
        {
            if (!fInitialState)
            {
                wait(milliseconds);
            }
        }
        catch (InterruptedException ex)
        {
        }
    }

    public synchronized void Set()
    {
        fInitialState = true;
        notify();
    }
    
    public boolean getState()
    {
        return fInitialState;
    }
}
