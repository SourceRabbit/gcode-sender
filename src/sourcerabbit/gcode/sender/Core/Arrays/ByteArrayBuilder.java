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
package sourcerabbit.gcode.sender.Core.Arrays;

import java.util.Arrays;

/**
 *
 * @author Nikos Siatras
 */
public class ByteArrayBuilder
{

    private byte[] fBytes;
    private final Object fLock;

    public ByteArrayBuilder()
    {
        fLock = new Object();
        fBytes = new byte[0];
    }

    /**
     * Append data to ByteArrayBuilder
     *
     * @param data is the byte[] array to append
     */
    public void Append(byte[] data)
    {
        synchronized (fLock)
        {
            final byte[] newArray = new byte[fBytes.length + data.length];
            System.arraycopy(fBytes, 0, newArray, 0, fBytes.length);
            System.arraycopy(data, 0, newArray, fBytes.length, data.length);
            fBytes = newArray;
        }
    }

    public int IndexOf(byte[] subArray)
    {
        synchronized (fLock)
        {
            int i = 0;
            int subArrayLength = subArray.length;

            // Find subArray in fBytes
            for (i = 0; i < fBytes.length; i++)
            {
                byte[] arrayToCompare = Arrays.copyOfRange(fBytes, i, i + subArrayLength);

                if (Arrays.equals(arrayToCompare, subArray))
                {
                    return i;
                }
            }

            return -1;
        }
    }

    public void Delete(int indexFrom, int indexTo)
    {
        synchronized (fLock)
        {
            final byte[] firstPart = Arrays.copyOfRange(fBytes, 0, indexFrom);
            final byte[] secondPart = Arrays.copyOfRange(fBytes, indexTo, fBytes.length);

            final byte[] C = new byte[firstPart.length + secondPart.length];
            System.arraycopy(firstPart, 0, C, 0, firstPart.length);
            System.arraycopy(secondPart, 0, C, firstPart.length, secondPart.length);

            fBytes = C;
        }
    }

    public byte[] SubList(int fromIndex, int toIndex)
    {
        synchronized (fLock)
        {
            return Arrays.copyOfRange(fBytes, fromIndex, toIndex);
        }
    }

    public void Dispose()
    {
        synchronized (fLock)
        {
            this.fBytes = new byte[0];
        }
    }
}
