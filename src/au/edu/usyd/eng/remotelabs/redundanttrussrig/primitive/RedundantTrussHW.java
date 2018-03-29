package au.edu.usyd.eng.remotelabs.redundanttrussrig.primitive;

import java.io.*;

import gnu.io.CommPortIdentifier; 
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent; 
import gnu.io.SerialPortEventListener; 
import java.util.Enumeration;
import java.util.StringTokenizer;

import au.edu.usyd.eng.remotelabs.redundanttrussrig.primitive.RedundantTrussHW;
import au.edu.uts.eng.remotelabs.rigclient.util.ConfigFactory;
import au.edu.uts.eng.remotelabs.rigclient.util.IConfig;
import au.edu.uts.eng.remotelabs.rigclient.util.ILogger;
import au.edu.uts.eng.remotelabs.rigclient.util.LoggerFactory;
import java.util.*;

import java.net.URL;
import java.net.URLDecoder;



public class RedundantTrussHW extends Thread implements SerialPortEventListener {

	private boolean running = true;
	private String threadName;
    private int sleepmSec = 50;
	
	//Arduino variable - Data Acquisition
    private volatile int daq_mode;
	private volatile float[] daq_strain = new float[10];
	private volatile float[] strain_base = new float[10];
	private volatile int[] daq_health = new int[10];
	private volatile int daq_watchdog;
	
	// Arduino command buffer & flag - Data Acquisition
	private volatile float[] daq_scale = new float[10];
	private volatile int[] daq_offset = new int[10];
	
	//Arduino variable - Control
	private volatile int ctrl_mode;
	private volatile int ctrl_angleMode;
	private volatile int ctrl_loadMode;
	private volatile int ctrl_curDistance;
	private volatile int ctrl_tgtDistance;

	private volatile float ctrl_curLoad;
	private volatile float ctrl_tgtLoad;
	private volatile int ctrl_loadHealth;
	private volatile int ctrl_angleHealth;
	private volatile int ctrl_watchdog;
	
	private volatile float ctrl_scale;
	private volatile int ctrl_offset;
	
	// Arduino command buffer & flag - Control

	
	private volatile int ctrl_angleStepBuffer;
	private volatile boolean ctrl_angleStepFlag;
	private volatile int ctrl_loadStepBuffer;
	private volatile boolean ctrl_loadStepFlag;
	private volatile float ctrl_tgtLoadBuffer;
	private volatile boolean ctrl_tgtLoadFlag;
	private volatile int ctrl_tgtDistanceBuffer;
	private volatile boolean ctrl_tgtDistanceFlag;
	
	private volatile int ctrl_modeBuffer;
	private volatile boolean ctrl_modeFlag;
	private volatile int daq_modeBuffer;
	private volatile boolean daq_modeFlag;
	
	private volatile boolean daq_updateFileFlag;
	private volatile boolean ctrl_updateFileFlag;
	private volatile boolean daq_fileReadyFlag;
	private volatile boolean ctrl_fileReadyFlag;
	private volatile boolean writeToConfigFlag;

	
	// Internal variable - local calculation
	private volatile int curDistance;
	private volatile int tgtDistance;
	private volatile float curAngle;
	private volatile float tgtAngle;
	private volatile float curLoad;
	private volatile float tgtLoad;
	private volatile int wdDaq;
	private volatile int wdCtrl;
	private volatile int stepsize;
	private volatile int refDistance = 150;					// Built-in default, in case the file reading fails
	
	// hardware Specs
	private static final float SCREW_HEIGHT = 145.0f;		// Height from truss load point to pulley vertically
	private static final float PITCH_VAL = 8.0f;			// Pitch for the screw
	private static final float STEP_REV = 400.0f;			// steps per revolution
	
    ILogger logger;

    // Arduino interfacing
	SerialPort daq_serialPort;
	SerialPort ctrl_serialPort;
	
	/** The port we're normally going to use. */
	private static final String PORT_NAMES[] = {
//		"/dev/tty.usbserial-A9007UX1", // Mac OS X
//        "/dev/ttyACM0", // Raspberry Pi
//        "/dev/ttyUSB0", // Linux
		"COM8", // Windows
	};
	
    private BufferedReader daq_serial_input;
    private BufferedReader ctrl_serial_input;
    private static OutputStream daq_output;
    private static OutputStream ctrl_output;
    private static final int TIME_OUT = 2000;
    private static final int DATA_RATE = 115200;
    
    private static final String DAQ_SERIAL = "DAQ";
    private static final String CTRL_SERIAL = "CTRL";
    
//    String inputLine;
//    boolean inputLineComplete = false;

    // Config file handling
    private final IConfig config;
    String rigname = "dontknowyet";
    String daqConfigpath = "empty";
    String ctrlConfigpath = "empty";
    String daqPortNameFromConfig = "empty";
    String ctrlPortNameFromConfig = "empty"; 
        
