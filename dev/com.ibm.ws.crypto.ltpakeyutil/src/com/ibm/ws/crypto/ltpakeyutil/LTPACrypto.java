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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.GCMParameterSpec;
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
        private int hashcode;
        private byte[] result;

        @Trivial
        private CachingKey(byte[] key, byte[] data) {
            this.key = key;
            this.data = data;
            this.successfulUses = 0;
            this.reused = false;

            this.hashcode = 0;
            if (key != null && key.length > 0) {
                hashcode += key[0];
            }
            if (data != null) {
                for (int i = 0; i < data.length && i < 10; i++) {
                    hashcode += data[i];
                }
                for (int i = data.length - 1; i >= 0 && i > data.length - 10; i--) {
                    this.hashcode += data[i];
                }
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
            if (!Arrays.equals(key, ck.key)) {
                return false;
            }
            if (!Arrays.equals(data, ck.data)) {
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
     * Sign a pre-computed digest with the given RSA private key.
     *
     * Results are cached by (privateKey.getEncoded(), digest),
     * mirroring the verify cache structure.
     * @param privateKey JCA RSA private key (from {@link LTPAPrivateKey#getRawKey()})
     * @param digest     Pre-computed message digest (SHA-1 or SHA-512)
     * @return RSA signature bytes
     */
    @Trivial
    protected static final byte[] signRSA(PrivateKey privKey, byte[] data) throws Exception {
        CachingKey ck = new CachingKey(privKey.getEncoded(), data);
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

        rsaSig.initSign(privKey);
        rsaSig.update(data);
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
        private final byte[] sig;
        private int hashcode;
        private boolean result;

        @Trivial
        private CachingVerifyKey(byte[] key, byte[] data, byte[] sig) {
            this.key = key;
            this.data = data;
            this.sig = sig;
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
            if (!Arrays.equals(key, ck.key)) {
                return false;
            }
            if (!Arrays.equals(data, ck.data)) {
                return false;
            }
            if (!Arrays.equals(sig, ck.sig)) {
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
     * Verify an RSA signature over a pre-computed digest.
     * Results are cached by (publicKey.getEncoded(), digest, signature),
     * mirroring the ML-DSA verify cache in PQCSignatureHelper.
     *
     * @param publicKey JCA RSA public key (from {@link LTPAPublicKey#getRawKey()})
     * @param digest    Pre-computed message digest (SHA-1 or SHA-512)
     * @param signature Signature bytes to verify
     * @return {@code true} if the signature is valid
     */
    @Trivial
    protected static final boolean verifyRSA(PublicKey pubKey, byte[] data, byte[] sig) throws Exception {
        CachingVerifyKey ck = new CachingVerifyKey(pubKey.getEncoded(), data, sig);
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

        rsaSig.initVerify(pubKey);
        rsaSig.update(data);
        verified = rsaSig.verify(sig);

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

    /**
     * Encrypt the data using AES-GCM (authenticated encryption).
     * PQC Issue #35556 - Task 2.6: AES-GCM provides authenticated encryption,
     * preventing tampering attacks that are possible with AES-CBC.
     *
     * @param data The byte representation of the data
     * @param key  The key used to encrypt the data
     * @return The encrypted data with format: [IV (12 bytes)][Ciphertext][Auth Tag (16 bytes)]
     * @throws Exception if encryption fails
     */
    @Trivial
    protected static final byte[] encryptGCM(byte[] data, byte[] key) throws Exception {
        // Generate random 12-byte IV for GCM
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[12];
        random.nextBytes(iv);
        
        // Create AES key
        int keyLength = fipsEnabled ? CryptoUtils.AES_256_KEY_LENGTH_BYTES : CryptoUtils.AES_128_KEY_LENGTH_BYTES;
        SecretKeySpec keySpec = new SecretKeySpec(key, 0, keyLength, CryptoUtils.ENCRYPT_ALGORITHM_AES);
        
        // Initialize cipher with GCM mode
        Cipher cipher = (provider == null) 
            ? Cipher.getInstance("AES/GCM/NoPadding")
            : Cipher.getInstance("AES/GCM/NoPadding", provider);
        
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // 128-bit auth tag
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        
        // Encrypt data (includes authentication tag)
        byte[] ciphertext = cipher.doFinal(data);
        
        // Combine IV and ciphertext: [IV][Ciphertext+Tag]
        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
        
        return result;
    }

    /**
     * Decrypt the data using AES-GCM (authenticated encryption).
     * PQC Issue #35556 - Task 2.6: AES-GCM verifies authentication tag,
     * ensuring data has not been tampered with.
     *
     * @param encryptedData The encrypted data with format: [IV (12 bytes)][Ciphertext][Auth Tag (16 bytes)]
     * @param key           The key used to decrypt the data
     * @return The decrypted data (plaintext)
     * @throws Exception if decryption fails or authentication tag is invalid
     */
    @Trivial
    protected static final byte[] decryptGCM(byte[] encryptedData, byte[] key) throws Exception {
        // Extract IV (first 12 bytes)
        byte[] iv = new byte[12];
        System.arraycopy(encryptedData, 0, iv, 0, 12);
        
        // Extract ciphertext + auth tag (remaining bytes)
        byte[] ciphertext = new byte[encryptedData.length - 12];
        System.arraycopy(encryptedData, 12, ciphertext, 0, ciphertext.length);
        
        // Create AES key
        int keyLength = fipsEnabled ? CryptoUtils.AES_256_KEY_LENGTH_BYTES : CryptoUtils.AES_128_KEY_LENGTH_BYTES;
        SecretKeySpec keySpec = new SecretKeySpec(key, 0, keyLength, CryptoUtils.ENCRYPT_ALGORITHM_AES);
        
        // Initialize cipher with GCM mode
        Cipher cipher = (provider == null)
            ? Cipher.getInstance("AES/GCM/NoPadding")
            : Cipher.getInstance("AES/GCM/NoPadding", provider);
        
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // 128-bit auth tag
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        
        // Decrypt and verify authentication tag
        return cipher.doFinal(ciphertext);
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
        KeyPairGenerator keyGen = null;
        int keySizeBits = CryptoUtils.isFips140_3Enabled() ? 256 * 8 : 128 * 8;
        try {
            keyGen = (provider == null) ? KeyPairGenerator.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA)
                    : KeyPairGenerator.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA, provider);
            keyGen.initialize(keySizeBits, new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            // instrumented ffdc or handled unsupport operation exception
            return null;
        }
    }
}
