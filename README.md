SourceRabbit GCode Sender
------
<p align="center">
<a href="https://www.sourcerabbit.com/Shop/"><img src="https://raw.githubusercontent.com/nsiatras/sourcerabbit-gcode-sender/master/Images/GitHubPageBanner.png" alt="SourceRabbit.com"></a>
</p>

Downloads
------

To download the latest release visit:<br>
[https://www.sourcerabbit.com/](https://www.sourcerabbit.com/Shop/pr-i-80-t-grbl-gcode-sender.htm)<br>


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
* Support all GRBL CNC Routers, Lasers, Plasma Cutters and Milling Machines
* Can be used on computers with small amount of RAM and CPU.
