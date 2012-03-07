/*
 *  Copyright 2011 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.sso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Symmetric cipher that encrypts and decrypts jcr {@link Credentials}. It's key is generated dynamically, being unique
 * for the lifetime of the class.
 * <p/>
 * When using the this class to encrypt or decrypt using provided methods, make sure that the same "instance" is used
 * for both the operations. Otherwise you might get exception while decrypting.
 */
public final class CredentialCipher {

    static final Logger log = LoggerFactory.getLogger(CredentialCipher.class);

    private final static CredentialCipher instance = new CredentialCipher();

    public static final String HIPPO_CLUSTER_KEY = "hippo.cluster.sso.key";

    public static CredentialCipher getInstance() {
        return instance;
    }

    private SecretKeySpec secret;

    CredentialCipher() {
        String sharedKey = System.getProperty(HIPPO_CLUSTER_KEY);
        if (sharedKey != null) {
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                KeySpec spec = new PBEKeySpec(sharedKey.toCharArray(), HIPPO_CLUSTER_KEY.getBytes(), 1024, 128);
                SecretKey tmp = factory.generateSecret(spec);
                secret = new SecretKeySpec(tmp.getEncoded(), "AES");
                return;
            } catch (InvalidKeySpecException e) {
                log.error("Could not initialize secret from shared secret in system property ''" + HIPPO_CLUSTER_KEY + "', generating own key", e);
            } catch (NoSuchAlgorithmException e) {
                log.error("Could not initialize secret from shared secret in system property ''" + HIPPO_CLUSTER_KEY + "', generating own key", e);
            }
        }
        KeyGenerator kgen;
        try {
            kgen = KeyGenerator.getInstance("AES");
            kgen.init(128);
            secret = new SecretKeySpec(kgen.generateKey().getEncoded(), "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Encryption method AES could not be found", e);
        }
    }

    public byte[] encrypt(String key, SimpleCredentials credentials) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secret);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(key);
            oos.writeObject(credentials);

            return cipher.doFinal(baos.toByteArray());
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        } catch (BadPaddingException e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        }
    }

    /**
     * Get the credentials as UrlSafeBase64 encoded String.
     *
     * @param credentials JCR Simple Credentials
     * @return Base64 Encoded string of the encrypted credentials.
     */
    public String getEncryptedString(String key, SimpleCredentials credentials) {
        return UrlSafeBase64.encode(encrypt(key, credentials));
    }

    /**
     * Get the Credentials by decrypting UrlSafeBase64 encoded String.
     *
     * @param credentialString UrlSafeBase64 encoded string which contains encrypted JCR credential String.
     * @return JcrCredentials (SimpleCredentials).
     */
    public Credentials decryptFromString(String key, String credentialString) throws SignatureException {
        return decrypt(key, UrlSafeBase64.decode(credentialString));
    }

    public Credentials decrypt(String key, byte[] bytes) throws SignatureException {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secret);

            byte[] decrypted = cipher.doFinal(bytes);
            ByteArrayInputStream baos = new ByteArrayInputStream(decrypted);
            ObjectInputStream ois = new ObjectInputStream(baos);
            String encryptedKey = (String) ois.readObject();
            if (!key.equals(encryptedKey)) {
                throw new SignatureException("Provided key does not match encrypted key");
            }
            return (Credentials) ois.readObject();
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        } catch (IOException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        } catch (BadPaddingException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        }
    }
}
