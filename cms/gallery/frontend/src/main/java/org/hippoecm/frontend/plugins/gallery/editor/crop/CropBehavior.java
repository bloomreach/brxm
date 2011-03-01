package org.hippoecm.frontend.plugins.gallery.editor.crop;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.template.TextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.IAjaxSettings;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.javascript.AjaxSettings;
import org.hippoecm.frontend.plugins.yui.javascript.YuiType;
import org.onehippo.yui.YahooNamespace;
import org.onehippo.yui.YuiNamespace;

/**
 * Created by IntelliJ IDEA. User: mchatzidakis Date: 2/28/11 Time: 3:07 PM To change this template use File | Settings
 * | File Templates.
 */
public class CropBehavior extends AbstractYuiBehavior {

    private String regionInputId;
    private String imagePreviewContainerId;


    public CropBehavior(String regionInputId, String imagePreviewContainerId){
        this.regionInputId = regionInputId;
        this.imagePreviewContainerId = imagePreviewContainerId;
    }

    @Override
    public void bind(final Component component) {
        super.bind(component);
        component.setOutputMarkupId(true);
    }

    @Override
    public void addHeaderContribution(IYuiContext context)  {
        context.addModule(YahooNamespace.NS, "imagecropper");
        context.addOnDomLoad(new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                return getInitString();
            }
        });
        context.addCssReference(new ResourceReference(YahooNamespace.class, YahooNamespace.NS.getPath()+"imagecropper/assets/skins/sam/imagecropper-skin.css"));
        context.addCssReference(new ResourceReference(YahooNamespace.class, YahooNamespace.NS.getPath()+"resize/assets/skins/sam/resize-skin.css"));
    }

    private String getInitString() {

        // create image cropper instance
        return  "var imgCrop = new YAHOO.widget.ImageCropper('" + getComponent().getMarkupId() + "', {keyTick:5});" +
                "var imgpreviewdiv = YAHOO.util.Dom.get('"+ imagePreviewContainerId +"');" +
                "var imgpreview = YAHOO.util.Dom.getFirstChild(imgpreviewdiv);" +
                "var regionInput = YAHOO.util.Dom.get('"+ regionInputId +"');" +
                "imgCrop.on('moveEvent', function() { " +
                "   var region = imgCrop.getCropCoords(); " +
                "   regionInput.value = YAHOO.lang.JSON.stringify(region);" +
                "   imgpreview.style.top = '-' + region.top + 'px';" +
                "   imgpreview.style.left = '-' + region.left + 'px';" +
                "   imgpreviewdiv.style.height = region.height + 'px';" +
                "   imgpreviewdiv.style.width = region.width + 'px';" +
                "});";


        // listen to events
    }


}

