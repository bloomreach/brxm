/*
 *  Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.onehippo.cm.model.impl.ModuleImpl;

import static org.onehippo.cm.model.Constants.DEFAULT_DIGEST;

public class DigestUtils {

    /**
     * Helper method to compute a digest string from a ConfigurationModel manifest.
     * @param modelManifest the manifest whose digest we want to compute
     * @return a digest string comparable to the baseline digest string, or "" if none can be computed
     */
    public static String computeManifestDigest(final String modelManifest) {
        try {
            MessageDigest md = MessageDigest.getInstance(DEFAULT_DIGEST);
            byte[] digest = md.digest(StandardCharsets.UTF_8.encode(modelManifest).array());
            return toDigestHexString(digest);
        }
        catch (NoSuchAlgorithmException e) {
            // NOTE: this should never happen, since the Java spec requires MD5 to be supported
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper to compute a digest string from an InputStream. This method ensures that the stream is closed.
     * @param is the InputStream to digest
     * @return a digest string suitable for use in a module manifest
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String digestFromStream(final InputStream is) {
        try {
            // use MD5 because it's fast and guaranteed to be supported, and crypto attacks are not a concern here
            MessageDigest md = MessageDigest.getInstance(DEFAULT_DIGEST);

            // digest the InputStream by copying it and discarding the output
            try (InputStream dis = new DigestInputStream(is, md)) {
                IOUtils.copyLarge(dis, NullOutputStream.NULL_OUTPUT_STREAM);
            }

            // prepend algorithm using same style as used in Hippo CMS password hashing
            return toDigestHexString(md.digest());
        }
        catch (IOException|NoSuchAlgorithmException e) {
            throw new RuntimeException("Exception while computing resource digest", e);
        }
    }

    /**
     * Helper method to convert a byte[] produced by MessageDigest into a hex string marked with the digest algorithm.
     * @param digest the raw digest byte[]
     * @return a String suitable for long-term storage and eventual comparisons
     */
    public static String toDigestHexString(final byte[] digest) {
        // prepend algorithm using same style as used in Hippo CMS password hashing
        return "$" + DEFAULT_DIGEST + "$" + DatatypeConverter.printHexBinary(digest);
    }

}
