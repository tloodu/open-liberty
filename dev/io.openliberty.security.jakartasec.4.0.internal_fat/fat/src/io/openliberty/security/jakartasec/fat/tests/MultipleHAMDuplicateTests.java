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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import multiple.ham.common.MultipleHAMProtectedResource;
import multiple.ham.custom.hams.CustomHAMOne;
import multiple.ham.custom.hams.CustomHAMOneDuplicate;

/**
 * Tests appSecurity-6.0
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class MultipleHAMDuplicateTests {

    public static final String SERVER_NAME = "jakartaSec40Server";
    public static final String APP_NAME = "CustomHAMApp";
    private static final String CONTEXT_ROOT = "/" + APP_NAME;
    private static final String RESOURCE_PATH = "/resource/test";

    private static String url = null;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive multipleHamApp = ShrinkWrap.create(WebArchive.class,
                                                      APP_NAME + ".war").addPackage("multiple.ham.custom").addClass(MultipleHAMProtectedResource.class).addClass(CustomHAMOne.class).addClass(CustomHAMOneDuplicate.class);

        // The URL is not expected to be modified during this test scope
        url = "http://localhost:" + server.getHttpDefaultPort() + CONTEXT_ROOT + RESOURCE_PATH;

        ShrinkHelper.exportDropinAppToServer(server, multipleHamApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    /*
     * Assert that we find jakarta.enterprise.inject.AmbiguousResolutionException
     *
     * CustomHAMOne and CustomHAMOneDuplicate both have Priority = 100
     */
    @Test
    @ExpectedFFDC({ "jakarta.enterprise.inject.AmbiguousResolutionException" })
    public void testAmbiguousResolutionException() throws Exception {

        // Mark log to check for error message
        server.setMarkToEndOfLog();

        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        int responseCode = conn.getResponseCode();
        assertEquals("Expected status code 403 but got " + responseCode, 403, responseCode);

        // Check that error message appears
        assertNotNull("AmbiguousResolutionException error message should appear in log",
                      server.waitForStringInLogUsingMark("Unable to determine which HttpAuthenticationMechanism to handle request."));

        assertNotNull("AmbiguousResolutionException error message should appear in log",
                      server.waitForStringInLogUsingMark("The following HttpAuthenticationMechanisms have the same priority or Http Authentiation Mechanism Type CustomHAMOne Priority = 100, CustomHAMOneDuplicate Priority = 100"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKS2605E");
    }

}
