/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.gallery.upgrade;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GalleryUpgrader18a implements UpdaterModule {

    static final Logger log = LoggerFactory.getLogger(GalleryUpgrader18a.class);

    @Override
    public void register(final UpdaterContext context) {
        context.registerName("v18-cms-gallery-updater");

        context.registerStartTag("v18-gallery-start");
        context.registerEndTag("v18-cms-gallery");

        reloadContentDefinitions(context);
        updateGalleryProcessor(context);
        
        reloadNamespace(context);
        visitImagetSets(context);
        visitImageGalleries(context);
        visitHtmlContent(context);
    }

    private void reloadNamespace(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippogallery", getClass()
                .getClassLoader().getResourceAsStream("hippogallery.cnd")));
    }

    private void visitHtmlContent(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippostd:html") {
            Pattern pattern = Pattern.compile("(<img[^>]*src=\"[^\"]*)/hippogallery:picture", Pattern.MULTILINE);
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                Property htmlProperty = node.getProperty("hippostd:content");
                Matcher matcher = pattern.matcher(htmlProperty.getString());
                StringBuffer sb = new StringBuffer();
                while(matcher.find()) {
                    matcher.appendReplacement(sb, "$1/hippogallery:original");
                }
                matcher.appendTail(sb);
                htmlProperty.setValue(new String(sb));
              }
        });
    }

    private void visitImageGalleries(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippogallery:stdImageGallery") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasProperty("hippostd:gallerytype")) {
                    Value[] values = getValues(node, "hippostd:gallerytype");
                    if (values.length > 0) {
                        for (int i = 0; i < values.length; i++) {
                            if ("hippogallery:exampleImageSet".equals(values[i].getString())) {
                                values[i] = node.getSession().getValueFactory().createValue("hippogallery:imageset");
                            }
                        }
                        node.setProperty("hippostd:gallerytype", values);
                    }
                }
            }
        });
    }

    private void visitImagetSets(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippogallery:exampleImageSet") {

            /**
             * Convert images from exampleImageSet to imageset

                [hippogallery:image] > hippo:resource
                - hippogallery:width (long)
                - hippogallery:height (long)

                [hippogallery:imageset] > hippo:document orderable
                - hippogallery:filename (string)
                - hippogallery:description (string)
                + hippogallery:thumbnail (hippogallery:image) = hippogallery:image primary mandatory autocreated
                + hippogallery:original (hippogallery:image) = hippogallery:image

                [hippogallery:exampleImageSet] > hippo:document
                + hippogallery:thumbnail (hippo:resource) = hippo:resource primary mandatory autocreated
                + hippogallery:picture (hippo:resource) = hippo:resource

             **/
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                context.setPrimaryNodeType(node, "hippogallery:imageset");

                Node parent = node.getParent();
                if (parent.isNodeType("hippo:handle") && parent.isNodeType("hippo:translated")) {
                    NodeIterator translations = parent.getNodes("hippo:translation");
                    if (translations.hasNext()) {
                        Node translation = translations.nextNode();
                        node.setProperty("hippogallery:filename", translation.getProperty("hippo:message").getString());
                    }
                }

                if (node.hasNode("hippogallery:picture")) {
                    Node picture = node.getNode("hippogallery:picture");
                    context.setPrimaryNodeType(picture, "hippogallery:image");
                    context.setName(picture, "hippogallery:original");
                    processImage(picture);
                }

                Node thumbnail = node.getNode("hippogallery:thumbnail");
                context.setPrimaryNodeType(thumbnail, "hippogallery:image");
                processImage(thumbnail);
            }

            private void processImage(Node picture) throws RepositoryException {
                ImageReader reader = getImageReader(picture.getProperty("jcr:mimeType").getString());
                if (reader != null) {
                    InputStream stream = picture.getProperty("jcr:data").getStream();
                    MemoryCacheImageInputStream mciis = new MemoryCacheImageInputStream(stream);
                    try {
                        reader.setInput(mciis);

                        picture.setProperty("hippogallery:width", reader.getWidth(0));
                        picture.setProperty("hippogallery:height", reader.getHeight(0));
                    } catch (IOException e) {
                        log.error("Could not retrieve image width and/or height", e);
                    } finally {
                        reader.dispose();
                        try {
                            mciis.close();
                        } catch (IOException e) {
                            log.error("error closing memory cached stream", e);
                        }
                        try {
                            stream.close();
                        } catch (IOException e) {
                            log.error("error closing image data stream", e);
                        }
                    }
                }
            }
        });
    }

    private void reloadContentDefinitions(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {
             @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                 if (node.hasNode("hippogallery-editor")) {
                     node.getNode("hippogallery-editor").remove();
                 }
                 if (node.hasNode("hippogallery-editor-fr")) {
                     node.getNode("hippogallery-editor-fr").remove();
                 }
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:namespaces/hippogallery") {
             @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                 node.remove();
            }
        });
    }

    private void updateGalleryProcessor(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                String plugin = node.getProperty("plugin.class").getString();
                if ("org.hippoecm.frontend.plugins.gallery.GalleryProcessorPlugin".equals(plugin)) {
                    node.setProperty("plugin.class", "org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessorPlugin");

                    Node thumbnailConfig = node.addNode("hippogallery:thumbnail", "frontend:pluginconfig");
                    thumbnailConfig.setProperty("height", 60);
                    thumbnailConfig.setProperty("width", 60);
                    thumbnailConfig.setProperty("upscaling", false);

                    Node originalConfig = node.addNode("hippogallery:original", "frontend:pluginconfig");
                    originalConfig.setProperty("height", 0);
                    originalConfig.setProperty("width", 0);
                    originalConfig.setProperty("upscaling", false);
                }
            }
        });
    }

    private ImageReader getImageReader(String mimeType) {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(mimeType);
        if (readers == null || !readers.hasNext()) {
            log.warn("Unsupported mimetype, cannot read: {}", mimeType);
            return null;
        }
        return readers.next();
    }

    private static Value[] getValues(Node node, String name) throws RepositoryException, ValueFormatException {
        if (node.hasProperty(name)) {
            Property categoryProp = node.getProperty(name);
            Value[] categories;
            if (categoryProp.isMultiple()) {
                categories = categoryProp.getValues();
            } else {
                categories = new Value[] { categoryProp.getValue() };
            }
            return categories;
        } else {
            return new Value[0];
        }
    }
    
}
