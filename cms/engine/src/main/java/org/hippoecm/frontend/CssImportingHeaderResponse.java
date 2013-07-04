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
package org.hippoecm.frontend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.DecoratingHeaderResponse;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A header response that doesn't immediately render stylesheet references but
 * collects them until close and just before closing creates a <code>&lt;style&gt;</code> tag in the head
 * with <code>@import</code> statements for the individual css references.
 * </p>
 * <p>
 * This is a work-around for the limitation in Internet Explorer that doesn't allow more than 31 stylesheet
 * objects per document.
 * </p>
 * <p>
 * Note: Internet Explorer does not support inline definition of a stylesheet's media type. Since we're only 
 * interested in media="screen", we've put the media attribute on the style element and skip every stylesheet that 
 * explicitly defines a different media type.
 * </p>
 */
public class CssImportingHeaderResponse extends DecoratingHeaderResponse {
    
    static final Logger log = LoggerFactory.getLogger(CssImportingHeaderResponse.class);
    
    private List<Stylesheet> stylesheets = new ArrayList<Stylesheet>();
    
    public CssImportingHeaderResponse(IHeaderResponse real) {
        super(real);
    }

    @Override
    public void render(final HeaderItem item) {
        if (item instanceof CssReferenceHeaderItem) {
            CssReferenceHeaderItem cssHeaderItem = (CssReferenceHeaderItem) item;
            renderCSSReference(cssHeaderItem.getReference(), cssHeaderItem.getMedia());
        } else {
            super.render(item);
        }
    }

    private void renderCSSReference(ResourceReference reference, String media) {
        addImport(new Import(reference, media), 0);
    }

    private void addImport(Import imp, int styleSheetId) {
        Stylesheet stylesheet = null;
        if (stylesheets.size() > styleSheetId) {
            stylesheet = stylesheets.get(styleSheetId);
            if (stylesheet.containsImport(imp)) {
                return;
            }
            if (stylesheet.isFull()) {
                addImport(imp, styleSheetId+1);
                return;
            }
        }
        if (stylesheet == null) {
            stylesheets.add(stylesheet = new Stylesheet(styleSheetId));
        }
        stylesheet.addImport(imp);
    }
    
    @Override
    public void close() {
        for (Stylesheet stylesheet : stylesheets) {
            renderStylesheet(stylesheet);
        }
        super.close();
    }

    private void renderStylesheet(Stylesheet stylesheet) {
        getResponse().write("<style type=\"text/css\" id=\"wicketimportstyle" + stylesheet.getId() + "\" media=\"screen\">");
        for (Import imp : stylesheet.getImports()) {
            renderImport(imp);
        }
        getResponse().write("</style>");
    }

    private void renderImport(Import imp) {
        CharSequence cssUrl = RequestCycle.get().urlFor(imp.getResourceReference(), null);
        getResponse().write("@import url('" + cssUrl + "');");

        String media = imp.getMedia();
        if(media != null && !media.equals("screen") && Application.get().getConfigurationType().equals(RuntimeConfigurationType.DEVELOPMENT)) {
            log.warn("CssImportingHeaderResponse only accepts stylesheets of @media='screen', css file {} will be skipped.", cssUrl);
        }
    }
    
    private static class Stylesheet {
        private final Collection<Import> imports = new LinkedHashSet<Import>(31);
        private final int id;
        
        private Stylesheet(int id) {
            this.id = id;
        }
        
        private int getId() {
            return id;
        }
        
        private Collection<Import> getImports() {
            return imports;
        }
        
        private boolean isFull() {
            return imports.size() > 30;
        }
        
        private void addImport(Import imp) {
            if (isFull()) {
                throw new IndexOutOfBoundsException("No more than 31 imports per stylesheet are supported by IE");
            }
            imports.add(imp);
        }
        
        private boolean containsImport(Import imp) {
            return imports.contains(imp);
        }
    }
    
    private static class Import {
        private final ResourceReference reference;
        private final String media;
        
        private Import(ResourceReference reference, String media) {
            this.reference = reference;
            this.media = media;
        }
        
        private ResourceReference getResourceReference() {
            return reference;
        }
        
        private String getMedia() {
            return media;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Import) {
                Import that = ((Import) obj);
                return Objects.equal(media, that.media) && Objects.equal(reference, that.reference);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            int result = 17;
            result = 37 * result + (media != null ? media.hashCode() : 0);
            result = 37 * result + reference.hashCode();
            return result;
        }
    }

}
