/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.brokenlinks;

import java.io.Serializable;
import java.util.Calendar;

public class Link implements Comparable<Object>, Serializable {

    private static final long serialVersionUID = 1289708150121627355L;
    public static final int ERROR_CODE = -1;
    public static final int EXCEPTION_CODE = 1000;

    private String sourceNodeIdentifier;
    private String url;
    private boolean isBroken;
    private int resultCode;
    private String resultMessage;
    private Exception resultException;
    private Calendar lastTimeChecked;
    private Calendar brokenSince;
    private String excerpt;

    public Link(String url, String sourceNodeIdentifier) {
        this.url = url;
        this.sourceNodeIdentifier = sourceNodeIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Link) {
            Link other = (Link)o;
            return url.equals(other.url);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    public int compareTo(Object o) {
        if (o instanceof Link) {
            Link other = (Link)o;
            return url.compareTo(other.url);
        } else {
            throw new ClassCastException("Cannot compare a Link to a " + o.getClass().getCanonicalName());
        }
    }

    public String getSourceNodeIdentifier() {
        return sourceNodeIdentifier;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isBroken() {
        return isBroken;
    }

    public void setBroken(boolean broken) {
        isBroken = broken;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public Exception getResultException() {
        return resultException;
    }

    public void setResultException(Exception resultException) {
        this.resultException = resultException;
    }

    public Calendar getLastTimeChecked() {
        return lastTimeChecked;
    }

    public void setLastTimeChecked(Calendar lastTimeChecked) {
        this.lastTimeChecked = lastTimeChecked;
    }

    public Calendar getBrokenSince() {
        return brokenSince;
    }

    public void setBrokenSince(Calendar brokenSince) {
        this.brokenSince = brokenSince;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Link");
        sb.append("{url='").append(url).append('\'');
        sb.append(", isBroken=").append(isBroken);
        sb.append(", resultCode=").append(resultCode);
        sb.append(", resultMessage='").append(resultMessage).append('\'');
        sb.append(", resultException=").append(resultException);
        sb.append(", broken=").append(isBroken());
        sb.append('}');
        return sb.toString();
    }
}

