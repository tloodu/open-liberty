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
package com.ibm.ws.crypto.ltpakeyutil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.KeyPair;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.common.encoder.Base64Coder;

/**
 *
 */
public class LTPAKeyFileUtilityImpl implements LTPAKeyFileUtility {

	/** {@inheritDoc} */
	@Override
	public Properties createLTPAKeysFile(String keyFile, byte[] keyPasswordBytes) throws Exception {
		Properties ltpaProps = generateLTPAKeys(keyPasswordBytes, "defaultRealm");
		addLTPAKeysToFile(getOutputStream(keyFile), ltpaProps);
		return ltpaProps;
	}

	/**
	 * Generates the LTPA keys and stores them into a Properties object.
	 *
	 * @param keyPasswordBytes
	 * @param realm
	 * @return
	 * @throws Exception
	 */
	protected final Properties generateLTPAKeys(byte[] keyPasswordBytes, final String realm) throws Exception {
		return generateLTPAKeys(keyPasswordBytes, null, null, null, realm);
	}

	/**
	 * Generates the LTPA keys and stores them into a Properties object.
	 *
	 * In the case of generating new ltpa keys, pass null in sharedKeyBytes,
	 * privateKeyBytes, and publicKeyBytes to generate them.
	 *
	 * Otherwise, in the case of re-encrypting existing ltpa keys, pass the bytes in
	 * sharedKeyBytes, privateKeyBytes, and publicKeyBytes to reuse them.
	 *
	 * @param keyPasswordBytes
	 * @param sharedKeyBytes
	 * @param privateKeyBytes
	 * @param publicKeyBytes
	 * @param realm
	 * @return
	 * @throws Exception
	 */
	protected final Properties generateLTPAKeys(byte[] keyPasswordBytes, byte[] sharedKeyBytes, byte[] privateKeyBytes,
			byte[] publicKeyBytes, final String realm) throws Exception {
		return generateLTPAKeys(keyPasswordBytes, sharedKeyBytes, privateKeyBytes, publicKeyBytes, realm, null, 0);
	}

	/**
	 * Generates the LTPA keys and stores them into a Properties object.
	 *
	 * @param keyPasswordBytes  password used to encrypt stored private keys
	 * @param sharedKeyBytes    existing shared key, or {@code null} to generate
	 * @param privateKeyBytes   existing RSA private key, or {@code null} to generate (classical only)
	 * @param publicKeyBytes    existing RSA public key, or {@code null} to generate (classical only)
	 * @param realm             realm name stored in the key file
	 * @param mldsaAlgorithm    ML-DSA algorithm (e.g. "ML-DSA-65"); non-null selects PQC mode
	 * @param classicalKeySize  RSA key size in bytes (e.g. 128=1024-bit, 256=2048-bit); 0 uses FIPS-aware default
	 * @return Properties containing all LTPA key attributes
	 * @throws Exception
	 */
	protected final Properties generateLTPAKeys(byte[] keyPasswordBytes, byte[] sharedKeyBytes, byte[] privateKeyBytes,
			byte[] publicKeyBytes, final String realm, String mldsaAlgorithm, int classicalKeySize) throws Exception {
		Properties expProps = null;

		try {
			KeyEncryptor encryptor = new KeyEncryptor(keyPasswordBytes);

			// Shared AES key — always required for token payload encryption.
			if (sharedKeyBytes == null) {
				sharedKeyBytes = LTPACrypto.generateSharedKey();
			}
			byte[] encryptedSharedKeyBytes = encryptor.encrypt(sharedKeyBytes);

			expProps = new Properties();
			expProps.put(KEYIMPORT_SECRETKEY, Base64Coder.base64EncodeToString(encryptedSharedKeyBytes));
			expProps.put(KEYIMPORT_REALM, realm);
			expProps.put(CREATION_HOST_PROPERTY, "localhost");
			expProps.put(LTPA_VERSION_PROPERTY, CryptoUtils.isFips140_3Enabled() ? "2.0" : "1.0");
			expProps.put(CREATION_DATE_PROPERTY, (new java.util.Date()).toString());

			if (mldsaAlgorithm == null) {
				if (publicKeyBytes == null && privateKeyBytes == null) {
					LTPAKeyPair pair = LTPADigSignature.generateLTPAKeyPair(classicalKeySize);
					publicKeyBytes = pair.getPublic().getEncoded();
					privateKeyBytes = pair.getPrivate().getEncoded();
				}
				byte[] encryptedPrivateKeyBytes = encryptor.encrypt(privateKeyBytes);
				expProps.put(KEYIMPORT_PRIVATEKEY, Base64Coder.base64EncodeToString(encryptedPrivateKeyBytes));
				expProps.put(KEYIMPORT_PUBLICKEY, Base64Coder.base64EncodeToString(publicKeyBytes));
			} else {
				KeyPair mldsaKeyPair = generateMLDSAKeyPair(mldsaAlgorithm);
				if (mldsaKeyPair != null) {
					byte[] encryptedMLDSAPrivateKeyBytes = encryptor.encrypt(mldsaKeyPair.getPrivate().getEncoded());
					expProps.put(KEYIMPORT_MLDSA_PUBLICKEY, Base64Coder.base64EncodeToString(mldsaKeyPair.getPublic().getEncoded()));
					expProps.put(KEYIMPORT_MLDSA_PRIVATEKEY, Base64Coder.base64EncodeToString(encryptedMLDSAPrivateKeyBytes));
				}
			}
		} catch (Exception e) {
			throw e;
		}

		return expProps;
	}

