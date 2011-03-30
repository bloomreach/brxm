package org.onehippo.sso;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class CredentialCipherTest {
    @Test
    public void testEncryptionAndDecryption() throws Exception {

        SimpleCredentials sc = new SimpleCredentials("admin", "admin".toCharArray());
        byte[] encrypted = CredentialCipher.getInstance().encrypt(sc);

        String base64EncString = new String(Base64.encodeBase64(encrypted, false));

        SimpleCredentials decryptedCred = (SimpleCredentials) CredentialCipher.getInstance().decrypt(Base64.decodeBase64(base64EncString));
        org.junit.Assert.assertEquals("admin", decryptedCred.getUserID());
        org.junit.Assert.assertArrayEquals("admin".toCharArray(), decryptedCred.getPassword());

    }

}
