# README #

This Android Application has been developed as a course project at the University of Oulu during academic year 2015-2016. All the code produced during the course is the property of their respective authors. Use without written consent is not permitted.

### CONTRIBUTORS ###

First member: Aapo Keskimölö, 1832390, aapokesk@gmail.com, akeskimo

### REPOSITORY ###

* Repository for OHAP Client 13 that can be used to monitor and actuate in-house devices

### REQUIREMENTS ###
* Java JDK 1.7
* Minimum Android SDK 15 (target SDK21)
* Android phone or Emulator API15+
* AppCombat 7.21

### BUILD INSTRUCTIONS ###
* Build 'app' / Make project in Android Studio

### INSTRUCTIONS FOR TESTING THE APP ON OHAP SERVER ###
* Run app on Android device or built-in emulator (>API15)
* Press "ONLINE" on start-up
* Start OHAP Server by executing
    java -jar ohap-tcp-server\bin\jar\tcp-ohap-server.jar
  or by running Windows batch file
    start_tcp_ohap_server.bat
* Make sure that both OHAP App and Server are connected to the same Local Area Network (LAN)
* Check LOCALHOST address (ipconfig/ifconfig) and configure the : 
  settings -> Auto-connect -> Enabled
  settings -> Server -> http://192.168.XXX.XXX
  settings -> Port -> 18001

### INSTRUCTIONS WITH SIMULATION ONLY ###
* Run app on Android device or built-in emulator (>API15)
* Press "SIMULATION" on start-up

### TEST RESULTS ###

* The application has been tested and verified on Nexus 4 API15 emulator and HTC Android Device
* Supports OHAP protocol version 0.3.1

### Who do I talk to? ###

* Aapo Keskimolo (repository owner and developer)
