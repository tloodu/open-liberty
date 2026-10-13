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
	 * @param classicalKeySize  RSA key size in bytes (128=RSA-1024, 256=RSA-2048); 0 uses default
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

			expProps = new Properties();
			expProps.put(KEYIMPORT_REALM, realm);
			expProps.put(CREATION_HOST_PROPERTY, "localhost");
			expProps.put(CREATION_DATE_PROPERTY, (new java.util.Date()).toString());

			if (mldsaAlgorithm == null) {
				if (classicalKeySize <= 0) {
					classicalKeySize = LTPADigSignature.keySize;
				}
				// v2.0 = RSA-2048 (classicalKeySize >= 256 bytes).
				// v1.0 = RSA-1024 (classicalKeySize < 256 bytes).
				boolean rsa2048 = (classicalKeySize >= 256);
				expProps.put(LTPA_VERSION_PROPERTY, rsa2048 ? "2.0" : "1.0");

				if (rsa2048) {
					// v2.0: encrypt with AES regardless of FIPS mode so any server can load the file.
					expProps.put(KEYIMPORT_SECRETKEY_V2,
					             Base64Coder.base64EncodeToString(encryptor.encryptAES(sharedKeyBytes)));
				} else {
					// v1.0: legacy DES/AES encryption determined by FIPS mode.
					expProps.put(KEYIMPORT_SECRETKEY_V1,
					             Base64Coder.base64EncodeToString(encryptor.encrypt(sharedKeyBytes)));
				}

				if (publicKeyBytes == null && privateKeyBytes == null) {
					LTPAKeyPair pair = LTPADigSignature.generateLTPAKeyPair(classicalKeySize);
					publicKeyBytes = pair.getPublic().getEncoded();
					privateKeyBytes = pair.getPrivate().getEncoded();
				}
				byte[] encryptedPrivateKeyBytes = rsa2048
				        ? encryptor.encryptAES(privateKeyBytes)
				        : encryptor.encrypt(privateKeyBytes);
				expProps.put(KEYIMPORT_PRIVATEKEY, Base64Coder.base64EncodeToString(encryptedPrivateKeyBytes));
				expProps.put(KEYIMPORT_PUBLICKEY, Base64Coder.base64EncodeToString(publicKeyBytes));
			} else {
				// v3.0 = PQC (ML-DSA keys only, no RSA keys).
				// Encrypt with AES regardless of FIPS mode so any server can load the file.
				expProps.put(LTPA_VERSION_PROPERTY, "3.0");
				expProps.put(KEYIMPORT_SECRETKEY_V1,
				             Base64Coder.base64EncodeToString(encryptor.encryptAES(sharedKeyBytes)));
				KeyPair mldsaKeyPair = generateMLDSAKeyPair(mldsaAlgorithm);
				if (mldsaKeyPair != null) {
					byte[] encryptedMLDSAPrivateKeyBytes = encryptor.encryptAES(mldsaKeyPair.getPrivate().getEncoded());
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
	 * Generate an ML-DSA key pair for PQC support.
	 *
	 * @param algorithm ML-DSA algorithm name (e.g. "ML-DSA-44", "ML-DSA-65", "ML-DSA-87")
	 * @return KeyPair containing ML-DSA public and private keys, or null if generation fails
	 */
	private KeyPair generateMLDSAKeyPair(String algorithm) {
		try {
			java.security.KeyPairGenerator keyGen = java.security.KeyPairGenerator.getInstance(algorithm);
			return keyGen.generateKeyPair();
		} catch (java.security.NoSuchAlgorithmException e) {
			return null;
		} catch (Exception e) {
			return null;
		}
	}
}
