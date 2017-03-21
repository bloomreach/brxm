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
package org.hippoecm.frontend.plugins.standards.list.render;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconRenderer;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MimeTypeIconRenderer extends IconRenderer {

    static final Logger log = LoggerFactory.getLogger(MimeTypeIconRenderer.class);

    private static final Map<String, Icon> MIMETYPE_TO_ICON = new HashMap<>();

    static {
        //Microsoft Office
        //.doc
        MIMETYPE_TO_ICON.put("application/msword", Icon.MIMETYPE_DOC);

        //.xls
        MIMETYPE_TO_ICON.put("application/vnd.ms-excel", Icon.MIMETYPE_XLS);
        MIMETYPE_TO_ICON.put("application/excel", Icon.MIMETYPE_XLS);
        MIMETYPE_TO_ICON.put("application/x-excel", Icon.MIMETYPE_XLS);
        MIMETYPE_TO_ICON.put("application/x-msexcel", Icon.MIMETYPE_XLS);

        //.ppt
        MIMETYPE_TO_ICON.put("application/mspowerpoint", Icon.MIMETYPE_PPT);
        MIMETYPE_TO_ICON.put("application/powerpoint", Icon.MIMETYPE_PPT);
        MIMETYPE_TO_ICON.put("application/vnd.ms-powerpoint", Icon.MIMETYPE_PPT);
        MIMETYPE_TO_ICON.put("application/x-mspowerpoint", Icon.MIMETYPE_PPT);

        ///Microsoft Open XML
        //.docx
        MIMETYPE_TO_ICON.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                Icon.MIMETYPE_DOCX);

        //.xlsx
        MIMETYPE_TO_ICON.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                Icon.MIMETYPE_XLSX);

        //.pptx
        MIMETYPE_TO_ICON.put("application/vnd.openxmlformats-officedocument.presentationml.presentation",
                Icon.MIMETYPE_PPTX);

        //OpenOffice 1
        //.sxw
        MIMETYPE_TO_ICON.put("application/vnd.sun.xml.writer", Icon.MIMETYPE_SXW);

        //.sxc
        MIMETYPE_TO_ICON.put("application/vnd.sun.xml.calc", Icon.MIMETYPE_SXC);

        //.sxi
        MIMETYPE_TO_ICON.put("application/vnd.sun.xml.impress", Icon.MIMETYPE_SXI);

        //OpenOffice 2
        //*.odt
        MIMETYPE_TO_ICON.put("application/opendocument", Icon.MIMETYPE_ODT);
        MIMETYPE_TO_ICON.put("application/vnd.oasis.opendocument.text", Icon.MIMETYPE_ODT);

        //*.ods
        MIMETYPE_TO_ICON.put("application/vnd.oasis.opendocument.spreadsheet", Icon.MIMETYPE_ODS);

        //*.odp
        MIMETYPE_TO_ICON.put("application/vnd.oasis.opendocument.presentation", Icon.MIMETYPE_ODP);

        //General
        //image
        MIMETYPE_TO_ICON.put("image", Icon.MIMETYPE_IMAGE);

        //video
        MIMETYPE_TO_ICON.put("video", Icon.MIMETYPE_VIDEO);

        //audio
        MIMETYPE_TO_ICON.put("audio", Icon.MIMETYPE_AUDIO);

        //text
        MIMETYPE_TO_ICON.put("text", Icon.MIMETYPE_TEXT);

        //.pdf
        MIMETYPE_TO_ICON.put("application/pdf", Icon.MIMETYPE_PDF);

        //.swf
        MIMETYPE_TO_ICON.put("application/x-shockwave-flash", Icon.MIMETYPE_FLASH);

        //zips
        MIMETYPE_TO_ICON.put("application/zip", Icon.MIMETYPE_ZIP);
        MIMETYPE_TO_ICON.put("application/x-zip-compressed", Icon.MIMETYPE_ZIP);
        MIMETYPE_TO_ICON.put("application/x-compress", Icon.MIMETYPE_ZIP);
        MIMETYPE_TO_ICON.put("application/x-compressed", Icon.MIMETYPE_ZIP);
        MIMETYPE_TO_ICON.put("application/x-bzip", Icon.MIMETYPE_ZIP);
        MIMETYPE_TO_ICON.put("application/x-bzip2", Icon.MIMETYPE_ZIP);
        MIMETYPE_TO_ICON.put("application/x-gzip", Icon.MIMETYPE_ZIP);
        MIMETYPE_TO_ICON.put("multipart/x-zip", Icon.MIMETYPE_ZIP);

        //.rtf
        MIMETYPE_TO_ICON.put("application/rtf", Icon.MIMETYPE_RTF);

        //Octet stream
        MIMETYPE_TO_ICON.put("application/octet-stream", Icon.MIMETYPE_BINARY);
    }

    @Override
    protected HippoIcon getIcon(String id, Node node) throws RepositoryException {
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
                        final Icon iconType = mimetypeToIcon(mimeType);
                        if (iconType != null) {
                            return HippoIcon.fromSprite(id, iconType, IconSize.L);
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
        return super.getIcon(id, node);
    }

    //Mimetypes other than application/* are matched by category only (the part before the slash)
    private Icon mimetypeToIcon(String mimeType) {
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
