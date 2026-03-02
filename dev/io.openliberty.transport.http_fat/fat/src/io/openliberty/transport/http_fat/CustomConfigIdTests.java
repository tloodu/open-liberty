/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http_fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/*
 * Verifies config IS picked up when a custom ID is specified 
 *  - httpOptions, headers, compression, remoteIp, and samesite elements are tested. 
 *  - There are exceptions (element picked up regarddless of ID):
 *        -  httpEndpoint, tcpOptions, and httpAccessLogging.
 *  - Note: defaultSSLOptions is not tested 
 * Links:
 * https://github.com/OpenLiberty/open-liberty/issues/32872 
 * https://github.ibm.com/websphere/POC-Meeting/issues/192 
 */
// @Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CustomConfigIdTests {

    private static final String CLASS_NAME = CustomConfigIdTests.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    private static final String APP_NAME = "ConfigTest";

    @Server("CustomConfigIdServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, APP_NAME + ".war", "io.openliberty.transport.http.config");

        LOG.info("Starting server with custom (non-default*) configuration IDs");
        server.startServer(CustomConfigIdTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Testing that testHeaders configuration is applied
     * Custom headers should appear in the response.
     */
    @Test
    public void testCustomHeadersApplied() throws Exception {        
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            conn.connect();
            
            String customHeader = conn.getHeaderField("Custom-Header");
            String missingHeader = conn.getHeaderField("Test-Missing");
            
            assertNotNull("testHeaders should work - X-Custom-Test-Header should be present", customHeader);
            assertTrue("X-Custom-Test-Header should have correct value", 
                      "Test-Custom".equals(customHeader));
            
            assertNotNull("testHeaders should work - X-Test-Missing should be present", missingHeader);
            assertTrue("X-Test-Missing should have correct value",
                      "TestMissingValue".equals(missingHeader));
            
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Testing that testCompression configuration is applied
     * Response should be compressed.
     */
    @Test
    public void testCustomCompressionApplied() throws Exception {
        
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            conn.connect();
            
            String vary = conn.getHeaderField("Vary");
            
            assertNotNull("testCompression should work - Vary header should be present", vary);
            assertTrue("Vary header should contain Accept-Encoding",
                      vary != null && vary.contains("Accept-Encoding"));
            
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Testing that testRemoteIp configuration is applied
     * When RemoteIP is active, X-Forwarded-For header should be processed
     * and the forwarded IP should be returned as the remote address.
     */
    @Test
    public void testCustomRemoteIpApplied() throws Exception {
        LOG.info("Testing that testRemoteIp configuration is applied");
        
        // Make a request with X-Forwarded-For header
                URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/remoteip");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            // Set X-Forwarded-For header with a fake IP
            conn.setRequestProperty("X-Forwarded-For", "192.168.1.100");
            conn.setRequestMethod("GET");
            conn.connect();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            String remoteAddr = null;
            
            while ((line = reader.readLine()) != null) {
                LOG.info("Response: " + line);
                if (line.startsWith("RemoteAddr: ")) {
                    remoteAddr = line.substring("RemoteAddr: ".length());
                }
            }
            reader.close();
            
            // With testRemoteIp, the X-Forwarded-For should be used
            // and we should see the forwarded IP (192.168.1.100)
            assertNotNull("RemoteAddr should be present in response", remoteAddr);
            assertTrue("RemoteAddr should be the X-Forwarded-For value (192.168.1.100), got: " + remoteAddr,
                      remoteAddr.equals("192.168.1.100"));
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Testing that testSameSite configuration is picked up.
     * TestCookie should have SameSite applied. 
     */
    @Test
    public void testCustomSameSiteApplied() throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                          + "/" + APP_NAME + "/cookie");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.connect();

            int responseCode = conn.getResponseCode();
            LOG.info("Response code: " + responseCode);

            List<String> cookies = conn.getHeaderFields().get("Set-Cookie");

            LOG.info("Cookie: " + (cookies != null ? cookies : "null"));

            // Verify the TestCookie was set
            boolean foundTestCookie = false;
            for (String cookie : cookies) {
                if (cookie.contains("TestCookie=")) {
                    foundTestCookie = true;
                    assertTrue("defaultSameSite should be ignored - SameSite attribute should NOT be found! " + cookie,
                               cookie.toLowerCase().contains("samesite="));
                }
            }
            assertTrue("TestCookie should be present in Set-Cookie headers", foundTestCookie);

        } finally {
            conn.disconnect();
        }
    }

    /**
     * Verifies that testTCPOptions configuration is applied.
     * The server should log the maxOpenConnections value from the testTCPOptions config.
     */
    @Test
    public void testCustomTCPOptionsApplied() throws Exception {
        assertNotNull("testTCPOptions should be applied - expected maxOpenConnections log message",
                     server.waitForStringInTrace(".*maxOpenConnections: 10.*"));
    }

    /**
     * Verifies that custom HTTP Options configuration is applied.
     */
    @Test
    public void testCustomHttpOptionsApplied() throws Exception {                
        assertNotNull("Custom readTimeout should be loaded",
                     server.waitForStringInTrace(".*Config: Read timeout is 200000*"));
    }

    /**
     * Verifies access logging works, and the pattern matches. 
     */
    @Test
    public void testDefaultAccessLoggingNotIgnored() throws Exception {
        Integer initialLineCount = server.findStringsInFileInLibertyServerRoot("GET", "logs/http_access.log").size();

        // Make a request to generate access log entry
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.connect();
            int responseCode = conn.getResponseCode();
            LOG.info("Response code: " + responseCode);
        } finally {
            conn.disconnect();
        }

        // Small pause
        Thread.sleep(1000);

        // Check that access log has one more entry than before
        List<String> lines = server.findStringsInFileInLibertyServerRoot("GET", "logs/http_access.log");
        assertTrue("Access log should have one more line entry", (initialLineCount+1) == lines.size());

        String lastLine = lines.get(lines.size() - 1);
        LOG.info("Last access log line: " + lastLine);

        // No timestamp -- for easier matching
        assertTrue("Access log entry does not match expected format '%h %u \"%r\" %s %b'. Entry: " + lastLine,
                   lastLine.contains("127.0.0.1 - \"GET / HTTP/1.1\" 200 17152"));
    }
}
