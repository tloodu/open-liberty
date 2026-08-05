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
     * password bytes and ML-DSA algorithm.
     *
     * @param locService
     * @param keyFile
     * @param keyPasswordBytes
     * @param mldsaAlgorithm ML-DSA algorithm to use (e.g. "ML-DSA-65")
     * @return A Properties object containing the various attributes created for the LTPA keys
     * @throws Exception
     */
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive byte[] keyPasswordBytes, String mldsaAlgorithm) throws Exception;

    /**
     * Create the LTPA keys file at the specified location using the specified
     * password bytes, ML-DSA algorithm, and ML-KEM algorithm.
     *
     * @param locService
     * @param keyFile
     * @param keyPasswordBytes
     * @param mldsaAlgorithm ML-DSA algorithm to use (e.g. "ML-DSA-44")
     * @param mlkemAlgorithm ML-KEM algorithm to use (e.g. "ML-KEM-768")
     * @return A Properties object containing the various attributes created for the LTPA keys
     * @throws Exception
     */
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive byte[] keyPasswordBytes, String mldsaAlgorithm, String mlkemAlgorithm) throws Exception;

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