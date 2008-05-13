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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.hippoecm.hst.core.Context;
import org.hippoecm.hst.core.HSTConfiguration;
import org.hippoecm.hst.core.URLPathTranslator;

/**
 * NewsTag showing news items from a certain location in the repository. 
 */
public class NewsTag extends SimpleTagSupport {
    
    public static final String PROPERTY_TYPE_LINK = "link";
    public static final String PROPERTY_TYPE_DATE = "date";
    public static final String PROPERTY_TYPE_STRING = "string";
    
    private static final String KEY_CONTEXT_NAME = "newstag.context.name";
    private static final String KEY_LOCATION = "newstag.location";
    private static final String KEY_MAX_ITEMS = "newstag.max.items";
    private static final String KEY_ITEM_PROPERTIES = "newstag.item.properties";
    private static final String KEY_ITEM_PROPERTY_TYPES = "newstag.item.property.types";
    private static final String KEY_FORMAT_DATE = "newstag.format.date";
    private static final String KEY_ITEM_VIEWFILE = "newstag.item.viewfile";

    private static final String DEFAULT_CONTEXT_NAME = "context";
    private static final String DEFAULT_LOCATION = "news";
    private static final Integer DEFAULT_MAX_ITEMS = new Integer(5);
    private static final String[] DEFAULT_ITEM_PROPERTIES = 
        new String[] {"defaultcontent:title",
                      "defaultcontent:date",
                      "defaultcontent:introduction"};
    private static final String[] DEFAULT_ITEM_PROPERTY_TYPES = 
        new String[] {PROPERTY_TYPE_LINK, PROPERTY_TYPE_DATE, PROPERTY_TYPE_STRING};
    private static final String DEFAULT_FORMAT_DATE = "MM/dd/yyyy"; 
    
    private String contextName = null;
    private String location = null;
    private Integer maxItems = null;
    private String[] itemProperties = null;
    private String[] itemPropertyTypes = null;
    private String dateFormat = null;
    private String itemViewFile = null;

