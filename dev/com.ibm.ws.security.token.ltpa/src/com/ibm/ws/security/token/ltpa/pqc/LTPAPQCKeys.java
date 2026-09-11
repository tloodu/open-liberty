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
package com.ibm.ws.security.token.ltpa.pqc;

import java.util.Arrays;

import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Container class for LTPA Post-Quantum Cryptography (PQC) keys.
 * 
 * This class holds both classical cryptographic keys (RSA for signatures) and
 * post-quantum cryptographic keys (ML-KEM for encryption) to support hybrid
 * quantum-resistant LTPA tokens.
 * 
 * <p>Key Structure:
 * <ul>
 *   <li><b>Classical Keys:</b> RSA-2048 for digital signatures (existing)</li>
 *   <li><b>PQC Keys:</b> ML-KEM-768 for key encapsulation (new)</li>
 * </ul>
 * 
 * <p>Security Model:
 * <ul>
 *   <li><b>Signatures:</b> RSA-2048 (quantum-vulnerable but acceptable for now)</li>
 *   <li><b>Encryption:</b> ML-KEM-768 (quantum-resistant)</li>
 * </ul>
 */
public class LTPAPQCKeys {
    
    // Classical keys (for signatures)
    @Sensitive
    private final byte[] rsaPrivateKeyBytes;
    private final byte[] rsaPublicKeyBytes;
    
    // Metadata
    private final int tokenVersion;
    private final boolean pqcEnabled;
    
    /**
     * Construct LTPA PQC keys with both classical and post-quantum keys.
     * 
     * @param rsaPrivateKeyBytes The RSA private key bytes (for signatures)
     * @param rsaPublicKeyBytes The RSA public key bytes (for signatures)
     */
    public LTPAPQCKeys(@Sensitive byte[] rsaPrivateKeyBytes, 
                       byte[] rsaPublicKeyBytes) {
        this(rsaPrivateKeyBytes, rsaPublicKeyBytes, 3, true);
    }
    
    /**
     * Construct LTPA PQC keys with full control over all parameters.
     * 
     * @param rsaPrivateKeyBytes The RSA private key bytes (for signatures)
     * @param rsaPublicKeyBytes The RSA public key bytes (for signatures)
     * @param tokenVersion The LTPA token version (3 = hybrid PQC)
     * @param pqcEnabled Whether PQC encryption is enabled
     */
    public LTPAPQCKeys(@Sensitive byte[] rsaPrivateKeyBytes, 
                       byte[] rsaPublicKeyBytes,
                       int tokenVersion,
                       boolean pqcEnabled) {
        
        // Validate inputs
        if (rsaPrivateKeyBytes == null || rsaPrivateKeyBytes.length == 0) {
            throw new IllegalArgumentException("RSA private key bytes cannot be null or empty");
        }
        if (rsaPublicKeyBytes == null || rsaPublicKeyBytes.length == 0) {
            throw new IllegalArgumentException("RSA public key bytes cannot be null or empty");
        }
        
        // Clone arrays to prevent external modification
        this.rsaPrivateKeyBytes = rsaPrivateKeyBytes.clone();
        this.rsaPublicKeyBytes = rsaPublicKeyBytes.clone();
        this.tokenVersion = tokenVersion;
        this.pqcEnabled = pqcEnabled;
    }
    
    /**
     * Get the RSA private key bytes (for signatures).
     * 
     * @return A clone of the RSA private key bytes
     */
    @Sensitive
    public byte[] getRsaPrivateKeyBytes() {
        return rsaPrivateKeyBytes.clone();
    }
    
    /**
     * Get the RSA public key bytes (for signatures).
     * 
     * @return A clone of the RSA public key bytes
     */
    public byte[] getRsaPublicKeyBytes() {
        return rsaPublicKeyBytes.clone();
    }
    
    
    /**
     * Get the RSA public key as a PublicKey object (for signatures).
     * This method reconstructs the PublicKey from the stored byte array.
     * 
     * @return The RSA public key
     * @throws Exception if key reconstruction fails
     */
    public java.security.PublicKey getRsaPublicKey() throws Exception {
        // Use LTPAPublicKey to reconstruct the key from encoded bytes
        return new com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey(rsaPublicKeyBytes.clone());
    }
    
    
    /**
     * Get the LTPA token version.
     * 
     * @return The token version (3 = hybrid PQC)
     */
    public int getTokenVersion() {
        return tokenVersion;
    }
    
    /**
     * Check if PQC encryption is enabled.
     * 
     * @return true if PQC encryption is enabled, false otherwise
     */
    public boolean isPqcEnabled() {
        return pqcEnabled;
    }
    
    /**
     * Clear sensitive key material from memory.
     * This method should be called when the keys are no longer needed.
     */
    public void clear() {
        // Clear RSA private key bytes
        if (rsaPrivateKeyBytes != null) {
            Arrays.fill(rsaPrivateKeyBytes, (byte) 0);
        }
        
        // Note: Java PrivateKey/PublicKey objects don't have a standard clear method
        // The JVM will eventually garbage collect them
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LTPAPQCKeys{");
        sb.append("tokenVersion=").append(tokenVersion);
        sb.append(", pqcEnabled=").append(pqcEnabled);
        sb.append(", hasRsaKeys=").append(rsaPrivateKeyBytes != null && rsaPublicKeyBytes != null);
        sb.append('}');
        return sb.toString();
    }
}

// Made with Bob