    //------------------------------------------------------------------
    // HW module constructor
	RedundantTrussHW( String name){
		threadName = name;
        this.config = ConfigFactory.getInstance();
        
        rigname = config.getProperty("Rig_Name");
        daqConfigpath = config.getProperty("Daq_Config_File_Path");
        ctrlConfigpath = config.getProperty("Ctrl_Config_File_Path");
        daqPortNameFromConfig = config.getProperty("COM_Port_Daq");
        ctrlPortNameFromConfig = config.getProperty("COM_Port_Ctrl");
        
        
        daq_watchdog = 0;
        wdDaq = 999;
        ctrl_watchdog = 0;
        wdCtrl = 999;
        
        curDistance = 0;
        tgtDistance = 0;
        curAngle = 0.0f;
        tgtAngle = 0.0f;
        curLoad = 0.0f;
        tgtLoad = 0.0f;
        stepsize = 0;
        
        daq_modeBuffer = 0;
        ctrl_modeBuffer = 0;

        ctrl_tgtDistanceBuffer = 0;
        ctrl_tgtLoadBuffer = 0.0f;
        ctrl_loadStepBuffer = 0;
        ctrl_angleStepBuffer = 0;
        
        daq_modeFlag = false;
        ctrl_modeFlag = false;

        ctrl_tgtDistanceFlag = false;
        ctrl_tgtLoadFlag = false;
        ctrl_angleStepFlag = false;
        ctrl_loadStepFlag = false;
         
        daq_updateFileFlag = false;
        ctrl_updateFileFlag = false;
        daq_fileReadyFlag = false;
        ctrl_fileReadyFlag = false;
        
        writeToConfigFlag = false;

        for (int i = 0; i<10; i++){
        	strain_base[i] = 0.0f;
        }
	}
	
    //------------------------------------------------------------------
    // HW module main operations
	
