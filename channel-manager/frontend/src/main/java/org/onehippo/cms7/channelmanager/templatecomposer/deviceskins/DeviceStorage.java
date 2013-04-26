package org.onehippo.cms7.channelmanager.templatecomposer.deviceskins;

import java.util.List;

import org.wicketstuff.js.ext.data.ExtStore;

/**
 * @version "$Id$"
 */
public interface DeviceStorage {

    /**
     * Important overwrite to retrieve the store needed by the DeviceManager. Every Device Service need to have at least
     * this method to populate the extjscombobox appropriately.
     *
     * @return the extstore used for the device manager combobox
     */
    public ExtStore<StyleableDevice> getStore();

    /**
     * @return List of Stylable objects used for the CSS util.
     */
    public List<StyleableDevice> getStylables();

}