    public void setContext(String contextName) {
        this.contextName = contextName;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
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
                
                String location = getLocation();
    
                Context newsContext;
                if ("".equals(location)) {
                    newsContext = context;
                }
                else {
                    Object obj = context.get(location);
                    if (!(obj instanceof Context)) {
                        throw new JspException("Object gotten from context " + context.getLocation()
                                + " and location " + location + " is not of type Context, it is " 
                                + ((obj == null) ? "null" : (obj.getClass().getName() + ", " + obj)));
                    }
                    newsContext = (Context) obj;
                }

                String itemViewFile = getItemViewFile();
                if (itemViewFile == null) {
                    doOutputDefault(newsContext, pageContext);
                }
                else {
                    doOutputByIncludes(newsContext, pageContext, itemViewFile);
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
        
        // first by setter
        if (this.contextName != null) {
            return this.contextName;    
        }

        // second by configuration
        if (this.contextName == null) {
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            this.contextName = HSTConfiguration.get(request.getSession().getServletContext(), 
                        KEY_CONTEXT_NAME, false/*not required*/);
        }
        
        // third by default
        if (this.contextName == null) {
            this.contextName = DEFAULT_CONTEXT_NAME;    
        }

        return this.contextName;
    }

    private String getLocation() {
        
        // first by setter
        if (this.location != null) {
            return this.location;    
        }

        // second by configuration
        if (this.location == null) {
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            this.location = HSTConfiguration.get(request.getSession().getServletContext(), 
                        KEY_LOCATION, false/*not required*/);
        }
        
        // third by default
        if (this.location == null) {
            this.location = DEFAULT_LOCATION;    
        }

        return this.location;
    }

    private Integer getMaxItems() {
        
        // first by setter
        if (this.maxItems != null) {
            return this.maxItems;    
        }

        // second by configuration
        if (this.maxItems == null) {
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            String maxItemsString = HSTConfiguration.get(request.getSession().getServletContext(), 
                        KEY_MAX_ITEMS, false/*not required*/);
            if (maxItemsString != null) {
                this.maxItems = new Integer(maxItemsString);
            }    
        }

        // third by default
        if (this.maxItems == null) {
            this.maxItems = DEFAULT_MAX_ITEMS;    
        }
        
        return this.maxItems;
    }
    
    private String[] getItemProperties() {
        
        // no setter but may have been set by configuration or default
        if (this.itemProperties != null) {
            return this.itemProperties;    
        }

        // first by configuration
        if (this.itemProperties == null) {
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            String itemPropertiesString = HSTConfiguration.get(request.getSession().getServletContext(), 
                        KEY_ITEM_PROPERTIES, false/*not required*/);
            if (itemPropertiesString != null) {
                this.itemProperties = itemPropertiesString.split(",");
            }    
        }

        // second by default
        if (this.itemProperties == null) {
            this.itemProperties = DEFAULT_ITEM_PROPERTIES;    
        }
        
        return this.itemProperties;
    }
    
    private String[] getItemPropertyTypes() {
        
        // no setter but may have been set by configuration or default
        if (this.itemPropertyTypes != null) {
            return this.itemPropertyTypes;    
        }

        // first by configuration
        if (this.itemPropertyTypes == null) {
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            String itemPropertyTypesString = HSTConfiguration.get(request.getSession().getServletContext(), 
                        KEY_ITEM_PROPERTY_TYPES, false/*not required*/);
            if (itemPropertyTypesString != null) {
                this.itemPropertyTypes = itemPropertyTypesString.split(",");
            }    
        }

        // second by default
        if (this.itemPropertyTypes == null) {
            this.itemPropertyTypes = DEFAULT_ITEM_PROPERTY_TYPES;    
        }
        
        return this.itemPropertyTypes;
    }
    
    private String getItemViewFile() {
        
        // may have been set by configuration
        if (this.itemViewFile != null) {
            return this.itemViewFile;
        }
        
        // by configuration only
        if (this.itemViewFile == null) {
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            String itemViewFile = HSTConfiguration.get(request.getSession().getServletContext(), 
                        KEY_ITEM_VIEWFILE, false/*not required*/);
            if (itemViewFile != null) {
                this.itemViewFile = itemViewFile;
            }    
        }

        // may return null, a hardcoded view will be output 
        return this.itemViewFile;
    }

    private String getDateFormat() {
       
       // may have been set by configuration or default
       if (this.dateFormat != null) {
           return this.dateFormat;
       }
       
       // first by configuration
       if (this.dateFormat == null) {
           HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
           String format = HSTConfiguration.get(request.getSession().getServletContext(), 
                       KEY_FORMAT_DATE, false/*not required*/);
           if (format != null) {
               this.dateFormat = format;
           }    
       }

       // second by default
       if (this.dateFormat == null) {
           this.dateFormat = DEFAULT_FORMAT_DATE;    
       }
       
       return this.dateFormat;
    }

    /** Output default HTML by configured or default properties */
    @SuppressWarnings("unchecked")
    private void doOutputDefault(Context context, PageContext pageContext) throws IOException, JspException {

        String[] itemProperties = getItemProperties();
        String[] itemPropertyTypes = getItemPropertyTypes();
        URLPathTranslator urlPathTranslator = null;
        SimpleDateFormat dateFormat = null;

        if (itemProperties.length != itemPropertyTypes.length) {
            throw new JspException("Number of item properties is different from the number " +
            		"of item property types: " + itemProperties + " and " + itemPropertyTypes);
        }    
        
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("<div class=\"hst-list\">\n");
        
        // loop subcontexts representing handle subnodes
        Iterator items = context.entrySet().iterator();
        
        int counter = 0;
        while (items.hasNext() && (counter < getMaxItems().intValue())) {
           Context itemHandle = (Context) items.next();
            
            // get first subnode of the handle
            Iterator handleIterator = itemHandle.entrySet().iterator();
            if (!handleIterator.hasNext()) {
                continue;
            }
            
            Context itemContext = (Context) handleIterator.next();

            buffer.append("  <div class=\"hst-list-item\">\n");
            
            for (int i = 0; i < itemProperties.length; i++) {
                
                Object property = itemContext.get(itemProperties[i]);
                String propertyType = itemPropertyTypes[i];
                
                if (property != null) {
                    if (PROPERTY_TYPE_LINK.equals(propertyType)) {
                        
                        // create lazily
                        if (urlPathTranslator == null) {
                            String contextPath = ((HttpServletRequest) pageContext.getRequest()).getContextPath();
                            urlPathTranslator = new URLPathTranslator(contextPath, context.getURLBasePath(), context.getBaseLocation());
                        }   

                        String translatedURL = urlPathTranslator.documentPathToURL(itemContext.getLocation());
                        
                        buffer.append("    <div class=\"hst-list-item-link\">");
                        buffer.append("<a href=\"").append(translatedURL).append("\">").append(property).append("</a>");
                        buffer.append("</div>\n");
                    }
                    else if (PROPERTY_TYPE_DATE.equals(propertyType)) {
                        
                        if (!(property instanceof Date)) {
                            throw new JspException("Property " + property + " is not of type Date, it is " 
                                + ((property == null) ? "null" : (property.getClass().getName() + ", " + property)));
                        }
                        
                        // create lazily
                        if (dateFormat == null) {
                            dateFormat = new SimpleDateFormat(getDateFormat());
                        }    
    
                        buffer.append("    <div class=\"hst-list-item-date\">");
                        buffer.append(dateFormat.format(property));
                        buffer.append("</div>\n");
                    }
                    else {
                        buffer.append("    <div class=\"hst-list-item-text\">");
                        buffer.append(property);
                        buffer.append("</div>\n");
                    }
                }    
            }
            
            buffer.append("  </div>\n");

            counter++;
        }

        buffer.append("</div>\n");
        
        pageContext.getOut().append(buffer);
    }

    /** Ouput items by including (jsp) files */
    @SuppressWarnings("unchecked")
    private void doOutputByIncludes(Context context, PageContext pageContext, String itemViewFile) 
                                throws JspException, ServletException, IOException {

        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

        // loop subcontexts representing subnodes
        Iterator items = context.entrySet().iterator();
        int counter = 0;
        while (items.hasNext() && (counter < getMaxItems().intValue())) {
            Context itemContext = (Context) items.next();
            
            request.setAttribute(getContextName(), itemContext);

            // do include
            RequestDispatcher dispatcher = request.getRequestDispatcher(itemViewFile);
            if (dispatcher == null) {
                throw new JspException("No dispatcher could be obtained for file " + itemViewFile);
            }
            dispatcher.include(request, response);
            
            counter++;
        }
    }
}
