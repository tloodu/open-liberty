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

import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;

import com.ibm.ws.common.crypto.CryptoUtils;

final class LTPADigSignature {

	static int keySize = (CryptoUtils.isFips140_3Enabled() ? 256 : 128);

	static MessageDigest md1 = CryptoUtils.getMessageDigestForLTPA();
	static private Object lockObj1 = new Object();
	static long created = 0;
	static long cacheHits = 0;

	public LTPADigSignature() {
		super();
	}

	static boolean verify(byte[] mesg, byte[] signature, LTPAPublicKey pubKey) throws Exception {
		byte[] data;
		synchronized (lockObj1) {
			data = md1.digest(mesg);
		}
		return LTPACrypto.verifyRSA(pubKey.getRawKey(), data, signature);
	}

	static LTPAKeyPair generateLTPAKeyPair() {
		KeyPair pair = LTPACrypto.rsaKey();
		LTPAPublicKey pubKey = new LTPAPublicKey((RSAPublicKey) pair.getPublic());
		LTPAPrivateKey privKey = new LTPAPrivateKey((RSAPrivateCrtKey) pair.getPrivate());
		return new LTPAKeyPair(pubKey, privKey);
	}
}
