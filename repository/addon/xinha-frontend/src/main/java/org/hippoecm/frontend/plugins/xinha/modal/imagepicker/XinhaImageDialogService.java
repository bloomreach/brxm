package org.hippoecm.frontend.plugins.xinha.modal.imagepicker;

import java.util.EnumMap;
import java.util.Map;

import org.hippoecm.frontend.plugins.xinha.modal.XinhaDialogService;

public class XinhaImageDialogService extends XinhaDialogService<XinhaImage> {
    private static final long serialVersionUID = 1L;
    
    @Override
    protected EnumMap<XinhaImage, String> createEnumMap() {
        return new EnumMap<XinhaImage, String>(XinhaImage.class);
    }

    @Override
    protected String getXinhaParameterName(XinhaImage k) {
        return k.getValue();
    }

    @Override
    public void update(Map<String, String> parameters) {
        for(XinhaImage xi : XinhaImage.values()) {
            values.put(xi, parameters.get(xi.getValue()));
        }
    }

}
