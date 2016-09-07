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
package sourcerabbit.gcode.sender.Core.CNCController.Position;

/**
 *
 * @author Nikos Siatras
 */
public class Position4D
{

    private Float fX, fY, fZ, fA;

    public Position4D(Float x, Float y, Float z, Float a)
    {
        fX = x;
        fY = y;
        fZ = z;
        fA = a;
    }

    public void setX(Float x)
    {
        fX = x;
    }

    public void setY(Float y)
    {
        fY = y;
    }

    public void setZ(Float z)
    {
        fZ = z;
    }

    public void setA(Float a)
    {
        fA = a;
    }

    public void setPosition(Float x, Float y, Float z, Float a)
    {
        fX = x;
        fY = y;
        fZ = z;
        fA = a;
    }

    public Float getX()
    {
        return fX;
    }

    public Float getY()
    {
        return fY;
    }

    public Float getZ()
    {
        return fZ;
    }

    public Float getA()
    {
        return fA;
    }
}
