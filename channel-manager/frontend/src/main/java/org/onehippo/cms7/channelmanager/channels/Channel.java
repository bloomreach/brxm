package org.onehippo.cms7.channelmanager.channels;

import java.io.Serializable;

public class Channel implements Serializable {

    private String title;
    private String url; //Probably not needed for all channels ?
    private String type; //Channel type - preview/live.
    private String previewConfigPath; //preview hst config node path
    private String liveConfigPath; //live hst config node path


    public Channel(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPreviewConfigPath() {
        return previewConfigPath;
    }

    public void setPreviewConfigPath(String previewConfigPath) {
        this.previewConfigPath = previewConfigPath;
    }

    public String getLiveConfigPath() {
        return liveConfigPath;
    }

    public void setLiveConfigPath(String liveConfigPath) {
        this.liveConfigPath = liveConfigPath;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "type='" + type + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