	public void run() {

		String response="";
	    logger = LoggerFactory.getLoggerInstance();
        logger.info("Primitive Controller HW Interface created");
        
        // Construct file path of the configuration file
        String pathRaw = RedundantTrussHW.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String pathFolder = pathRaw.substring(0,pathRaw.lastIndexOf("/"));
        String pathJar = "";
        try{
        	pathJar = URLDecoder.decode(pathFolder, "UTF-8");
        } catch (UnsupportedEncodingException e){
        	pathJar = "NAN";
        }
        String daq_pathFile = pathJar +"/" + daqConfigpath;
        String ctrl_pathFile = pathJar +"/" + ctrlConfigpath;
        logger.debug("Primitive Controller HW Interface - Daq Config File Path = "+daq_pathFile);
        logger.debug("Primitive Controller HW Interface - Ctrl Config File Path = "+ctrl_pathFile);
        
        // initialize serial ports
        serialInitialise();
		
        // Give serial port time to start
        try {
			Thread.sleep(5000);
		 } catch (Exception e) {
             System.err.println(e.toString());
		 }
		
		logger.info("Primitive Controller HW Interface - Initialized");
		
		try {
			while (getRunning()) {
				// update value from Arduino
				updateValue();
				Thread.sleep(10);
				// Handshake with Arduino - Mode management
				if (getDaqMode() == 0){
					setDaqModeBuffer(11);
					setDaqFileFlag();
				}else if (getDaqMode() == 11){
					if (getDaqFileReadyFlag()){
						for (int i=0;i<10;i++){
							pushDaqScale(i);
							Thread.sleep(10);
						}
						for (int i=0;i<10;i++){
							pushDaqOffset(i);
							Thread.sleep(10);
						}
						offDaqFileReadyFlag();
					}
				}else if (getDaqMode() == 14){
					setDaqModeBuffer(20);
					setBaseStrain();
				}else if (getDaqMode() == 20){
					// normal operation
				}
				Thread.sleep(10);
				if (getCtrlMode() == 0){
					setCtrlModeBuffer(11);
					setCtrlFileFlag();
				}else if (getCtrlMode() == 11){
					if (getCtrlFileReadyFlag()){
						pushCtrlScale();
						Thread.sleep(10);
						pushCtrlOffset();
						Thread.sleep(10);
						offCtrlFileReadyFlag();
					}
				}else if (getCtrlMode() == 13){
					setCtrlModeBuffer(20);
				}else if (getCtrlMode() == 20){
					// Normal Operation
				}else if (getCtrlMode() == 31){
					// cleanup
				}
				Thread.sleep(10);
				// file handling
				if (getDaqFileFlag()){
					//Retrieve info from file
					try {
						BufferedReader daq_input = new BufferedReader(new InputStreamReader(new FileInputStream(daq_pathFile)));
						logger.debug("Primitive Controller HW Interface - Daq Config File Identified");
			            // Get file info - line by line
			            while ( daq_input.ready() )
			            {
			               String daq_nextline = daq_input.readLine();
			               logger.debug(daq_nextline);
			               if (daq_nextline == null) continue;
			               // Break the line down
			               StringTokenizer daq_tokens = new StringTokenizer (daq_nextline);
			               int daq_numargs = daq_tokens.countTokens();
			               if ( daq_numargs == 0 ) continue;
			               String daq_attribute = daq_tokens.nextToken();
			               if (daq_attribute.equals("#")) continue;
			               // Check the attribute
			               if (daq_attribute.equals("Scale")){
			            	   int scale_index = (Integer.valueOf(daq_tokens.nextToken()).intValue())-1;
			            	   setDaqScale(scale_index, Float.parseFloat(daq_tokens.nextToken()));
			               }else if (daq_attribute.equals("Offset")){
			            	   int offset_index = (Integer.valueOf(daq_tokens.nextToken()).intValue())-1;
			            	   setDaqOffset(offset_index, Integer.parseInt(daq_tokens.nextToken()));
			               }else if (daq_attribute.equals("LoadScale")){
			            	   setCtrlScale(Float.parseFloat(daq_tokens.nextToken()));
			               }else if (daq_attribute.equals("LoadOffset")){
			            	   setCtrlOffset(Integer.parseInt(daq_tokens.nextToken()));
			               }
			            }
			            logger.info("Primitive Controller HW Interface - Daq Config Extracted");
			            daq_input.close();
			         // update flag
						setDaqFileReadyFlag();
						offDaqFileFlag();
					} catch (IOException e) {
						logger.error("Primitive Controller HW Interface - Cannot get Daq Config file");
					}
					
				}
				Thread.sleep(10);
				if (getCtrlFileFlag()){
					//Retrieve info from file
					try {
						BufferedReader ctrl_input = new BufferedReader(new InputStreamReader(new FileInputStream(ctrl_pathFile)));
						logger.debug("Primitive Controller HW Interface - Ctrl Config File Identified");
			            // Get file info - line by line
			            while ( ctrl_input.ready() )
			            {
			               String ctrl_nextline = ctrl_input.readLine();
			               if (ctrl_nextline == null) continue;
			               logger.debug(ctrl_nextline);
			               // Break the line down
			               StringTokenizer ctrl_tokens = new StringTokenizer (ctrl_nextline);
			               int ctrl_numargs = ctrl_tokens.countTokens();
			               if ( ctrl_numargs == 0 ) continue;

			               String ctrl_attribute = ctrl_tokens.nextToken();
			               if (ctrl_attribute.equals("#")) continue;

			               // Check the attribute
			               if (ctrl_attribute.equals("Reference_Distance")){
			            	   setRefDistance(Integer.parseInt(ctrl_tokens.nextToken()));
			               }
			            }
			            logger.info("Primitive Controller HW Interface - Ctrl Config Extracted");
			            ctrl_input.close();
			         // update flag
						setCtrlFileReadyFlag();
						offCtrlFileFlag();
					} catch (IOException e) {
						logger.error("Primitive Controller HW Interface - Cannot get Control Config file");
			        }
					
				}
				Thread.sleep(10);
				// command handling
				
				if (getTgtDistanceFlag()){
					// Actions
					response = pushTgtDistance();
					// update flag
					offTgtDistanceFlag();
				}
				Thread.sleep(10);
				if (getTgtLoadFlag()){
					// Actions
					response = pushTgtLoad();
					// update flag
					offTgtLoadFlag();
				}
				Thread.sleep(10);
				if (getDaqModeFlag()){
					// Actions
					response = pushDaqMode();
					// update flag
					offDaqModeFlag();
				}
				Thread.sleep(10);
				if (getCtrlModeFlag()){
					// Actions
					response = pushCtrlMode();
					// update flag
					offCtrlModeFlag();
				}
				Thread.sleep(10);
				if (getLoadStepFlag()){
					// Actions
					response = pushLoadStep();
					// update flag
					offLoadStepFlag();
				}
				if (getAngleStepFlag()){
					// Actions
					response = pushAngleStep();
					// update flag
					offAngleStepFlag();
				}
				
				if (getWriteToConfigFlag()){
					try {
						BufferedReader CleanupIn = new BufferedReader(new InputStreamReader(new FileInputStream(ctrl_pathFile)));
						logger.debug("Primitive Controller HW Interface - Config File Identified");
						String cleanupOut = "Reference_Distance " + String.valueOf(getRefDistance()) + System.getProperty("line.separator");
			            // Get file info - line by line
						FileOutputStream fileOut = new FileOutputStream(ctrl_pathFile);
				        fileOut.write(cleanupOut.getBytes());
				        fileOut.close();
			            logger.info("Primitive Controller HW Interface - Config file updated");
	
					} catch (IOException e) {
						logger.error("Primitive Controller HW Interface - Cannot get Config file");
						logger.error("Primitive Controller HW Interface - " + e.toString());
			        }
					offWriteToConfigFlag();
				}

				Thread.sleep(10);
				// Watchdog updates
				refreshWatchdog();
				
				Thread.sleep(100);
			}
			logger.info("Primitive Controller HW Interface - Time to end=");
			pushCleanup();
			serialClose();
			logger.info("Primitive Controller HW Interface - Serial port closed=");

		} catch (InterruptedException e) {
			logger.error("Primitive Controller HW Interface - Running exception");
			// System.out.println("Thread " +  threadName + " interrupted.");
		}
        logger.info("Primitive Controller HW Interface - Thread " +  threadName + " exiting.");
	}
	
