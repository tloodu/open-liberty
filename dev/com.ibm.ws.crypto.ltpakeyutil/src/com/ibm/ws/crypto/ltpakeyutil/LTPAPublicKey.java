/*******************************************************************************
 * Copyright (c) 1997, 2026 IBM Corporation and others.
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

import java.security.PublicKey;

import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * Represents an LTPA Public Key based on RSA/SHA-1. Its based on a 128 byte RSA
 * key. With FIPS enabled, it is based on RSA/SHA-512 (256 byte RSA key).
 */
public final class LTPAPublicKey implements PublicKey {

	private static final boolean fipsEnabled = CryptoUtils.isFips140_3Enabled();
	private static final long serialVersionUID = 6585779055758956436L;
	private static final int MODULUS = 0;
	private static final int EXPONENT = 1;
	private static final int EXPONENT_LENGTH = 3;
	private final byte[][] rawKey;
	private final byte[] encodedKey;

	LTPAPublicKey(byte[][] rawKey) {
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
	private byte[][] decode(byte[] encodedPublicKey) {
		int modulusLength = encodedPublicKey.length - EXPONENT_LENGTH;
		byte[][] decodedKey = new byte[2][];
		decodedKey[MODULUS] = new byte[modulusLength];
		decodedKey[EXPONENT] = new byte[EXPONENT_LENGTH];
		System.arraycopy(encodedPublicKey, 0, decodedKey[MODULUS], 0, modulusLength);
		System.arraycopy(encodedPublicKey, modulusLength, decodedKey[EXPONENT], 0, EXPONENT_LENGTH);
		return decodedKey;
	}

	private byte[] encode() {
		int modulusLength = rawKey[MODULUS].length;
		int publicKeyLength = modulusLength + EXPONENT_LENGTH;
		byte[] encodedPublicKey = new byte[publicKeyLength];
		System.arraycopy(rawKey[MODULUS], 0, encodedPublicKey, 0, modulusLength);
		System.arraycopy(rawKey[EXPONENT], 0, encodedPublicKey, modulusLength, EXPONENT_LENGTH);
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

	protected final byte[][] getRawKey() {
		if (rawKey == null) {
			return null;
		} else {
			return rawKey.clone();
		}
	}
}
