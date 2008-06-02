/*
 * Copyright 2008 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.taglib;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.hippoecm.hst.components.SiteMap;
import org.hippoecm.hst.components.SiteMapItem;
import org.hippoecm.hst.core.Context;
import org.hippoecm.hst.core.HSTConfiguration;
import org.hippoecm.hst.core.URLPathTranslator;

/**
 * Site map tag showing a dynamic site map from a certain location in the 
 * repository. 
 */
public class SiteMapTag extends SimpleTagSupport {
    
    private static final String DEFAULT_CONTEXT_NAME = "context";
    private static final String DEFAULT_LOCATION = "/";
    private static final String DEFAULT_ID = "hst-sitemap";
    private static final Integer DEFAULT_LEVEL = new Integer(0);
    private static final Integer DEFAULT_DEPTH = new Integer(2);
    private static final String[] DEFAULT_DOCUMENT_EXCLUDE_NAMES = new String[]{"index"};
    
    private final String KEY_CONTEXT_NAME = "sitemaptag.context.name";
    private final String KEY_LOCATION = "sitemaptag.location";
    private final String KEY_VIEWFILE = "sitemaptag.viewfile";
    private final String KEY_DOCUMENT_LABEL_PROPERTIES = "sitemaptag.document.label.properties";
    private final String KEY_DOCUMENT_EXCLUDE_NAMES = "sitemaptag.document.exclude.names";

    private String contextName = null;
    private String location = null;
    private String viewFile = null;
    private String id = null;
    private Integer level = null;
    private Integer depth = null;
    private String[] documentLabelProperties = null;
    private String[] documentExcludeNames = null;

    private URLPathTranslator urlPathTranslator;

    /** Setter for the tag attribute 'context'. */
    public void setContext(String contextName) {
        this.contextName = contextName;
    }

    /** Setter for the tag attribute 'location'. */
    public void setLocation(String location) {
        this.location = location;
    }

    /** Setter for the tag attribute 'id'. */
    public void setId(String id) {
        this.id = id;
    }

    /** Setter for the tag attribute 'level'. */
    public void setLevel(Integer level) {
        this.level = level;
    }

    /** Setter for the tag attribute 'depth'. */
    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    // javadoc from super
    public void doTag() throws JspException {
    
        PageContext pageContext = (PageContext) this.getJspContext(); 
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        String contextName = getContextName(); 
        Context context = (Context) request.getAttribute(contextName);
        
        if (context == null) {
            throw new JspException("No context found in request by attribute name '" + contextName + "'.");
        }
        
        try {
            pageContext.getOut().flush();
            
            String relativeLocation = getLocation();

            // starting with /: relative to the base location, else relative to 
            // the complete location
            String location;
            if (relativeLocation.startsWith("/")) {
                location = context.getBaseLocation() + relativeLocation; 
            }
            else {
                String loc = context.getLocation();
                location = loc.endsWith("/") ? loc + relativeLocation 
                                            : loc + "/" + relativeLocation;
            }
 
            SiteMap siteMap = SiteMap.getSiteMap(request.getSession(), location, 
                    getDocumentExcludeNames(), getDocumentLabelProperties());
            
            String viewFile = getViewFile();
            if (viewFile == null) {
                doOutputDefault(siteMap, pageContext);
            }
            else {
                doOutputByInclude(siteMap, pageContext, viewFile);
            }
        } 
        catch (ServletException ex) {
            throw new JspException(ex);
        } catch (IOException ex) {
            throw new JspException(ex);
        } finally {
            // always reset the context as the include type output may have overwritten it
            request.setAttribute(contextName, context);
        }
    }
 
    private String getContextName() {
        
        // lazy, or set by setter (first)
        if (this.contextName == null) {

            // second by configuration
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            this.contextName = HSTConfiguration.get(request.getSession().getServletContext(), 
                        KEY_CONTEXT_NAME, false/*not required*/);
        
            // third by default
            if (this.contextName == null) {
                this.contextName = DEFAULT_CONTEXT_NAME;    
            }
        }
        
        return this.contextName;
    }

    private String getId() {
        
        // lazy, or set by setter (first)
        if (this.id == null) {

            // second by default
            if (this.id == null) {
                this.id = DEFAULT_ID;
            }    
        }

        return this.id;
    }

