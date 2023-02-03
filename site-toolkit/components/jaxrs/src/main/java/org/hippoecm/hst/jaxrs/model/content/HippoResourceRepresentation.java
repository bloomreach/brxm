/*
 *  Copyright 2010-2023 Bloomreach
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
package org.hippoecm.hst.jaxrs.model.content;

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.content.beans.standard.HippoResourceBean;

/**
 * @version $Id$
 */
@XmlRootElement(name = "resource")
public class HippoResourceRepresentation extends NodeRepresentation {

    private String mimeType;
    private long length;
    private boolean blank;

    public HippoResourceRepresentation represent(HippoResourceBean bean) throws RepositoryException {
        super.represent(bean);
        mimeType = bean.getMimeType();
        length = bean.getLength();
        blank = bean.isBlank();
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public boolean isBlank() {
        return blank;
    }

    public void setBlank(final boolean blank) {
        this.blank = blank;
    }
}
