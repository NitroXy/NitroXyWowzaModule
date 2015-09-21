NitroXy Wowza Module
===================

NitroXy Media's module for Wowza.

* Stream switching
* Remote control via json (and website for doing that)

Future features
--------------
Access control by ip

Setup
======
* To build: Use Wowza IDE
* To install: Put as a module in application and copy nitroxy.conf to conf/{app}/

Compiling
=========

* Install Eclipse and [Wowza IDE](http://www.wowza.com/streaming/developers/wowza-ide-software-update)
* Put the repo in a folder called NitroxyWowzaModule (case sensitive due to the way wowza builds modules)
* Add a new "Wowza Media Server Java Project" with these settings:  
Project name: NitroxyWowzaModule  
Wowza location: /usr/local/WowzaStreamingEngine  
Package: com.nitroxy.wmz.module  
Name: NitroXyModule
* Also ensure the java compatibility level is at least 1.6 (it is stored both globally for eclipse and per project)