	//------------------------------------------------------------------
    // Operation support functions (private access)
	//------------------------------------------------------------------
	// update local variable with live data from Arduino
	private String updateValue(){
		String daq_msg1 = "";
		String daq_msg2 = "";
		String daq_msg3 = "";
		String ctrl_msg1 = "";
		String ctrl_msg2 = "";
		String ctrl_msg3 = "";
		
		String daq_feedback1 = "";
		String daq_feedback2 = "";
		String daq_feedback3 = "";
		String ctrl_feedback1 = "";
		String ctrl_feedback2 = "";
		String ctrl_feedback3 = "";
		String[] daq_response1 = new String[5];
		String[] daq_response2 = new String[5];
		String[] daq_response3 = new String[12];
		String[] ctrl_response1 = new String[3];
		String[] ctrl_response2 = new String[3];
		String[] ctrl_response3 = new String[5];
		try{
			daq_msg1 = sendCmd("rlab://REQV?addr=01",DAQ_SERIAL);
			if (daq_msg1.startsWith("Cpl")){
				daq_response1 = daq_msg1.substring(4).trim().split(";",5);
				for (int i=0;i<5;i++){
					setDaqStrain(i,Float.parseFloat(daq_response1[i]));
				}
				daq_feedback1 = "Cpl";
			}else{
				daq_feedback1 = "Err";
			}
			Thread.sleep(sleepmSec);
			
			daq_msg2 = sendCmd("rlab://REQV?addr=02",DAQ_SERIAL);
			if (daq_msg2.startsWith("Cpl")){
				daq_response2 = daq_msg2.substring(4).trim().split(";",5);
				for (int i=0;i<5;i++){
					setDaqStrain(i+5,Float.parseFloat(daq_response2[i]));
				}
				daq_feedback2 = "Cpl";
			}else{
				daq_feedback2 = "Err";
			}
			Thread.sleep(sleepmSec);
			
			daq_msg3 = sendCmd("rlab://REQV?addr=03",DAQ_SERIAL);
			if (daq_msg3.startsWith("Cpl")){
				daq_response3 = daq_msg3.substring(4).trim().split(";",12);
				for (int i=0;i<10;i++){
					setDaqHealth(i,Integer.parseInt(daq_response3[i]));
				}
				setDaqMode(Integer.parseInt(daq_response3[10]));
				setDaqWD(Integer.parseInt(daq_response3[11]));
				daq_feedback3 = "Cpl";
			}else{
				daq_feedback3 = "Err";
			}
			Thread.sleep(sleepmSec);
			
			ctrl_msg1 = sendCmd("rlab://REQV?addr=01",CTRL_SERIAL);
			if (ctrl_msg1.startsWith("Cpl")){
				ctrl_response1 = ctrl_msg1.substring(4).trim().split(";",3);
				setCtrlCurDistance(Integer.parseInt(ctrl_response1[0]));
				setCtrlTgtDistance(Integer.parseInt(ctrl_response1[1]));
				setCtrlAngleMode(Integer.parseInt(ctrl_response1[2]));
				ctrl_feedback1 = "Cpl";
			}else{
				ctrl_feedback1 = "Err";
			}
			Thread.sleep(sleepmSec);
			
			ctrl_msg2 = sendCmd("rlab://REQV?addr=02",CTRL_SERIAL);
			if (ctrl_msg2.startsWith("Cpl")){
				ctrl_response2 = ctrl_msg2.substring(4).trim().split(";",3);
				setCtrlCurLoad(Float.parseFloat(ctrl_response2[0]));
				setCtrlTgtLoad(Float.parseFloat(ctrl_response2[1]));
				setCtrlLoadMode(Integer.parseInt(ctrl_response2[2]));
				ctrl_feedback2 = "Cpl";
			}else{
				ctrl_feedback2 = "Err";
			}
			Thread.sleep(sleepmSec);
			
			ctrl_msg3 = sendCmd("rlab://REQV?addr=03",CTRL_SERIAL);
			if (ctrl_msg3.startsWith("Cpl")){
				ctrl_response3 = ctrl_msg3.substring(4).trim().split(";",5);
				setCtrlLoadHealth(Integer.parseInt(ctrl_response3[0]));
				setCtrlAngleHealth(Integer.parseInt(ctrl_response3[1]));
				setCtrlMode(Integer.parseInt(ctrl_response3[2]));
				setCtrlWD(Integer.parseInt(ctrl_response3[3]));
				ctrl_feedback3 = "Cpl";
			}else{
				ctrl_feedback3 = "Err";
			}
			Thread.sleep(sleepmSec);
			
			updateInternalCalc();
			
        }
        catch (Exception e) {
            System.err.println(e.toString());
            logger.error("Primitive Controller HW Interface - parsing data error");
            logger.error("Primitive Controller HW Interface - " + e.toString());
        }
        String feedback = daq_feedback1 + ";" + daq_feedback2 + ";" + daq_feedback3;
        feedback = feedback + ";" + ctrl_feedback1 + ";" + ctrl_feedback2 + ";" + ctrl_feedback3;
		return feedback;
	}
	
