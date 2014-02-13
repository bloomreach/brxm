package org.onehippo.cms7.essentials.rest.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.Restful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
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
public class RestList<T extends Restful> extends RestfulList<T>{

    private static final long serialVersionUID = 1L;



    @XmlElementRefs({
            @XmlElementRef(type = PluginRestful.class),
            @XmlElementRef(type = PowerpackRestful.class),
            @XmlElementRef(type = VendorRestful.class),
            @XmlElementRef(type = StatusRestful.class),
            @XmlElementRef(type = MessageRestful.class),
            @XmlElementRef(type = ControllerRestful.class),
            @XmlElementRef(type = KeyValueRestful.class),
            @XmlElementRef(type = DocumentType.class),
            @XmlElementRef(type = ImageProcessorRestful.class),
            @XmlElementRef(type = ImageSetRestful.class),
            @XmlElementRef(type = ImageVariantRestful.class),
            @XmlElementRef(type = TranslationRestful.class),
            @XmlElementRef(type = PropertyRestful.class),
            @XmlElementRef(type = Compounds.class)
    })
    @Override
    public List<T> getItems() {
        return items;
    }


}
