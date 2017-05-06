SourceRabbit GCode Sender
------
<p align="center">
<img src="https://raw.githubusercontent.com/nsiatras/sourcerabbit-gcode-sender/master/Images/SourceRabbit.png" alt="SourceRabbit.com">
</p>

Downloads
------

To download the latest installer for <b>Windows</b> and <b>Mac</b> please visit:<br>
[https://www.sourcerabbit.com/GCode-Sender/](https://www.sourcerabbit.com/GCode-Sender/#DownloadsSection)<br>

For other operating systems:<br>
[https://github.com/nsiatras/sourcerabbit-gcode-sender/releases/latest](https://github.com/nsiatras/sourcerabbit-gcode-sender/releases/latest)<br>

If you don't have Java installed please visit [https://java.com/en/download/manual.jsp](https://java.com/en/download/manual.jsp).

About SourceRabbit GCode Sender
------
SourceRabbit GCode Sender is a <b>GRBL</b> compatible, cross platform G-Code sender written in Java. It features a highly optimized and asynchronous (event-driven) UI and USB-to-Serial communication and can be also used on computers with small amount of RAM and CPU.

<b>Note for MAC users:</b> You may need to create a "/var/lock" directory with write permission. To do this open the Terminal application and run the following two commands: <br>
sudo mkdir /var/lock <br>
sudo chmod 777 /var/lock 

Technical details:
* Compatible with GRBL v0.9 and above
* Uses JSSC for serial communication
* Event-Driven UI and USB-to-Serial communication
* Developed with <b>NetBeans IDE</b>
* To build you need to open the project in Netbeans and just... build

Goals:
* Provide a fast, accurate and easy to use software
* Support all GRBL CNC router and milling machines
* Can be used on computers with small amount of RAM and CPU.


![Connect to your CNC!](https://github.com/nsiatras/sourcerabbit-gcode-sender/blob/master/Images/ConnectForm.png "Connect to your CNC!")

![GCode Sender Control Form](https://github.com/nsiatras/sourcerabbit-gcode-sender/blob/master/Images/ControlForm.png "CNC Control Form")

![Touch Probe](https://github.com/nsiatras/sourcerabbit-gcode-sender/blob/master/Images/Probe.png "Touch Probe")

![Hole Center Finder](https://github.com/nsiatras/sourcerabbit-gcode-sender/blob/master/Images/HoleCenterFinder.png "Hole Center Finder")


Changelog
------
1.1.8 -> 1.1.9
* Keyboard Jogging Implemented

1.1.7 -> 1.1.8
* New GCode Sender Settings window implemented

1.1.6 -> 1.1.7
* Supports GRBL v1.1
* New GCode optimizer

1.1.5 -> 1.1.6
* Changes for better Touch Probe Operations
* Small UI changes

1.1.4 -> 1.1.5
* Small UI changes

1.1.3 -> 1.1.4
* Small UI changes
* Hole Center Finder implemented

1.1.2 -> 1.1.3
* Small UI changes
* About form implemented
* Check for updates procedure implemented

1.1.1 -> 1.1.2
* Small UI changes
* Better Touch Probe algorithm/sequence

1.1.0 -> 1.1.1
* Serial communication procedure optimized
* Small UI changes

1.0.9 -> 1.1.0
* Added appropriate UI to use touch probe and touch plates

1.0.8 -> 1.0.9
* Tool to set Machine Position implemented 

1.0.7 -> 1.0.8
* UI bugs fixed 

1.0.6 -> 1.0.7
* GRBL settings form editor implemented
* Events for machine status implemented (UI)

1.0.5 -> 1.0.6
* Console is now showing more info (comments, GRBL Controller messages etc...)

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
