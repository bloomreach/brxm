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

import java.security.SignatureException;

import javax.jcr.SimpleCredentials;

import org.junit.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class CredentialCipherTest {
    @Test
    public void testEncodeDecode() {
        String original = "admin";
        String encoded = UrlSafeBase64.encode(original.getBytes());
        Assert.assertEquals("YWRtaW4=", encoded);
        String decoded = new String(UrlSafeBase64.decode(encoded));
        Assert.assertEquals(original, decoded);
    }

    @Test
    public void testPadding() {
        String original = "x";
        String encoded = UrlSafeBase64.encode(original.getBytes());
        Assert.assertEquals(4, encoded.length());
        assertTrue(encoded.endsWith("=="));
        String decoded = new String(UrlSafeBase64.decode(encoded));
        Assert.assertEquals(original, decoded);

        original = "xx";
        encoded = UrlSafeBase64.encode(original.getBytes());
        Assert.assertEquals(4, encoded.length());
        assertTrue(encoded.endsWith("="));
        decoded = new String(UrlSafeBase64.decode(encoded));
        Assert.assertEquals(original, decoded);

        original = "xxx";
        encoded = UrlSafeBase64.encode(original.getBytes());
        Assert.assertEquals(4, encoded.length());
        decoded = new String(UrlSafeBase64.decode(encoded));
        Assert.assertEquals(original, decoded);
    }

    @Test
    public void testEncoding() {
        byte[] bytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            bytes[i++] = (byte) i;
        }

        String encoded = UrlSafeBase64.encode(bytes);
        byte[] decoded = UrlSafeBase64.decode(encoded);
        for (int i = 0; i < 256; i++) {
            assertEquals(bytes[i], decoded[i]);
        }
    }

    @Test
    public void testEncryptionAndDecryption() throws Exception {

        String key = "secret";
        SimpleCredentials sc = new SimpleCredentials("admin", "admin".toCharArray());
        byte[] encrypted = CredentialCipher.getInstance().encrypt(key, sc);

        String base64EncString = UrlSafeBase64.encode(encrypted);
        byte[] decrypted = UrlSafeBase64.decode(base64EncString);

        SimpleCredentials decryptedCred = (SimpleCredentials) CredentialCipher.getInstance().decrypt(key, decrypted);
        org.junit.Assert.assertEquals("admin", decryptedCred.getUserID());
        org.junit.Assert.assertArrayEquals("admin".toCharArray(), decryptedCred.getPassword());
    }

    @Test
    public void testInvalidKeyIsRejected() throws Exception {
        String key = "aap";
        SimpleCredentials sc = new SimpleCredentials("admin", "admin".toCharArray());
        byte[] bytes = CredentialCipher.getInstance().encrypt(key, sc);
        try {
            CredentialCipher.getInstance().decrypt("noot", bytes);
            fail("Decryption succeeded with invalid key");
        } catch (SignatureException exception) {
            // this is ok
        }

    }

    @Test
    public void testSharedKey() throws Exception {
        System.setProperty(CredentialCipher.HIPPO_CLUSTER_KEY, "geheim");
        try {
            CredentialCipher cipher = new CredentialCipher();
            byte[] bytes = cipher.encrypt("client-secret", new SimpleCredentials("aap", "noot".toCharArray()));

            CredentialCipher other = new CredentialCipher();
            SimpleCredentials credentials = (SimpleCredentials) other.decrypt("client-secret", bytes);
            assertEquals("aap", credentials.getUserID());
            assertArrayEquals("noot".toCharArray(), credentials.getPassword());
        } finally {
           System.clearProperty(CredentialCipher.HIPPO_CLUSTER_KEY);
        }
    }
}
