package org.onehippo.cms7.essentials.rest.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.model.Restful;
import org.onehippo.cms7.essentials.dashboard.model.VendorRestful;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.rest.model.contentblocks.Compounds;
import org.onehippo.cms7.essentials.rest.model.contentblocks.DocumentType;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageProcessorRestful;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageSetRestful;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageVariantRestful;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "collection")
public class RestList<T extends Restful> extends RestfulList<T> {

    private static final long serialVersionUID = 1L;


    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(PluginRestful.class),
            @JsonSubTypes.Type(VendorRestful.class),
            @JsonSubTypes.Type(StatusRestful.class),
            @JsonSubTypes.Type(MessageRestful.class),
            @JsonSubTypes.Type(ControllerRestful.class),
            @JsonSubTypes.Type(KeyValueRestful.class),
            @JsonSubTypes.Type(PostPayloadRestful.class),
            @JsonSubTypes.Type(DocumentType.class),
            @JsonSubTypes.Type(ImageProcessorRestful.class),
            @JsonSubTypes.Type(ImageSetRestful.class),
            @JsonSubTypes.Type(ImageVariantRestful.class),
            @JsonSubTypes.Type(TranslationRestful.class),
            @JsonSubTypes.Type(PropertyRestful.class),
            @JsonSubTypes.Type(Compounds.class)
    })
    @Override
    public List<T> getItems() {
        return super.getItems();
    }


}