	private String pushDaqScale(int index){
		String feedback="";
		try{
			String msg = String.format("%f",getDaqScale(index));
            feedback = sendCmd("rlab://SETS?addr="+ String.valueOf(index+10) +"&val=" + msg,DAQ_SERIAL);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }		
		return feedback;
	}
	private String pushDaqOffset(int index){
		String feedback="";
		try{
			String msg = String.valueOf(getDaqOffset(index));
            feedback = sendCmd("rlab://SETO?addr="+ String.valueOf(index+10) +"&val=" + msg, DAQ_SERIAL);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }		
		return feedback;
	}
	private String pushDaqMode(){
		String feedback="";
		try{
			String msg = String.valueOf(getDaqModeBuffer());
            feedback = sendCmd("rlab://SETV?addr=01&val=" + msg, DAQ_SERIAL);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }		
		return feedback;
	}
	
	private String pushCtrlMode(){
		String feedback="";
		try{
			String msg = String.valueOf(getCtrlModeBuffer());
            feedback = sendCmd("rlab://SETV?addr=01&val=" + msg,CTRL_SERIAL);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }		
		return feedback;
	}

	
	private String pushTgtLoad(){
		String feedback="";
		try{
			String msg = String.format("%f",getTgtLoadBuffer());
            feedback = sendCmd("rlab://SETV?addr=11&val=" + msg,CTRL_SERIAL);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }		
		return feedback;
	}
	
	private String pushLoadStep(){
		String feedback="";
		try{
			String msg = String.valueOf(getLoadStepBuffer());
            feedback = sendCmd("rlab://SETV?addr=12&val=" + msg,CTRL_SERIAL);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }		
		return feedback;
	}
	
	private String pushCtrlScale(){
		String feedback="";
		try{
			String msg = String.format("%f",getCtrlScale());
            feedback = sendCmd("rlab://SETV?addr=13&val=" + msg,CTRL_SERIAL);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }		
		return feedback;
	}
	
	private String pushCtrlOffset(){
		String feedback="";
		try{
			String msg = String.valueOf(getCtrlOffset());
            feedback = sendCmd("rlab://SETV?addr=14&val=" + msg,CTRL_SERIAL);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }		
		return feedback;
	}
	
