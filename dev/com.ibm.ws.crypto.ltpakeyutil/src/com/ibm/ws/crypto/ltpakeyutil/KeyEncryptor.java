/*******************************************************************************
 * Copyright (c) 1997, 2024 IBM Corporation and others.
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

import java.security.MessageDigest;
import java.security.SecureRandom;

import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * A package local class for performing encryption and decryption of keys based
 * on admin's password
 */
public class KeyEncryptor {

	private static final boolean fipsEnabled = CryptoUtils.isFips140_3Enabled();
	private static final int size = 32;
	public static final int GCM_IV_LENGTH = 12;
	private final byte[] key;
	private final byte[] iv;

	/**
	 * A KeyEncryptor constructor.
	 *
	 * @param password The key password
	 */
	public KeyEncryptor(byte[] password) throws Exception {
		MessageDigest md = MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM);
		byte[] digest = md.digest(password);
		key = new byte[size];
		System.arraycopy(digest, 0, key, 0, digest.length);
		if (!fipsEnabled) {
			key[20] = (byte) 0x00;
			key[21] = (byte) 0x00;
			key[22] = (byte) 0x00;
			key[23] = (byte) 0x00;
		}
		iv = new byte[GCM_IV_LENGTH];
		new SecureRandom().nextBytes(iv);
	}

	/**
	 * Decrypt the key.
	 *
	 * @param encryptedKey The encrypted key
	 * @return The decrypted key
	 */
	public byte[] decrypt(byte[] encryptedKey, byte[] iv) throws Exception {
		return LTPACrypto.decryptGCM(encryptedKey, key, iv);
	}

	/**
	 * Encrypt the key
	 *
	 * @param key The key
	 * @return The encrypted key
	 */
	public byte[] encrypt(byte[] key) throws Exception {
		return LTPACrypto.encryptGCM(key, this.key, iv);
	}

	/**
	 * Get IV used for encryption
	 * 
	 * @return The 12-byte AES-GCM IV
	 */
	public byte[] getIV() {
		return iv;
	}
}
