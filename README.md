SourceRabbit GCode Sender
------
<p align="center">
![SourceRabbit.com](https://raw.githubusercontent.com/nsiatras/sourcerabbit-gcode-sender/master/Images/SourceRabbit.png "SourceRabbit.com")
</p>

SourceRabbit GCode Sender is a GRBL compatible, cross platform G-Code sender written in Java. It features a highly optimized and asynchronous (event-driven) UI and USB-to-Serial communication and can be also used on computers with small amount of RAM and CPU.

To run simply download and unzip the .zip file and double click the SourceRabbit-GCODE-Sender.jar file. On some platforms you will need to run an included start script.

Note for MAC users: You may need to create a "/var/lock" directory on OSX to fix a bug in the serial library. To do this open the Terminal application and run the following two commands: 
sudo mkdir /var/lock 
sudo chmod 777 /var/lock 

Technical details:
* Compatible only with GRBL 0.9 and later versions
* Uses JSSC for serial communication
* Event-Driven UI and USB-to-Serial communication
* Developed with NetBeans 8.0.2
* To build you need to open the project in Netbeans and just... build.

Goals:
* SourceRabbit GCode Sender's primary goal is to provide an easy to use, fast and accurate GCode sender software for all GRBL compatible CNC router and milling machines that can run on every computer you can get (included those with small amount of ram and slow cpus). 

Downloads
------

[1.0.2](https://github.com/nsiatras/sourcerabbit-gcode-sender/releases/download/1.0.2/1.0.2.zip) - Requires Java 7 or higher.


![Connect to your CNC!](https://github.com/nsiatras/sourcerabbit-gcode-sender/blob/master/Images/ConnectForm.png "Connect to your CNC!")

![CNC Control Form](https://github.com/nsiatras/sourcerabbit-gcode-sender/blob/master/Images/ControllForm.png "CNC Control Form")



Changelog
------
1.0.1 -> 1.0.2
* Fixed several UI bugs.
* Information Message box on GCode cycle finish.

1.0.0 -> 1.0.1
* Clicking the cancel button during GCode cycle stops the machine instantly.
* GCode cycle events implemented.
* GCode Sender can identify if the USB cable is plugged or not.
* Automatically skip blank lines and comments when sending a file.
