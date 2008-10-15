package org.hippoecm.frontend.editor.tagging;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.types.IFieldDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeDescriptor;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Displays the current assigned tags of the document 
 * @author jeroen
 *
 */
public class TagsPlugin extends RenderPlugin implements IJcrNodeModelListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TagsPlugin.class);

    public static String FIELD_NAME = "hippostd:tags";

    private ITemplateEngine engine;
    JcrNodeModel nodeModel;
    private String cols = "20";
    private String rows = "3";

    protected IFieldDescriptor field;

    public TagsPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, IJcrService.class.getName());

        engine = getPluginContext().getService(getPluginConfig().getString(ITemplateEngine.ENGINE),
                ITemplateEngine.class);

        nodeModel = (JcrNodeModel) getModel();
        
        try {
            nodeModel.getNode().getProperty(FIELD_NAME);
        } catch (PathNotFoundException e) {
            log.warn(FIELD_NAME + " does not exist, attempting to create it.");
            try{
                nodeModel.getNode().setProperty(TagsPlugin.FIELD_NAME, new String[0]);
            }catch (RepositoryException re){
                log.error("Creation of " + FIELD_NAME + " failed.", re);
            }
        } catch (RepositoryException e) {
            log.error("Repository error", e);
        }
        
        String mode = config.getString("mode");
        TagsModel tagModel = createTagModel(nodeModel);
        if (tagModel == null) {
            add(new Label("value", "TagModel not found"));
            log.error("TagModel can not be null.");
        } else if (ITemplateEngine.EDIT_MODE.equals(mode)) {
            if (config.getString("rows") != null) {
                rows = config.getString("rows");
            }
            if (config.getString("cols") != null) {
                cols = config.getString("cols");
            }
            add(createTextArea(tagModel));
        } else {
            add(new Label("value", tagModel));
        }
    }

    public void onFlush(JcrNodeModel nodeModel) {
        log.debug("Received flush");
        if (nodeModel.equals(this.nodeModel)) {
            TagsModel tagModel = createTagModel(nodeModel);
            replace(createTextArea(tagModel));
            redraw();
        }
    }

    public TextAreaWidget createTextArea(TagsModel tagModel) {
        TextAreaWidget widget = new TextAreaWidget("value", tagModel);
        widget.setRows(rows);
        widget.setCols(cols);
        return widget;
    }

    private TagsModel createTagModel(JcrNodeModel nodeModel) {
        ITypeDescriptor type = engine.getType(nodeModel);
        if (type == null) {
            log.error("Type not found.");
            return null;
        }
        JcrPropertyModel propertyModel = new JcrPropertyModel(nodeModel.getItemModel().getPath() + "/" + FIELD_NAME);
        TagsModel tagModel = new TagsModel(propertyModel);
        log.debug((String) tagModel.getObject());
        return tagModel;
    }

    

}
