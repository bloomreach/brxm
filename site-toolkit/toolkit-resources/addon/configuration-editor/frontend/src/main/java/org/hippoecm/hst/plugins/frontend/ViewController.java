package org.hippoecm.hst.plugins.frontend;

import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ViewController implements IClusterable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ViewController.class);

    public static final String VIEWERS = "browser.viewers";
    public static final String MODEL_ROOT = "browser.root";
    public static final String MODEL = "browser.model";

    private String viewerName;
    private IClusterControl viewer;
    private JcrNodeModel rootModel;

    private IPluginContext context;
    private IPluginConfig config;

    protected ViewController(IPluginContext context, IPluginConfig config, JcrNodeModel rootModel) {
        this.config = config;
        this.context = context;

        if (config.containsKey(MODEL_ROOT)) {
            this.rootModel = new JcrNodeModel(config.getString(MODEL_ROOT));
        } else {
            this.rootModel = rootModel;
        }

        @SuppressWarnings("unchecked")
        IModelReference<JcrNodeModel> modelService = context.getService(config.getString(MODEL), IModelReference.class);
        if (modelService != null) {
            onModelChanged(modelService.getModel());
        } else {
            final ModelReference<JcrNodeModel> modelRef = new ModelReference<JcrNodeModel>(config.getString(MODEL),
                    this.rootModel);
            modelRef.init(context);
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return modelRef;
                }

                public void onEvent(Iterator<? extends IEvent> event) {
                    onModelChanged(modelRef.getModel());
                }

            }, IObserver.class.getName());
            onModelChanged(modelRef.getModel());
        }
    }

    protected void onModelChanged(JcrNodeModel model) {
        boolean handled = false;
        try {
            Node node = (model).getNode();
            Node root = rootModel.getNode();
            IPluginConfigService pluginConfig = context.getService(IPluginConfigService.class.getName(),
                    IPluginConfigService.class);
            while (node != null && !handled) {
                handled = handleNode(node, pluginConfig.listClusters(config.getString(VIEWERS)));
                if (!node.isSame(root)) {
                    node = node.getParent();
                } else {
                    break;
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        if (!handled) {
            resetViewer();
        }
    }

    protected boolean handleNode(Node node, List<String> viewers) throws RepositoryException {
        for (String type : viewers) {
            if (node.isNodeType(type)) {
                if (!type.equals(viewerName)) {
                    resetViewer();

                    IPluginConfigService pluginConfig = context.getService(IPluginConfigService.class.getName(),
                            IPluginConfigService.class);
                    IClusterConfig cluster = pluginConfig.getCluster(config.getString(VIEWERS) + "/" + type);
                    IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig("viewer.options"));
                    parameters.put("wicket.id", getExtensionPoint());
                    parameters.put(MODEL, config.getString(MODEL));
                    onViewInit(parameters);
                    viewer = context.newCluster(cluster, parameters);
                    viewer.start();
                    viewerName = type;
                    onViewStarted();
                }
                return true;
            }
        }
        return false;
    }

    protected void onViewInit(IPluginConfig parameters) {
    }

    protected void onViewStarted() {
    }

    private void resetViewer() {
        if (viewer != null) {
            viewer.stop();
            viewer = null;
            viewerName = null;
        }
    }

    protected abstract String getExtensionPoint();
}