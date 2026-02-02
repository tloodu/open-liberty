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
package web.cdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.ws.jca.fat.classloading.cdilib.CDIExtension;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.resource.AdministeredObjectDefinition;
import jakarta.servlet.annotation.WebServlet;
import ra.DummyInterface;

@AdministeredObjectDefinition(name = "java:comp/env/jca/dummyaod",
                              description = "Test Administered Object",
                              resourceAdapter = "#fvtra",
                              className = "ra.DummyImpl",
                              interfaceName = "ra.DummyInterface",
                              properties = { "message=DUMMY_MESSAGE" })
@WebServlet("/*")
public class CDITestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Resource(name = "jca/dummyaodRef", lookup = "java:comp/env/jca/dummyaod")
    DummyInterface dummy;

    /**
     * Test that CDI extension from library is loaded and invoked by the CDI container.
     */
    @Test
    public void testCDIBeanFromLibrary() throws Exception {

        assertTrue("CDI Extension was not loaded", CDIExtension.isExtensionLoaded());
        assertEquals(CDIExtension.MESSAGE, CDIExtension.getMessage());

        assertNotNull(dummy);
        assertEquals("DUMMY_MESSAGE", dummy.message());

    }
}
