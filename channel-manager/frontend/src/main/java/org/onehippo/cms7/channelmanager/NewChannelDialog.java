package org.onehippo.cms7.channelmanager;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class NewChannelDialog extends AbstractDialog {

    public NewChannelDialog(IPluginContext context, IPluginConfig config) {

    }

    @Override
    public IModel getTitle() {
        return new Model<String>("Create new Channel");
    }


    @Override
    public IValueMap getProperties() {
        return AbstractDialog.MEDIUM;
    }

    @Override
    protected void onOk() {
        super.onOk();
    }
}
