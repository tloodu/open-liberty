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

import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests to ensure that the netty configuration options are read.
 * Note that this bucket doesn't verify the properties actually work. 
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class NettyConfigurationTests {

    private static final String CLASS_NAME = NettyConfigurationTests.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    private static final String NETTY_TCP_CLASS_NAME = "io.openliberty.netty.internal.tcp.TCPUtils";
    private static boolean runningNetty = false;

    @Server("NettyConfigServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
        server.startServer(NettyConfigurationTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Checks that the config is read from the server.xml.
     * 
     * @throws Exception
     */
    @Test
    public void testNettyConfigurationPickedUp() throws Exception {
            assertNotNull("scalerMinThreads not found",
                          server.waitForStringInTrace(".*scalerMinThreads=1.*"));
            assertNotNull("scalerMaxThreads not found",
                          server.waitForStringInTrace(".*scalerMaxThreads=10.*"));
            assertNotNull("scalerWindowSize not found",
                          server.waitForStringInTrace(".*scalerWindowSize=1500.*"));
            assertNotNull("scalerDownThreshold not found",
                          server.waitForStringInTrace(".*scalerDownThreshold=0\\.15.*"));
            assertNotNull("scalerUpThreshold not found",
                          server.waitForStringInTrace(".*scalerUpThreshold=0\\.85.*"));
            assertNotNull("scalerDownStep not found",
                          server.waitForStringInTrace(".*scalerDownStep=1.*"));
            assertNotNull("scalerUpStep not found",
                          server.waitForStringInTrace(".*scalerUpStep=1.*"));
            assertNotNull("scalerCycles not found",
                          server.waitForStringInTrace(".*scalerCycles=3.*"));
            assertNotNull("scalerMetricsWindowSize not found",
                          server.waitForStringInTrace(".*scalerMetricsWindowSize=5000.*"));
            assertNotNull("useNativeIO not found",
                          server.waitForStringInTrace(".*useNativeIO=false.*"));
            server.resetLogMarks();
    }

}
