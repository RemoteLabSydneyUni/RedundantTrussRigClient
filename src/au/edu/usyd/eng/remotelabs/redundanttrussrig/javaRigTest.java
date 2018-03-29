package au.edu.usyd.eng.remotelabs.redundanttrussrig;
import java.io.*;
import java.util.Enumeration;

import gnu.io.CommPortIdentifier; 
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent; 
import gnu.io.SerialPortEventListener;
import java.util.Enumeration;

class javaRigTest {
	private static final String PORT_NAMES[] = {
		"/dev/tty.usbserial-A9007UX1", // Mac OS X
		"/dev/ttyACM0", // Raspberry Pi
		"/dev/ttyUSB0", // Linux
		"COM8", // Windows
	};
	public static void main(String arg[]){
		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
		try{
			while (portEnum.hasMoreElements()) {
				CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
				System.out.println(currPortId.getName());
				System.out.println(currPortId.isCurrentlyOwned());
				for (String portName : PORT_NAMES) {
					if (currPortId.getName().equals(portName)) {
						portId = currPortId;
						System.out.println("OK");
						break;
					}
				}
			}
		}catch (Exception e) {
			System.err.println(e.toString());
		}
	}
}

