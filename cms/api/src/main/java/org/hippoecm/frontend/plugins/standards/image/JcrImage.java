/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.image;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.resource.JcrResource;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrImage extends Image {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrImage.class);

    private static MessageDigest MD = null;
    static {
        try {
            MD = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            log.warn("Cannot append path digest to image path");
        }
    }

    JcrResourceStream stream;
    Time lastModified;
    int width;
    int height;

    public JcrImage(String id, JcrResourceStream rs) {
        this(id, rs, 0, 0);
    }

    public JcrImage(String id, JcrResourceStream rs, int width, int height) {
        super(id, new JcrResource(rs));
        stream = rs;
        lastModified = rs.lastModifiedTime();
        this.width = width;
        this.height = height;
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        if (lastModified != null) {
            final String url = tag.getAttributes().getString("src");
            final StringBuilder sb = new StringBuilder(url);
            sb.append(((url.contains("?")) ? "&" : "?"));
            sb.append("w:lm=");
            sb.append((lastModified.getMilliseconds() / 1000));
            if (MD != null) {
                sb.append("&h:pathmd=");
                final String path = JcrUtils.getNodePathQuietly(stream.getChainedModel().getObject());
                sb.append(new BigInteger(1, MD.digest(path.getBytes())).toString(16));
            }
            tag.put("src", sb.toString());
        }
        if (width > 0) {
            tag.put("width", width);
        }
        if (height > 0) {
            tag.put("height", height);
        }
    }

    @Override
    protected boolean shouldAddAntiCacheParameter() {
        return lastModified == null;
    }

    @Override
    protected void onDetach() {
        stream.detach();
        super.onDetach();
    }

}
