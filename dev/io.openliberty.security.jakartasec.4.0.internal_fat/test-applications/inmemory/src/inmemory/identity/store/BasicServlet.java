/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package inmemory.identity.store;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.servlet.annotation.WebServlet;

/*
 *
 */
@SuppressWarnings("serial")
@WebServlet("/basicServlet")
public class BasicServlet extends FATServlet {

    @Test
    public void testPassed() {
        assertEquals("test passed", "test passed");
    }
}