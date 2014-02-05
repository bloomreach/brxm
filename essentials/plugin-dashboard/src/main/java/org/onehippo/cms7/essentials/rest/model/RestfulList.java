package org.onehippo.cms7.essentials.rest.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.Restful;
import org.onehippo.cms7.essentials.rest.model.contentblocks.Compounds;
import org.onehippo.cms7.essentials.rest.model.contentblocks.DocumentType;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageProcessorRestful;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageSetRestful;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageVariantRestful;

import com.google.common.collect.Lists;

/**
 * @version "$Id: RestfulList.java 174870 2013-08-23 13:56:24Z mmilicevic $"
 */
@XmlRootElement(name = "collection")
public class RestfulList<T extends Restful> implements Serializable {

    private static final long serialVersionUID = 1L;
    private List<T> items = Lists.newArrayList();

    public void add(T resource) {
        items.add(resource);
    }

    public void addAll(Collection<T> items) {
        items.addAll(items);
    }

    public Iterator<T> iterator() {
        return items.iterator();
    }

    @XmlElementRefs({
            @XmlElementRef(type = PluginRestful.class),
            @XmlElementRef(type = PowerpackRestful.class),
            @XmlElementRef(type = VendorRestful.class),
            @XmlElementRef(type = StatusRestful.class),
            @XmlElementRef(type = MessageRestful.class),
            @XmlElementRef(type = ControllerRestful.class),
            /*@XmlElementRef(type = DocumentTypeRestful.class),*/
            @XmlElementRef(type = KeyValueRestful.class),
            @XmlElementRef(type = DocumentType.class),
            @XmlElementRef(type = ImageProcessorRestful.class),
            @XmlElementRef(type = ImageSetRestful.class),
            @XmlElementRef(type = ImageVariantRestful.class),
            @XmlElementRef(type = TranslationRestful.class),
            @XmlElementRef(type = PropertyRestful.class),
            @XmlElementRef(type = Compounds.class)
    })
    public List<T> getItems() {
        return items;
    }


}
