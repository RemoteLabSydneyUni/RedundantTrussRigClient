/**
 * SAHARA Rig Client
 * 
 * Software abstraction of physical rig to provide rig session control
 * and rig device control. Automatically tests rig hardware and reports
 * the rig status to ensure rig goodness.
 *
 * @license See LICENSE in the top level directory for complete license terms.
 *
 * Copyright (c) 2009, University of Technology, Sydney
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in the 
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of the University of Technology, Sydney nor the names 
 *    of its contributors may be used to endorse or promote products derived from 
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Changelog:
 * - 14/10/2009 - mdiponio - Initial file creation.
 */
package au.edu.uts.eng.remotelabs.rigclient.rig.tests;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import au.edu.uts.eng.remotelabs.rigclient.rig.AbstractRig;
import au.edu.uts.eng.remotelabs.rigclient.rig.AbstractRig.ActionType;
import au.edu.uts.eng.remotelabs.rigclient.rig.IAccessAction;
import au.edu.uts.eng.remotelabs.rigclient.rig.IActivityDetectorAction;
import au.edu.uts.eng.remotelabs.rigclient.rig.IFilesDetectorAction;
import au.edu.uts.eng.remotelabs.rigclient.rig.INotifyAction;
import au.edu.uts.eng.remotelabs.rigclient.rig.IResetAction;
import au.edu.uts.eng.remotelabs.rigclient.rig.IRigSession.Session;
import au.edu.uts.eng.remotelabs.rigclient.rig.ISlaveAccessAction;
import au.edu.uts.eng.remotelabs.rigclient.rig.ITestAction;
import au.edu.uts.eng.remotelabs.rigclient.type.RigFactory;
import au.edu.uts.eng.remotelabs.rigclient.util.ConfigFactory;
import au.edu.uts.eng.remotelabs.rigclient.util.IConfig;


/**
 * Tests the <code>AbstractRig</code> class (actually its the
 * <code>MockRig</code> class which is instantiated, since 
 * <code>AbstractRig</code> is abstract. 
 */
@SuppressWarnings("all")
public class AbstractRigTester extends TestCase
{
    /** Class under test. */
    private MockRig rig;
    
    /** Mock configuration class. */
    IConfig mockConfig;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.mockConfig = createMock(IConfig.class);
        expect(this.mockConfig.getProperty("Logger_Type"))
                .andReturn("SystemErr");
        expect(this.mockConfig.getProperty("Log_Level"))
                .andReturn("DEBUG");
        expect(this.mockConfig.getProperty("Action_Failure_Threshold"))
                .andReturn("2");
        expect(this.mockConfig.getProperty("Default_Log_Format", "[__LEVEL__] - [__ISO8601__] - __MESSAGE__"))
            .andReturn("[__LEVEL__] - [__ISO8601__] - __MESSAGE__");
        expect(this.mockConfig.getProperty("FATAL_Log_Format")).andReturn(null);
        expect(this.mockConfig.getProperty("PRIORITY_Log_Format")).andReturn(null);
        expect(this.mockConfig.getProperty("ERROR_Log_Format")).andReturn(null);
        expect(this.mockConfig.getProperty("WARN_Log_Format")).andReturn(null);
        expect(this.mockConfig.getProperty("INFO_Log_Format")).andReturn(null);
        expect(this.mockConfig.getProperty("DEBUG_Log_Format")).andReturn(null);
        expect(this.mockConfig.getProperty("Rig_Client_IP_Address")).andReturn("127.0.0.1");
        expect(this.mockConfig.getProperty("Listening_Network_Interface")).andReturn(null);
        expect(this.mockConfig.getProperty("Rig_Class")).andReturn("does.not.exist");
        
        expect(this.mockConfig.getProperty("Data_Transfer_Method", "WEBDAV")).andReturn("ATTACHMENT");
        expect(this.mockConfig.getProperty("Data_Transfer_Local_Directory", "")).andReturn(".");
        expect(this.mockConfig.getProperty("Data_Transfer_Restore_File", "./dfrestore")).andReturn("./dfrestore");
        expect(this.mockConfig.getProperty("Delete_Data_Files_After_Transfer")).andReturn("false");
        expect(this.mockConfig.getProperty("Scheduling_Server_Address")).andReturn("localhost");
        expect(this.mockConfig.getProperty("Scheduling_Server_Port", "8080")).andReturn("8080");  
        
        replay(this.mockConfig);
        
        Field configField = ConfigFactory.class.getDeclaredField("instance");
        configField.setAccessible(true);
        configField.set(null, this.mockConfig);
        
