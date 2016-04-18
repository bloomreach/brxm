/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.StringTokenizer;

import org.apache.jackrabbit.util.Base64;

/**
 * <p>
 * Password helper utility class for generating and checking password hashes.
 * </p>
 * <p>
 * For the client side the method {@link #getHash(char[])} can be used to
 * generate a valid password hash for storing in the password field of a
 * user in the repository.
 * </p>
 * <p>
 * For the server side the method {@link #checkHash(char[], String)} can be
 * used to verify the (user supplied) password against the hash stored in
 * the repository.
 * </p>
 * <p>
 * The following option can be adjusted:
 * <ul>
 *   <li> The hashing algorithm (default SHA-256)
 *   <li> The salt size (default 8)
 *   <li>
 * </ul>
 * </p>
 */
public class PasswordHelper {


    /**
     * Algorithm to use for random number generation
     */
    private static final String randomAlgorithm = "SHA1PRNG";

    /**
     * The number of digest iterations is fixed
     */
    private static final int DIGEST_ITERATIONS = 1039;

    /**
     * Use fixed encoding before digesting
     */
    private static final String FIXED_ENCODING = "UTF-8";
    private static final String SEPARATOR = "$";

    /**
     * This (default) size of the salt
     */
    private static int saltSize = 8;

    /**
     * The (default) hashing algorithm
     */
    private static String hashingAlgorithm = "SHA-256";

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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Base64.decode(data, out);
        return out.toByteArray();
    }

    /**
     * From a byte[] returns a base 64 representation
     * @param data byte[]
     * @return String
     * @throws IOException
     * @throws IOException
     */
    public static String byteToBase64(byte[] data) throws IOException {
        StringWriter writer = new StringWriter();
        Base64.encode(data, 0, data.length, writer);
        return writer.toString();
    }

    /**
     * Generate a random salt
     * @return a byte array with the random salt
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    synchronized public static byte[] getSalt() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] salt = new byte[saltSize];
        SecureRandom random = SecureRandom.getInstance(randomAlgorithm);
        random.setSeed(System.nanoTime());
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Generate the digest string from the plain text
     * @param algorithm
     * @param plainText
     * @param salt
     * @return String the base64 digest string
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String getDigest(String algorithm, char[] plainText, byte[] salt) throws NoSuchAlgorithmException,
            IOException {

        // the null encryption :(
        if (algorithm == null || algorithm.length() == 0 || algorithm.endsWith("plain")) {
            return new String(plainText);
        }

        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.reset();

        // salting
        if (salt != null && salt.length > 0) {
            md.update(salt);
        }

        byte[] digest = md.digest(new String(plainText).getBytes(FIXED_ENCODING));

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
     * <li>$algorithm$salt$digest</li>
     * </ul>
     * @param plainText usually the password
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String getHash(char[] plainText) throws NoSuchAlgorithmException, IOException {

        // the null encryption :(
        if (hashingAlgorithm == null || hashingAlgorithm.length() == 0 || hashingAlgorithm.endsWith("plain")) {
            return new String(plainText);
        }
        byte[] salt = getSalt();
        return buildHashString(hashingAlgorithm, plainText, salt);
    }

    /**
     * Helper function to build the hash sting in the correct format
     * @param algorithm
     * @param plainText
     * @param salt
     * @return String
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String buildHashString(String algorithm, char[] plainText, byte[] salt)
            throws NoSuchAlgorithmException, IOException {
        return SEPARATOR + algorithm + SEPARATOR + byteToBase64(salt) + SEPARATOR + getDigest(algorithm, plainText, salt);
    }

    /**
     * Check the password against the hash
     * @param password
     * @param hash the (encrypted) hash
     * @return boolean
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    synchronized public static boolean checkHash(char[] password, String hash) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {

        // no hash or empty hash doesn't match anything
        if (hash == null || hash.isEmpty()) {
            return false;
        }

        // don't allow empty passwords
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Empty passwords are not allowed");
        }

        StringTokenizer st = new StringTokenizer(hash, SEPARATOR);
        int tokens = st.countTokens();

        // invalid encrypted string
        if (tokens != 1 && tokens != 3) {
            throw new IllegalArgumentException("Invalid hash: " + hash);
        }

        // plain text hash
        if (tokens == 1) {
            return hash.equals(new String(password));
        }

        // check encrypted hash
        if (st.countTokens() == 3) {
            String algorithm = st.nextToken();
            String salt = st.nextToken();
            try {
                String newHash = buildHashString(algorithm, password, base64ToByte(salt));
                return hash.equals(newHash);
            } catch (IOException e) {
                return false;
            }

        }
        throw new IllegalStateException("Something went wrong with the password check.");
    }

    /**
     * Get HashingAlgorithm
     * @return String
     * @deprecated use #org.hippoecm.repository.PasswordHelper#getHashingAlgorithm()
     */
    @Deprecated
    public static String getHashingAlogrithm() {
        return hashingAlgorithm;
    }

    /**
     * Set the hashing algorithm for generating new hashes
     * @param hashingAlgorithm
     * @deprecated use #org.hippoecm.repository.PasswordHelper#setHashingAlgorithm(java.lang.String)
     */
    @Deprecated
    public static void setHashingAlogrithm(String hashingAlgorithm) {
        PasswordHelper.hashingAlgorithm = hashingAlgorithm;
    }


    public static String getHashingAlgorithm() {
        return hashingAlgorithm;
    }

    public static void setHashingAlgorithm(final String hashingAlgorithm) {
        PasswordHelper.hashingAlgorithm = hashingAlgorithm;
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
