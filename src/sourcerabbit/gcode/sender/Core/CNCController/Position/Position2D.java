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
public class Position2D
{

    private double fX, fY;

    public Position2D()
    {

    }

    public Position2D(double x, double y)
    {
        fX = x;
        fY = y;
    }

    public void setX(double x)
    {
        fX = x;
    }

    public void setY(double y)
    {
        fY = y;
    }

    public void setPosition(double x, double y)
    {
        fX = x;
        fY = y;
    }

    public double getX()
    {
        return fX;
    }

    public double getY()
    {
        return fY;
    }
}
