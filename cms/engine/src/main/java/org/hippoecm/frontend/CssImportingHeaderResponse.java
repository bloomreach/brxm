/*
 *  Copyright 2011 Hippo.
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

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.DecoratingHeaderResponse;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.util.lang.Objects;

public class CssImportingHeaderResponse extends DecoratingHeaderResponse {
    
    private List<Stylesheet> stylesheets = new ArrayList<Stylesheet>();
    
    public CssImportingHeaderResponse(IHeaderResponse real) {
        super(real);
    }

    @Override
    public void renderCSSReference(ResourceReference reference) {
        renderCSSReference(reference, null);
    }

    @Override
    public void renderCSSReference(ResourceReference reference, String media) {
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
        getResponse().println("<style type=\"text/css\" id=\"wicketimportstyle" + stylesheet.getId() + "\">");
        for (Import imp : stylesheet.getImports()) {
            renderImport(imp);
        }
        getResponse().println("</style>");
    }
    
    private void renderImport(Import imp) {
        getResponse().write("@import url('" + RequestCycle.get().urlFor(imp.getResourceReference()) + "')");
        if (imp.getMedia() != null) {
            getResponse().write(" " + imp.getMedia());
        }
        getResponse().println(";");
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
