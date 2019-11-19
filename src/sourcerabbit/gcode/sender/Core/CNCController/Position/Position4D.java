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

    private Double fX, fY, fZ, fA;

    public Position4D(Double x, Double y, Double z, Double a)
    {
        fX = x;
        fY = y;
        fZ = z;
        fA = a;
    }

    public void setX(double x)
    {
        fX = x;
    }

    public void setY(double y)
    {
        fY = y;
    }

    public void setZ(double z)
    {
        fZ = z;
    }

    public void setA(double a)
    {
        fA = a;
    }

    public void setPosition(double x, double y, double z, double a)
    {
        fX = x;
        fY = y;
        fZ = z;
        fA = a;
    }

    public Double getX()
    {
        return fX;
    }

    public Double getY()
    {
        return fY;
    }

    public Double getZ()
    {
        return fZ;
    }

    public Double getA()
    {
        return fA;
    }
}
