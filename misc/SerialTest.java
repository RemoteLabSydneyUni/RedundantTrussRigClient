import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import gnu.io.CommPortIdentifier; 
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent; 
import gnu.io.SerialPortEventListener; 
import java.util.Enumeration;



public class SerialTest implements SerialPortEventListener {

	SerialPort serialPort;

	/** The port we're normally going to use. */
	private static final String PORT_NAMES[] = { 
			"/dev/tty.usbserial-A9007UX1", // Mac OS X
                        "/dev/ttyACM0", // Raspberry Pi
			"/dev/ttyUSB0", // Linux
			"COM5", // Windows
	};


    private BufferedReader input;
    private static OutputStream output;
    private static final int TIME_OUT = 2000;
    private static final int DATA_RATE = 115200;
    
    String inputLine;
    boolean inputLineComplete = false;

    public void initialize() {

		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		//First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			System.out.println("Could not find COM port.");
			return;
		}

		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			// open the streams
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();

            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);

		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}


    public synchronized void serialEvent(SerialPortEvent oEvent) {
        if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                inputLine=input.readLine();
                setDataReady(true);
            } catch (Exception e) {
                System.err.println(e.toString());
            }
        }
    }


    public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}


    public synchronized void send(int b){
        try{
            output.write(b);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    
    public synchronized int read(){
        int b = 0;

        try{
            b = (int)input.read();
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
        return b;
    }


    private synchronized boolean getDataReady() {
    	return inputLineComplete;
    }
    
    private synchronized void setDataReady(boolean val) {
    	inputLineComplete = val;
    }
    
    private String sendCmd(String cmd) {
		String response="";
		
		try {
			System.out.print("Sending Cmd=");
			System.out.println(cmd);
			output.write(cmd.getBytes());
			output.write('\n');
			output.flush();
			
			while (!getDataReady()) {
				Thread.sleep(100);
			}
			setDataReady(false);
			
			System.out.print("Received Rsp=");
			System.out.println(inputLine);
		 } catch (Exception e) {
			 System.out.println("could not write to port");
             System.err.println(e.toString());
		 }
		return response;
	}
	
	public void runCmds() {
				
		String response;
		try {
			Thread.sleep(2000);
			for (int i=0; i<3; i++) {
				System.out.println("Sending");
				response=sendCmd("rlab://13fl?02000");
				response=sendCmd("rlab://13on");
				response=sendCmd("rlab://13off");
				System.out.println("Going to sleep");
				Thread.sleep(2000);
				System.out.println("Woke up");
			}
		 } catch (Exception e) {
			 System.out.println("could not write to port");
		 }
	}

	public static void main(String[] args) throws Exception {
		
		try {
			SerialTest main = new SerialTest();
			main.initialize();
			main.runCmds();
			main.close();
		}
		catch(Exception e){}
	}
}