    private String getLocation() {
        
        // lazy, or set by setter (first)
        if (this.location == null) {

            // second by configuration
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            this.location = HSTConfiguration.get(request.getSession().getServletContext(), 
                        KEY_LOCATION, false/*not required*/);
        
            // third by default
            if (this.location == null) {
                this.location = DEFAULT_LOCATION;
            }    
        }

        return this.location;
    }

    private Integer getLevel() {
        
        // lazy, or set by setter (first)
        if (this.level == null) {

            // second by default
            this.level = DEFAULT_LEVEL;
        }
        
        return this.level;
    }
    
    private String getViewFile() {
        
        // lazy (no setter)
        if (this.viewFile == null) {
        
            // by configuration only
            if (this.viewFile == null) {
                HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
    
                // first try key postfixed by .id
                String viewFile = HSTConfiguration.get(request.getSession().getServletContext(), 
                            KEY_VIEWFILE + "." + getId(), false/*not required*/);
                
                // then by general key
                if (viewFile == null) {
                    viewFile = HSTConfiguration.get(request.getSession().getServletContext(), 
                            KEY_VIEWFILE, false/*not required*/);
                }
                
                if (viewFile != null) {
                    this.viewFile = viewFile;
                }
            }    
        }
    
        // may return null, the hardcoded view will be output 
        return this.viewFile;
    }

    private Integer getDepth() {
        
        // lazy, or set by setter (first)
        if (this.depth == null) {

            // second by default
            this.depth = DEFAULT_DEPTH;
        }
        
        return this.depth;
    }
    
   
    /**
     * Get optional names for documents to exclude as site map items.
     */
    private String[] getDocumentExcludeNames() {
        
        // lazy (no setter)
        if (this.documentExcludeNames  == null) {
        
            // by configuration 
            if (this.documentExcludeNames  == null) {
                HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
    
                if (documentExcludeNames  == null) {
                    String excludeNames = HSTConfiguration.get(request.getSession().getServletContext(), 
                            KEY_DOCUMENT_EXCLUDE_NAMES, false/*not required*/);
    
                    if (excludeNames != null) {
                        this.documentExcludeNames = excludeNames.split(",");
                    }
                }
            }    

            // by default 
            if (this.documentExcludeNames  == null) {
                this.documentExcludeNames = DEFAULT_DOCUMENT_EXCLUDE_NAMES;
            }
        }
    
        return this.documentExcludeNames;
    }

    /**
     * Get optional properties for documents to set as label for site map items.
     */
    private String[] getDocumentLabelProperties() {
        
        // lazy (no setter)
        if (this.documentLabelProperties == null) {
        
            // by configuration only
            if (this.documentLabelProperties == null) {
                HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();

                if (documentLabelProperties == null) {
                    String labelProperties = HSTConfiguration.get(request.getSession().getServletContext(), 
                            KEY_DOCUMENT_LABEL_PROPERTIES, false/*not required*/);

                    if (labelProperties != null) {
                        this.documentLabelProperties = labelProperties.split(",");
                    }
                }
            }    
        }

        // may return null 
        return this.documentLabelProperties;
    }

    private URLPathTranslator getURLPathTranslator(HttpServletRequest request) {
        
        // lazy
        if (urlPathTranslator == null) {
            Context context = (Context) request.getAttribute(getContextName());
            urlPathTranslator = new URLPathTranslator(request.getContextPath(), 
                        context.getURLBasePath(), context.getBaseLocation());
        }

        return urlPathTranslator;
    }

    /** Output default HTML by configured or default properties */
    private void doOutputDefault(SiteMap siteMap, PageContext pageContext) throws IOException, JspException {
        
        StringBuffer buffer = new StringBuffer();

        writeDefaultOutput(buffer, siteMap.getDocuments(), siteMap.getFolders(), 0/*currentLevel*/, pageContext);

        pageContext.getOut().append(buffer);
    }

    /** Output by including (jsp) file */
    private void doOutputByInclude(SiteMap siteMap, PageContext pageContext, String viewFile) 
                                throws JspException, ServletException, IOException {

        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

        // do include
        RequestDispatcher dispatcher = request.getRequestDispatcher(viewFile);
        if (dispatcher == null) {
            throw new JspException("No dispatcher could be obtained for file " + viewFile);
        }
        
        // set site map under id in the request for easy access, plus other tag 
        // attributes 
        request.setAttribute(getId(), siteMap);
        request.setAttribute(getId() + "-level", getLevel());
        request.setAttribute(getId() + "-depth", getDepth());
        
        dispatcher.include(request, response);
    }

