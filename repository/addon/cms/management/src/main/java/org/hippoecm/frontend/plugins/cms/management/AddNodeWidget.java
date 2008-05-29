package org.hippoecm.frontend.plugins.cms.management;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoSession;

public abstract class AddNodeWidget extends AjaxEditableLabel {
    private static final long serialVersionUID = 1L;
    
    private final JcrNodeModel parentNodeModel;
    private String label;
    private String nodeType;
    
    public AddNodeWidget(String id, IModel model, JcrNodeModel parentNodeModel, String nodeType) {
        super(id, model);
        this.parentNodeModel = parentNodeModel;
        this.nodeType = nodeType;
    }
    
    @Override
    protected void onEdit(AjaxRequestTarget target) {
        label = (String)getModel().getObject();
        setModel(new Model(""));
        super.onEdit(target);
    }

    @Override
    protected void onCancel(AjaxRequestTarget target) {
        setModel(new Model(label));
        super.onCancel(target);
    }
    
    //TODO: implement decent exception handling with feedback panel and logger
    @Override
    protected void onSubmit(AjaxRequestTarget target) {
        super.onSubmit(target);
        
        try {
            String nodeName = (String) getModel().getObject();
            Node node = null;
            if(parentNodeModel.getNode().hasNode(nodeName)) {
                node = parentNodeModel.getNode().getNode(nodeName);
            } else {
                String path = parentNodeModel.getNode().getPath() + "/" + nodeName;
                String prefix = nodeType.substring(0, nodeType.indexOf((":")));
                String prototypePath = "/hippo:namespaces/" + prefix + "/" + nodeType + "/hippo:prototype/hippo:prototype";
                Node prototype = parentNodeModel.getNode().getSession().getRootNode().getNode(prototypePath.substring(1));
                node = ((HippoSession) parentNodeModel.getNode().getSession()).copy(prototype, path);
            }
            onAddNode(target, node);
        } catch (ItemExistsException e) {
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            e.printStackTrace();
        } catch (VersionException e) {
            e.printStackTrace();
        } catch (ConstraintViolationException e) {
            e.printStackTrace();
        } catch (LockException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

        setModel(new Model(label));
    }
    
    protected abstract void onAddNode(AjaxRequestTarget target, Node node);
}
