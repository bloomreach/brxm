package org.hippoecm.hst.demo.channel;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
        @FieldGroup(
                titleKey = "fields.demochannel",
                value = { "exampleValue" }
        )
})
public interface DemoChannelInfo extends ChannelInfo {

    @Parameter(name = "exampleValue")
    String getExampleValue();

}
