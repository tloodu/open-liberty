/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.pqc;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for PQCRuntimeSupport - ML-DSA availability and Java version detection.
 */
public class PQCRuntimeSupportTest {

    @Test
    public void testIsMLDSAAvailable() {
        // Just verifies the method runs without throwing; result depends on JVM
        boolean result = PQCRuntimeSupport.isMLDSAAvailable();
        System.out.println("ML-DSA available: " + result);
    }

    @Test
    public void testIsPQCSupported_ReturnsBooleanWithoutThrowing() {
        // isPQCSupported() now delegates to isMLDSAAvailable()
        boolean supported = PQCRuntimeSupport.isPQCSupported();
        System.out.println("PQC (ML-DSA) Support: " + (supported ? "Available" : "Not Available"));
    }

    @Test
    public void testGetJavaVersion() {
        String version = PQCRuntimeSupport.getJavaVersion();
        assertNotNull("Java version should not be null", version);
        assertFalse("Java version should not be empty", version.isEmpty());
        System.out.println("Java version: " + version);
    }

    @Test
    public void testGetProviderInfo() {
        String providerInfo = PQCRuntimeSupport.getProviderInfo();
        assertNotNull("Provider info should not be null", providerInfo);
        assertFalse("Provider info should not be empty", providerInfo.isEmpty());
        System.out.println("Provider info: " + providerInfo);
    }
}
