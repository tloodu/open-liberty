/*******************************************************************************
 * Copyright (c) 1997, 2011, 2025 IBM Corporation and others.
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

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.common.crypto.CryptoUtils;

final class LTPACrypto {

    private static final boolean fipsEnabled = CryptoUtils.isFips140_3Enabled();

    private static final String provider = CryptoUtils.getProvider();

    private static final String signatureAlgorithm = CryptoUtils.getSignatureAlgorithm();

    private static int MAX_CACHE = 500; // has to be greater than 0 and a multiple of 5
    private static IvParameterSpec ivs8 = null;
    private static IvParameterSpec ivs16 = null;

    @Trivial
    private static class CachingKey {

        private boolean reused = false;
        private long successfulUses;
        private final byte[] key;
        private final byte[] data;
        private final int off;
        private final int len;
        private int hashcode;
        private byte[] result;

        @Trivial
        private CachingKey(byte[] key, byte[] data, int off, int len) {
            this.key = key;
            this.data = data;
            this.off = off;
            this.len = len;
            this.successfulUses = 0;
            this.reused = false;

            this.hashcode = 0;
            if (key != null && key.length > 0) {
                hashcode += key[0];
            }
            if (data != null) {
                for (int i = 0; i < data.length; i++) {
                    hashcode += data[i];
                }
            }

            hashcode += off + len;
            if (off != 0) {
                hashcode *= off;
            }
            hashcode *= 2;
        }

        @Trivial
        @Override
        public boolean equals(Object to) {
            if (!(to instanceof CachingKey)) {
                return false;
            }

            CachingKey ck = (CachingKey) to;

            if (hashcode != ck.hashcode) {
                return false;
            }

            if (len != ck.len) {
                return false;
            }

            if (key != null) {
                if (ck.key == null) {
                    return false;
                } else {
                    if (key.length != ck.key.length) {
                        return false;
                    }
                }
                for (int i = 0; i < key.length; i++) {
                    if (key[i] != ck.key[i]) {
                        return false;
                    }
                }
            } else {
                if (ck.key != null) {
                    return false;
                }
            }

            if (data != null) {
                if (ck.data == null) {
                    return false;
                } else {
                    if (data.length != ck.data.length) {
                        return false;
                    }
                }
                for (int i = 0; i < data.length; i++) {
                    if (data[i] != ck.data[i]) {
                        return false;
                    }
                }
            } else {
                if (ck.data != null) {
                    return false;
                }
            }

            if (off != ck.off) {
                return false;
            }

            return true;
        }

        @Override
        @Trivial
        public int hashCode() {
            return hashcode;
        }

    }

    private static final ConcurrentHashMap<CachingKey, CachingKey> cryptoKeysMap = new ConcurrentHashMap<CachingKey, CachingKey>();

    /**
     * Sign the data.
     *
     * @param key  The key used to sign the data
     * @param data The byte representation of the data
     * @param off  The offset of the data
     * @param len  The length of the data
     * @return The signature of the data
     */
    @Trivial
    protected static final byte[] signISO9796(LTPAPrivateKey key, byte[] data, int off, int len) throws Exception {
        CachingKey ck = new CachingKey(key.getEncodedKey(), data, off, len);
        CachingKey result = cryptoKeysMap.get(ck);

        if (result != null) {
            result.successfulUses += 1;
            result.reused = true;
            return result.result;
        } else {
            if (cryptoKeysMap.size() >= MAX_CACHE) {
                try {
                    int cryptoKeysMapSize = cryptoKeysMap.size();
                    CachingKey[] keys = cryptoKeysMap.keySet().toArray(new CachingKey[cryptoKeysMapSize]);
                    Arrays.sort(keys, cachingKeyComparator);
                    if (cachingKeyComparator.compare(keys[0], keys[keys.length - 1]) < 0) {
                        for (int i = 0; i < cryptoKeysMapSize / 5; i++) {
                            cryptoKeysMap.remove(keys[i]);
                            keys[i + 1 * cryptoKeysMapSize / 5].successfulUses--;
                            keys[i + 2 * cryptoKeysMapSize / 5].successfulUses--;
                            keys[i + 3 * cryptoKeysMapSize / 5].successfulUses--;
                            keys[i + 4 * cryptoKeysMapSize / 5].successfulUses--;
                        }
                    } else { // TODO: consider removing bc this likely isn't used since we sort the keys above (except when all keys have the same num of uses)
                        for (int i = 0; i < cryptoKeysMapSize / 5; i++) {
                            cryptoKeysMap.remove(keys[keys.length - 1 - i]);
                            keys[keys.length - 1 - i - 1 * cryptoKeysMapSize / 5].successfulUses--;
                            keys[keys.length - 1 - i - 2 * cryptoKeysMapSize / 5].successfulUses--;
                            keys[keys.length - 1 - i - 3 * cryptoKeysMapSize / 5].successfulUses--;
                            keys[keys.length - 1 - i - 4 * cryptoKeysMapSize / 5].successfulUses--;
                        }
                    }
                } catch (Exception e) {
                    // do nothing. since this code is used for the command line
                    // utility, no log is
                    // taken.
                }

            }
        }

        Signature rsaSig = null;

        rsaSig = (provider == null) ? Signature.getInstance(signatureAlgorithm)
                : Signature.getInstance(signatureAlgorithm, provider);

        rsaSig.initSign(key.getRawKey());
        rsaSig.update(data, off, len);
        byte[] sig = rsaSig.sign();

        cryptoKeysMap.put(ck, ck);
        ck.result = sig;
        ck.successfulUses = 0;

        return sig;
    }

    @Trivial
    protected static int getSignCacheSize() {
        return cryptoKeysMap.size();
    }

    @Trivial
    protected static void emptySignCache() {
        cryptoKeysMap.clear();
    }


    private static final ConcurrentHashMap<CachingVerifyKey, CachingVerifyKey> verifyKeysMap = new ConcurrentHashMap<CachingVerifyKey, CachingVerifyKey>();

    @Trivial
    private static class CachingVerifyKey {

        private long successfulUses;
        private final byte[] key;
        private final byte[] data;
        private final int off;
        private final int len;
        private final byte[] sig;
        private final int sigOff;
        private final int sigLen;
        private int hashcode;
        private boolean result;

        @Trivial
        private CachingVerifyKey(byte[] key, byte[] data, int off, int len, byte[] sig, int sigOff, int sigLen) {
            this.key = key;
            this.data = data;
            this.off = off;
            this.len = len;
            this.sig = sig;
            this.sigOff = sigOff;
            this.sigLen = sigLen;
            this.successfulUses = 0;

            this.hashcode = 0;
            if (key != null && key.length > 0) {
                this.hashcode += key[0];
            }
            if (data != null) {
                for (int i = 0; i < data.length && i < 10; i++) {
                    this.hashcode += data[i];
                }
                for (int i = data.length - 1; i >= 0 && i > data.length - 10; i--) {
                    this.hashcode += data[i];
                }
            }

            this.hashcode += off;
            if (off != 0) {
                this.hashcode *= off;
            }
            this.hashcode *= 2;
        }

        @Override
        @Trivial
        public boolean equals(Object to) {
            if (!(to instanceof CachingVerifyKey)) {
                return false;
            }

            CachingVerifyKey ck = (CachingVerifyKey) to;

            if (this.hashcode != ck.hashcode) {
                return false;
            }

            if (this.len != ck.len) {
                return false;
            }

            if (this.key != null) {
                if (ck.key == null) {
                    return false;
                } else {
                    if (this.key.length != ck.key.length) {
                        return false;
                    }
                }
                for (int i = 0; i < this.key.length; i++) {
                    if (this.key[i] != ck.key[i]) {
                        return false;
                    }
                }
            } else {
                if (ck.key != null) {
                    return false;
                }
            }

            if (this.data != null) {
                if (ck.data == null) {
                    return false;
                } else {
                    if (this.data.length != ck.data.length) {
                        return false;
                    }
                }
                for (int i = 0; i < this.data.length; i++) {
                    if (this.data[i] != ck.data[i]) {
                        return false;
                    }
                }
            } else {
                if (ck.data != null) {
                    return false;
                }
            }

            if (this.sig != null) {
                if (ck.sig == null) {
                    return false;
                } else {
                    if (this.sig.length != ck.sig.length) {
                        return false;
                    }
                }
                for (int i = 0; i < this.sig.length; i++) {
                    if (this.sig[i] != ck.sig[i]) {
                        return false;
                    }
                }
            } else {
                if (ck.sig != null) {
                    return false;
                }
            }

            if (this.off != ck.off) {
                return false;
            }

            if (this.sigOff != ck.sigOff) {
                return false;
            }

            if (this.sigLen != ck.sigLen) {
                return false;
            }

            return true;
        }

        @Override
        @Trivial
        public int hashCode() {
            return this.hashcode;
        }

    }

    private static final Comparator<CachingVerifyKey> cachingVerifyKeyComparator = new Comparator<CachingVerifyKey>() {
        @Override
        @Trivial
        public int compare(CachingVerifyKey o1, CachingVerifyKey o2) {
            if (o1.successfulUses < o2.successfulUses) {
                return -1;
            } else if (o1.successfulUses == o2.successfulUses) {
                return 0;
            } else {
                return 1;
            }
        }
    };
    private static final Comparator<CachingKey> cachingKeyComparator = new Comparator<CachingKey>() {
        @Override
        @Trivial
        public int compare(CachingKey o1, CachingKey o2) {
            if (!o1.reused) {
                if (o2.reused) {
                    return -1;
                }
            } else {
                if (!o2.reused) {
                    return 1;
                }
            }
            if (o1.successfulUses < o2.successfulUses) {
                return -1;
            } else if (o1.successfulUses == o2.successfulUses) {
                return 0;
            } else {
                return 1;
            }
        }
    };

    /**
     * Verify if the signature of the data is correct.
     *
     * @param key  The key used to verify the data
     * @param data The byte representation of the data
     * @param off  The offset of the data
     * @param len  The length of the data
     * @param sig  The signature of the data
     * @param off  The offset of the signature
     * @param len  The length of the signature
     * @return True if the signature of the data is correct
     */
    @Trivial
    protected static final boolean verifyISO9796(LTPAPublicKey key, byte[] data, int off, int len, byte[] sig, int sigOff, int sigLen) throws Exception {
        CachingVerifyKey ck = new CachingVerifyKey(key.getEncodedKey(), data, off, len, sig, sigOff, sigLen);
        CachingVerifyKey result = verifyKeysMap.get(ck);

        if (result != null) {
            result.successfulUses += 1;
            return result.result;
        } else {
            if (verifyKeysMap.size() >= MAX_CACHE) {
                int verifyKeysMapSize = verifyKeysMap.size();
                CachingVerifyKey[] keys = verifyKeysMap.keySet().toArray(new CachingVerifyKey[verifyKeysMapSize]);
                Arrays.sort(keys, cachingVerifyKeyComparator);
                if (cachingVerifyKeyComparator.compare(keys[0], keys[keys.length - 1]) < 0) {
                    for (int i = 0; i < verifyKeysMapSize / 5; i++) {
                        verifyKeysMap.remove(keys[i]);
                        keys[i + 1 * verifyKeysMapSize / 5].successfulUses--;
                        keys[i + 2 * verifyKeysMapSize / 5].successfulUses--;
                        keys[i + 3 * verifyKeysMapSize / 5].successfulUses--;
                        keys[i + 4 * verifyKeysMapSize / 5].successfulUses--;
                    }
                } else { // TODO: consider removing bc this likely isn't used since we sort the keys above (except when all keys have the same num of uses)
                    for (int i = 0; i < verifyKeysMapSize / 5; i++) {
                        verifyKeysMap.remove(keys[keys.length - 1 - i]);
                        keys[keys.length - 1 - i - 1 * verifyKeysMapSize / 5].successfulUses--;
                        keys[keys.length - 1 - i - 2 * verifyKeysMapSize / 5].successfulUses--;
                        keys[keys.length - 1 - i - 3 * verifyKeysMapSize / 5].successfulUses--;
                        keys[keys.length - 1 - i - 4 * verifyKeysMapSize / 5].successfulUses--;
                    }
                }
            }
        }

        boolean verified = false;
        Signature rsaSig = null;

        rsaSig = (provider == null) ? Signature.getInstance(signatureAlgorithm)
                : Signature.getInstance(signatureAlgorithm, provider);

        rsaSig.initVerify(key.getRawKey());
        rsaSig.update(data, off, len);
        verified = rsaSig.verify(sig, sigOff, sigLen);

        verifyKeysMap.put(ck, ck);
        ck.result = verified;
        ck.successfulUses = 0;

        return verified;
    }


    @Trivial
    protected static int getVerifyCacheSize() {
        return verifyKeysMap.size();
    }

    @Trivial
    protected static void emptyVerifyCache() {
        verifyKeysMap.clear();
    }

    /**
     * @param key
     * @param cipher
     * @return
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    @Trivial
    private static SecretKey constructSecretKey(byte[] key, String cipher)
            throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        SecretKey sKey = null;
        if (cipher.indexOf(CryptoUtils.ENCRYPT_ALGORITHM_AES) != -1) {
            int keyLength = fipsEnabled ? CryptoUtils.AES_256_KEY_LENGTH_BYTES : CryptoUtils.AES_128_KEY_LENGTH_BYTES;
            sKey = new SecretKeySpec(key, 0, keyLength, CryptoUtils.ENCRYPT_ALGORITHM_AES);
        } else {
            DESedeKeySpec kSpec = new DESedeKeySpec(key);
            SecretKeyFactory kFact = null;

            kFact = (provider == null) ? SecretKeyFactory.getInstance(CryptoUtils.ENCRYPT_ALGORITHM_DESEDE)
                    : SecretKeyFactory.getInstance(CryptoUtils.ENCRYPT_ALGORITHM_DESEDE, provider);

            sKey = kFact.generateSecret(kSpec);
        }
        return sKey;
    }

    /**
     * @param key
     * @param cipher
     * @param sKey
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     */
    @Trivial
    private static Cipher createCipher(int cipherMode, byte[] key, String cipher, SecretKey sKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, NoSuchProviderException {

        Cipher ci = null;
        ci = (provider == null) ? Cipher.getInstance(cipher) : Cipher.getInstance(cipher, provider);

        if (cipher.indexOf(CryptoUtils.ENCRYPT_MODE_ECB) == -1) {
            if (cipher.indexOf(CryptoUtils.ENCRYPT_ALGORITHM_AES) != -1) {
                setIVS16(key);
                ci.init(cipherMode, sKey, ivs16);
            } else {
                setIVS8(key);
                ci.init(cipherMode, sKey, ivs8);
            }
        } else {
            ci.init(cipherMode, sKey);
        }
        return ci;
    }

    /**
     * Encrypt the data.
     *
     * @param data   The byte representation of the data
     * @param key    The key used to encrypt the data
     * @param cipher The cipher algorithm
     * @return The encrypted data (ciphertext)
     */
    @Trivial
    protected static final byte[] encrypt(byte[] data, byte[] key, String cipher) throws Exception {
        SecretKey sKey = constructSecretKey(key, cipher);
        Cipher ci = createCipher(Cipher.ENCRYPT_MODE, key, cipher, sKey);
        return ci.doFinal(data);
    }

    /**
     * Decrypt the specified msg.
     *
     * @param msg    The byte representation of the data
     * @param key    The key used to decrypt the data
     * @param cipher The cipher algorithm
     * @return The decrypted data (plaintext)
     */
    @Trivial
    protected static final byte[] decrypt(byte[] msg, byte[] key, String cipher) throws Exception {
        SecretKey sKey = constructSecretKey(key, cipher);
        Cipher ci = createCipher(Cipher.DECRYPT_MODE, key, cipher, sKey);
        return ci.doFinal(msg);
    }

    /*
     * Set the maximam size of the cache. It is used only for the junit tests.
     *
     * @param maxCache The maximam size of the cache
     */
    @Trivial
    protected static void setMaxCache(int maxCache) {
        MAX_CACHE = maxCache;
    }

    /*
     * Set the 8-byte initialization vector.
     *
     * @param key The key
     */
    @Trivial
    private static final synchronized void setIVS8(byte[] key) {
        byte[] iv8 = new byte[8];
        for (int i = 0; i < 8; i++) {
            iv8[i] = key[i];
        }
        ivs8 = new IvParameterSpec(iv8);
    }

    /*
     * Set the 16-byte initialization vector.
     *
     * @param key The key
     */
    @Trivial
    private static final synchronized void setIVS16(byte[] key) {
        byte[] iv16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            iv16[i] = key[i];
        }
        ivs16 = new IvParameterSpec(iv16);
    }


    @Trivial
    static final byte[] generateSharedKey() {
        return (fipsEnabled) ? CryptoUtils.generateRandomBytes(CryptoUtils.AES_256_KEY_LENGTH_BYTES)
                : CryptoUtils.generateRandomBytes(CryptoUtils.DESEDE_KEY_LENGTH_BYTES);
    }

    @Trivial
    static final KeyPair rsaKey() {
        int keyLengthBits = CryptoUtils.isFips140_3Enabled() ? 2048 : 1024;
        KeyPairGenerator keyGen = null;
        try {
            keyGen = (provider == null) ? KeyPairGenerator.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA)
                    : KeyPairGenerator.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA, provider);

            keyGen.initialize(keyLengthBits, new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (java.security.NoSuchAlgorithmException | java.security.NoSuchProviderException e) {
            // instrumented ffdc
        } catch (java.lang.UnsupportedOperationException uoe) {
            // Fallback for providers that do not support KeyPairGenerator.generateKeyPair()
            int halfLenBytes = (keyLengthBits / 8) / 2;
            BigInteger p, q, n, d;
            BigInteger e = BigInteger.valueOf(0x10001);
            BigInteger one = BigInteger.ONE;
            BigInteger two = BigInteger.valueOf(2);
            byte[] b = new byte[halfLenBytes + 1];

            for (p = null;;) {
                for (q = null;;) {
                    if (q == null) {
                        byte[] seed = CryptoUtils.generateRandomBytes(halfLenBytes);
                        System.arraycopy(seed, 0, b, 1, halfLenBytes);
                        b[1] |= (byte) 0xC0;
                        b[halfLenBytes] |= 1;
                        q = new BigInteger(b);
                    } else {
                        q = q.add(two);
                        if (q.bitLength() > halfLenBytes * 8) {
                            q = null;
                            continue;
                        }
                    }

                    if (q.isProbablePrime(32) && e.gcd(q.subtract(one)).equals(one))
                        break;
                }

                if (p == null)
                    p = q;
                else {
                    n = p.multiply(q);
                    if (n.bitLength() == keyLengthBits) {
                        d = e.modInverse((p.subtract(one)).multiply(q.subtract(one)));
                        if (((p.modPow(e, n)).modPow(d, n)).equals(p))
                            break;
                    }
                    p = null;
                }
            }

            if (p.compareTo(q) < 0) {
                BigInteger tmp = p;
                p = q;
                q = tmp;
            }

            BigInteger ep = d.remainder(p.subtract(one));
            BigInteger eq = d.remainder(q.subtract(one));
            BigInteger c = q.modInverse(p);

            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(n, e);
            RSAPrivateCrtKeySpec privKeySpec = new RSAPrivateCrtKeySpec(n, e, d, p, q, ep, eq, c);

            try {
                KeyFactory kFact = (provider == null) ? KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA)
                        : KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA, provider);
                return new KeyPair(kFact.generatePublic(pubKeySpec), kFact.generatePrivate(privKeySpec));
            } catch (Exception ex) {
                try {
                    KeyFactory kFact = KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA);
                    return new KeyPair(kFact.generatePublic(pubKeySpec), kFact.generatePrivate(privKeySpec));
                } catch (Exception ex2) {
                    // instrumented ffdc
                }
            }
        }

        return null;
    }
}