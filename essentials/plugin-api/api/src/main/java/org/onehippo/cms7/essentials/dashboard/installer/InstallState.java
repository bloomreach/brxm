package org.onehippo.cms7.essentials.dashboard.installer;

import java.io.Serializable;

/**
 * Enum for install states. It maps to the String Resource keys in the InstallablePlugin.properties file.
 */
public enum InstallState implements Serializable {
    UNINSTALLED("uninstalled"), INSTALLED("installed"), INSTALLED_AND_RESTARTED("installed_and_restarted");
    private String state;

    InstallState(String state) {
        this.state = state;
    }

    public String getMessage() {
        return state;
    }
}