        this.rig = new MockRig();
        Field factField = RigFactory.class.getDeclaredField("rig");
        factField.setAccessible(true);
        factField.set(null, this.rig);
    }
    
    /**
     * Tests the <code>AbstractRig.addSlave</code> method.
     */
    @Test
    public void testAddSlave()
    {
        final String master = "mastuser", passive = "passiveuser", active = "activeuser";
        ISlaveAccessAction mockSlave = createMock(ISlaveAccessAction.class);
        expect(mockSlave.getActionType())
            .andReturn("MockSlave");
        expect(mockSlave.assign(passive, true))
            .andReturn(true);
        expect(mockSlave.assign(active, false))
            .andReturn(true);
        expect(mockSlave.assign(passive, false))
            .andReturn(true);
        expect(mockSlave.assign(active, true))
            .andReturn(true);
        expect(mockSlave.revoke(active, false))
            .andReturn(true);
        expect(mockSlave.revoke(passive, true))
            .andReturn(true);
        replay(mockSlave);
        
        assertTrue(this.rig.register(mockSlave, ActionType.SLAVE_ACCESS));
        
        /* Can add slaves if there is no session. */
        assertFalse(this.rig.addSlave(active, false));
        assertFalse(this.rig.addSlave(passive, true));
        assertFalse(this.rig.isSessionActive());
        assertTrue(this.rig.isInSession(active) == Session.NOT_IN);
        assertTrue(this.rig.isInSession(passive) == Session.NOT_IN);
        
        /* Start a session and add slaves. */
        assertTrue(this.rig.assign(master));
        assertTrue(this.rig.addSlave(active, false));
        assertTrue(this.rig.addSlave(passive, true));
        assertTrue(this.rig.isInSession(active) == Session.SLAVE_ACTIVE);
        assertTrue(this.rig.isInSession(passive) == Session.SLAVE_PASSIVE);
        
        assertFalse(this.rig.addSlave(active, false));
        assertFalse(this.rig.addSlave(passive, true));
        assertTrue(this.rig.isInSession(active) == Session.SLAVE_ACTIVE);
        assertTrue(this.rig.isInSession(passive) == Session.SLAVE_PASSIVE);
        
        /* Invert the slaves permission. */
        assertTrue(this.rig.addSlave(active, true));
        assertTrue(this.rig.addSlave(passive, false));
        assertTrue(this.rig.isInSession(active) == Session.SLAVE_PASSIVE);
        assertTrue(this.rig.isInSession(passive) == Session.SLAVE_ACTIVE);
        
        verify(mockSlave);
    }
    
    /**
     * Tests the <code>AbstractRig.revokeSlave</code> method.
     */
    @Test
    public void testRevokeSlave()
    {
        final String master = "mastuser", passive = "passiveuser", active = "activeuser";
        ISlaveAccessAction mockSlave = createMock(ISlaveAccessAction.class);
        expect(mockSlave.getActionType())
            .andReturn("MockSlave");
        expect(mockSlave.assign(passive, true))
            .andReturn(true);
        expect(mockSlave.assign(active, false))
            .andReturn(true);
        expect(mockSlave.revoke(active, false))
            .andReturn(true);
        expect(mockSlave.revoke(passive, true))
            .andReturn(true);
        replay(mockSlave);
        
        assertTrue(this.rig.register(mockSlave, ActionType.SLAVE_ACCESS));
        
        /* Start a session and add slaves. */
        assertTrue(this.rig.assign(master));
        assertTrue(this.rig.addSlave(active, false));
        assertTrue(this.rig.addSlave(passive, true));
        assertTrue(this.rig.isInSession(active) == Session.SLAVE_ACTIVE);
        assertTrue(this.rig.isInSession(passive) == Session.SLAVE_PASSIVE);
        
        assertTrue(this.rig.revokeSlave(active));
        assertTrue(this.rig.revokeSlave(passive));
        assertTrue(this.rig.isInSession(active) == Session.NOT_IN);
        assertTrue(this.rig.isInSession(passive) == Session.NOT_IN);
    }
    
    /**
     * Tests the rig being put offline because of a failed action.
     */
    @Test
    public void testActionFailOffline()
    {
        final String tuser = "testuser";
        ITestAction mockTest = createMock(ITestAction.class);
        expect(mockTest.getActionType())
            .andReturn("MockTestAction")
            .atLeastOnce();
        mockTest.stopTest();
        expectLastCall().times(3);
        mockTest.startTest();
        expectLastCall().times(3);
        expect(mockTest.getStatus())
            .andReturn(true)
            .atLeastOnce();
        
        IAccessAction mockAccess = createMock(IAccessAction.class);
        expect(mockAccess.assign(tuser))
            .andReturn(false).times(3);  // Action failure
        expect(mockAccess.getActionType())
            .andReturn("MockAccessAction")
            .atLeastOnce();
        expect(mockAccess.getFailureReason())
            .andReturn("Cause I told it to").atLeastOnce();
        
        replay(mockTest);
        replay(mockAccess);
        
        assertTrue(this.rig.register(mockAccess, ActionType.ACCESS));
        assertTrue(this.rig.register(mockTest, ActionType.TEST));
        
        assertFalse(this.rig.isSessionActive());
        assertFalse(this.rig.assign(tuser)); 
        assertFalse(this.rig.isSessionActive());
        assertFalse(this.rig.assign(tuser));
        assertFalse(this.rig.isSessionActive());
        assertTrue(Session.NOT_IN == this.rig.isInSession(tuser)); // Failed but below threshold so not offline
        assertTrue(this.rig.isMonitorStatusGood());
        assertFalse(this.rig.assign(tuser)); // Threshold exceeded so should put the experiment offline
        assertTrue(Session.NOT_IN == this.rig.isInSession(tuser));
        assertFalse(this.rig.isMonitorStatusGood());
        
        final String failure = this.rig.getMonitorReason();
        this.assertTrue(failure.indexOf("Cause I told it to") > 0);
        
        verify(mockTest);
        verify(mockAccess);
    }
    
    /**
     * Tests the getting the failure reason of an action failure. 
     */
    @Test
    public void testAccessActionFailureReason()
    {
        final String tuser = "testuser";
        IAccessAction mockSucceed = createMock(IAccessAction.class);
        expect(mockSucceed.getActionType())
            .andReturn("MockSucceedAccessAction")
            .atLeastOnce();
        expect(mockSucceed.assign(tuser))
            .andReturn(true);
        expect(mockSucceed.getFailureReason())
            .andReturn(null);
        
        IAccessAction mockFail = createMock(IAccessAction.class);
        expect(mockFail.assign(tuser))
            .andReturn(false);
        expect(mockFail.getActionType())
            .andReturn("MockFailAccessAction")
            .atLeastOnce();
        expect(mockFail.getFailureReason())
            .andReturn("Cause I told it to")
            .atLeastOnce();
        
        replay(mockSucceed);
        replay(mockFail);
        
        assertTrue(this.rig.register(mockSucceed, ActionType.ACCESS));
        assertTrue(this.rig.register(mockFail, ActionType.ACCESS));
        
        assertFalse(this.rig.assign(tuser)); 

        assertTrue(this.rig instanceof AbstractRig);
        AbstractRig arig = (AbstractRig)this.rig;
        
        String failureReason = arig.getActionFailureReason(ActionType.ACCESS);
        assertNotNull(failureReason);
        assertTrue(failureReason.startsWith(mockFail.getActionType()));
        assertTrue(failureReason.endsWith(mockFail.getFailureReason()));
        
        verify(mockSucceed);
        verify(mockFail);
    }
    
    /**
     * Tests the getting the failure reason of an action failure. 
     */
    @Test
    public void testSlaveAccessActionFailureReason()
    {
        final String tuser = "testuser";
        ISlaveAccessAction mockSucceed = createMock(ISlaveAccessAction.class);
        expect(mockSucceed.getActionType())
            .andReturn("MockSucceedSlaveAction")
            .atLeastOnce();
        expect(mockSucceed.assign(tuser, false))
            .andReturn(true);
        expect(mockSucceed.getFailureReason())
            .andReturn(null);
        
        ISlaveAccessAction mockFail = createMock(ISlaveAccessAction.class);
        expect(mockFail.assign(tuser, false))
            .andReturn(false);
        expect(mockFail.getActionType())
            .andReturn("MockFailSlaveAction")
            .atLeastOnce();
        expect(mockFail.getFailureReason())
            .andReturn("Cause I told it to")
            .atLeastOnce();
        
        replay(mockSucceed);
        replay(mockFail);
        
        assertTrue(this.rig.register(mockSucceed, ActionType.SLAVE_ACCESS));
        assertTrue(this.rig.register(mockFail, ActionType.SLAVE_ACCESS));
        
        assertTrue(this.rig.assign("master"));
        assertFalse(this.rig.addSlave(tuser, false));

        assertTrue(this.rig instanceof AbstractRig);
        AbstractRig arig = (AbstractRig)this.rig;
        
        String failureReason = arig.getActionFailureReason(ActionType.SLAVE_ACCESS);
        System.out.println(failureReason);
        assertNotNull(failureReason);
        assertTrue(failureReason.startsWith(mockFail.getActionType()));
        assertTrue(failureReason.endsWith(mockFail.getFailureReason()));
        
        verify(mockSucceed);
        verify(mockFail);
    }

    /**
     * Tests the <code>AbstractRig.hasPermission</code> method. The permission stack
     * is verified. 
     */
    @Test
    public void testHasPermission()
    {
        final String master = "masteruser", spassive = "slavepassive", sactive = "slaveactive", notin = "notuser";
        
        assertTrue(this.rig.assign(master));
        assertTrue(this.rig.addSlave(spassive, true));
        assertTrue(this.rig.addSlave(sactive, false));
        
        /* Not a user. */
        assertFalse(this.rig.hasPermission(notin, Session.MASTER));
        assertFalse(this.rig.hasPermission(notin, Session.SLAVE_ACTIVE));
        assertFalse(this.rig.hasPermission(notin, Session.SLAVE_PASSIVE));
        assertTrue(this.rig.hasPermission(notin, Session.NOT_IN));
        
        /* Slave passive. */
        assertFalse(this.rig.hasPermission(spassive, Session.MASTER));
        assertFalse(this.rig.hasPermission(spassive, Session.SLAVE_ACTIVE));
        assertTrue(this.rig.hasPermission(spassive, Session.SLAVE_PASSIVE));
        assertTrue(this.rig.hasPermission(spassive, Session.NOT_IN));
        
        /* Master stack -> all power to the master. */
        assertTrue(this.rig.hasPermission(master, Session.MASTER));
        assertTrue(this.rig.hasPermission(master, Session.SLAVE_ACTIVE));
        assertTrue(this.rig.hasPermission(master, Session.SLAVE_PASSIVE));
        assertTrue(this.rig.hasPermission(master, Session.NOT_IN));
        
        /* Slave active. */
        assertFalse(this.rig.hasPermission(sactive, Session.MASTER));
        assertTrue(this.rig.hasPermission(sactive, Session.SLAVE_ACTIVE));
        assertTrue(this.rig.hasPermission(sactive, Session.SLAVE_PASSIVE));
        assertTrue(this.rig.hasPermission(sactive, Session.NOT_IN));
    }
    
    /**
     * Tests the <code>AbstractRig.notify</code> method.
     */
    @Test
    public void testNotify()
    {
        final String users[] = {"Foo", "Bar", "Baz"};
        
        assertFalse(this.rig.notify("Failure."));
        
        assertTrue(this.rig.assign(users[0]));
        assertTrue(this.rig.addSlave(users[1], false));
        assertTrue(this.rig.addSlave(users[2], true));
        
        INotifyAction mockNotify = createMock(INotifyAction.class);
        expect(mockNotify.notify(eq("Test Message"), (String[])notNull()))
            .andReturn(true);
        expect(mockNotify.getActionType())
            .andReturn("MockNotify");
        replay(mockNotify);
        
        assertTrue(this.rig.register(mockNotify, ActionType.NOTIFY));
        assertTrue(this.rig.notify("Test Message"));
        
        verify(mockNotify);
    }
    
    /**
     * Tests the <code>AbstractRig.assign</code> method attempting to assign a user
     * who already has been previosuly assigned.
     */
    @Test
    public void testAssignFailSameUser()
    {
        final String tuser = "testuser";
        ITestAction mockTest = createMock(ITestAction.class);
        expect(mockTest.getActionType())
            .andReturn("MockTestAction").times(2);
        expect(mockTest.getStatus())
            .andReturn(true);
        mockTest.stopTest();
        expectLastCall();
        
        IAccessAction mockAccess = createMock(IAccessAction.class);
        expect(mockAccess.assign(tuser))
            .andReturn(true);
        expect(mockAccess.getActionType())
            .andReturn("MockAccessAction");
        
        replay(mockTest);
        replay(mockAccess);
        
        assertTrue(this.rig.register(mockAccess, ActionType.ACCESS));
        assertTrue(this.rig.register(mockTest, ActionType.TEST));
        
        assertFalse(this.rig.isSessionActive());
        assertTrue(Session.NOT_IN == this.rig.isInSession(tuser));
        assertFalse(this.rig.hasPermission(tuser, Session.MASTER));
        assertTrue(this.rig.assign(tuser));
        assertTrue(this.rig.isSessionActive());
        assertTrue(Session.MASTER== this.rig.isInSession(tuser));
        assertTrue(this.rig.hasPermission(tuser, Session.MASTER));
        
        verify(mockTest);
        verify(mockAccess);
        
        final String imposter = "Imposter";
        assertFalse(this.rig.assign(tuser)); // Try the same user again
        assertFalse(this.rig.assign(imposter)); // Try a different user
        assertFalse(this.rig.hasPermission(imposter, Session.MASTER));
        assertTrue(this.rig.isInSession(imposter) == Session.NOT_IN);
        
        /* Make sure the orignal user is still assigned. */
        assertTrue(this.rig.isSessionActive());
        assertTrue(this.rig.isInSession(tuser) == Session.MASTER);
        assertTrue(this.rig.hasPermission(tuser, Session.MASTER));
    }
    
    /**
     * Tests the <code>AbstractRig.assign</code> method.
     */
    @Test
    public void testAssign()
    {
        final String tuser = "testuser";
        ITestAction mockTest = createMock(ITestAction.class);
        expect(mockTest.getActionType())
            .andReturn("MockTestAction").times(2);
        expect(mockTest.getStatus())
            .andReturn(true);
        mockTest.stopTest();
        expectLastCall();
        
        IAccessAction mockAccess = createMock(IAccessAction.class);
        expect(mockAccess.assign(tuser))
            .andReturn(true);
        expect(mockAccess.getActionType())
            .andReturn("MockAccessAction");
        
        replay(mockTest);
        replay(mockAccess);
        
        assertTrue(this.rig.register(mockAccess, ActionType.ACCESS));
        assertTrue(this.rig.register(mockTest, ActionType.TEST));
        
        assertFalse(this.rig.isSessionActive());
        assertTrue(Session.NOT_IN == this.rig.isInSession(tuser));
        assertFalse(this.rig.hasPermission(tuser, Session.MASTER));
        assertTrue(this.rig.assign(tuser));
        assertTrue(this.rig.isSessionActive());
        assertTrue(Session.MASTER== this.rig.isInSession(tuser));
        assertTrue(this.rig.hasPermission(tuser, Session.MASTER));
        
        verify(mockTest);
        verify(mockAccess);
    }
    
    /**
     * Tests the <code>AbstractRig.revoke</code> method.
     */
    @Test
    public void testRevoke()
    {
        final String tuser = "testuser";
        ITestAction mockTest = createMock(ITestAction.class);
        expect(mockTest.getActionType())
            .andReturn("MockTestAction").times(2);
        expect(mockTest.getStatus())
            .andReturn(true);
        mockTest.stopTest();
        expectLastCall();
        
        IAccessAction mockAccess = createMock(IAccessAction.class);
        expect(mockAccess.assign(tuser))
            .andReturn(true);
        expect(mockAccess.getActionType())
            .andReturn("MockAccessAction");
        replay(mockTest);
        replay(mockAccess);
        
        /* Assign master access. */
        assertTrue(this.rig.register(mockAccess, ActionType.ACCESS));
        assertTrue(this.rig.register(mockTest, ActionType.TEST));
        assertFalse(this.rig.isSessionActive());
        assertTrue(Session.NOT_IN == this.rig.isInSession(tuser));
        assertFalse(this.rig.hasPermission(tuser, Session.MASTER));
        assertTrue(this.rig.assign(tuser));
        assertTrue(this.rig.isSessionActive());
        assertTrue(Session.MASTER== this.rig.isInSession(tuser));
        assertTrue(this.rig.hasPermission(tuser, Session.MASTER));
        verify(mockTest);
        verify(mockAccess);
        
        /* Assign slave access. */
        final String sactive = "SlaveActive", spassive = "SlavePassive";
        ISlaveAccessAction mockSlave = createMock(ISlaveAccessAction.class);
        expect(mockSlave.getActionType())
            .andReturn("SlaveAction");
        expect(mockSlave.assign(sactive, false))
            .andReturn(true);
        expect(mockSlave.assign(spassive, true))
            .andReturn(true);
        replay(mockSlave);
        assertTrue(this.rig.register(mockSlave, ActionType.SLAVE_ACCESS));
        assertTrue(this.rig.addSlave(sactive, false));
        assertTrue(this.rig.addSlave(spassive, true));
        assertTrue(this.rig.isInSession(sactive) == Session.SLAVE_ACTIVE);
        assertTrue(this.rig.isInSession(spassive) == Session.SLAVE_PASSIVE);
        verify(mockSlave);
        
        /* Revoke master and slave. */
        IResetAction mockReset = createMock(IResetAction.class);
        reset(mockTest);
        reset(mockAccess);
        reset(mockSlave);
        expect(mockAccess.revoke(tuser))
            .andReturn(true);
        expect(mockSlave.revoke(sactive, false))
            .andReturn(true);
        expect(mockSlave.revoke(spassive, true))
            .andReturn(true);
        expect(mockReset.getActionType())
            .andReturn("MockResetAction");
        expect(mockReset.reset())
            .andReturn(true);
        mockTest.startTest();
        expectLastCall();
        replay(mockTest);
        replay(mockAccess);
        replay(mockSlave);
        replay(mockReset);
        
        assertTrue(this.rig.register(mockReset, ActionType.RESET));
        
        assertTrue(this.rig.revoke());
        assertFalse(this.rig.isSessionActive());
        assertTrue(Session.NOT_IN == this.rig.isInSession(tuser));
        assertFalse(Session.MASTER == this.rig.isInSession(tuser));
        assertFalse(this.rig.hasPermission(tuser, Session.MASTER));
        assertFalse(Session.SLAVE_ACTIVE == this.rig.isInSession(sactive));
        assertFalse(Session.SLAVE_PASSIVE == this.rig.isInSession(spassive));
        
        verify(mockTest);
        verify(mockAccess);
        verify(mockSlave);
        verify(mockReset);
    }
    
    /**
     * Tests the <code>AbstractRig.setInterval</code> method.
     */
    @Test
    public void testSetInterval()
    {
        final int interval = 20;
        ITestAction mockTest = createMock(ITestAction.class);
        expect(mockTest.getActionType())
            .andReturn("MockTestAction").times(2);
        mockTest.setInterval(interval);
        expectLastCall();
        replay(mockTest);
        
        this.rig.register(mockTest, ActionType.TEST);
        
        assertTrue(this.rig.setInterval(interval));
        verify(mockTest);
    }
    
    /**
     * Tests the <code>AbstractRig</code> monitor functions with a
     * defined failed test.
     */
    @Test
    public void testMonitorSuccessfulFail()
    {
        ITestAction mockTest = createMock(ITestAction.class);
        
        expect(mockTest.getActionType())
            .andReturn("MockTestAction")
            .times(3);
        expect(mockTest.getStatus())
            .andReturn(false)
            .times(2);
        expect(mockTest.getReason())
            .andReturn("Test Reason");
        
        replay(mockTest);
        assertTrue(this.rig.register(mockTest, ActionType.TEST));
        
        assertFalse(this.rig.isMonitorStatusGood());
        assertEquals("MockTestAction: Test Reason", this.rig.getMonitorReason().trim());
        verify(mockTest);
    }
    
    /**
     * Tests the <code>AbstractRig</code> monitor functions with a
     * defined successful test.
     */
    @Test
    public void testMonitorSuccessfulTest()
    {
        ITestAction mockTest = createMock(ITestAction.class);
        expect(mockTest.getStatus())
            .andReturn(true)
            .times(2);
        expect(mockTest.getActionType())
            .andReturn("MockTestAction")
            .times(2);
        
        replay(mockTest);
        assertTrue(this.rig.register(mockTest, ActionType.TEST));
        
        assertTrue(this.rig.isMonitorStatusGood());
        assertNull(this.rig.getMonitorReason());
        verify(mockTest);
    }
    
    /**
     * Tests the <code>AbstractRig</code> monitor functions with no
     * defined tests.
     */
    @Test
    public void testMonitorGood()
    {
        assertTrue(this.rig.isMonitorStatusGood());
        assertNull(this.rig.getMonitorReason());
    }
    
    /** 
     * Tests the <code>AbstractRig.setMaintenance</code> method with
     * a session active. This should terminate the session and revoke
     * the user.
     */
    @Test
    public void testMaintenanceRunTests()
    {
        /* Register test. */
        ITestAction mockTest = createMock(ITestAction.class);
        expect(mockTest.getActionType())
            .andReturn("MockTest").times(2);
        replay(mockTest);
        assertTrue(this.rig.register(mockTest, ActionType.TEST));
        verify(mockTest);
        
        /* Stop then start tests. */
        reset(mockTest);
        mockTest.startTest();
        expectLastCall();
        replay(mockTest);
        assertTrue(this.rig.setMaintenance(true, "Test reason", true));
        assertFalse(this.rig.isNotInMaintenance());
        assertEquals("Test reason", this.rig.getMaintenanceReason());
        
        /* Removal of maintenance should start tests. */
        reset(mockTest);
        mockTest.startTest();
        expectLastCall();
        replay(mockTest);
        assertTrue(this.rig.setMaintenance(false, null, false));
        assertTrue(this.rig.isNotInMaintenance());
        assertNull(this.rig.getMaintenanceReason());
        verify(mockTest);
    }
    
    /** 
     * Tests the <code>AbstractRig.setMaintenance</code> method with
     * a session active. This should terminate the session and revoke
     * the user.
     */
    @Test
    public void testMaintenanceStopTests()
    {
        /* Register test. */
        ITestAction mockTest = createMock(ITestAction.class);
        expect(mockTest.getActionType())
            .andReturn("MockTest").times(2);
        replay(mockTest);
        this.rig.register(mockTest, ActionType.TEST);
        verify(mockTest);
        
        /* Stop tests. */
        reset(mockTest);
        mockTest.stopTest();
        expectLastCall();
        replay(mockTest);
        assertTrue(this.rig.setMaintenance(true, "Test reason", false));
        assertFalse(this.rig.isNotInMaintenance());
        assertEquals("Test reason", this.rig.getMaintenanceReason());
        
        /* Start tests. */
        reset(mockTest);
        mockTest.startTest();
        expectLastCall();
        replay(mockTest);
        assertTrue(this.rig.setMaintenance(false, null, true));
        assertTrue(this.rig.isNotInMaintenance());
        assertNull(this.rig.getMaintenanceReason());
        verify(mockTest);
    }
    
    /** 
     * Tests the <code>AbstractRig.setMaintenance</code> method with
     * a session active. This should terminate the session and revoke
     * the user.
     */
    @Test
    public void testMaintenanceInSession()
    {
        String name = "SessionUser";
        assertFalse(this.rig.isSessionActive());
        assertTrue(this.rig.assign(name));
        assertTrue(this.rig.isSessionActive());
        assertTrue(Session.MASTER == this.rig.isInSession(name));

        assertTrue(this.rig.isInSession(name) == Session.MASTER);
        assertTrue(this.rig.isNotInMaintenance());
        
        /* Not setting maintenance, so should no affect running session. */
        assertTrue(this.rig.setMaintenance(false, null, false));
        assertTrue(this.rig.isSessionActive());
        assertTrue(this.rig.isInSession(name) == Session.MASTER);
        assertTrue(this.rig.isNotInMaintenance());
        
        assertTrue(this.rig.setMaintenance(true, "Test run", false));
        assertFalse(this.rig.isNotInMaintenance());
        assertEquals("Test run", this.rig.getMaintenanceReason());
        assertFalse(this.rig.isSessionActive());
        assertTrue(this.rig.isInSession(name) == Session.NOT_IN);
    }
    
    /**
     * Tests the <code>AbstractRig.setMaintenance</code>,
     * <code>AbstractRig.isNotInMaintenance</code> and 
     * <code>AbstractRig.getMaintenance</code> methods.
     * The test flow is:
     *    - Set maintenance with a reason.
     *    - Check maintenance is set with the correct reason.
     *    - Clear maintenance
     *    - Check maintenance is clear and reason not set.
     */
    @Test
    public void testMaintenance()
    {
        assertTrue(this.rig.setMaintenance(true, "Test reason", false));
        assertFalse(this.rig.isNotInMaintenance());
        assertEquals("Test reason", this.rig.getMaintenanceReason());
        
        assertTrue(this.rig.setMaintenance(false, null, false));
        
        assertTrue(this.rig.isNotInMaintenance());
        assertNull(this.rig.getMaintenanceReason());
    }
    
    /**
     * Tests the <code>AbstractRig.getRigName()</code> method.
     */
    @Test
    public void testGetName()
    {
        reset(this.mockConfig);
        expect(this.mockConfig.getProperty("Rig_Name"))
            .andReturn("test1");
        replay(this.mockConfig);
        
        assertEquals("test1", this.rig.getName());
        verify(this.mockConfig);
    }
    
    /**
     * Tests the <code>AbstractRig.getRigType()</code> method.
     */
    @Test
    public void testGetType()
    {
        reset(this.mockConfig);
        expect(this.mockConfig.getProperty("Rig_Type"))
            .andReturn("TestType");
        replay(this.mockConfig);
        
        assertEquals("TestType", this.rig.getType());
        verify(this.mockConfig);
    }
    
    /**
     * Tests the <code>AbstractRig.getCapabilities()</code> method.
     */
    @Test
    public void testGetCapabilities()
    {
        reset(this.mockConfig);
        expect(this.mockConfig.getProperty("Rig_Capabilites"))
            .andReturn("cap1,cap2,cap3");
        replay(this.mockConfig);
        
        String[] caps = this.rig.getCapabilities();
        assertNotNull(caps);
        assertEquals(3, caps.length);
        
        /* Order is not important. */
        assertTrue(this.inArray(caps, "cap1"));
        assertTrue(this.inArray(caps, "cap2"));
        assertTrue(this.inArray(caps, "cap3"));
        
        verify(this.mockConfig);
    }
    
    /**
     * Tests the <code>AbstractRig.getCapabilities()</code> method with
     * white space in the configuration string.
     */
    @Test
    public void testGetCapabilitiesWithWhitespace()
    {
        reset(this.mockConfig);
        expect(this.mockConfig.getProperty("Rig_Capabilites"))
            .andReturn("  cap1 ,  cap2  ,  cap3  ");
        replay(this.mockConfig);
        
        String[] caps = this.rig.getCapabilities();
        assertNotNull(caps);
        assertEquals(3, caps.length);
        
        /* Order is not important. */
        assertTrue(this.inArray(caps, "cap1"));
        assertTrue(this.inArray(caps, "cap2"));
        assertTrue(this.inArray(caps, "cap3"));
        
        verify(this.mockConfig);
    }
    
    /**
     * Tests the <code>AbstractRig.getAllRigAttributes()</code> method.
     */
    @Test
    public void testGetAllRigAttributes()
    {
        // Setup
        Map<String, String> prop = new HashMap<String, String>();
        prop.put("Key1", "Val1");
        prop.put("Key2", "Val2");
        prop.put("Key3", "Val3");
        prop.put("Key4", "Val4");
        prop.put("Key5", "Val5");
        
        reset(this.mockConfig);
        expect(this.mockConfig.getAllProperties())
                .andReturn(prop);
        replay(this.mockConfig);
        
        /* Check all key value pairs are the same. */
        Map<String, String> attrib = this.rig.getAllRigAttributes();
        for (String k : attrib.keySet())
        {
            assertTrue(prop.containsKey(k));
            assertEquals(attrib.get(k), prop.get(k));
        }
        
        /* Remove the keys to ensure they are the only ones
         * present. */
        assertEquals("Val1", attrib.remove("Key1"));
        assertEquals("Val2", attrib.remove("Key2"));
        assertEquals("Val3", attrib.remove("Key3"));
        assertEquals("Val4", attrib.remove("Key4"));
        assertEquals("Val5", attrib.remove("Key5"));
        assertTrue(attrib.isEmpty());
        
        verify(this.mockConfig);
    }
    
    /**
     * Tests the <code>AbstractRig.getAllRigAttributes()</code> method.
     */
    @Test
    public void testGetAllRigAttributesSubs()
    {
        // Setup
        Map<String, String> prop = new HashMap<String, String>();
        prop.put("Camera1", "http://__IP__/stream1.jpg");
        prop.put("Camera2", "http://__HOSTNAME__/stream2.jpg");
        
        reset(this.mockConfig);
        expect(this.mockConfig.getAllProperties())
                .andReturn(prop);
        replay(this.mockConfig);
        
        /* Check all key value pairs are the same. */
        Map<String, String> attrib = this.rig.getAllRigAttributes();
        assertEquals("http://127.0.0.1/stream1.jpg", attrib.remove("Camera1"));
        assertEquals("http://localhost/stream2.jpg", attrib.remove("Camera2"));
        assertTrue(attrib.isEmpty());
        
        verify(this.mockConfig);
    }
    
    /**
     * Tests the <code>AbstractRig.getRigAttribute()</code> method.
     */
    @Test
    public void testGetAttribute()
    {
        reset(this.mockConfig);
        expect(this.mockConfig.getProperty("IP"))
            .andReturn("192.168.0.1");
        replay(this.mockConfig);
        
        assertEquals("192.168.0.1", this.rig.getRigAttribute("IP"));
        verify(this.mockConfig);
    }
    
    /**
     * Tests the <code>AbstractRig.getRigAttribute()</code> method.
     */
    @Test
    public void testGetAttributeIpSub()
    {
        reset(this.mockConfig);
        expect(this.mockConfig.getProperty("IP"))
            .andReturn("__IP__");
        replay(this.mockConfig);
        
        assertEquals("127.0.0.1", this.rig.getRigAttribute("IP"));
        verify(this.mockConfig);
    }
    
    /**
     * Tests the <code>AbstractRig.getRigAttribute()</code> method.
     */
    @Test
    public void testGetAttributeHostNameSub()
    {
        reset(this.mockConfig);
        expect(this.mockConfig.getProperty("IP"))
            .andReturn("__HOSTNAME__");
        replay(this.mockConfig);
        
        assertEquals("localhost", this.rig.getRigAttribute("IP"));
        verify(this.mockConfig);
    }
    
    /**
     * Tests the <code>AbstractRig.getRigAttribute()</code> method
     * with a property that isn't found.
     */
    @Test
    public void testGetAttributeIsNull()
    {
        reset(this.mockConfig);
        expect(this.mockConfig.getProperty("Not_Found"))
            .andReturn(null);
        replay(this.mockConfig);
        
        assertNull(this.rig.getRigAttribute("Not_Found"));
        verify(this.mockConfig);
    }
    
    /**
     * Tests the detecting activity success with registered action.
     */
    @Test
    public void testActivityDetecion()
    {
        assertTrue(this.rig.assign("mdiponio"));
        IActivityDetectorAction detectorMock = createMock(IActivityDetectorAction.class);
        expect(detectorMock.getActionType())
            .andReturn("Detector");
        expect(detectorMock.detectActivity())
            .andReturn(true);
        replay(detectorMock);
        
        IActivityDetectorAction detMock = createMock(IActivityDetectorAction.class);
        expect(detMock.getActionType())
            .andReturn("Detector 2");
        expect(detMock.detectActivity())
            .andReturn(false);
        replay(detMock);
        
        assertTrue(this.rig.register(detMock, ActionType.DETECT));
        assertTrue(this.rig.register(detectorMock, ActionType.DETECT));
        assertTrue(this.rig.isActivityDetected());
        
        verify(detectorMock);
        verify(detMock);
    }
    
    /**
     * Tests the detecting activity failure
     */
    @Test
    public void testActivityDetectionFailed()
    {
        assertTrue(this.rig.assign("mdiponio"));
        IActivityDetectorAction detectorMock = createMock(IActivityDetectorAction.class);
        expect(detectorMock.getActionType())
            .andReturn("Detector");
        expect(detectorMock.detectActivity())
            .andReturn(false);
        replay(detectorMock);
        
        IActivityDetectorAction detMock = createMock(IActivityDetectorAction.class);
        expect(detMock.getActionType())
            .andReturn("Detector 2");
        expect(detMock.detectActivity())
            .andReturn(false);
        replay(detMock);
        
        assertTrue(this.rig.register(detMock, ActionType.DETECT));
        assertTrue(this.rig.register(detectorMock, ActionType.DETECT));
        assertFalse(this.rig.isActivityDetected());
        
        verify(detectorMock);
        verify(detMock);
    }
    
    /**
     * Tests the detecting activity success with no registered actions.
     */
    @Test
    public void testActivityDetectionNoAction()
    {
        assertTrue(this.rig.assign("mdiponio"));
        assertTrue(this.rig.isActivityDetected());
    }
    
    /**
     * Tests the detecting activity success with no registered actions.
     */
    @Test
    public void testActivityDetectionNoSession()
    {
        assertFalse(this.rig.isActivityDetected());
    }
    
    /**
     * Tests the files detector when in session.
     * 
     * @throws Exception 
     */
    @Test
    public void testFilesDetector() throws Exception
    {
        IFilesDetectorAction detector = createMock(IFilesDetectorAction.class);
        expect(detector.getActionType()).andReturn("Mock Files Detector").atLeastOnce();
        
        Set<File> files = new HashSet<File>();  
        files.add(new File("./file1"));
        files.add(new File("./file1").getAbsoluteFile());
        files.add(new File("./file2"));
        files.add(new File("./file3"));
        expect(detector.listFiles()).andReturn(files).atLeastOnce();
        replay(detector);
        
        assertTrue(this.rig.register(detector, ActionType.FILES));
        
        Set<File> retFiles = this.rig.detectSessionFiles();
        assertNotNull(retFiles);
        assertEquals(3, retFiles.size());
        assertTrue(files.equals(retFiles));
        
        verify(detector);
    }
    
    /**
     * Tests registering a files detector with no session.
     */
    @Test
    public void testFilesDetectorNoSession()
    {
        IFilesDetectorAction detector = createMock(IFilesDetectorAction.class);
        expect(detector.getActionType()).andReturn("Mock Files Detector").atLeastOnce();
        replay(detector);
        
        assertTrue(this.rig.register(detector, ActionType.FILES));
        assertEquals(0, this.rig.detectSessionFiles().size());
        
        verify(detector);
    }
    
    /**
     * Test file detection with no detector.
     */
    @Test
    public void testFilesDetectorNoAction() 
    {
        assertTrue(this.rig.assign("mdiponio"));
        assertEquals(0, this.rig.detectSessionFiles().size());
    }

    /**
     * Returns true if the key is in the array.
     * 
     * @param arr array to search
     * @param val value to search for 
     * @return true if in array, false otherwise
     */
    private boolean inArray(String[] arr, String val)
    {
        for (String s : arr)
        {
            if (val.equals(s)) return true;
        }
        return false;
    }
}
