package au.edu.usyd.eng.remotelabs.redundanttrussrig.primitive;

import au.edu.uts.eng.remotelabs.rigclient.rig.IRigControl.PrimitiveRequest;
import au.edu.uts.eng.remotelabs.rigclient.rig.IRigControl.PrimitiveResponse;
import au.edu.uts.eng.remotelabs.rigclient.rig.primitive.IPrimitiveController;
import au.edu.uts.eng.remotelabs.rigclient.util.ILogger;
import au.edu.uts.eng.remotelabs.rigclient.util.LoggerFactory;

import gnu.io.CommPortIdentifier;

import java.io.*;
import java.util.Enumeration;


public class RedundantTrussRigController implements IPrimitiveController {

	   /** Logger. **/
    private ILogger logger;

       /** HW simulator or interface **/
    RedundantTrussHW rtHW;

	@Override
    public boolean initController()
    {
        this.logger = LoggerFactory.getLoggerInstance();
        this.logger.info("Primitive Controller created");
        
        rtHW = new RedundantTrussHW("HWThread-1");
        // if(rtHW == null){
        // 	this.logger.warn("Fail to create HW");
        // 	return false;
        // }
		rtHW.start();
		return true;
    }
    
    public PrimitiveResponse getValsAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);
			response.addResult("stepsize",		String.valueOf(rtHW.getStepSize()));
		
            response.addResult("angle", 		String.valueOf(rtHW.getCurAngle()));
			response.addResult("angletarget", 	String.valueOf(rtHW.getTgtAngle()));
			response.addResult("anglemode", 	String.valueOf(rtHW.getAngleMode()));
			response.addResult("anglehealth", 	String.valueOf(rtHW.getAngleHealth()));
			
            response.addResult("load", 			String.valueOf(rtHW.getCurLoad()));
			response.addResult("loadtarget", 	String.valueOf(rtHW.getTgtLoad()));
			response.addResult("loadmode", 		String.valueOf(rtHW.getLoadMode()));
			response.addResult("loadhealth", 	String.valueOf(rtHW.getLoadHealth()));
			
			for (int i=0;i<10;i++){
				if (rtHW.getDaqHealth(i)==0){
					response.addResult("StrainValue"+String.valueOf(i+1),String.valueOf(rtHW.getStrain(i)));
				}else{
					response.addResult("StrainValue"+String.valueOf(i+1),"NaN");
				}
			}
			if (rtHW.getDaqWatchdog()==0) response.addResult("daqlinksts", String.valueOf(0));
			else if (rtHW.getDaqWatchdog()== 999) response.addResult("daqlinksts", String.valueOf(2));
			else response.addResult("daqlinksts", String.valueOf(1));
			
			if (rtHW.getCtrlWatchdog()==0) response.addResult("ctrllinksts", String.valueOf(0));
			else if (rtHW.getCtrlWatchdog()== 999) response.addResult("ctrllinksts", String.valueOf(2));
			else response.addResult("ctrllinksts", String.valueOf(1));
			
			// for troubleshooting
			response.addResult("rawdistance", 	String.valueOf(rtHW.getRawDistance()));
			response.addResult("refdistance",	String.valueOf(rtHW.getRefDistance()));
			response.addResult("daqmode",		String.valueOf(rtHW.getDaqMode()));
			response.addResult("ctrlmode",		String.valueOf(rtHW.getCtrlMode()));
			
            return response;
    }
	
	public PrimitiveResponse setAngleAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            float val = Float.parseFloat(request.getParameters().get("targetAngle"));
            rtHW.setAngleTarget(val);
            return response;
    }
	public PrimitiveResponse incLoadStepAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);
            rtHW.incLoadStep();
            return response;
    }
	public PrimitiveResponse decLoadStepAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);
            rtHW.decLoadStep();
            return response;
    }
	public PrimitiveResponse incAngleStepAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);
            rtHW.incAngleStep();
            return response;
    }
	public PrimitiveResponse decAngleStepAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);
            rtHW.decAngleStep();
            return response;
    }
	
	public PrimitiveResponse setTgtLoadAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            float val = Float.parseFloat(request.getParameters().get("targetLoad"));
            rtHW.setLoadTarget(val);
            return response;
    }

	public PrimitiveResponse setStepSizeAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);
            int val = Integer.parseInt(request.getParameters().get("tgtStepsize"));
            rtHW.setStepSize(val);
            return response;
    }
	
	public PrimitiveResponse caliScrewAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);
            rtHW.calibrateScrew();
            return response;
    }

	public PrimitiveResponse resetWatchdogAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);
            rtHW.resetWatchdog();
            return response;
    }
	public PrimitiveResponse zeroStrainGaugeAction (PrimitiveRequest request) throws IOException
	{
			PrimitiveResponse response = new PrimitiveResponse();
	        response.setSuccessful(true);
	        rtHW.setBaseStrain();
	        return response;
	}

    @Override
    public boolean preRoute()
    {
    	return true;
    }
    
    @Override
    public boolean postRoute()
    {
    	
        return true;
    }

    @Override
    public void cleanup()
    {
    	// destroy HW
        this.logger.info("Primitive Controller cleanup complete");
    	rtHW.stopRunning();
    }
}
