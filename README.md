SourceRabbit GCode Sender
------
<p align="center">
<a href="https://www.sourcerabbit.com/Shop/"><img src="https://raw.githubusercontent.com/nsiatras/sourcerabbit-gcode-sender/master/Images/GitHubPageBanner.png" alt="SourceRabbit.com"></a>
</p>

Join our Discord Server:<br> 
![Discord Shield](https://discordapp.com/api/guilds/952140843546972161/widget.png?style=shield)

End of Life - 13/Jun/2021
------
The course of SourceRabbit GCode Sender began in 2015 when Nikos Siatras (<a href="https://github.com/nsiatras">GitHub</a>, <a href="https://twitter.com/nsiatras">Twitter</a>), CEO of SourceRabbit, decided to build the controllers of the company's CNC machines, on the GRBL firmware.

From 2015 until 2021, the GCode Sender was improved and acquired new features that were fully compatible with the CNC machines we manufacture. At the beginning of 2021 we started to build a new multi-axis software, <a href="https://www.sourcerabbit.com/Shop/pr-i-91-t-focus-cnc-control-software.htm">Focus</a>, which replaced the  "classic" GCode Sender and which we will fully support from now on.

This GitHub repository will remain available to anyone who wants to see or use the code but unfortunately we will stop supporting it.

Thank you for your support<br>
The SourceRabbit Team<br>


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
