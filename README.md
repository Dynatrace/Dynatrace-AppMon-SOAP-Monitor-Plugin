<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>SOAP Monitor Plugin</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=EmulateIE8" />
    <meta content="Scroll Wiki Publisher" name="generator"/>
    <link type="text/css" rel="stylesheet" href="css/blueprint/liquid.css" media="screen, projection"/>
    <link type="text/css" rel="stylesheet" href="css/blueprint/print.css" media="print"/>
   <link type="text/css" rel="stylesheet" href="css/content-style.css" media="screen, projection, print"/>
    <link type="text/css" rel="stylesheet" href="css/screen.css" media="screen, projection"/>
    <link type="text/css" rel="stylesheet" href="css/print.css" media="print"/>
</head>
<body>
                <h1>SOAP Monitor Plugin</h1>
    <div class="tablewrap">
        <table>
<thead class=" "></thead><tfoot class=" "></tfoot><tbody class=" ">    <tr>
            <td rowspan="1" colspan="1">
        <p>
Name    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
<strong class=" ">SOAP Monitor Plugin</strong>    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Description    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
The SOAP Monitor verifies availability, content, and access time for specified SOAP requests to ensure the availability of SOAP Services. It supports secure communication and HTTP proxies.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Plug-In Version    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
1.0    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Compatible with    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
dynaTrace 4    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Author    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Chuck Miller (<a href="mailto:chuck.miller@dynatrace.com">chuck.miller@dynatrace.com</a>)    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
License    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
<a href="attachments_5275722_2_dynaTraceBSD.txt">dynaTrace BSD</a>    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Support    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
<a href="https://community/display/DL/Support+Levels#SupportLevels-Community">Not Supported </a>    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Downloads    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
<a href="attachments_62160901_1_com.dynatrace.diagnostics.plugin.SOAPMonitor_4.0.0.2599.jar">SOAP Monitor Plugin</a><br/><a href="attachments_62160904_1_WebSphere_MessageBroker.profile.xml">Sample System Profile</a><br/><a href="attachments_62160905_1_SOAPMonitor_Dashboard.dashboard.xml">Sample Dashboard</a>    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Technical overview    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
This monitor plugin provides the following measures:    </p>
<ul class=" "><li class=" ">    <p>
ConnectionCloseDelay    </p>
</li><li class=" ">    <p>
ContentVerified    </p>
</li><li class=" ">    <p>
FirstResponseDelay    </p>
</li><li class=" ">    <p>
HeaderSize    </p>
</li><li class=" ">    <p>
HostReachable    </p>
</li><li class=" ">    <p>
HttpStatusCode    </p>
</li><li class=" ">    <p>
ResponseCompleteTime    </p>
</li><li class=" ">    <p>
ResponseSize    </p>
</li><li class=" ">    <p>
Throughput    </p>
</li></ul>            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Install Description    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Import the Plugin into the dynaTrace Server. For details how to do this please refer to the <a href="https://community/display/DOCDT40/Plugin+Management">dynaTrace  documentation</a>.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Configuration    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
The SOAP Monitor is derived from the <a href="https://community/display/DOCDT40/URL+Monitor">URL Monitor</a> and requires similar configuration information such as protocol, port, path, and host(s) to monitor. In addition, authorization credentials and proxy information can be specified.<br/><br/>Furthermore, SOAP envelope data to be sent with a POST request has to be specified. As an example, we provide a sample <a href="attachments_62160904_1_WebSphere_MessageBroker.profile.xml">system profile</a> and a <a href="attachments_62160905_1_SOAPMonitor_Dashboard.dashboard.xml">dashboard</a> configured to monitor WebSphere Message Broker.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Screenshots    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Showing a dashboard with some of the monitor's measures and the configuration dialogs:    </p>
    <div class="tablewrap">
        <table>
<thead class=" "></thead><tfoot class=" "></tfoot><tbody class=" ">    <tr>
            <td rowspan="1" colspan="1">
        <p>
            <img src="images_community/download/attachments/61833233/soapmonitordashboard.png" alt="images_community/download/attachments/61833233/soapmonitordashboard.png" class="" />
            </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
                </td>
        </tr>
</tbody>        </table>
            </div>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Known Problems    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
    </p>
            </td>
        </tr>
</tbody>        </table>
            </div>
            </div>
        </div>
        <div class="footer">
        </div>
    </div>
</body>
</html>
