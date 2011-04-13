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

public final class UrlSafeBase64 {
    /**
     * standard encoding, with the exception of url save characters -, _ instead of + and /
     */
    private static final String URL_SAFE_ENCODING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private UrlSafeBase64() {
    }

    public static String encode(byte[] bytes) {
        StringBuilder encoded = new StringBuilder();
        int paddingCount = (3 - (bytes.length % 3)) % 3;
        for (int i = 0; i < bytes.length; i += 3) {
            int first = bytes[i];
            int second = ((i + 1) < bytes.length ? bytes[i + 1] : 0);
            int third = ((i + 2) < bytes.length ? bytes[i + 2] : 0);
            int value = ((first << 16) & 0xff0000) | ((second << 8) & 0xff00) | (third & 0xff);

            encoded.append(URL_SAFE_ENCODING.charAt((int) (value >> 18) & 0x3f));
            encoded.append(URL_SAFE_ENCODING.charAt((int) (value >> 12) & 0x3f));
            encoded.append(URL_SAFE_ENCODING.charAt((int) (value >> 6) & 0x3f));
            encoded.append(URL_SAFE_ENCODING.charAt((int) value & 0x3f));
        }
        return encoded.substring(0, encoded.length() - paddingCount) + "==".substring(0, paddingCount);
    }

    /**
     * @param string
     * @return
     * @throws IllegalArgumentException
     */
    public static byte[] decode(String string) throws IllegalArgumentException {
        if ((string.length() % 4) != 0) {
            throw new IllegalArgumentException("invalid length");
        }
        int nbytes = 3 * (string.length() / 4);
        int padding = string.indexOf('=');
        if (padding != -1) {
            if ((string.length() - padding) > 2) {
                throw new IllegalArgumentException("Unsupported padding");
            }
            nbytes -= string.length() - padding;
        }
        byte[] bytes = new byte[nbytes];
        int curbyte = 0;
        for (int i = 0; i < string.length(); i += 4) {
            int value = 0;
            int count = 3;
            for (int j = 0; j < 4; j++) {
                int ch = string.charAt(i + j);
                if (ch >= 'A' && ch <= 'Z') {
                    ch = ch - 'A';
                } else if (ch >= 'a' && ch <= 'z') {
                    ch = ch - 'a' + 26;
                } else if (ch >= '0' && ch <= '9') {
                    ch = ch - '0' + 52;
                } else if (ch == '-') {
                    ch = 62;
                } else if (ch == '_') {
                    ch = 63;
                } else if (ch == '=') {
                    ch = 0;
                    --count;
                }
                value = ((value << 6) & 0xffffffc0) | (ch & 0xff);
            }

            bytes[curbyte++] = (byte) ((value >> 16) & 0xff);
            if (count > 1) {
                bytes[curbyte++] = (byte) ((value >> 8) & 0xff);
            }
            if (count > 2) {
                bytes[curbyte++] = (byte) ((value) & 0xff);
            }
        }
        return bytes;
    }

}
