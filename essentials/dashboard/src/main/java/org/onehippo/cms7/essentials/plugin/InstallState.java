package org.onehippo.cms7.essentials.plugin;

public enum InstallState {
    DISCOVERED("discovered"),
    BOARDING("boarding"),
    ONBOARD("onBoard"),
    INSTALLING("installing"),
    INSTALLED("installed");

    private final String name;

    InstallState(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static InstallState fromString(final String name) {
        for (InstallState s : InstallState.values()) {
            if (s.name.equals(name)) {
                return s;
            }
        }
        return null;
    }
}
