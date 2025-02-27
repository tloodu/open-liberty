/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.token.ltpa.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.LTPA;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;

@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LTPAInitializationVectorTests {

    // Initialize needed strings for the tests
    protected static String METHODS = null;
    protected static final String APP_NAME = "ltpaKeyRotationTestServer";
    protected static final String PROGRAMMATIC_API_SERVLET = "ProgrammaticAPIServlet";
    protected static final String authTypeForm = "FORM";
    protected static final String cookieName = "LtpaToken2";

    // Initialize two liberty servers for form login
    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.initializationVectorTestServer1");
    private static LibertyServer server2 = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.initializationVectorTestServer2");

    private static final Class<?> thisClass = LTPAKeyRotationTests.class;

    // Initialize the user
    private static final String validUser = "user1";
    private static final String validPassword = "user1pwd";

    private static final String[] serverShutdownMessages = { "CWWKG0058E", "CWWKG0083W", "CWWKS4106E", "CWWKS4109W", "CWWKS4110E", "CWWKS4111E", "CWWKS4112E", "CWWKS4113W",
                                                             "CWWKS4114W", "CWWKS4115W", "CWWKS1859E" };

    private static String validationKeyPassword = "{xor}Lz4sLCgwLTs=";
    private static String validationKeyFIPSPassword = "{xor}CDo9Hgw=";

    // Initialize the FormLogin Clients
    private static FormLoginClient server1FlClient1;
    private static FormLoginClient server1FlClient2;
    private static FormLoginClient server2FlClient1;
    private static FormLoginClient server2FlClient2;

//    private static FormLoginClient server1FlClient1 = new FormLoginClient(server1, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin1");
//    private static FormLoginClient server1FlClient2 = new FormLoginClient(server1, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin2");
//    private static FormLoginClient server2FlClient1 = new FormLoginClient(server2, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin1");
//    private static FormLoginClient server2FlClient2 = new FormLoginClient(server2, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin2");

    // Define server.xml files
    private static final String DEFAULT_SERVER_XML = "server.xml";
    private static final String DEFAULT_FIPS_SERVER_XML = "serverFIPS.xml";

    // Define the paths to the key files
    private static final String DEFAULT_KEY_PATH = "resources/security/ltpa.keys";
    private static final String CONFIGURED_VALIDATION_KEY1_PATH = "resources/security/configuredValidation1.keys";
    private static final String VALIDATION_KEYS_PATH = "resources/security/";
    private static final String VALIDATION_KEY1_PATH = "resources/security/validation1.keys";
    private static final String VALIDATION_KEY2_PATH = "resources/security/validation2.keys";

    // Define the paths to the alternate key files
    private static String ALT_VALIDATION_KEY1_PATH = "alternate/validation1.keys";
    private static String ALT_VALIDATION_KEY2_PATH = "alternate/validation2.keys";

    // Define the paths to the server.xml files
    private static final String relativeDirectory1 = server1.getServerRoot();
    private static final String wlpDirectory1 = server1.getInstallRoot();
    private static final String baseDirectory1 = server1.getInstallRootParent();

    private static final String relativeDirectory2 = server2.getServerRoot();
    private static final String wlpDirectory2 = server2.getInstallRoot();
    private static final String baseDirectory2 = server2.getInstallRootParent();

    // Define the remote message log file
    private static RemoteFile messagesLogFile1 = null;
    private static RemoteFile messagesLogFile2 = null;

    @Rule
    public TestRule passwordChecker1 = new LeakedPasswordChecker(server1);
    public TestRule passwordChecker2 = new LeakedPasswordChecker(server2);

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        // Function to make it easier to see when each test starts and ends
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(), "\n@@@@@@@@@@@@@@@@@\nEntering test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(), "\n@@@@@@@@@@@@@@@@@\nExiting test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }
    };

    @BeforeClass
    public static void setUp() throws Exception {

        // Copy validation key file (validation1.keys) to the server

        server2.useSecondaryHTTPPort();

        LibertyServer[] servers = { server1, server2 };

        for (LibertyServer server : servers) {

            server.setupForRestConnectorAccess();

            server.startServer(true);

            assertNotNull("Featurevalid did not report update was complete",
                          server.waitForStringInLog("CWWKF0008I"));
            assertNotNull("Security service did not report it was ready",
                          server.waitForStringInLog("CWWKS0008I"));
            assertNotNull("The application did not report is was started",
                          server.waitForStringInLog("CWWKZ0001I"));
            // Wait for the LTPA configuration to be ready
            assertNotNull("Expected LTPA configuration ready message not found in the log.",
                          server.waitForStringInLog("CWWKS4105I"));

        }

        messagesLogFile1 = server1.getDefaultLogFile();
        messagesLogFile2 = server2.getDefaultLogFile();

        server1FlClient1 = new FormLoginClient(server1, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin1");
        server1FlClient2 = new FormLoginClient(server1, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin2");
        server2FlClient1 = new FormLoginClient(server2, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin1");
        server2FlClient2 = new FormLoginClient(server2, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin2");

        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server1);
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY2_PATH, server2);

    }

    @Before
    public void moveLogMark() throws Exception {
        moveLogMarkForServer(server1);
        moveLogMarkForServer(server2);

    }

    @After
    public void after() throws Exception {
        resetConnection();
        resetServer(server1);
        resetServer(server2);
    }

    public void resetConnection() {
        server1FlClient1.resetClientState();
        server1FlClient2.resetClientState();

        server2FlClient1.resetClientState();
        server2FlClient2.resetClientState();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server1.stopServer(serverShutdownMessages);
            server2.stopServer(serverShutdownMessages);
        } finally {
            server1FlClient1.resetClientState();
            server1FlClient2.resetClientState();

            server2FlClient1.resetClientState();
            server2FlClient2.resetClientState();
        }
    }

    /*
     * POSITIVE TEST CASE:
     * 1. Server #1 and Server #2 contain different primary LTPA Keys with different/same LTPA keys passwords
     * 2. Access a simple servlet with form login using valid credentials on Server #1
     * authentication should be successful and retrieve the SSO cookie
     * 3. Copy the LTPA primary key from Server #1 and place it in Server #2
     * rename the copied key to the name specified for the validation key in server.xml, and ensure that the password is set to that of the primary key in Server #1
     * 4. Server #2 must be restarted
     * 5. Attempt to access the simple servlet with form login on Server #2 using the SSO cookie from Server #1
     * Authentication should be successful because the correct validation key, password and IV is provided
     */

    @Mode(TestMode.LITE)
    @Test
    public void testLTPAValidationKeyUsage_twoServers_samePW() throws Exception {

        // Configure both servers, and replace the randomly generated LTPA keys with known valid keys
        configureServer("true", "10", true, server1);
        renameFileIfExists(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, true, server1);

        configureServer("true", "10", true, server2);
        renameFileIfExists(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, true, server2);

        // Initial login to simple servlet for form login1
        String response1 = server1FlClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookie from the login
        String server1Cookie = server1FlClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", server1Cookie);

        // Attempt to login to the simple servlet on server #2 and assert that the login fails
        assertTrue("An invalid cookie should result in authorization challenge",
                   server2FlClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, server1Cookie));

        // Copy the ltpa.keys file to server #2 and rename it as validation1.key
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server2);
        renameFileIfExists(VALIDATION_KEY1_PATH, CONFIGURED_VALIDATION_KEY1_PATH, true, server2);

        server2.stopServer(serverShutdownMessages);
        server2.startServer(true);

        // Attempt to login to the simple servlet on server #2 and assert that the login is successful
        server2FlClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, server1Cookie);

    }

    public void moveLogMarkForServer(LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(messagesLogFile1);
        server.setMarkToEndOfLog(messagesLogFile2);
    }

    // Function to do the server configuration for all the tests.
    public void configureServer(String monitorValidationKeysDir, String monitorInterval, Boolean waitForLTPAConfigReadyMessage, LibertyServer server) throws Exception {
        configureServer(monitorValidationKeysDir, monitorInterval, waitForLTPAConfigReadyMessage, true, server);
    }

    public void configureServer(String monitorValidationKeysDir, String monitorInterval, Boolean waitForLTPAConfigReadyMessage, boolean setLogMarkToEnd,
                                LibertyServer server) throws Exception {
        configureServer("polled", monitorValidationKeysDir, monitorInterval, waitForLTPAConfigReadyMessage, true, server);
    }

    /**
     * Function to do the server configuration for all the tests.
     * Assert that the server has with a default ltpa.keys file.
     *
     * @param updateTrigger
     * @param monitorValidationKeysDir
     * @param monitorInterval
     * @param waitForLTPAConfigReadyMessage
     * @param setLogMarkToEnd
     *
     * @throws Exception
     */
    public void configureServer(String updateTrigger, String monitorValidationKeysDir, String monitorInterval, Boolean waitForLTPAConfigReadyMessage,
                                boolean setLogMarkToEnd, LibertyServer server) throws Exception {
        moveLogMarkForServer(server);
        // Get the server configuration
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();

        // Check if the configuration needs to be updated
        boolean configurationUpdateNeeded = false;

        configurationUpdateNeeded = setLTPAupdateTriggerElement(ltpa, updateTrigger)
                                    | setLTPAmonitorValidationKeysDirElement(ltpa, monitorValidationKeysDir)
                                    | setLTPAmonitorIntervalElement(ltpa, monitorInterval);

        // Apply server configuration update if needed
        if (configurationUpdateNeeded) {
            updateConfigDynamically(server, serverConfiguration);

            if (updateTrigger.equals("polled") && monitorValidationKeysDir.equals("true") && monitorInterval.equals("0")) {
                // Wait for a warning message message to be logged
                assertNotNull("Expected LTPA configuration warning message not found in the log.",
                              server.waitForStringInLog("CWWKS4113W", 5000));
            }

            if (waitForLTPAConfigReadyMessage) {
                // Wait for the LTPA configuration to be ready after the change
                assertNotNull("Expected LTPA configuration ready message not found in the log.",
                              server.waitForStringInLog("CWWKS4105I", 5000));
            }
        }

        // Assert that a default ltpa.keys file is generated
        assertFileWasCreated(DEFAULT_KEY_PATH, server);
        server.setKeysAndJVMOptsForFips();
        if (setLogMarkToEnd)
            server.setMarkToEndOfLog(messagesLogFile1);
        server.setMarkToEndOfLog(messagesLogFile2);
    }

    // Function to set the monitorValidationKeysDir to true or false
    public boolean setLTPAmonitorValidationKeysDirElement(LTPA ltpa, String value) {
        if (!ltpa.monitorValidationKeysDir.equals(value)) {
            ltpa.monitorValidationKeysDir = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure monitorInterval to a specific value
    public boolean setLTPAmonitorIntervalElement(LTPA ltpa, String value) {
        if (!ltpa.monitorInterval.equals(value)) {
            ltpa.monitorInterval = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure the expiration time for the LTPA token to a specific value
    public boolean setLTPAexpiryElement(LTPA ltpa, String value) {
        if (!ltpa.expiration.equals(value)) {
            ltpa.expiration = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure the updateTrigger to a specific value
    public boolean setLTPAupdateTriggerElement(LTPA ltpa, String value) {
        if (!ltpa.updateTrigger.equals(value)) {
            ltpa.updateTrigger = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to update the server configuration dynamically
    public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());

        server.updateServerConfiguration(config);
        //CWWKG0017I: The server configuration was successfully updated in {0} seconds.
        //CWWKG0018I: The server configuration was not updated. No functional changes were detected.
        String logLine = server.waitForStringInLogUsingMark("CWWKG001[7-8]I");

        // Wait for feature update to be completed or LTPA configuration to get ready
        Thread.sleep(2000);
    }

    /**
     * Delete the file if it exists. If we can't delete it, then
     * throw an exception as we need to be able to delete these files.
     *
     * @param filePath
     *
     * @throws Exception
     */
    private static void deleteFileIfExists(String filePath, boolean checkFileIsGone, LibertyServer server) throws Exception {
        Log.info(thisClass, "deleteFileIfExists", "filepath: " + filePath);
        if (fileExists(filePath, 1, server)) {
            Log.info(thisClass, "deleteFileIfExists", "file exists, deleting...");
            server.deleteFileFromLibertyServerRoot(filePath);

            // Double check to make sure the file is gone
            if (checkFileIsGone && fileExists(filePath, 1, server))
                throw new Exception("Unable to delete file: " + filePath);
        }
    }

    /**
     * Assert that file was created otherwise print a message saying it's an intermittent failure.
     *
     * @param filePath
     *
     * @throws Exception
     */
    private void assertFileWasCreated(String filePath, LibertyServer server) throws Exception {
        assertTrue("The file was not created as expected. If this is an intermittent failure, then increase the wait time.",
                   fileExists(filePath, server));
    }

    /**
     * Check to see if the file exists. We will wait a bit to ensure
     * that the system was not slow to flush the file.
     *
     * @param filePath
     *
     * @throws Exception
     */
    private static boolean fileExists(String filePath, LibertyServer server) throws Exception {
        return fileExists(filePath, 5, server);
    }

    /**
     * Check to see if the file exists. We will retry a few times to ensure
     * that the system was not slow to flush the file.
     *
     * @param filePath
     * @param numberOfTries
     *
     * @throws Exception
     */
    private static boolean fileExists(String filePath, int numberOfTries, LibertyServer server) throws Exception {
        boolean exists = false;
        boolean exceptionHasBeenPrinted = false;
        int count = 0;
        do {
            // Sleep 2 seconds
            if (count != 0) {
                Thread.sleep(3000);
                Log.info(thisClass, "fileExists", "waiting 2s...");
            }
            try {
                exists = server.getFileFromLibertyServerRoot(filePath).exists();
            } catch (Exception e) {
                // The file does not exist if there's an exception
                Log.info(thisClass, "fileExists", "The file does not exist");
                exists = false;
                // We don't want to print the same exception over and over again... so we'll only print it one time.
                if (!exceptionHasBeenPrinted) {
                    e.printStackTrace();
                    exceptionHasBeenPrinted = true;
                }
            }
            count++;
        }
        // Wait up to 10 seconds for the key file to appear
        while ((!exists) && count < numberOfTries);

        return exists;
    }

    /**
     * Copies a file to the "server/resources/security/" directory
     *
     * @param sourceFile
     *
     * @throws Exception
     */
    private static void copyFileToServerResourcesSecurityDir(String sourceFile, LibertyServer server) throws Exception {
        Log.info(thisClass, "copyFileToServerResourcesSecurityDir", "sourceFile: " + sourceFile);
        String serverRoot = server.getServerRoot();
        String securityResources = serverRoot + "/resources/security";
        server.setServerRoot(securityResources);
        server.copyFileToLibertyServerRoot(sourceFile);
        server.setServerRoot(serverRoot);
    }

    /**
     * Rename the file if it exists. If we can't rename it, then
     * throw an exception as we need to be able to rename these files.
     * If checkFileIsGone is true, then we will double check to make
     * sure the file is gone.
     *
     * @param filePath
     * @param newFilePath
     * @param checkFileIsGone
     *
     * @throws Exception
     */
    private static void renameFileIfExists(String filePath, String newFilePath, boolean checkFileIsGone, LibertyServer server) throws Exception {
        Log.info(thisClass, "renameFileIfExists", "\nfilepath: " + filePath + "\nnewFilePath: " + newFilePath);
        server.setMarkToEndOfLog(server.getDefaultLogFile());

        if (fileExists(filePath, 1, server)) {
            if (fileExists(newFilePath, 1, server)) {
                LibertyFileManager.moveLibertyFile(server.getFileFromLibertyServerRoot(filePath), server.getFileFromLibertyServerRoot(newFilePath));
            } else {
                Log.info(thisClass, "renameFileIfExists", "Calling server.renameLibertyServerRootFile");
                server.renameLibertyServerRootFile(filePath, newFilePath);
            }
        }

        // Double check to make sure the file is gone
        if (checkFileIsGone && fileExists(filePath, 1, server))
            throw new Exception("Unable to rename file: " + filePath);
    }

    /**
     * Reset the server to the default configuration
     *
     * @throws Exception
     */
    private void resetServer(LibertyServer server) throws Exception {
        Log.info(thisClass, "resetServer", "entering");

        // Make sure the mark is at the end of the log, so we don't use earlier messages.
        moveLogMarkForServer(server);

        // We need to put the base config back, otherwise the waits below will timeout on some tests
        configureServer("true", "10", true, server);

        // Delete all ltpa keys files in the security directory
        deleteFileIfExists(DEFAULT_KEY_PATH, false, server);
        deleteFileIfExists(VALIDATION_KEY1_PATH, true, server);
        deleteFileIfExists(VALIDATION_KEY2_PATH, true, server);
        deleteFileIfExists(CONFIGURED_VALIDATION_KEY1_PATH, true, server);

        // Wait for the LTPA configuration to be ready after the change
//        assertNotNull("Expected LTPA configuration ready message not found in the log.",
//                      server.waitForStringInLog("CWWKS4105I", 10000));

        // Assert that a default ltpa.keys file exists prior to next test case
        assertFileWasCreated(DEFAULT_KEY_PATH, server);
        Log.info(thisClass, "resetServer", "exiting");
    }
}