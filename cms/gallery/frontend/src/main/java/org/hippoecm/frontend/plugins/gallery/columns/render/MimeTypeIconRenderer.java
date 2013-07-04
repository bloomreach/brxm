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
package org.hippoecm.frontend.plugins.gallery.columns.render;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconRenderer;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MimeTypeIconRenderer extends IconRenderer {

    static final Logger log = LoggerFactory.getLogger(MimeTypeIconRenderer.class);

    private static final Map<String, String> MIMETYPE_TO_ICON = new HashMap<String, String>();

    static {
        //Microsoft Office
        //.doc
        MIMETYPE_TO_ICON.put("application/msword", "res/mimetype-doc-16.png");

        //.xls
        MIMETYPE_TO_ICON.put("application/vnd.ms-excel", "res/mimetype-xls-16.png");
        MIMETYPE_TO_ICON.put("application/excel", "res/mimetype-xls-16.png");
        MIMETYPE_TO_ICON.put("application/x-excel", "res/mimetype-xls-16.png");
        MIMETYPE_TO_ICON.put("application/x-msexcel", "res/mimetype-xls-16.png");

        //.ppt
        MIMETYPE_TO_ICON.put("application/mspowerpoint", "res/mimetype-ppt-16.png");
        MIMETYPE_TO_ICON.put("application/powerpoint", "res/mimetype-ppt-16.png");
        MIMETYPE_TO_ICON.put("application/vnd.ms-powerpoint", "res/mimetype-ppt-16.png");
        MIMETYPE_TO_ICON.put("application/x-mspowerpoint", "res/mimetype-ppt-16.png");

        ///Microsoft Open XML
        //.docx
        MIMETYPE_TO_ICON.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "res/mimetype-docx-16.png");

        //.xlsx
        MIMETYPE_TO_ICON.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "res/mimetype-xlsx-16.png");

        //.pptx
        MIMETYPE_TO_ICON.put("application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "res/mimetype-pptx-16.png");

        //OpenOffice 1
        //.sxw
        MIMETYPE_TO_ICON.put("application/vnd.sun.xml.writer", "res/mimetype-sxw-16.png");

        //.sxc
        MIMETYPE_TO_ICON.put("application/vnd.sun.xml.calc", "res/mimetype-sxc-16.png");

        //.sxi
        MIMETYPE_TO_ICON.put("application/vnd.sun.xml.impress", "res/mimetype-sxi-16.png");

        //OpenOffice 2
        //*.odt
        MIMETYPE_TO_ICON.put("application/opendocument", "res/mimetype-odt-16.png");
        MIMETYPE_TO_ICON.put("application/vnd.oasis.opendocument.text", "res/mimetype-odt-16.png");

        //*.ods
        MIMETYPE_TO_ICON.put("application/vnd.oasis.opendocument.spreadsheet", "res/mimetype-ods-16.png");

        //*.odp
        MIMETYPE_TO_ICON.put("application/vnd.oasis.opendocument.presentation", "res/mimetype-odp-16.png");

        //General
        //image
        MIMETYPE_TO_ICON.put("image", "res/mimetype-image-16.png");

        //video
        MIMETYPE_TO_ICON.put("video", "res/mimetype-video-16.png");

        //audio
        MIMETYPE_TO_ICON.put("audio", "res/mimetype-audio-16.png");

        //text
        MIMETYPE_TO_ICON.put("text", "res/mimetype-text-16.png");

        //.pdf
        MIMETYPE_TO_ICON.put("application/pdf", "res/mimetype-pdf-16.png");

        //.swf
        MIMETYPE_TO_ICON.put("application/x-shockwave-flash", "res/mimetype-swf-16.png");

        //zips
        MIMETYPE_TO_ICON.put("application/zip", "res/mimetype-zip-16.png");
        MIMETYPE_TO_ICON.put("application/x-zip-compressed", "res/mimetype-zip-16.png");
        MIMETYPE_TO_ICON.put("application/x-compress", "res/mimetype-zip-16.png");
        MIMETYPE_TO_ICON.put("application/x-compressed", "res/mimetype-zip-16.png");
        MIMETYPE_TO_ICON.put("application/x-bzip", "res/mimetype-zip-16.png");
        MIMETYPE_TO_ICON.put("application/x-bzip2", "res/mimetype-zip-16.png");
        MIMETYPE_TO_ICON.put("application/x-gzip", "res/mimetype-zip-16.png");
        MIMETYPE_TO_ICON.put("multipart/x-zip", "res/mimetype-zip-16.png");

        //.rtf
        MIMETYPE_TO_ICON.put("application/rtf", "res/mimetype-rtf-16.png");

        //Octet stream
        MIMETYPE_TO_ICON.put("application/octet-stream", "res/mimetype-binary-16.png");
    }

    @Override
    protected ResourceReference getResourceReference(Node node) throws RepositoryException {
        try {
            if (node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName())) {
                Node imageSet = node.getNode(node.getName());
                try {
                    Item primItem = JcrHelper.getPrimaryItem(imageSet);
                    if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                        if (!((Node) primItem).hasProperty("jcr:mimeType")) {
                            log.warn("Unset mime type of document");
                            return null;
                        }
                        String mimeType = ((Node) primItem).getProperty("jcr:mimeType").getString();
                        String iconPath = mimetypeToPath(mimeType);
                        if (iconPath != null) {
                            Session session = Session.get();
                            return new PackageResourceReference(MimeTypeIconRenderer.class,
                                    iconPath,
                                    session.getLocale(),
                                    session.getStyle(),
                                    null);
                        }
                    } else {
                        log.warn("primary item of image set must be of type "
                                + HippoNodeType.NT_RESOURCE);
                    }
                } catch (ItemNotFoundException e) {
                    log.warn("ImageSet must have a primary item. " + node.getPath()
                            + " probably not of correct image set type");
                }
            }
        } catch (RepositoryException ex) {
            log.error("Unable to determine mime type of document", ex);
        }
        return super.getResourceReference(node);
    }

    //Mimetypes other than application/* are matched by category only (the part before the slash)
    private String mimetypeToPath(String mimeType) {
        if (!mimeType.startsWith("application")) {
            mimeType = StringUtils.substringBefore(mimeType, "/");
        }
        mimeType = mimeType.toLowerCase();
        if (MIMETYPE_TO_ICON.containsKey(mimeType)) {
            return MIMETYPE_TO_ICON.get(mimeType);
        }
        return null;
    }

}
