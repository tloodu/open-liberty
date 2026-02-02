/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.jca.fat.classloading;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.cdi.CDITestServlet;

/**
 * Tests classloading and application start sequence for an ear application
 * that has an embedded RAR and a bean defining library.
 */
@RunWith(FATRunner.class)
public class JCACDITest extends FATServletClient {

    private static final String CDI_WAR_NAME = "cdiweb";
    private static final String CDI_APP_NAME = "cdiapp";
    private static final String RAR_NAME = "fvtra";

    @Server("com.ibm.ws.jca.fat.cdi")
    @TestServlet(servlet = CDITestServlet.class, path = CDI_WAR_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        // Create cdi library jar with service provider
        JavaArchive cdilib = ShrinkHelper.buildJavaArchive("cdilib", "com.ibm.ws.jca.fat.classloading.cdilib");
        cdilib.addAsServiceProvider("jakarta.enterprise.inject.spi.Extension", "com.ibm.ws.jca.fat.classloading.cdilib.CDIExtension");

        // Create web application
        WebArchive war = ShrinkHelper.buildDefaultApp(CDI_WAR_NAME, "web.cdi");

        // Create rar application
        ResourceAdapterArchive rar = ShrinkHelper.buildDefaultRar(RAR_NAME, "ra");

        // Create ear with library directory containing the CDI library
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, CDI_APP_NAME + ".ear")
                        .addAsModule(war)
                        .addAsModule(rar)
                        .addAsLibrary(cdilib);
        ShrinkHelper.addDirectory(ear, "test-applications/" + CDI_APP_NAME + "/resources/");

        ShrinkHelper.exportAppToServer(server, ear);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }
}
