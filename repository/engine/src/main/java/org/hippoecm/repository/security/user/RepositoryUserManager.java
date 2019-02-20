/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.user;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.AuthorizableTypeException;
import org.apache.jackrabbit.api.security.user.User;
import org.hippoecm.repository.PasswordHelper;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.ManagerContext;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.security.JvmCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserManager backend that stores the users inside the JCR repository
 */
public class RepositoryUserManager extends AbstractUserManager {

    private static final Logger log = LoggerFactory.getLogger(RepositoryUserManager.class);
    private static final String SECURITY_PATH = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.SECURITY_PATH;

    // note we include a random uuid to make sure that *before* a successful login the attribute name is not known
    // on the SimpleCredentials object
    private static final String IDENTITY_SECRET = RepositoryUserManager.class.getName() + ".identitySecret";

    private static final long ONEDAYMS = 1000 * 3600 * 24;

    private boolean maintenanceMode = false;

    public void initManager(ManagerContext context) throws RepositoryException {
        initialized = true;
        maintenanceMode = context.isMaintenanceMode();
    }

    public boolean isPasswordExpired(String rawUserId) throws RepositoryException {
        if (isSystemUser(rawUserId)) {
            // system users password does not expire
            return false;
        }
        long passwordMaxAge = getPasswordMaxAge();
        if (passwordMaxAge > 0) {
            Node user = getUser(rawUserId);
            if (user.hasProperty(HippoNodeType.HIPPO_PASSWORDLASTMODIFIED)) {
                long passwordLastModified = user.getProperty(HippoNodeType.HIPPO_PASSWORDLASTMODIFIED).getLong();
                return passwordLastModified + passwordMaxAge < System.currentTimeMillis();
            }
        }
        return false;
    }

    private long getPasswordMaxAge() throws RepositoryException {
        Node securityNode = session.getRootNode().getNode(SECURITY_PATH);
        if (securityNode.hasProperty(HippoNodeType.HIPPO_PASSWORDMAXAGEDAYS)) {
            return (long) (securityNode.getProperty(HippoNodeType.HIPPO_PASSWORDMAXAGEDAYS).getDouble() * ONEDAYMS);
        }
        return -1l;
    }


    /**
     * Authenticate the user against the hash stored in the user node
     */
    public boolean authenticate(SimpleCredentials creds) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        // check if user exists
        if (!hasUser(creds.getUserID())) {
            return false;
        }
        try {
            char[] password = creds.getPassword();
            Node userinfo = getUser(creds.getUserID());

            String passkey = JcrUtils.getStringProperty(userinfo, HippoNodeType.HIPPO_PASSKEY, null);
            if (passkey != null && password != null && password.length > 0) {
                if (JvmCredentials.PASSKEY.equals(passkey)) {
                    log.info("User '{}' has {} passkey: attempting to log in with Jvm credentials", creds.getUserID(), JvmCredentials.PASSKEY);
                    final JvmCredentials jvmCreds = JvmCredentials.getCredentials(creds.getUserID());
                    if (Arrays.equals(jvmCreds.getPassword(), password)) {
                        log.info("User '{}' authenticated via jvm credentials", creds.getUserID());
                        return true;
                    }
                    log.info("Jvm credentials did not match for user '{}'. Continuing with regular authentication", creds.getUserID());
                } else if (Arrays.equals(password, passkey.toCharArray())) {
                    return true;
                }
            }

            if (maintenanceMode) {
                return true;
            }

            byte[] storedIdentitySecret = (byte[])creds.getAttribute(IDENTITY_SECRET);

            if (storedIdentitySecret != null) {
                final int identity = IdentityCipher.INSTANCE.decrypt(storedIdentitySecret);
                return identity == System.identityHashCode(creds);
            }

            // do regular password check
            final boolean result = PasswordHelper.checkHash(password, getPasswordHash(userinfo));

            // store a secret for authentication on creds instance, but clear the actual password to avoid plaintext attack
            if (result) {
                final byte[] identitySecret = IdentityCipher.INSTANCE.encrypt(creds);
                creds.setAttribute(IDENTITY_SECRET, identitySecret);
            }
            zeroOutPassword(creds.getPassword());
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RepositoryException("Unknown algorithm found when authenticating user: " + creds.getUserID(), e);
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException("Unsupported encoding found when authenticating user: " + creds.getUserID(), e);
        }
    }

    private void zeroOutPassword(final char[] password) {
        Arrays.fill(password, (char)0);
    }

    public String getNodeType() {
        return HippoNodeType.NT_USER;
    }

    public boolean isCaseSensitive() {
        return true;
    }

    /**
     * The backend is the repository, no need to sync anything.
     */
    public void syncUserInfo(String userId) {
    }

    /**
     * Get the (optionally) hashed password of the user
     * @return the password hash
     * @throws RepositoryException
     */
    private String getPasswordHash(Node user) throws RepositoryException {
        if (user.hasProperty(HippoNodeType.HIPPO_PASSWORD)) {
            return user.getProperty(HippoNodeType.HIPPO_PASSWORD).getString();
        } else {
            return null;
        }
    }

    @Override
    public <T extends Authorizable> T getAuthorizable(final String id, final Class<T> authorizableClass) throws AuthorizableTypeException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public Authorizable getAuthorizableByPath(final String path) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public User createSystemUser(final String userID, final String intermediatePath) throws AuthorizableExistsException, RepositoryException {
        throw new UnsupportedRepositoryOperationException("Not yet implemented.");
    }

    public boolean isAutoSave() {
        return false;
    }


    public static final class IdentityCipher {

        private static final IdentityCipher INSTANCE = new IdentityCipher();

        private final SecretKeySpec secret;

        private IdentityCipher() {
            try {
                KeyGenerator kgen = KeyGenerator.getInstance("AES");
                kgen.init(128);
                secret = new SecretKeySpec(kgen.generateKey().getEncoded(), "AES");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Encryption method AES could not be found", e);
            }
        }

        public byte[] encrypt(final Object object) {
            try {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, secret);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(System.identityHashCode(object));
                return cipher.doFinal(baos.toByteArray());
            } catch (Exception e) {
                throw new RuntimeException("Failed to encrypt object identity", e);
            }
        }

        public int decrypt(byte[] bytes) {
            try {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, secret);
                byte[] decrypted = cipher.doFinal(bytes);
                ByteArrayInputStream baos = new ByteArrayInputStream(decrypted);
                ObjectInputStream ois = new ObjectInputStream(baos);
                return (int) ois.readObject();
            } catch (Exception e) {
                throw new RuntimeException("Could not decrypt object identity", e);
            }
        }
    }
}
