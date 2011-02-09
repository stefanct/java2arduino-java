/** Provides classes to interact with AVR microcontrollers via a serial link over bluetooth.

 j2arduino and its counterpart arduino2j (implemented in C) allow to
 communicate between Java enabled devices (standard edition as well as mobile edition)
 and <a href="http://www.arduino.cc/en/Guide/ArduinoBT">ArduinoBTs</a>.
 in a RMI-like fashion. {@link ArduinoGateway} can be queried for Bluetooth
 device in range, which will be represented as {@link Arduino} objects.
 After successfully connecting to an %Arduino, Java code can send arguments in the form
 of a byte array (up to 255 Bytes/{@link Arduino.A2J_MAX_PAYLOAD} long) to specially crafted C functions
 on the microcontroller just by specifying
 the target function (by its string representation/name) and the byte array payload.
 Transactions can be done \link arduinoSync synchronously\endlink and
 \link arduinoAsync asynchronously\endlink.
 There exist also methods for \ref j2amany "bigger payloads".

 {@link ArduinoActivityListener} allows implementers to monitor Bluetooth activities of
 any of the j2arduino classes. Also it is possible to read out static properties from remote devices
 (e.g. characteristics that vary from device to device but have to be taken into account by a
 single Java program) with the help of {@link ArduinoProperties}.


 <h2>License</h2>

 <pre>
 Copyright (C) 2009 Stefan Tauner

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 </pre>

 To read the full copy of the GNU General Public License version 3
 see <a href="license.html">License</a>.

 <h2>Related Documentation</h2>

 See also the documentation of the counterpart \ref arduino2j.c "arduino2j". */
package j2arduino;