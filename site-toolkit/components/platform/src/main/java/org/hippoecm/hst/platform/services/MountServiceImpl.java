package org.hippoecm.hst.platform.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.internal.PreviewDecorator;
import org.hippoecm.hst.platform.api.MountService;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistryImpl;

public class MountServiceImpl implements MountService {

    private final HstModelRegistryImpl hstModelRegistry;
    private final PreviewDecorator previewDecorator;

    public MountServiceImpl(final HstModelRegistryImpl hstModelRegistry, final PreviewDecorator previewDecorator) {
        this.hstModelRegistry = hstModelRegistry;
        this.previewDecorator = previewDecorator;
    }

    @Override
    public Map<String, Mount> getLiveMounts(final String hostGroup) {
        return getMounts(hostGroup, false);
    }

    @Override
    public Map<String, Mount> getPreviewMounts(final String hostGroup) {
        return getMounts(hostGroup, true);
    }

    private Map<String, Mount> getMounts(final String hostGroup, final boolean preview) {
        final Map<String, Mount> mounts = new HashMap<>();

        if (hostGroup == null) {
            throw new IllegalArgumentException("Host group is not allowed to be null");
        }

        for (HstModel hstModel : hstModelRegistry.getModels().values()) {

            final VirtualHosts virtualHosts = hstModel.getVirtualHosts();

            final List<Mount> mountsByHostGroup = virtualHosts.getMountsByHostGroup(hostGroup);
            if (preview) {
                mountsByHostGroup.stream().forEach(mount -> mounts.put(mount.getIdentifier(), previewDecorator.decorateMountAsPreview(mount)));
            } else {
                mountsByHostGroup.stream().forEach(mount -> mounts.put(mount.getIdentifier(), mount));
            }
        }
        return mounts;
    }
}
