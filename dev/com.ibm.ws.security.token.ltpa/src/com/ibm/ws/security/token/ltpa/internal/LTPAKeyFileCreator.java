/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.token.ltpa.internal;

import java.util.Properties;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 * Utility class to create the LTPA keys file.
 */
public interface LTPAKeyFileCreator extends LTPAKeyFileUtility {

    /**
     * Create the LTPA keys file at the specified location using
     * the specified password bytes.
     * <p>
     * Access the keyFile using the WsLocationAdmin
     *
     * @param locService
     * @param keyFile
     * @param keyPasswordBytes
     * @return A Properties object containing the various attributes created for the LTPA keys
     * @throws Exception
     */
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive byte[] keyPasswordBytes) throws Exception;

    /**
     * Create the LTPA keys file at the specified location using the specified
     * password bytes and classical RSA key size.
     * <p>
     * RSA keys are always generated. {@code classicalKeySize} sets the RSA key size
     * explicitly instead of using the FIPS-aware default.
     *
     * @param locService
     * @param keyFile
     * @param keyPasswordBytes
     * @param classicalKeySize
     * @return A Properties object containing the various attributes created for the LTPA keys
     * @throws Exception
     */
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive byte[] keyPasswordBytes, int classicalKeySize) throws Exception;

    /**
     * Create the LTPA keys file at the specified location using the specified
     * password bytes and ML-DSA algorithm (PQC mode).
     * <p>
     * RSA keys are always generated alongside the ML-DSA keys using the FIPS-aware
     * default key size.
     *
     * @param locService
     * @param keyFile
     * @param keyPasswordBytes
     * @param mldsaAlgorithm
     * @return A Properties object containing the various attributes created for the LTPA keys
     * @throws Exception
     */
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive byte[] keyPasswordBytes, String mldsaAlgorithm) throws Exception;

    /**
     * Create the LTPA keys file at the specified location using
     * the specified password bytes, shared key bytes, private key bytes, and public key bytes.
     * <p>
     * Access the keyFile using the WsLocationAdmin
     *
     * @param locService
     * @param keyFile
     * @param keyPasswordBytes
     * @param sharedKeyBytes
     * @param privateKeyBytes
     * @param publicKeyBytes
     * @return A Properties object containing the various attributes created for the LTPA keys
     * @throws Exception
     */
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive byte[] keyPasswordBytes,
                                         @Sensitive byte[] sharedKeyBytes, @Sensitive byte[] privateKeyBytes, @Sensitive byte[] publicKeyBytes) throws Exception;

}