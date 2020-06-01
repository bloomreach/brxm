/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.compare;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamComparer implements IComparer<InputStream> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(StreamComparer.class);

    public boolean areEqual(InputStream baseStream, InputStream currentStream) {
        try {
            byte[] baseBytes = new byte[32 * 1024];
            byte[] currentBytes = new byte[32 * 1024];
            while (baseStream.available() > 0 && currentStream.available() > 0) {
                int baseRead = baseStream.read(baseBytes);
                int currentRead = currentStream.read(currentBytes);
                if (baseRead != currentRead) {
                    return false;
                }
                if (baseRead == -1) {
                    break;
                }
                if (!Arrays.equals(baseBytes, currentBytes)) {
                    return false;
                }
            }
        } catch (IOException ex) {
            log.error("Could not compare streams", ex.getMessage());
            return false;
        } finally {
            try {
                baseStream.close();
            } catch (IOException e) {
                log.error("Error closing stream", e);
            }
            try {
                currentStream.close();
            } catch (IOException e) {
                log.error("Error closing stream", e);
            }
        }
        return true;
    }

    public int getHashCode(InputStream stream) {
        int hashCode = 13;
        try {
            byte[] bytes = new byte[32 * 1024];
            while (stream.available() > 0) {
                int read = stream.read(bytes);
                for (int i = 0; i < read; i++) {
                    hashCode = (hashCode << 1) ^ hashCode ^ bytes[i];
                }
            }
        } catch (IOException ex) {
            log.error("Error when calculating hash code", ex);
        }
        return hashCode;
    }

}
