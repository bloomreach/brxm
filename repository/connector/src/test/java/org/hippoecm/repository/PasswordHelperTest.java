/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

/**
 * Unit test the org.hippoecm.repository.PasswordHelper main methods
 */
public class PasswordHelperTest {

    private static String DEFAULT_HASHING_ALGORITHM = "SHA-256";

    @Test
    public void testDefaultPasswordHashIsSHA256() throws IOException, NoSuchAlgorithmException {
        Assert.assertEquals(DEFAULT_HASHING_ALGORITHM, PasswordHelper.getHashingAlgorithm());
        String password = "Password2016";
        String passwordHash = PasswordHelper.getHash(password.toCharArray());
        StringTokenizer st = new StringTokenizer(passwordHash, "$");
        Assert.assertEquals(3, st.countTokens());
        Assert.assertEquals(DEFAULT_HASHING_ALGORITHM, st.nextToken());
    }

    @Test
    public void testCheckHash() throws IOException, NoSuchAlgorithmException {
        String password = "Password2016*#$";
        String passwordHash = PasswordHelper.getHash(password.toCharArray());
        Assert.assertTrue("hash is not matched",PasswordHelper.checkHash(password.toCharArray(), passwordHash));
    }
}
