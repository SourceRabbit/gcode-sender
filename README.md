SourceRabbit GCode Sender
------
<p align="center">
<img src="https://raw.githubusercontent.com/nsiatras/sourcerabbit-gcode-sender/master/Images/SourceRabbit.png" alt="SourceRabbit.com"> 
</p>

SourceRabbit GCode Sender is a <b>GRBL</b> compatible, cross platform G-Code sender written in Java. It features a highly optimized and asynchronous (event-driven) UI and USB-to-Serial communication and can be also used on computers with small amount of RAM and CPU.

To run simply <b>download</b>, <b>unzip</b> the .zip file and <b>double click</b> the SourceRabbit-GCODE-Sender.jar file. On some platforms you will need to run an included start script.

<b>Note for MAC users:</b> You may need to create a "/var/lock" directory with write permission. To do this open the Terminal application and run the following two commands: <br>
sudo mkdir /var/lock <br>
sudo chmod 777 /var/lock 

Technical details:
* Compatible only with <b>GRBL 0.9</b> and later versions
* Uses JSSC for serial communication
* Event-Driven UI and USB-to-Serial communication
* Developed with <b>NetBeans 8.0.2</b>
* To build you need to open the project in Netbeans and just... build

Goals:
* Provide a fast, accurate and easy to use software
* Support all GRBL CNC router and milling machines
* Can be used on computers with small amount of RAM and CPU.


Downloads
------
[1.0.6] (https://github.com/nsiatras/sourcerabbit-gcode-sender/releases/download/1.0.6/SourceRabbit-GCode-Sender.zip) - Requires Java 7 or higher. <br>
[1.0.5] (https://github.com/nsiatras/sourcerabbit-gcode-sender/releases/download/1.0.5/SourceRabbit-GCode-Sender.zip) - Requires Java 7 or higher. <br>
[1.0.4](https://github.com/nsiatras/sourcerabbit-gcode-sender/releases/download/1.0.4/SourceRabbit-GCode-Sender.zip) - Requires Java 7 or higher. <br>
[1.0.3](https://github.com/nsiatras/sourcerabbit-gcode-sender/releases/download/1.0.3/SourceRabbit-GCode-Sender-1.0.3.zip) - Requires Java 7 or higher. <br>



![Connect to your CNC!](https://github.com/nsiatras/sourcerabbit-gcode-sender/blob/master/Images/ConnectForm.png "Connect to your CNC!")

![CNC Control Form](https://github.com/nsiatras/sourcerabbit-gcode-sender/blob/master/Images/ControllForm.png "CNC Control Form")


Changelog
------
1.0.4 -> 1.0.5
* Macro commands added inside the control form

1.0.3 -> 1.0.4
* Console "output" added to ControlForm

1.0.2 -> 1.0.3
* "Enable GCode Log" check box added to ControlForm. This helps users with slower computers
* Point4D class added for future use with 4 Axis CNC
* USB-to-Serial communication optimized

1.0.1 -> 1.0.2
* Fixed several UI bugs.
* Information Message box on GCode cycle finish.

1.0.0 -> 1.0.1
* Clicking the cancel button during GCode cycle stops the machine instantly.
* GCode cycle events implemented.
* GCode Sender can identify if the USB cable is plugged or not.
* Automatically skip blank lines and comments when sending a file.
