/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
 * Unit tests for MLDSAAlgorithmType enum - Phase 2 ML-DSA algorithm types.
 */
public class MLDSAAlgorithmTypeTest {

    @Test
    public void testML_DSA_44_Properties() {
        MLDSAAlgorithmType algo = MLDSAAlgorithmType.ML_DSA_44;

        assertEquals("Algorithm name should be ML-DSA-44", 
                "ML-DSA-44", algo.getAlgorithmName());
        assertEquals("Security level should be 1", 1, algo.getSecurityLevel());
        assertEquals("NIST security level should be 1", 1, algo.getNistSecurityLevel());
        assertEquals("Public key size should be 1312", 1312, algo.getPublicKeySize());
        assertEquals("Private key size should be 2560", 2560, algo.getPrivateKeySize());
        assertEquals("Signature size should be 2420", 2420, algo.getSignatureSize());
    }

    @Test
    public void testML_DSA_65_Properties() {
        MLDSAAlgorithmType algo = MLDSAAlgorithmType.ML_DSA_65;

        assertEquals("Algorithm name should be ML-DSA-65", 
                "ML-DSA-65", algo.getAlgorithmName());
        assertEquals("Security level should be 3", 3, algo.getSecurityLevel());
        assertEquals("NIST security level should be 3", 3, algo.getNistSecurityLevel());
        assertEquals("Public key size should be 1952", 1952, algo.getPublicKeySize());
        assertEquals("Private key size should be 4032", 4032, algo.getPrivateKeySize());
        assertEquals("Signature size should be 3309", 3309, algo.getSignatureSize());
    }

    @Test
    public void testML_DSA_87_Properties() {
        MLDSAAlgorithmType algo = MLDSAAlgorithmType.ML_DSA_87;

        assertEquals("Algorithm name should be ML-DSA-87", 
                "ML-DSA-87", algo.getAlgorithmName());
        assertEquals("Security level should be 5", 5, algo.getSecurityLevel());
        assertEquals("NIST security level should be 5", 5, algo.getNistSecurityLevel());
        assertEquals("Public key size should be 2592", 2592, algo.getPublicKeySize());
        assertEquals("Private key size should be 4896", 4896, algo.getPrivateKeySize());
        assertEquals("Signature size should be 4627", 4627, algo.getSignatureSize());
    }

