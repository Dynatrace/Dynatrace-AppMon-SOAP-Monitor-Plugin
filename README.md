# SOAP Monitor Plugin

## Overview
The SOAP Monitor verifies availability, content, and access time for specified SOAP requests to ensure the availability of SOAP Services. It supports secure communication and HTTP proxies.


## Plugin Details

| Name |SOAP Monitor Plugin
| :--- | :---
| Author | Chuck Miller ([chuck.miller@dynatrace.com](mailto:chuck.miller@dynatrace.com))
| Supported dynaTrace Version | >= 5.5
| License | [dynaTrace BSD](dynaTraceBSD.txt)
| Support | [Not Supported ](https://community.compuwareapm.com/community/display/DL/Support+Levels#SupportLevels-Community)
| Release History | 1.0
| Downloads | [SOAP Monitor Plugin](com.dynatrace.diagnostics.plugin.SOAPMonitor_4.0.0.2599.jar)  
|| [Sample System Profile](WebSphere_MessageBroker.profile.xml)  
|| [Sample Dashboard](SOAPMonitor_Dashboard.dashboard.xml)

##Technical overview

This monitor plugin provides the following measures:

  * ConnectionCloseDelay 

  * ContentVerified 

  * FirstResponseDelay 

  * HeaderSize 

  * HostReachable 

  * HttpStatusCode 

  * ResponseCompleteTime 

  * ResponseSize 

  * Throughput 

##Install Description

Import the Plugin into the dynaTrace Server. For details how to do this please refer to the [dynaTrace documentation](https://community.compuwareapm.com/community/display/DOCDT40/Plugin+Management).

##Configuration

The SOAP Monitor is derived from the [URL Monitor](https://community.compuwareapm.com/community/display/DOCDT40/URL+Monitor) and requires similar configuration information such as protocol, port, path, and host(s) to monitor.
In addition, authorization credentials and proxy information can be specified.  
  
Furthermore, SOAP envelope data to be sent with a POST request has to be specified. As an example, we provide a sample [system profile](WebSphere_MessageBroker.profile.xml) and
a [dashboard](SOAPMonitor_Dashboard.dashboard.xml) configured to monitor WebSphere Message Broker.

##Screenshots

Showing a dashboard with some of the monitor's measures and the configuration dialogs:

![images_community/download/attachments/61833233/soapmonitordashboard.png](images_community/download/attachments/61833233/soapmonitordashboard.png)