	/**
	 * Obtain the OutputStream for the given file.
	 *
	 * @param keyFile
	 * @return
	 * @throws IOException
	 */
	private OutputStream getOutputStream(final String keyFile) throws IOException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<OutputStream>() {
				@Override
				public OutputStream run() throws IOException {
					return new FileOutputStream(new File(keyFile));
				}
			});
		} catch (PrivilegedActionException e) {
			// Wrap the wrapped IOException from doPriv in an IOException and re-throw
			throw new IOException(e.getCause());
		}
	}

	/**
	 * Write the LTPA key properties to the given OutputStream. This method will
	 * close the OutputStream.
	 *
	 * @param keyImportFile The import file to be created
	 * @param ltpaProps     The properties containing the LTPA keys
	 *
	 * @throws TokenException
	 * @throws IOException
	 */
	protected void addLTPAKeysToFile(OutputStream os, Properties ltpaProps) throws Exception {
		try {
			// Write the ltpa key propeperties to
			ltpaProps.store(os, null);
		} catch (IOException e) {
			throw e;
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
				}
		}

		return;
	}

	/**
	 * Generate ML-DSA (Dilithium) key pair for PQC support. Uses Java 25's built-in
	 * ML-DSA support without requiring MLDSAParameterSpec. The KeyPairGenerator
	 * uses default parameters (ML-DSA-65) when not explicitly initialized.
	 *
	 * @param algorithm ML-DSA algorithm name (e.g. "ML-DSA-44", "ML-DSA-65", "ML-DSA-87")
	 * @return KeyPair containing ML-DSA public and private keys, or null if
	 *         generation fails
	 */
	private KeyPair generateMLDSAKeyPair(String algorithm) {
		System.out.println("DEBUG: Starting ML-DSA key pair generation");
		System.out.println("DEBUG: Java version: " + System.getProperty("java.version"));
		System.out.println("DEBUG: Java vendor: " + System.getProperty("java.vendor"));

		try {
			// List all available security providers
			System.out.println("DEBUG: Available security providers:");
			java.security.Provider[] providers = java.security.Security.getProviders();
			for (java.security.Provider provider : providers) {
				System.out.println("DEBUG:   - " + provider.getName() + " (version " + provider.getVersion() + ")");
				// Check if provider supports ML-DSA
				if (provider.getService("KeyPairGenerator", "ML-DSA") != null) {
					System.out.println("DEBUG:     * Supports ML-DSA KeyPairGenerator");
				}
			}

			// Generate ML-DSA key pair using default SUN provider
			// Note: IBM Java 25 doesn't expose MLDSAParameterSpec class, but the
			// KeyPairGenerator
			// works with default parameters (ML-DSA-65) without explicit initialization
			System.out.println("DEBUG: Getting KeyPairGenerator instance for ML-DSA...");
			java.security.KeyPairGenerator keyGen = java.security.KeyPairGenerator.getInstance(algorithm);
			System.out.println("DEBUG: KeyPairGenerator obtained, provider: " + keyGen.getProvider().getName());
			System.out.println("DEBUG: Using algorithm: " + algorithm);

			System.out.println("DEBUG: Generating ML-DSA key pair...");
			KeyPair keyPair = keyGen.generateKeyPair();
			System.out.println("DEBUG: ML-DSA key pair generated successfully!");
			System.out.println("DEBUG: Public key algorithm: " + keyPair.getPublic().getAlgorithm());
			System.out.println("DEBUG: Public key format: " + keyPair.getPublic().getFormat());
			System.out.println("DEBUG: Public key size: " + keyPair.getPublic().getEncoded().length + " bytes");
			System.out.println("DEBUG: Private key algorithm: " + keyPair.getPrivate().getAlgorithm());
			System.out.println("DEBUG: Private key format: " + keyPair.getPrivate().getFormat());
			System.out.println("DEBUG: Private key size: " + keyPair.getPrivate().getEncoded().length + " bytes");

			return keyPair;
		} catch (java.security.NoSuchAlgorithmException e) {
			System.err.println("ERROR: ML-DSA algorithm not available in any security provider");
			System.err.println("ERROR: Exception: " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		} catch (Exception e) {
			System.err.println("ERROR: ML-DSA key generation failed");
			System.err.println("ERROR: Exception: " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}
}
