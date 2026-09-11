/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa;

import java.io.Serializable;
import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;

/**
 * Hybrid key container for LTPA v3 tokens with PQC support.
 *
 * This class manages two key pairs plus a shared AES key:
 * 1. RSA-2048: Classical encryption (backward compatibility)
 * 2. ML-DSA: Post-quantum digital signatures (FIPS 204)
 * 3. Shared AES key: for AES-256-GCM token encryption (same role as in LTPAToken2)
 *
 * Key Format:
 * - RSA keys: PKCS#8 encoded (DER format)
 * - ML-DSA keys: Raw key bytes (FIPS 204 format)
 *
 * Security Properties:
 * - Hybrid approach provides defense-in-depth
 * - Quantum-resistant signatures prevent token forgery
 * - AES-256-GCM authenticated encryption protects token confidentiality
 * 
 * Thread Safety: Immutable after construction
 * 
 * @since Liberty 26.0.0.1
 */
public class LTPAHybridKeys implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(LTPAHybridKeys.class);

    // RSA-2048 keys (classical cryptography)
    private final byte[] rsaPrivateKeyBytes;
    private final byte[] rsaPublicKeyBytes;

    // ML-DSA keys (post-quantum signatures - FIPS 204)
    private final byte[] mldsaPrivateKeyBytes;
    private final byte[] mldsaPublicKeyBytes;
    private final String mldsaAlgorithm; // "ML-DSA-44", "ML-DSA-65", or "ML-DSA-87"

    // Shared AES key for token encryption (used by LTPAToken3 via LTPAKeyUtil.encryptGCM/decryptGCM)
    @Sensitive
    private final byte[] sharedKey;

    // Key metadata
    private final long creationTime;
    private final String keyVersion; // "3.0" for hybrid PQC

    /**
     * Construct hybrid keys with RSA, ML-DSA, and a shared AES key.
     *
     * @param rsaPrivateKeyBytes RSA-2048 private key (PKCS#8 encoded)
     * @param rsaPublicKeyBytes RSA-2048 public key (X.509 encoded)
     * @param mldsaPrivateKeyBytes ML-DSA private key (raw bytes)
     * @param mldsaPublicKeyBytes ML-DSA public key (raw bytes)
     * @param mldsaAlgorithm ML-DSA algorithm identifier
     * @param sharedKey 32-byte AES key for GCM token encryption
     * @throws IllegalArgumentException if any key is null or invalid
     */
    public LTPAHybridKeys(@Sensitive byte[] rsaPrivateKeyBytes,
                          byte[] rsaPublicKeyBytes,
                          @Sensitive byte[] mldsaPrivateKeyBytes,
                          byte[] mldsaPublicKeyBytes,
                          String mldsaAlgorithm,
                          @Sensitive byte[] sharedKey) {
        
        // Validate RSA keys
        if (rsaPrivateKeyBytes == null || rsaPrivateKeyBytes.length == 0) {
            throw new IllegalArgumentException("RSA private key cannot be null or empty");
        }
        if (rsaPublicKeyBytes == null || rsaPublicKeyBytes.length == 0) {
            throw new IllegalArgumentException("RSA public key cannot be null or empty");
        }

        // Validate ML-DSA keys
        if (mldsaPrivateKeyBytes == null || mldsaPrivateKeyBytes.length == 0) {
            throw new IllegalArgumentException("ML-DSA private key cannot be null or empty");
        }
        if (mldsaPublicKeyBytes == null || mldsaPublicKeyBytes.length == 0) {
            throw new IllegalArgumentException("ML-DSA public key cannot be null or empty");
        }
        if (mldsaAlgorithm == null || mldsaAlgorithm.isEmpty()) {
            throw new IllegalArgumentException("ML-DSA algorithm cannot be null or empty");
        }

        if (sharedKey == null || sharedKey.length == 0) {
            throw new IllegalArgumentException("Shared key cannot be null or empty");
        }

        // Store defensive copies
        this.rsaPrivateKeyBytes = rsaPrivateKeyBytes.clone();
        this.rsaPublicKeyBytes = rsaPublicKeyBytes.clone();
        this.mldsaPrivateKeyBytes = mldsaPrivateKeyBytes.clone();
        this.mldsaPublicKeyBytes = mldsaPublicKeyBytes.clone();
        this.mldsaAlgorithm = mldsaAlgorithm;
        this.sharedKey = sharedKey.clone();

        this.creationTime = System.currentTimeMillis();
        this.keyVersion = "3.0";

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Created hybrid keys: RSA-2048 + " + mldsaAlgorithm);
        }
    }

    /**
     * Get RSA private key as LTPAPrivateKey.
     * 
     * @return RSA private key
     */
    public LTPAPrivateKey getRsaPrivateKey() {
        return new LTPAPrivateKey(rsaPrivateKeyBytes.clone());
    }

    /**
     * Get RSA public key as LTPAPublicKey.
     * 
     * @return RSA public key
     */
    public LTPAPublicKey getRsaPublicKey() {
        return new LTPAPublicKey(rsaPublicKeyBytes.clone());
    }

    /**
     * Get RSA private key bytes (PKCS#8 encoded).
     * 
     * @return defensive copy of RSA private key bytes
     */
    @Sensitive
    public byte[] getRsaPrivateKeyBytes() {
        return rsaPrivateKeyBytes.clone();
    }

    /**
     * Get RSA public key bytes (X.509 encoded).
     * 
     * @return defensive copy of RSA public key bytes
     */
    public byte[] getRsaPublicKeyBytes() {
        return rsaPublicKeyBytes.clone();
    }

    /**
     * Get ML-DSA private key bytes.
     * 
     * @return defensive copy of ML-DSA private key bytes
     */
    @Sensitive
    public byte[] getMldsaPrivateKeyBytes() {
        return mldsaPrivateKeyBytes.clone();
    }

    /**
     * Get ML-DSA public key bytes.
     * 
     * @return defensive copy of ML-DSA public key bytes
     */
    public byte[] getMldsaPublicKeyBytes() {
        return mldsaPublicKeyBytes.clone();
    }

    /**
     * Get ML-DSA algorithm identifier.
     *
     * @return ML-DSA algorithm (e.g., "ML-DSA-65")
     */
    public String getMldsaAlgorithm() {
        return mldsaAlgorithm;
    }

    /**
     * Get the shared AES key for GCM token encryption.
     *
     * @return defensive copy of the shared key
     */
    @Sensitive
    public byte[] getSharedKey() {
        return sharedKey.clone();
    }

    /**
     * Get key creation timestamp.
     * 
     * @return creation time in milliseconds since epoch
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Get key version.
     * 
     * @return key version string (always "3.0" for hybrid keys)
     */
    public String getKeyVersion() {
        return keyVersion;
    }

    /**
     * Get security level based on ML-DSA algorithm.
     *
     * Security levels:
     * - Level 1: ML-DSA-44 (128-bit quantum security)
     * - Level 3: ML-DSA-65 (192-bit quantum security)
     * - Level 5: ML-DSA-87 (256-bit quantum security)
     *
     * @return security level (1, 3, or 5)
     */
    public int getSecurityLevel() {
        // Determine security level from ML-DSA algorithm
        if ("ML-DSA-44".equals(mldsaAlgorithm)) {
            return 1; // NIST Level 1 (128-bit quantum security)
        } else if ("ML-DSA-65".equals(mldsaAlgorithm)) {
            return 3; // NIST Level 3 (192-bit quantum security)
        } else if ("ML-DSA-87".equals(mldsaAlgorithm)) {
            return 5; // NIST Level 5 (256-bit quantum security)
        }
        return 0; // Unknown
    }

    /**
     * Clear sensitive key material from memory.
     * Should be called when keys are no longer needed.
     */
    public void clear() {
        Arrays.fill(rsaPrivateKeyBytes, (byte) 0);
        Arrays.fill(mldsaPrivateKeyBytes, (byte) 0);
        Arrays.fill(sharedKey, (byte) 0);
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Cleared sensitive key material");
        }
    }

    @Override
    public String toString() {
        return "LTPAHybridKeys[version=" + keyVersion +
               ", rsa=RSA-2048" +
               ", mldsa=" + mldsaAlgorithm +
               ", securityLevel=" + getSecurityLevel() +
               ", created=" + creationTime + "]";
    }
}

// Made with Bob
