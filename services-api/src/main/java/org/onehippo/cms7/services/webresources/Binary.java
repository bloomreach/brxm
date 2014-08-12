/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.webresources;

import java.io.InputStream;

/**
 * Binary data.
 */
public interface Binary {

    /**
     * Returns an {@link InputStream} representation of this binary. Each call to
     * <code>getStream()</code> returns a new stream. The caller is
     * responsible for calling <code>close()</code> on the returned stream.
     *
     * @return A stream representation of this binary.
     * @throws java.lang.IllegalStateException if {@link #dispose()} has already been called.
     * @throws WebResourceException if another error occurs.
     */
    InputStream getStream();

    /**
     * Reads successive bytes from the specified <code>position</code>
     * in this binary into the passed byte array until either the byte
     * array is full or the end of the <code>Binary</code> is encountered.
     *
     * @param buffer the buffer into which the data is read.
     * @param position the position in this binary from which to start reading bytes.
     *
     * @return the number of bytes read into the buffer, or -1 if there is no
     * more data because the end of the Binary has been reached.
     *
     * @throws java.io.IOException if an I/O error occurs.
     * @throws java.lang.NullPointerException if b is null.
     * @throws java.lang.IllegalArgumentException if offset is negative or {@link #dispose()} has already been called.
     * @throws WebResourceException if another error occurs.
     */
    int read(byte[] buffer, long l) throws java.io.IOException;

    /**
     * @return the size of this binary in bytes.
     * @throws java.lang.IllegalStateException if {@link #dispose()} has already been called.
     * @throws WebResourceException if another error occurs.
     */
    long getSize();

    /**
     * Releases all resources associated with this binary and informs the implementation that
     * these resources may now be reclaimed. An application should call this method when it
     * is finished with the binary object.
     */
    void dispose();

}
