/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Serializable} representation of a resource from the repository as a html page.
 * The binary page uses the absolute resource path as identifier. The binary data
 * is optionally stored in a byte array. When done so the binary must be able to fit in memory.
 */
public class BinaryPage implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(BinaryPage.class);

    private static final long serialVersionUID = 1L;

    private final String path;
    private String repositoryPath;
    private int status = HttpServletResponse.SC_NOT_FOUND;
    private String mimeType = null;
    private String fileName = null;
    private long lastModified = -1L;
    private long nextValidityCheckTimeStamp = -1L;
    private long creationTime;
    private long length;
    private boolean cacheable = true;
    private byte[] data = ArrayUtils.EMPTY_BYTE_ARRAY;
    private CacheKey cacheKey;

    /** 
     * Create a new binary page. This will just create the page holder. The fields have to
     * be set explicitly after the object creation.
     * @param resourcePath the absolute path of the resource in the repository.
     */
    public BinaryPage(String resourcePath) {
        if (resourcePath == null) {
            throw new IllegalArgumentException("Parameter resourcePath is not allowed to be null");
        }
        this.path = resourcePath;
        setCreatedNow();
    }

    /**
     * @return the absolute path
     */
    public String getResourcePath() {
        return path;
    }

    /**
     * Gets the absolute path of the repository. Note that this can be different than {@link #getResourcePath()} in case
     * when there are resourceContainers that modify the resourcePath to a different repository path
     *
     * @return the absolute repository path
     */
    public String getRepositoryPath() {
        return repositoryPath;
    }

    /**
     * Sets the absolute path of the repository. Note that this can be a different path than {@link #getResourcePath()} in case
     * when there are resourceContainers that modify the resourcePath to a different repository path
     *
     */
    public void setRepositoryPath(final String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    /**
     * Get the HTTP status for the resource.
     * @return the HTTP status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Get the mime type for the resource. Could be null.
     * @return the mime type or null when unknown.
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Get the unencoded file name to be used for Content-Disposition header
     * @return the file name or null when not set.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Get the ETag
     * @return the ETag
     */
    public String getETag() {
        return "\"" + Math.abs(getResourcePath().hashCode() * 17L + getLength() * 13L + getLastModified()) + "\"";
    }

    /**
     * Get the last modified date in milliseconds since epoch.
     * @return the last modified date or -1 if unknown
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Get the time the binary page cache object was create in milliseconds since epoch.
     * @return creation time
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Get the next Validity Check TimeStamp (millis since epoch)
     * @return the nextValidityCheckTimeStamp time
     */
    public long getNextValidityCheckTimeStamp() {
        return nextValidityCheckTimeStamp;
    }

    /**
     * Get the data length which can be used for the Content-Length header or cache checks.
     * @return the length of the binary data
     */
    public long getLength() {
        if (data.length > 0) {
            return data.length;
        } else {
            return length;
        }
    }

    /**
     * Check if the BinaryPage also contains the data next to the meta data.
     * @return true if the BinaryPage contains the data as well as the meta data
     */
    public boolean containsData() {
        return (data.length > 0);
    }

    /**
     * Get the {@link InputStream} to the byte buffer of the data.
     * @return the input stream
     */
    public InputStream getStream() {
        // don't use buffered stream as the data is already in memory, aka buffered.
        return new ByteArrayInputStream(data);
    }

    /**
     * Read the data from the input stream and close the stream when done.
     * @param input the input stream to read the data from.
     * @throws IOException when an error occurs while copying. The stream will be closed.
     */
    public void loadDataFromStream(InputStream input) throws IOException {
        try {
            data = IOUtils.toByteArray(input);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    public boolean isCacheable() {
        return cacheable;
    }

    public CacheKey getCacheKey() {
        if (cacheKey == null) {
            log.error("Cachekey should never be null! but was for '{}'. Implementation issue", getResourcePath(),
                    new Throwable("Cachey not allowed to be null"));
        }
        return cacheKey;
    }

    /**
     * Set the HTTP status. The status is not checked for validity.
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Set the mime type.
     * @param mimeType
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Set the (un-encoded) file name for the Content-Disposition header.
     * @param fileName
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Set the next validity check timestamp since epoch.
     * @param nextValidityCheckTimeStamp
     */
    public void setNextValidityCheckTime(long nextValidityCheckTimeStamp) {
        this.nextValidityCheckTimeStamp = nextValidityCheckTimeStamp;
    }

    /**
     * Set the last modification time in milliseconds since epoch.
     * @param lastModified
     */
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Set the size of the binary data. This method should only be used when
     * the data is not stored in the BinaryData object.
     * @param length the length of the data.
     */
    public void setLength(long length) {
        this.length = length;
    }

    public void setCacheable(final boolean cacheable) {
        this.cacheable = cacheable;
    }

    /**
     * Set the creation time to the current time.
     */
    private void setCreatedNow() {
        this.creationTime = System.currentTimeMillis();
    }


    public void setCacheKey(final CacheKey cacheKey) {
        this.cacheKey = cacheKey;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("path=").append(getResourcePath());
        sb.append("repositoryPath=").append(getRepositoryPath());
        sb.append(" status=").append(getStatus());
        sb.append(" mimetype=").append(getMimeType());
        sb.append(" filename=").append(getFileName());
        sb.append(" etag=").append(getETag());
        sb.append(" lastmodified=").append(getLastModified());
        sb.append(" size=").append(getLength());
        sb.append(" cacheable=").append(cacheable);
        sb.append(" cacheKey=").append(cacheKey);
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BinaryPage that = (BinaryPage) o;

        if (cacheable != that.cacheable) {
            return false;
        }
        if (lastModified != that.lastModified) {
            return false;
        }
        if (length != that.length) {
            return false;
        }
        if (path != null ? !path.equals(that.path) : that.path != null) {
            return false;
        }

        if (cacheKey != null ? !cacheKey.equals(that.cacheKey) : that.cacheKey != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (int) (lastModified ^ (lastModified >>> 32));
        result = 31 * result + (int) (length ^ (length >>> 32));
        result = 31 * result + (cacheable ? 1 : 0);
        result = cacheKey == null ? result : 31 * result + cacheKey.hashCode();
        return result;
    }

    public static class CacheKey implements Serializable {

        private final String sessionUserID;
        private final String resourcePath;

        public CacheKey(final String sessionUserID, final String resourcePath) {
            this.sessionUserID = sessionUserID;
            this.resourcePath = resourcePath;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final CacheKey cacheKey = (CacheKey)o;

            if (!resourcePath.equals(cacheKey.resourcePath)) {
                return false;
            }
            if (sessionUserID != null ? !sessionUserID.equals(cacheKey.sessionUserID) : cacheKey.sessionUserID != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = sessionUserID != null ? sessionUserID.hashCode() : 0;
            result = 31 * result + resourcePath.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "CacheKey{" +
                    "sessionUserID='" + sessionUserID + '\'' +
                    ", resourcePath='" + resourcePath + '\'' +
                    '}';
        }
    }

}