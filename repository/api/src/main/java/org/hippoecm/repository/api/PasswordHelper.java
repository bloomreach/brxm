/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.StringTokenizer;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * <p>
 * Password helper utility class for generating and checking password hashes.
 * </p>
 * <p>
 * For the client side the method {@link #getHash(String)} can be used to
 * generate a valid password hash for storing in the password field of a
 * user in the repository.
 * </p>
 * <p>
 * For the server side the method {@link #checkHash(String,String)} can be
 * used to verify the (user supplied) password against the hash stored in
 * the repository.
 * </p>
 * <p>
 * The following option can be adjusted:
 * <ul>
 *   <li> The hashing alogrithm (default SHA-1)
 *   <li> The salt size (default 8)
 *   <li>
 * </ul>
 * </p>
 */
public class PasswordHelper {

    /**
     * SVN ID
     */
    private final static String SVN_ID = "$Id$";

    /**
     * Alogrithm to use for random number generation
     */
    private static final String randomAlogrithm = "SHA1PRNG";

    /**
     * The number of digest iterations is fixed
     */
    private static final int DIGEST_ITERATIONS = 1039;

    /**
     * Use fixed encoding before digesting
     */
    private static final String FIXED_ENCODING = "UTF-8";

    /**
     * This (default) size of the salt
     */
    private static int saltSize = 8;

    /**
     * The (default) hashing alogrithm
     */
    private static String hashingAlogrithm = "SHA-1";

    /**
     * Prevent instances of this class
     */
    private PasswordHelper() {
    }

    /**
     * From a base 64 representation, returns the corresponding byte[]
     * @param data String The base64 representation
     * @return byte[]
     * @throws IOException
     * @throws IOException
     */
    public static byte[] base64ToByte(String data) throws IOException {
        BASE64Decoder decoder = new BASE64Decoder();
        return decoder.decodeBuffer(data);
    }

    /**
     * From a byte[] returns a base 64 representation
     * @param data byte[]
     * @return String
     * @throws IOException
     */
    public static String byteToBase64(byte[] data) {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }

    /**
     * Generate a random salt
     * @return a byte array with the random salt
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    synchronized public static byte[] getSalt() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] salt = new byte[saltSize];
        SecureRandom random = SecureRandom.getInstance(randomAlogrithm);
        random.setSeed(System.nanoTime());
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Generate the digest string from the plain text
     * @param alogrithm
     * @param plainText
     * @param salt
     * @return String the base64 digest string
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String getDigest(String alogrithm, String plainText, byte[] salt) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {

        // the null encryption :(
        if (alogrithm == null || alogrithm.length() == 0 || alogrithm.endsWith("plain")) {
            return plainText;
        }

        MessageDigest md = MessageDigest.getInstance(alogrithm);
        byte[] digest = null;
        md.reset();

        // salting
        if (salt != null && salt.length > 0) {
            md.update(salt);
        }
        digest = md.digest(plainText.getBytes(FIXED_ENCODING));

        // iterating
        for (int i = 0; i < DIGEST_ITERATIONS; i++) {
            md.reset();
            digest = md.digest(digest);
        }
        return byteToBase64(digest);
    }

    /**
     * Get the password hash. The hash is either in the form of:
     * <ul>
     * <li>plaintext</li>
     * <li>$alogrithm$salt$digest</li>
     * </ul>
     * @param plainText usually the password
     * @param salt
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String getHash(String plainText) throws NoSuchAlgorithmException, UnsupportedEncodingException {

        // the null encryption :(
        if (hashingAlogrithm == null || hashingAlogrithm.length() == 0 || hashingAlogrithm.endsWith("plain")) {
            return plainText;
        }
        byte[] salt = getSalt();
        return buildHashString(hashingAlogrithm, plainText, salt);
    }

    /**
     * Helper function to build the hash sting in the correct format
     * @param alogrithm
     * @param plainText
     * @param salt
     * @return String
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String buildHashString(String alogrithm, String plainText, byte[] salt)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return "$" + alogrithm + "$" + byteToBase64(salt) + "$" + getDigest(alogrithm, plainText, salt);
    }

    /**
     * Check the password against the hash
     * @param password
     * @param hash the (encrypted) hash
     * @return boolean
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    synchronized public static boolean checkHash(String password, String hash) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {

        // don't allow empty passwords
        if (password == null || hash == null || "".equals(password) || "".equals(hash)) {
            throw new IllegalArgumentException("Empty username's and password are not allowed");
        }

        StringTokenizer st = new StringTokenizer(hash, "$");
        int tokens = st.countTokens();

        // invalid encrypted string
        if (tokens != 1 && tokens != 3) {
            throw new IllegalArgumentException("Invalid hash: " + hash);
        }

        // plain text hash
        if (tokens == 1) {
            if (password.equals(hash)) {
                return true;
            } else {
                return false;
            }
        }

        // check encrypted hash
        if (st.countTokens() == 3) {
            String alogrithm = st.nextToken();
            String salt = st.nextToken();
            try {
                String newHash = buildHashString(alogrithm, password, base64ToByte(salt));
                if (hash.equals(newHash)) {
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }

        }
        throw new IllegalStateException("Something went wrong with the password check.");
    }

    /**
     * Get HashingAlogrithm
     * @return String
     */
    public static String getHashingAlogrithm() {
        return hashingAlogrithm;
    }

    /**
     * Set the hashing alogrithm for generating new hashes
     * @param hashingAlogrithm
     */
    public static void setHashingAlogrithm(String hashingAlogrithm) {
        PasswordHelper.hashingAlogrithm = hashingAlogrithm;
    }

    /**
     * Get salt size
     * @return int
     */
    public static int getSaltSize() {
        return saltSize;
    }

    /**
     * Set salt size for generating new hashes
     * @param saltSize
     */
    public static void setSaltSize(int saltSize) {
        PasswordHelper.saltSize = saltSize;
    }

}