	private String pushTgtDistance(){
		String feedback="";
		try{
			String msg = String.valueOf(getTgtDistanceBuffer());
            feedback = sendCmd("rlab://SETV?addr=21&val=" + msg,CTRL_SERIAL);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }		
		return feedback;
	}
	private String pushAngleStep(){
		String feedback="";
		try{
			String msg = String.valueOf(getAngleStepBuffer());
            feedback = sendCmd("rlab://SETV?addr=22&val=" + msg,CTRL_SERIAL);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }		
		return feedback;
	}
	private String pushCleanup(){
		String feedback1="";
		String feedback2="";
		try{
			String msg = String.valueOf(31);
            feedback1 = sendCmd("rlab://SETV?addr=01&val=" + msg, DAQ_SERIAL);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
        try{
			String msg = String.valueOf(31);
            feedback2 = sendCmd("rlab://SETV?addr=01&val=" + msg,CTRL_SERIAL);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }	
		return feedback1 + System.getProperty("line.separator")+feedback2;
	}
	
	
	private synchronized void refreshWatchdog(){
		if (daq_watchdog > 500){
			wdDaq = 1;
		}else{
			wdDaq = 0;
		}
		if (ctrl_watchdog > 500){
			wdCtrl = 1;
		}else{
			wdCtrl = 0;
		}
		if (daq_watchdog < 9999){
			daq_watchdog++;
		}
		if (ctrl_watchdog < 9999){
			ctrl_watchdog++;
		}
	}
	
	//update local register from Serial data
	private synchronized void setDaqStrain(int index, float val){
		daq_strain[index]=val;
	}
	private synchronized void setDaqHealth(int index, int val){
		daq_health[index]=val;
	}
	private synchronized void setDaqMode(int val){
		daq_mode = val;
	}
	private synchronized void setDaqWD(int val){
		daq_watchdog = val;
	}
	private synchronized void setCtrlCurDistance (int val){
		ctrl_curDistance = val;
	}
	private synchronized void setCtrlTgtDistance (int val){
		ctrl_tgtDistance = val;
	}

	private synchronized void setCtrlCurLoad (float val){
		ctrl_curLoad = val;
	}
	private synchronized void setCtrlTgtLoad (float val){
		ctrl_tgtLoad = val;
	}
	private synchronized void setCtrlLoadMode (int val){
		ctrl_loadMode = val;
	}
	private synchronized void setCtrlAngleMode (int val){
		ctrl_angleMode = val;
	}
	private synchronized void setCtrlLoadHealth (int val){
		ctrl_loadHealth = val;
	}
	private synchronized void setCtrlAngleHealth (int val){
		ctrl_angleHealth = val;
	}
	private synchronized void setCtrlMode (int val){
		ctrl_mode = val;
	}
	private synchronized void setCtrlWD(int val){
		ctrl_watchdog = val;
	}
	
	// Internal Request Handling
	private synchronized void setDaqModeBuffer(int val){
		daq_modeBuffer = val;
		daq_modeFlag = true;
	}
	private synchronized void setCtrlModeBuffer(int val){
		ctrl_modeBuffer = val;
		ctrl_modeFlag = true;
	}
	
	private synchronized void updateInternalCalc(){
        curDistance = refDistance - ctrl_curDistance;
        tgtDistance = refDistance - ctrl_tgtDistance;
        curAngle = distanceToAngle(curDistance);
        tgtAngle = distanceToAngle(tgtDistance);
        curLoad = ctrl_curLoad;
        tgtLoad = ctrl_tgtLoad;
	}
	


	// buffer & flags methods - mode change to arduino (DAQ & Ctrl)
	private synchronized int getDaqModeBuffer(){ return daq_modeBuffer;}
	private synchronized int getCtrlModeBuffer(){ return ctrl_modeBuffer;}
	private synchronized boolean getDaqModeFlag(){ return daq_modeFlag;}
	private synchronized boolean getCtrlModeFlag(){ return ctrl_modeFlag;}
	private synchronized void offDaqModeFlag(){ daq_modeFlag = false;}
	private synchronized void offCtrlModeFlag(){ ctrl_modeFlag = false;}
	
	// buffer fetch methods - command to arduino (ctrl)
	private synchronized int getTgtDistanceBuffer(){ return ctrl_tgtDistanceBuffer;}
	private synchronized int getAngleStepBuffer(){ return ctrl_angleStepBuffer;}
	private synchronized float getTgtLoadBuffer(){ return ctrl_tgtLoadBuffer;}
	private synchronized int getLoadStepBuffer(){ return ctrl_loadStepBuffer;}
	
	// flag operation methods - command to arduino (ctrl)
	private synchronized boolean getTgtDistanceFlag(){ return ctrl_tgtDistanceFlag;}
	private synchronized boolean getAngleStepFlag(){ return ctrl_angleStepFlag;}
	private synchronized boolean getTgtLoadFlag(){ return ctrl_tgtLoadFlag;}
	private synchronized boolean getLoadStepFlag(){ return ctrl_loadStepFlag;}
	
	private synchronized void offTgtDistanceFlag(){ ctrl_tgtDistanceFlag = false;}
	private synchronized void offAngleStepFlag(){ ctrl_angleStepFlag = false;}
	private synchronized void offTgtLoadFlag(){ ctrl_tgtLoadFlag = false;}
	private synchronized void offLoadStepFlag(){ ctrl_loadStepFlag = false;}
	
	// load cell calibration info operation methods
	private synchronized float getDaqScale(int index){ return daq_scale[index];}
	private synchronized float getCtrlScale(){ return ctrl_scale;}
	private synchronized int getDaqOffset(int index){ return daq_offset[index];}
	private synchronized int getCtrlOffset(){ return ctrl_offset;}
	
	private synchronized void setDaqScale(int index, float val){ daq_scale[index] = val;}
	private synchronized void setDaqOffset(int index, int val){ daq_offset[index] = val;}
	private synchronized void setCtrlScale(float val){ ctrl_scale = val;}
	private synchronized void setCtrlOffset(int val){ ctrl_offset = val;}
	
	// reference distance info operation methods
	private synchronized void setRefDistance(int val){ refDistance = val;}
	
	// flag operation methods - file operation
	private synchronized boolean getDaqFileFlag(){ return daq_updateFileFlag;}
	private synchronized void setDaqFileFlag(){daq_updateFileFlag = true;}
	private synchronized void offDaqFileFlag(){daq_updateFileFlag = false;}
	private synchronized boolean getCtrlFileFlag(){ return ctrl_updateFileFlag;}
	private synchronized void setCtrlFileFlag(){ctrl_updateFileFlag = true;}
	private synchronized void offCtrlFileFlag(){ctrl_updateFileFlag = false;}
	private synchronized boolean getDaqFileReadyFlag(){ return daq_fileReadyFlag;}
	private synchronized void setDaqFileReadyFlag(){daq_fileReadyFlag = true;}
	private synchronized void offDaqFileReadyFlag(){daq_fileReadyFlag = false;}
	private synchronized boolean getCtrlFileReadyFlag(){ return ctrl_fileReadyFlag;}
	private synchronized void setCtrlFileReadyFlag(){ctrl_fileReadyFlag = true;}
	private synchronized void offCtrlFileReadyFlag(){ctrl_fileReadyFlag = false;}
	private synchronized boolean getWriteToConfigFlag(){ return writeToConfigFlag;}
	private synchronized void offWriteToConfigFlag(){writeToConfigFlag = false;}
	
	float distanceToAngle(int val){
		float length = (float)val;
		return (float)Math.toDegrees(Math.atan((double)(length/SCREW_HEIGHT))) ;
	}
	int angleToDistance(float val){
		float length = (float)Math.tan(Math.toRadians((double)val))*SCREW_HEIGHT;
		return (int)length ;
	}
	

	//------------------------------------------------------------------
    // Serial port management functions
	//------------------------------------------------------------------
	
	public void serialInitialise() {
    	logger.info("Primitive Controller HW Interface - beginning serial initialisation");
		CommPortIdentifier portId1 = null;
		CommPortIdentifier portId2 = null;
    	logger.debug("Primitive Controller HW Interface - looking for ports");
    	try{
    		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
    		logger.debug("Primitive Controller HW Interface - finished looking for ports");
    		//First, Find an instance of serial port as set in PORT_NAMES.
			while (portEnum.hasMoreElements()) {
				CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
				if (currPortId.getName().equals(daqPortNameFromConfig)){
					portId1 = currPortId;
				}
				if (currPortId.getName().equals(ctrlPortNameFromConfig)){
					portId2 = currPortId;
				}
			}
		}catch (Exception e) {
			logger.error("Primitive Controller HW Interface - Exception - Could not find port.");
		}
    	
    	logger.debug("Primitive Controller HW Interface - finished finding port instance");
		if (portId1 == null) {
			logger.debug("Primitive Controller HW Interface - Could not find " + DAQ_SERIAL + " COM port.");
			return;
		}
		if (portId2 == null) {
			logger.debug("Primitive Controller HW Interface - Could not find " + CTRL_SERIAL + " COM port.");
			return;
		}
		logger.info("Primitive Controller HW Interface - Serial port ID found");

		try {
			// open serial port, and use class name for the appName.
			daq_serialPort = (SerialPort) portId1.open(this.getClass().getName()+ DAQ_SERIAL, TIME_OUT);
			logger.debug("Primitive Controller HW Interface - "+ DAQ_SERIAL +" port Open and set name");

			// set port parameters
			daq_serialPort.setSerialPortParams(DATA_RATE,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			
			logger.debug("Primitive Controller HW Interface - "+ DAQ_SERIAL +" port parameter set");

			// open the streams
			daq_serial_input = new BufferedReader(new InputStreamReader(daq_serialPort.getInputStream()));
			daq_output = daq_serialPort.getOutputStream();
			logger.debug("Primitive Controller HW Interface - "+ DAQ_SERIAL +" port stream open");

			daq_serialPort.addEventListener(this);
            logger.debug("Primitive Controller HW Interface - "+ DAQ_SERIAL +" port event listener added");
            
            daq_serialPort.notifyOnDataAvailable(true);
            logger.info("Primitive Controller HW Interface - "+ DAQ_SERIAL +" ports opened ");
		} catch (Exception e) {
			System.err.println(e.toString());
			logger.error("Primitive Controller HW Interface - "+ DAQ_SERIAL +" port open exception: "+e.toString());
		}
		
		try {
			// open serial port, and use class name for the appName.
			ctrl_serialPort = (SerialPort) portId2.open(this.getClass().getName()+ CTRL_SERIAL, TIME_OUT);
			logger.debug("Primitive Controller HW Interface - "+ CTRL_SERIAL +" port Open and set name");

			// set port parameters
			ctrl_serialPort.setSerialPortParams(DATA_RATE,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			
			logger.debug("Primitive Controller HW Interface - "+ CTRL_SERIAL +" port parameter set");

			// open the streams
			ctrl_serial_input = new BufferedReader(new InputStreamReader(ctrl_serialPort.getInputStream()));
			ctrl_output = ctrl_serialPort.getOutputStream();
			logger.debug("Primitive Controller HW Interface - "+ CTRL_SERIAL +" port stream open");

			ctrl_serialPort.addEventListener(this);
            logger.debug("Primitive Controller HW Interface - "+ CTRL_SERIAL +" port event listener added");
            
            ctrl_serialPort.notifyOnDataAvailable(true);
            logger.info("Primitive Controller HW Interface - "+ CTRL_SERIAL +" ports opened ");
		} catch (Exception e) {
			System.err.println(e.toString());
			logger.error("Primitive Controller HW Interface - "+ CTRL_SERIAL +" port open exception: "+e.toString());
		}
	}
	
	public synchronized void serialEvent (SerialPortEvent oEvent) {
        if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
            	// no action
            } catch (Exception e) {
                //System.err.println(e.toString());
                logger.error("Primitive Controller HW Interface - data exception: " + e.toString());
            }
        }
    }
	
	public synchronized void serialClose() {
		if (daq_serialPort != null) {
			daq_serialPort.removeEventListener();
			daq_serialPort.close();
		}
		if (ctrl_serialPort != null) {
			ctrl_serialPort.removeEventListener();
			ctrl_serialPort.close();
		}
	}
	
	private int read(String channel){
        int b = 0;
        try{
        	if (channel.equals(DAQ_SERIAL)){
        		b = (int)daq_serial_input.read();
        	}
        	if (channel.equals(CTRL_SERIAL)){
        		b = (int)ctrl_serial_input.read();
        	}
        }
        catch (Exception e) {
            System.err.println(e.toString());
            logger.error("Primitive Controller HW Interface - " + e.toString());
        }
        return b;
    }
	
	private void write(String channel, String cmd){
		try{
        	if (channel.equals(DAQ_SERIAL)){
        		daq_output.write(cmd.getBytes());
        		daq_output.write('\n');
        		daq_output.flush();
        	}
        	if (channel.equals(CTRL_SERIAL)){
        		ctrl_output.write(cmd.getBytes());
        		ctrl_output.write('\n');
        		ctrl_output.flush();
        	}
        }
        catch (Exception e) {
            System.err.println(e.toString());
            logger.error("Primitive Controller HW Interface - " + e.toString());
        }
	}
	
	private String sendCmd(String cmd, String channel){
		String response="";
		
		try {
			if (channel.equals(DAQ_SERIAL)){
				while (daq_serial_input.ready()) daq_serial_input.read();  // clear input buffer
			}
			if (channel.equals(CTRL_SERIAL)){
				while (ctrl_serial_input.ready()) ctrl_serial_input.read();  // clear input buffer
			}
			logger.debug("Primitive Controller HW Interface - Arduino Cmd=" + cmd);
			write(channel, cmd);
			Thread.sleep(50);
			
			int currentChar;
			int attempts=0;
			while (true) {
				currentChar = read(channel);
				if (currentChar == 0){
					logger.debug("Primitive Controller HW Interface - Arduino " + channel + " buffer null, attempt:" + attempts);
					Thread.sleep(50);
					attempts++;
				}

				if (currentChar==-1 ){
					logger.debug("Primitive Controller HW Interface - Arduino " + channel + " Cmd fb not ready, attempt:" + attempts);
					Thread.sleep(50);
					attempts++;
				}
				else if (currentChar == '\n') break;
				else if (currentChar != 0)response += (char) currentChar;
				if (attempts>60) {
					response = "Err:Timeout";
					logger.debug("Primitive Controller HW Interface - Arduino " + channel + " Cmd fb timeout");
					break;
				}
			}
			logger.debug("Primitive Controller HW Interface - Arduino " + channel + " Rsp=" + response);
			
		 } catch (Exception e) {
			 logger.error("Primitive Controller HW Interface - could not write to " + channel);
             System.err.println(e.toString());
		 }
		return response;
	}
	
	//------------------------------------------------------------------
    // Public access functions
	//------------------------------------------------------------------
	public synchronized boolean getRunning(){ return this.running; }
	public synchronized void stopRunning() { this.running = false ;	}
	
	public synchronized void resetWatchdog(){
		wdDaq = 0;
		wdCtrl = 0;
	}
	// External - Information fetch
	public synchronized float getDaqStrain(int index){ return daq_strain[index];}
	public synchronized float getStrain(int index){ return (daq_strain[index] - strain_base[index]);}
	public synchronized int getDaqHealth(int index){ return daq_health[index];}
	public synchronized int getDaqWatchdog(){return wdDaq;}
	
	public synchronized int getAngleMode(){ return ctrl_angleMode;}
	public synchronized float getCurAngle(){ return curAngle;}
	public synchronized float getTgtAngle(){ return tgtAngle;}
	public synchronized float getCurLoad(){ return curLoad;}
	public synchronized float getTgtLoad(){ return tgtLoad;}
	public synchronized int getLoadMode(){ return ctrl_loadMode;}
	public synchronized int getAngleHealth(){ return ctrl_angleHealth;}
	public synchronized int getLoadHealth(){ return ctrl_loadHealth;}
	public synchronized int getCtrlWatchdog(){return wdCtrl;}
	
	public synchronized int getRefDistance(){ return refDistance;}
	
	// External - Debug Info Fetch
	public synchronized int getRawDistance(){ return ctrl_curDistance;}
	
	// Internal used operation mode handling, external only read for debugging
	public synchronized int getDaqMode(){ return daq_mode;}
	public synchronized int getCtrlMode(){ return ctrl_mode;}
	
	
	// External - Remote zeroing strain gauge 
	public synchronized void setBaseStrain(){
		for (int i = 0; i < 10; i++){
			strain_base[i] = daq_strain[i];
		}
	}	
	
	// External - Specify Angle Target
	public synchronized void setAngleTarget(float angle){
		ctrl_tgtDistanceBuffer = refDistance - angleToDistance(angle);
		ctrl_tgtDistanceFlag = true;
	}
	
	// External - Screw Drive Motor Stepping
	public synchronized void incAngleStep(){
		ctrl_angleStepBuffer = stepsize;
		ctrl_angleStepFlag = true;
	}
	public synchronized void decAngleStep(){
		ctrl_angleStepBuffer = - stepsize;
		ctrl_angleStepFlag = true;
	}
	// External - Winch Drive Motor Stepping
	public synchronized void incLoadStep(){
		ctrl_loadStepBuffer = stepsize;
		ctrl_loadStepFlag = true;
	}
	public synchronized void decLoadStep(){
		ctrl_loadStepBuffer = 0 - stepsize;
		ctrl_loadStepFlag = true;
	}
	
	// External - Motor Stepping Auxiliaries 
	public synchronized int getStepSize(){ return stepsize;}
	public synchronized void setStepSize(int val){
		stepsize = val;
	}
	
	public synchronized void setLoadTarget(float val){
		ctrl_tgtLoadBuffer = val;
		ctrl_tgtLoadFlag = true;
	}
	
	public synchronized void calibrateScrew(){
		refDistance = ctrl_curDistance;
		writeToConfigFlag = true;
	}
	

	
}