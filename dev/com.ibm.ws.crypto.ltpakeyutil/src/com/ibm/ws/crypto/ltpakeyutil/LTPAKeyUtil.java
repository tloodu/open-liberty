/*******************************************************************************
 * Copyright (c) 2016, 2024 IBM Corporation and others.
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

public final class LTPAKeyUtil {

	public static byte[] encrypt(byte[] data, byte[] key, String cipher) throws Exception {
		return LTPACrypto.encrypt(data, key, cipher);
	}

	/**
	 * Encrypt data with an explicit AES key length.
	 * Intended for the token-creation path where {@code encryptionAlg} has been
	 * resolved to {@code AES-CBC-128} or {@code AES-CBC-256}.
	 *
	 * @param data           The byte representation of the data
	 * @param key            The encryption key
	 * @param cipher         The cipher algorithm
	 * @param keyLengthBytes AES key length in bytes (16 for AES-128, 32 for AES-256)
	 * @return The encrypted data (ciphertext)
	 */
	public static byte[] encrypt(byte[] data, byte[] key, String cipher, int keyLengthBytes) throws Exception {
		return LTPACrypto.encrypt(data, key, cipher, keyLengthBytes);
	}

	public static byte[] decrypt(byte[] msg, byte[] key, String cipher) throws Exception {
		return LTPACrypto.decrypt(msg, key, cipher, 0);
	}

	/**
	 * Decrypt data with an explicit AES key length.
	 * Intended for the token-validation path where the key size is probed
	 * to handle tokens produced under a different FIPS configuration.
	 *
	 * @param msg            The byte representation of the encrypted data
	 * @param key            The decryption key
	 * @param cipher         The cipher algorithm
	 * @param keyLengthBytes AES key length in bytes (16 for AES-128, 32 for AES-256)
	 * @return The decrypted data (plaintext)
	 */
	public static byte[] decrypt(byte[] msg, byte[] key, String cipher, int keyLengthBytes) throws Exception {
		return LTPACrypto.decrypt(msg, key, cipher, keyLengthBytes);
	}

	public static boolean verifyISO9796(LTPAPublicKey pubKey, byte[] digest, byte[] sig) throws Exception {
		return LTPACrypto.verifyRSA(pubKey.getRawKey(), digest, sig, null);
	}

	public static boolean verifyISO9796(LTPAPublicKey pubKey, byte[] digest, byte[] sig, String algorithm) throws Exception {
		return LTPACrypto.verifyRSA(pubKey.getRawKey(), digest, sig, algorithm);
	}

        /**
         * Encrypt data using AES-GCM (authenticated encryption).
         * PQC Issue #35556 - Task 2.6
         *
         * @param data The data to encrypt
         * @param key  The encryption key
         * @return The encrypted data with format: [IV (12 bytes)][Ciphertext][Auth Tag (16 bytes)]
         * @throws Exception if encryption fails
         */
         public static byte[] encryptGCM(byte[] data, byte[] key) throws Exception {
                 return LTPACrypto.encryptGCM(data, key);
        }

        /**
         * Decrypt data using AES-GCM (authenticated encryption).
         * PQC Issue #35556 - Task 2.6
         *
         * @param encryptedData The encrypted data
         * @param key           The decryption key
         * @return The decrypted plaintext
         * @throws Exception if decryption fails or authentication tag is invalid
         */
         public static byte[] decryptGCM(byte[] encryptedData, byte[] key) throws Exception {
                 return LTPACrypto.decryptGCM(encryptedData, key);
        }

	public static java.security.PrivateKey getRawKey(LTPAPrivateKey privKey) {
		return privKey.getRawKey();
	}

	public static byte[] signISO9796(LTPAPrivateKey privKey, byte[] digest) throws Exception {
		return LTPACrypto.signRSA(privKey.getRawKey(), digest, null);
	}

	public static byte[] signISO9796(LTPAPrivateKey privKey, byte[] digest, String algorithm) throws Exception {
		return LTPACrypto.signRSA(privKey.getRawKey(), digest, algorithm);
	}

	public static LTPAKeyPair generateLTPAKeyPair() {
		return LTPADigSignature.generateLTPAKeyPair();
	}

	public static byte[] generateSharedKey() {
		return LTPACrypto.generateSharedKey();
	}


}