    private String encodePath(String path, HttpServletRequest request, HttpServletResponse response) {
        
        // normally, use encodeURL as overridden by URLMappingContextFilter
        if (this.contextName == null) {
            return response.encodeURL(path);
        }
        
        // create a new translator from the given context  
        else {
            return getURLPathTranslator(request).documentPathToURL(path);
        }
    }

    private void writeDefaultOutput(StringBuffer buffer, List<SiteMapItem> documentItems, 
            List<SiteMapItem> folderItems, int currentLevel, PageContext pageContext) {
            
        // check end level
        int endLevel = getLevel() + getDepth() - 1;
        if (endLevel < currentLevel) {
            return;
        }

        // check (start) level
        if (getLevel() <= currentLevel) {
            
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

            boolean isFirst = getLevel() == currentLevel;
            
            // loop
            Iterator<SiteMapItem> documents = documentItems.iterator();
            Iterator<SiteMapItem> folders = folderItems.iterator();
            
            boolean doWriteDocuments = documents.hasNext(); 
            boolean doWriteFolders = folders.hasNext(); 
            
            if (doWriteDocuments || doWriteFolders) {

                if (isFirst) {
                    buffer.append("<div id=\"");
                    buffer.append(getId());
                    buffer.append("\">\n");
                }
                
                buffer.append("<ul class=\"level");
                buffer.append(currentLevel);
                buffer.append("\">\n");
                
                // loop folder items for output
                if (doWriteFolders) {
                    buffer.append("  <li class=\"folders\"><ul>\n");

                    while (folders.hasNext()) {
                        SiteMapItem folder = folders.next();
    
                        buffer.append("    <li class=\"folder\">");
                        buffer.append("<a title=\"");
                        buffer.append(folder.getLabel());
                        buffer.append("\" href=\"");
                        buffer.append(encodePath(folder.getPath(), request, response));
                        buffer.append("\">");
                        buffer.append(folder.getLabel());
                        buffer.append("</a>\n");
                    
                        // recursion!
                        writeDefaultOutput(buffer, folder.getDocuments(), folder.getFolders(), 
                                folder.getLevel() + 1, pageContext);
                    
                        buffer.append("    </li>\n");
                    }
    

                    buffer.append("  </ul></li>\n");
                }

                if (doWriteDocuments) {
                    buffer.append("  <li class=\"documents\"><ul>\n");
                
                    // loop document items for output
                    while (documents.hasNext()) {
                        SiteMapItem document = documents.next();
        
                        buffer.append("    <li class=\"document\">");
                        buffer.append("<a title=\"");
                        buffer.append(document.getLabel());
                        buffer.append("\" href=\"");
                        buffer.append(encodePath(document.getPath(), request, response));
                        buffer.append("\">");
                        buffer.append(document.getLabel());
                        buffer.append("</a>\n");
                    
                        // recursion!
                        writeDefaultOutput(buffer, document.getDocuments(), document.getFolders(), 
                                document.getLevel() + 1, pageContext);
                    
                        buffer.append("    </li>\n");
                    }

                    buffer.append("  </ul></li>\n");
                }

                buffer.append("</ul>\n");

                if (isFirst) {
                    buffer.append("</div>\n");
                }
            }
        }
        else {
            
            // not yet at start level so still loop items to find the right level
            Iterator<SiteMapItem> folders = folderItems.iterator();
            while (folders.hasNext()) {
                
                SiteMapItem folder = folders.next();
            
                // recursion!
                writeDefaultOutput(buffer, folder.getDocuments(), folder.getFolders(), 
                        folder.getLevel() + 1, pageContext);
            }

            Iterator<SiteMapItem> documents = documentItems.iterator();
            while (documents.hasNext()) {
                
                SiteMapItem document = documents.next();
            
                // recursion!
                writeDefaultOutput(buffer, document.getDocuments(), document.getFolders(), 
                        document.getLevel() + 1, pageContext);
            }

        }
    }
}
