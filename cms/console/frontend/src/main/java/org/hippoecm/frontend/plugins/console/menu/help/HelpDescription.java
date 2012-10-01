package org.hippoecm.frontend.plugins.console.menu.help;

import org.apache.wicket.IClusterable;

/**
 * Help definition holder
 *
 * @version "$Id$"
 */
public class HelpDescription implements IClusterable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private String shortcutName;


    public HelpDescription(String name, String description, String shortcutName) {
        this.name = name;
        this.description = description;
        this.shortcutName = shortcutName;
    }

    public HelpDescription() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShortcutName() {
        return shortcutName;
    }

    public void setShortcutName(String shortcutName) {
        this.shortcutName = shortcutName;
    }
}
