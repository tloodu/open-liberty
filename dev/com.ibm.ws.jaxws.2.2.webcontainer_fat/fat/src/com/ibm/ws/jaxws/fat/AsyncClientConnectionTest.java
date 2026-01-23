/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.samples.jaxws.catalog.server.servlet.AsyncClientConnectionServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class AsyncClientConnectionTest extends FATServletClient {

    private static final String APP_NAME = "calculator";

    @Server("AsyncClientConnectionServer")
    @TestServlet(servlet = AsyncClientConnectionServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    private static String wsdlLocation;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "com.ibm.samples.jaxws.catalog.server", "com.ibm.samples.jaxws.catalog.server.servlet");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("AsyncClientConnectionServer.log");
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        assertNotNull("Application calculator does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));
        wsdlLocation = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/").append(APP_NAME).append("/calculator?wsdl").toString();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
    }

}
