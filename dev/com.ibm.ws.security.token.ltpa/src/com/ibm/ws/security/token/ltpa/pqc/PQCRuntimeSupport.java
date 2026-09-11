/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.pqc;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Runtime support for Post-Quantum Cryptography (PQC) algorithms.
 *
 * This class provides a compatibility layer that:
 * - Compiles with Java 17 (using reflection where needed)
 * - Detects ML-DSA availability at class-load time (Java 26+ via JEP 496)
 * - Gracefully degrades on older Java versions
 *
 * ML-KEM support has been removed. Encryption is delegated to LTPAKeyUtil
 * (AES-256-GCM via LTPACrypto) using a pre-shared key.
 */
public class PQCRuntimeSupport {

    private static final TraceComponent tc = Tr.register(PQCRuntimeSupport.class);

    private static final boolean IS_JAVA_26_OR_LATER;

    static {
        boolean mldsaAvailable = false;
        try {
            // Probe for ML-DSA support (available in Java 26+ via JEP 496)
            KeyPairGenerator.getInstance("ML-DSA");
            mldsaAvailable = true;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Java 26+ ML-DSA support detected");
            }
        } catch (NoSuchAlgorithmException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Java 26+ ML-DSA support not available: " + e.getMessage());
            }
        }
        IS_JAVA_26_OR_LATER = mldsaAvailable;
    }

    /**
     * Check if ML-DSA support is available at runtime.
     *
     * @return true if running on Java 26+ with ML-DSA support
     */
    public static boolean isMLDSAAvailable() {
        return IS_JAVA_26_OR_LATER;
    }

    /**
     * Get the Java version string for diagnostic purposes.
     *
     * @return Java version (e.g., "17.0.12", "26.0.0")
     */
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }
}
