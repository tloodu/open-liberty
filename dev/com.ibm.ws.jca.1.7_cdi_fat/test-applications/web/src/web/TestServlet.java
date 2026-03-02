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
package web;

import static org.junit.Assert.assertTrue;

import componenttest.app.FATServlet;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/*")
public class TestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    public void testDropin() {
        System.out.println("KJA1017 called");
        assertTrue(true);
    }

}
