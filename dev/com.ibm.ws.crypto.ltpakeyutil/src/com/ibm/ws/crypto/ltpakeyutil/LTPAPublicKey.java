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
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * Represents an LTPA Public Key based on RSA/SHA-1. Its based on a 128 byte RSA
 * key. With FIPS enabled, it is based on RSA/SHA-512 (256 byte RSA key).
 */
public final class LTPAPublicKey implements PublicKey {

	private static final boolean fipsEnabled = CryptoUtils.isFips140_3Enabled();
	private static final long serialVersionUID = 6585779055758956436L;
	private final RSAPublicKey rawKey;
	private final byte[] encodedKey;

	LTPAPublicKey(RSAPublicKey rawKey) {
		this.rawKey = rawKey;
		this.encodedKey = encode();
	}

	public LTPAPublicKey(byte[] encodedKey) {
		this.encodedKey = encodedKey.clone();
		this.rawKey = decode(encodedKey);
	}

	/**
	 * encoding/decoding are based on non-standard LTPA specific algorithm.
	 * concatenates byte arrays of raw key to a format that can be decoded based on
	 * length of each component.
	 *
	 * @param encodedPublicKey The encoded key
	 */
	private RSAPublicKey decode(byte[] encodedPublicKey) {
		try {
			String provider = CryptoUtils.getProvider();
			KeyFactory kf = (provider == null)
					? KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA)
					: KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA, provider);
			RSAPublicKey key = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encodedPublicKey));
			return key;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to build RSA public key from encoded bytes", ex);
		}
	}

	private byte[] encode() {
		byte[] encodedPublicKey = rawKey.getEncoded();
		return encodedPublicKey;
	}

	/** {@inheritDoc} */
	@Override
	public final String getAlgorithm() {
		return (fipsEnabled ? CryptoUtils.RSA_SHA_512 : CryptoUtils.RSA_SHA_1);
	}

	/** {@inheritDoc} */
	@Override
	public final byte[] getEncoded() {
		return encodedKey.clone();
	}

	/** {@inheritDoc} */
	@Override
	public final String getFormat() {
		return "LTPAFormat";
	}

	protected final RSAPublicKey getRawKey() {
		return rawKey;
	}

	byte[] getEncodedKey() {
		return encodedKey;
	}
}
