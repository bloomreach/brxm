/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource.jdom;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jdom2.xpath.XPathHelper;
import org.onehippo.cms7.crisp.api.resource.AbstractResource;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceCollection;
import org.onehippo.cms7.crisp.api.resource.ValueMap;
import org.onehippo.cms7.crisp.core.resource.DefaultValueMap;
import org.onehippo.cms7.crisp.core.resource.EmptyValueMap;
import org.onehippo.cms7.crisp.core.resource.ListResourceCollection;
import org.onehippo.cms7.crisp.core.resource.util.ResourceCollectionUtils;

public class JdomResource extends AbstractResource {

    private static final long serialVersionUID = 1L;

    private Element jdomElem;
    private ValueMap internalValueMap;
    private List<Resource> internalAllChildren;

    public JdomResource(Element jdomElem) {
        super(jdomElem.getCType().toString());
        this.jdomElem = jdomElem;
    }

    public JdomResource(Element jdomElem, String name) {
        super(jdomElem.getCType().toString(), name);
        this.jdomElem = jdomElem;
    }

    public JdomResource(Resource parent, Element jdomElem, String name) {
        super(parent, jdomElem.getCType().toString(), name);
        this.jdomElem = jdomElem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() {
        return XPathHelper.getAbsolutePath(jdomElem);
    }

    @Override
    public Object getValue(String relPath) {
        XPathFactory xFactory = XPathFactory.instance();
        XPathExpression<Object> expr = xFactory.compile(relPath);
        List<Object> values = expr.evaluate(jdomElem);

        if (values == null || values.isEmpty()) {
            return null;
        }

        Object value = values.get(0);

        if (value instanceof Attribute) {
            return ((Attribute) value).getValue();
        } else if (value instanceof Content) {
            if (value instanceof Element) {
                final Element elem = (Element) value;
                if (jdomElem.equals(elem.getParent())) {
                    // if the resolved element is a child of this resource's element, then let's return the resolved
                    // child element to put the same parent in it.
                    for (Resource childRes : getInternalAllChildren()) {
                        if (elem.equals(((JdomResource) childRes).jdomElem)) {
                            return childRes;
                        }
                    }
                } else {
                    // if the resolved item is not a child, let's simply return a resource having no parent.
                    return new JdomResource(elem, elem.getName());
                }
            } else if (value instanceof Text) {
                return ((Text) value).getValue();
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getValue(String relPath, Class<T> type) {
        final Object value = getValue(relPath);

        if (value != null && type != null) {
            if (value instanceof String) {
                if (type == Integer.class) {
                    return (T) Integer.valueOf((String) value);
                } else if (type == Long.class) {
                    return (T) Long.valueOf((String) value);
                } else if (type == Double.class) {
                    return (T) Double.valueOf((String) value);
                } else if (type == Boolean.class) {
                    return (T) Boolean.valueOf((String) value);
                } else if (type == BigDecimal.class) {
                    return (T) new BigDecimal((String) value);
                }
            } else if (value instanceof Resource) {
                if (type == Integer.class) {
                    return (T) ((Resource) value).getValueMap().get("", Integer.class);
                } else if (type == Long.class) {
                    return (T) ((Resource) value).getValueMap().get("", Long.class);
                } else if (type == Double.class) {
                    return (T) ((Resource) value).getValueMap().get("", Double.class);
                } else if (type == Boolean.class) {
                    return (T) ((Resource) value).getValueMap().get("", Boolean.class);
                } else if (type == BigDecimal.class) {
                    return (T) ((Resource) value).getValueMap().get("", BigDecimal.class);
                } else if (type == String.class) {
                    return (T) ((Resource) value).getValueMap().get("", String.class);
                }
            }

            if (!type.isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException("The type doesn't match with the value type: " + value.getClass());
            }
        }

        return (T) value;
    }

    @Override
    public boolean isAnyChildContained() {
        return getChildCount() > 0;
    }

    @Override
    public long getChildCount() {
        final List<Resource> allChildren = getInternalAllChildren();
        return allChildren.size();
    }

    @Override
    public ResourceCollection getChildren(long offset, long limit) {
        final List<Resource> allChildren = getInternalAllChildren();
        return new ListResourceCollection(ResourceCollectionUtils.createSubList(allChildren, offset, limit));
    }

    @Override
    public ValueMap getMetadata() {
        return EmptyValueMap.getInstance();
    }

    @Override
    public ValueMap getValueMap() {
        return ((DefaultValueMap) getInternalValueMap()).toUnmodifiable();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof JdomResource)) {
            return false;
        }

        return Objects.equals(jdomElem, ((JdomResource) o).jdomElem);
    }

    @Override
    public int hashCode() {
        return jdomElem.hashCode();
    }

    public void write(XMLOutputter xmlOutputter, OutputStream out) throws IOException {
        xmlOutputter.output(jdomElem, out);
    }

    public void write(XMLOutputter xmlOutputter, Writer writer) throws IOException {
        xmlOutputter.output(jdomElem, writer);
    }

    Element getJdomElement() {
        return jdomElem;
    }

    private ValueMap getInternalValueMap() {
        if (internalValueMap == null) {
            ValueMap tempValueMap = new DefaultValueMap();

            String fieldName;
            Object fieldValue;

            for (Element childElem : jdomElem.getChildren()) {
                fieldName = childElem.getName();
                tempValueMap.put(fieldName, toChildFieldJacksonResource(childElem, fieldName));
            }

            for (Attribute attr : jdomElem.getAttributes()) {
                fieldName = attr.getName();
                fieldValue = attr.getValue();
                tempValueMap.put(fieldName, fieldValue);
            }

            tempValueMap.put("", jdomElem.getText());

            internalValueMap = tempValueMap;
        }

        return internalValueMap;
    }

    private List<Resource> getInternalAllChildren() {
        if (internalAllChildren == null) {
            List<Resource> list = new LinkedList<>();

            for (Element childElem : jdomElem.getChildren()) {
                list.add(toChildFieldJacksonResource(childElem, childElem.getName()));
            }

            internalAllChildren = list;
        }

        return internalAllChildren;
    }

    private JdomResource toChildFieldJacksonResource(Element element, String fieldName) {
        return new JdomResource(this, element, fieldName);
    }
}
