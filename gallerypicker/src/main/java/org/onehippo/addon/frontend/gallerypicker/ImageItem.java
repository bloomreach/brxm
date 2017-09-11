/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.addon.frontend.gallerypicker;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class management by the factory ImageItemFactory. You should not instantiate this class.
 * <p/>
 * The ImageItem is used to obtain the location of an image in the repository, it also gives the url of the
 * default image if the provided uuid is not correct or does not exist.
 *
 * @author Jeroen Reijn
 */
public class ImageItem implements IClusterable {

    private static final Logger log = LoggerFactory.getLogger(ImageItem.class);

    public static final String BASE_PATH_BINARIES = "binaries";
    public static final String BASE_IMAGES_PATH = "gallery";
    

    private String uuid;

    ImageItem() {
        this(null);
    }

    ImageItem(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    /**
     * Obtains the primary url of a node identified by it's uuid. If the uuid was not provided,
     * or if something goes horribly wrong, the default image path is returned.
     * <p/>
     * Beware that no check is done if the uuid actually belongs to an image node. Also is assumed
     * that the uuid is of a node of type handle.
     *
     * @return String containing the url of the requested image, if no image is specified return an empty String.
     */
    public String getPrimaryUrl() {
        if (StringUtils.isNotEmpty(uuid)) {
            Node handle;
            Item item;
            String path = "";
            try {
                handle = obtainJcrSession().getNodeByIdentifier(uuid);
                path = handle.getName();
                if (!"".equals(path) && !"/".equals(path) && !BASE_IMAGES_PATH.equals(path)) {
                    Node document = handle.getNode(path);
                    item = JcrHelper.getPrimaryItem(document);
                    return BASE_PATH_BINARIES + item.getPath();
                }
            } catch (ItemNotFoundException e) {
                log.warn("Unable to find item: {} : {} ", uuid, e);
            } catch (PathNotFoundException e) {
                log.warn("Uuid: {} exists, but path '{}' cannot be found", new Object[]{uuid, path}, e);
            } catch (RepositoryException e) {
                log.warn("Error while trying to get primary url for: {}", uuid, e);
            }
        }
        return "";
    }

    public boolean isValid() {
        return uuid != null;
    }

    /**
     * Utitlity method that returns the JCrSessionFrom the wicket session
     *
     * @return javax.jcr.Session session object to be used to interact with the repository
     */
    protected javax.jcr.Session obtainJcrSession() {
        return ((UserSession) Session.get()).getJcrSession();
    }

}