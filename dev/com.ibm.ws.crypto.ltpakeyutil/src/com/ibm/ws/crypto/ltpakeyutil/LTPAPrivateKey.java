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
	private final RSAPrivateCrtKey rawKey;
	private final byte[] encodedKey;

	LTPAPrivateKey(RSAPrivateCrtKey key) {
		this.rawKey = key;
		this.encodedKey = encode();
	}

	public LTPAPrivateKey(byte[] encodedKey) {
		this.encodedKey = encodedKey.clone();
		this.rawKey = decode(encodedKey);
	}

	/*
	 * Encoding/decoding are based on non-standard format; basically all the byte
	 * arrays are concatenated. As we know the length of the components, they can be
	 * reconstructed back.
	 *
	 * @param encodedPrivateKey The encoded key
	 */
	private RSAPrivateCrtKey decode(byte[] encodedPrivateKey) {
		long startDecode = System.currentTimeMillis();
		System.out.println("[LTPAPrivateKey] decode: start; encodedKey.length=" + encodedPrivateKey.length + " bytes");
		try {
			String provider = CryptoUtils.getProvider();

			long startKeyFactory = System.currentTimeMillis();
			KeyFactory kf = (provider == null)
					? KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA)
					: KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA, provider);
			long keyFactoryMs = System.currentTimeMillis() - startKeyFactory;
			System.out.println("[LTPAPrivateKey] decode: KeyFactory.getInstance took " + keyFactoryMs + " ms");

			long startGeneratePrivate = System.currentTimeMillis();
			RSAPrivateCrtKey key = (RSAPrivateCrtKey) kf.generatePrivate(new PKCS8EncodedKeySpec(encodedPrivateKey));
			long generatePrivateMs = System.currentTimeMillis() - startGeneratePrivate;
			System.out.println("[LTPAPrivateKey] decode: kf.generatePrivate took " + generatePrivateMs + " ms");

			long totalMs = System.currentTimeMillis() - startDecode;
			System.out.println("[LTPAPrivateKey] decode: end; keyFactory=" + keyFactoryMs + " ms, generatePrivate=" + generatePrivateMs + " ms, total=" + totalMs + " ms");
			return key;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to build RSA private key from encoded bytes", ex);
		}
	}

	private byte[] encode() {
		long startEncode = System.currentTimeMillis();
		System.out.println("[LTPAPrivateKey] encode: start");
		byte[] encodedKey = rawKey.getEncoded();
		long totalMs = System.currentTimeMillis() - startEncode;
		System.out.println("[LTPAPrivateKey] encode: end; encodedKey.length=" + encodedKey.length + " bytes, total=" + totalMs + " ms");
		return encodedKey;
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
	protected final RSAPrivateCrtKey getRawKey() {
		return rawKey;
	}

	byte[] getEncodedKey() {
		return encodedKey;
	}

}
