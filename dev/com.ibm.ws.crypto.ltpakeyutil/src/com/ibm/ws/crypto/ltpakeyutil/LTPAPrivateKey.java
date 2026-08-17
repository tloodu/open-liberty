/*******************************************************************************
 * Copyright (c) 1997, 2025 IBM Corporation and others.
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

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;

import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * Represents an LTPA Private Key; Encoding is non-standard. Uses 128 byte RSA.
 * With FIPS enabled, it is based on RSA/SHA-512 (256 byte RSA key).
 */
public final class LTPAPrivateKey implements PrivateKey {

	private static final boolean fipsEnabled = CryptoUtils.isFips140_3Enabled();
	private static final long serialVersionUID = -2566137894245694562L;
	private final PrivateKey rawKey;
	private final byte[] encodedKey;

	LTPAPrivateKey(RSAPrivateCrtKey rawKey) {
		this.rawKey = rawKey;
		this.encodedKey = encode();
	}

	public LTPAPrivateKey(byte[] encodedKey) {
		this.encodedKey = encodedKey.clone();
		this.rawKey = decode(encodedKey);
	}

	private static PrivateKey decode(byte[] encodedPrivateKey) {
		try {
			String provider = CryptoUtils.getProvider();
			KeyFactory kf = (provider == null)
					? KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA)
					: KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA, provider);
			long start = System.currentTimeMillis();
			PrivateKey key = kf.generatePrivate(new PKCS8EncodedKeySpec(encodedPrivateKey));
			System.out.println("[ltpakeyutil] LTPAPrivateKey.decode: " + (System.currentTimeMillis() - start) + " ms");
			return key;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to build RSA private key from encoded bytes", ex);
		}
	}

	private byte[] encode() {
		long start = System.currentTimeMillis();
		byte[] encoded = rawKey.getEncoded();
		System.out.println("[ltpakeyutil] LTPAPrivateKey.encode: " + (System.currentTimeMillis() - start) + " ms");
		return encoded;
	}

	/**
	 * Return the algorithm used - RSA/SHA-1.
	 *
	 * @return Always RSA/SHA-1
	 */
	@Override
	public final String getAlgorithm() {
		return (fipsEnabled ? CryptoUtils.RSA_SHA_512 : CryptoUtils.RSA_SHA_1);
	}

	/** {@inheritDoc} */
	@Override
	public final byte[] getEncoded() {
		return encodedKey.clone();
	}

	/**
	 * Get the format of the private key.
	 *
	 * @return Always LTPAFormat
	 */
	@Override
	public final String getFormat() {
		return "LTPAFormat";
	}

	/**
	 * Get the raw data of the private key.
	 *
	 * @return The raw data of the key
	 */
	protected final PrivateKey getRawKey() {
		return rawKey;
	}
}
