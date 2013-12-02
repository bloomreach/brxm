package org.onehippo.cms7.essentials.installer.panels;

import java.util.List;

import com.google.common.base.Strings;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.installer.HomePage;

/**
 * @author Jeroen Reijn
 */
public class MenuPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private Fragment nextFragment;
    private Fragment currentFragment;

    public MenuPanel(final String id, final HomePage dashboard, final List<Plugin> pluginList, final List<Plugin> mainPlugins) {

        super(id);
        final Fragment menuFragment = new MenuFragment("fragmentPanel", "menuFragment", this, dashboard, pluginList, mainPlugins);
        final Fragment buttonsFragment = new ButtonsFragment("fragmentPanel", "buttonsFragment", this, dashboard, pluginList, mainPlugins);
        nextFragment = menuFragment;
        currentFragment = buttonsFragment;

        add(buttonsFragment);
        setOutputMarkupId(true);



    }


    public void swapFragment(final AjaxRequestTarget target){
        Fragment temp = currentFragment;
        currentFragment.replaceWith(nextFragment);
        currentFragment = nextFragment;
        nextFragment = temp;
        target.add(currentFragment);

    }

}