    @Test
    public void testFromSecurityLevel() {
        assertEquals("Security level 1 should return ML-DSA-44",
                MLDSAAlgorithmType.ML_DSA_44, 
                MLDSAAlgorithmType.fromSecurityLevel(1));

        assertEquals("Security level 3 should return ML-DSA-65",
                MLDSAAlgorithmType.ML_DSA_65, 
                MLDSAAlgorithmType.fromSecurityLevel(3));

        assertEquals("Security level 5 should return ML-DSA-87",
                MLDSAAlgorithmType.ML_DSA_87, 
                MLDSAAlgorithmType.fromSecurityLevel(5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromSecurityLevel_Invalid() {
        MLDSAAlgorithmType.fromSecurityLevel(99);
    }

    @Test
    public void testFromAlgorithmName() {
        assertEquals("'ML-DSA-44' should return ML_DSA_44",
                MLDSAAlgorithmType.ML_DSA_44, 
                MLDSAAlgorithmType.fromAlgorithmName("ML-DSA-44"));

        assertEquals("'ML-DSA-65' should return ML_DSA_65",
                MLDSAAlgorithmType.ML_DSA_65, 
                MLDSAAlgorithmType.fromAlgorithmName("ML-DSA-65"));

        assertEquals("'ML-DSA-87' should return ML_DSA_87",
                MLDSAAlgorithmType.ML_DSA_87, 
                MLDSAAlgorithmType.fromAlgorithmName("ML-DSA-87"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromAlgorithmName_Invalid() {
        MLDSAAlgorithmType.fromAlgorithmName("INVALID-ALGORITHM");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromAlgorithmName_Null() {
        MLDSAAlgorithmType.fromAlgorithmName(null);
    }

    @Test
    public void testKeySizeProgression() {
        // Verify that key sizes increase with security level
        assertTrue("ML-DSA-65 public key should be larger than ML-DSA-44",
                MLDSAAlgorithmType.ML_DSA_65.getPublicKeySize() > 
                MLDSAAlgorithmType.ML_DSA_44.getPublicKeySize());

        assertTrue("ML-DSA-87 public key should be larger than ML-DSA-65",
                MLDSAAlgorithmType.ML_DSA_87.getPublicKeySize() > 
                MLDSAAlgorithmType.ML_DSA_65.getPublicKeySize());

        assertTrue("ML-DSA-65 private key should be larger than ML-DSA-44",
                MLDSAAlgorithmType.ML_DSA_65.getPrivateKeySize() > 
                MLDSAAlgorithmType.ML_DSA_44.getPrivateKeySize());

        assertTrue("ML-DSA-87 private key should be larger than ML-DSA-65",
                MLDSAAlgorithmType.ML_DSA_87.getPrivateKeySize() > 
                MLDSAAlgorithmType.ML_DSA_65.getPrivateKeySize());
    }

    @Test
    public void testSignatureSizeProgression() {
        // Verify that signature sizes increase with security level
        assertTrue("ML-DSA-65 signature should be larger than ML-DSA-44",
                MLDSAAlgorithmType.ML_DSA_65.getSignatureSize() > 
                MLDSAAlgorithmType.ML_DSA_44.getSignatureSize());

        assertTrue("ML-DSA-87 signature should be larger than ML-DSA-65",
                MLDSAAlgorithmType.ML_DSA_87.getSignatureSize() > 
                MLDSAAlgorithmType.ML_DSA_65.getSignatureSize());
    }

    @Test
    public void testToString() {
        assertEquals("ML_DSA_44 toString should return algorithm name",
                "ML-DSA-44", MLDSAAlgorithmType.ML_DSA_44.toString());

        assertEquals("ML_DSA_65 toString should return algorithm name",
                "ML-DSA-65", MLDSAAlgorithmType.ML_DSA_65.toString());

        assertEquals("ML_DSA_87 toString should return algorithm name",
                "ML-DSA-87", MLDSAAlgorithmType.ML_DSA_87.toString());
    }

    @Test
    public void testEnumValues() {
        MLDSAAlgorithmType[] values = MLDSAAlgorithmType.values();
        
        assertEquals("Should have exactly 3 ML-DSA variants", 3, values.length);
        assertEquals("First value should be ML_DSA_44", 
                MLDSAAlgorithmType.ML_DSA_44, values[0]);
        assertEquals("Second value should be ML_DSA_65", 
                MLDSAAlgorithmType.ML_DSA_65, values[1]);
        assertEquals("Third value should be ML_DSA_87", 
                MLDSAAlgorithmType.ML_DSA_87, values[2]);
    }

    @Test
    public void testEnumValueOf() {
        assertEquals("valueOf('ML_DSA_44') should return ML_DSA_44",
                MLDSAAlgorithmType.ML_DSA_44, 
                MLDSAAlgorithmType.valueOf("ML_DSA_44"));

        assertEquals("valueOf('ML_DSA_65') should return ML_DSA_65",
                MLDSAAlgorithmType.ML_DSA_65, 
                MLDSAAlgorithmType.valueOf("ML_DSA_65"));

        assertEquals("valueOf('ML_DSA_87') should return ML_DSA_87",
                MLDSAAlgorithmType.ML_DSA_87, 
                MLDSAAlgorithmType.valueOf("ML_DSA_87"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnumValueOf_Invalid() {
        MLDSAAlgorithmType.valueOf("INVALID");
    }

    @Test
    public void testSecurityLevelMapping() {
        // Verify NIST security level mapping
        assertEquals("Level 1 = 128-bit quantum security", 
                1, MLDSAAlgorithmType.ML_DSA_44.getNistSecurityLevel());
        assertEquals("Level 3 = 192-bit quantum security", 
                3, MLDSAAlgorithmType.ML_DSA_65.getNistSecurityLevel());
        assertEquals("Level 5 = 256-bit quantum security", 
                5, MLDSAAlgorithmType.ML_DSA_87.getNistSecurityLevel());
    }
}

// Made with Bob
