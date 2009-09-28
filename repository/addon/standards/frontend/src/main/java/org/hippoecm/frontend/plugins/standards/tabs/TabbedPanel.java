/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.standards.tabs;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.CancelEventIfNoAjaxDecorator;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.Loop.LoopItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;

public class TabbedPanel extends WebMarkupContainer {
	@SuppressWarnings("unused")
	private final static String SVN_ID = "$Id$";

	private static final long serialVersionUID = 1L;

	public static final String TAB_PANEL_ID = "panel";

	private final TabsPlugin plugin;

	private int maxTabLength = 12;
	private final List<TabsPlugin.Tab> tabs;
	private MarkupContainer panelContainer;
	private transient boolean redraw = false;

	public TabbedPanel(String id, TabsPlugin plugin, List<TabsPlugin.Tab> tabs) {
		super(id, new Model(Integer.valueOf(-1)));

		if (tabs == null) {
			throw new IllegalArgumentException("argument [tabs] cannot be null");
		}

		this.plugin = plugin;
		this.tabs = tabs;

		setOutputMarkupId(true);

		final IModel tabCount = new AbstractReadOnlyModel() {
			private static final long serialVersionUID = 1L;

			@Override
			public Object getObject() {
				return Integer.valueOf(TabbedPanel.this.tabs.size());
			}
		};

		WebMarkupContainer tabsContainer = new WebMarkupContainer(
				"tabs-container") {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				tag.put("class", "tab-row");
			}
		};
		tabsContainer.setOutputMarkupId(true);
		add(tabsContainer);

		// add the loop used to generate tab names
		tabsContainer.add(new Loop("tabs", tabCount) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(LoopItem item) {
				final int index = item.getIteration();

				final WebMarkupContainer titleLink = newLink(index);
				item.add(titleLink);
				item.add(newBehavior(index));
			}

			@Override
			protected LoopItem newItem(int iteration) {
				return newTabContainer(iteration);
			}

		});

		panelContainer = new WebMarkupContainer("panel-container");
		panelContainer.setOutputMarkupId(true);
		panelContainer.add(plugin.getEmptyPanel());
		add(panelContainer);
	}

	protected LoopItem newTabContainer(final int tabIndex) {
		LoopItem item = new LoopItem(tabIndex) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				String cssClass = (String) tag.getString("class");
				if (cssClass == null) {
					cssClass = " ";
				}
				cssClass += " tab" + getIteration();

				if (getIteration() == getSelectedTab()) {
					cssClass += " selected";
				}
				if (getIteration() == getTabs().size() - 1) {
					cssClass += " last";
				}
				tag.put("class", cssClass.trim());
			}
		};
		return item;
	}

	// used by superclass to add title to the container

	protected WebMarkupContainer newLink(final int index) {
		WebMarkupContainer container = new WebMarkupContainer("container",
				new Model(Integer.valueOf(index)));
		final TabsPlugin.Tab tabbie = (TabsPlugin.Tab) getTabs().get(index);
		if (tabbie.canClose()) {
			container.add(new AjaxFallbackLink("close") {
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target) {
					plugin.onClose(tabbie, target);
				}
			});
		} else {
			container.add(new Label("close").setVisible(false));
		}
		Component link;
		container.add(link = new AjaxFallbackLink("link") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				plugin.onSelect(tabbie, target);
			}
		}.add(new Label("title", new LoadableDetachableModel() {
			private static final long serialVersionUID = 1L;

			@Override
			protected Object load() {
				IModel titleModel = tabbie.getTitle();
				if (titleModel != null) {
					String title = (String) titleModel.getObject();
					if (title.length() > maxTabLength) {
						title = title.substring(0, maxTabLength - 2) + ".."; // leave
						// space
						// for
						// two
						// ..
						// then
						// add
						// them
					}
					return title;
				}
				return "title";
			}
		})));
		link.add(new AttributeAppender("title", tabbie.getTitle(), ""));

		return container;
	}

	protected IBehavior newBehavior(final int tabIndex) {
		return new AjaxEventBehavior("onclick") {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onEvent(AjaxRequestTarget target) {
				plugin.onSelect(tabs.get(tabIndex), target);
			}

			@Override
			protected IAjaxCallDecorator getAjaxCallDecorator() {
				return new CancelEventIfNoAjaxDecorator(null);
			}
		};
	}

	public void setMaxTitleLength(int maxTitleLength) {
		this.maxTabLength = maxTitleLength;
	}

	@Override
	public boolean isTransparentResolver() {
		return true;
	}

	public void redraw() {
		redraw = true;
	}

	public void render(PluginRequestTarget target) {
		if (redraw) {
			if (target != null) {
				target.addComponent(get("tabs-container"));
				target.addComponent(get("panel-container"));
			}
			redraw = false;
		}
	}

	// @see org.apache.wicket.Component#onAttach()
	@Override
	protected void onBeforeRender() {
		super.onBeforeRender();
		if (!hasBeenRendered() && getSelectedTab() == -1) {
			// select the first tab by default
			setSelectedTab(0);
		}
	}

	public final List<TabsPlugin.Tab> getTabs() {
		return tabs;
	}

	public void setSelectedTab(int index) {
		if (index < 0 || index >= tabs.size()) {
			panelContainer.replace(plugin.getEmptyPanel());
			return;
		}

		setModelObject(Integer.valueOf(index));

		ITab tab = (ITab) tabs.get(index);

		Panel panel = tab.getPanel(TAB_PANEL_ID);

		if (panel == null) {
			throw new WicketRuntimeException(
					"ITab.getPanel() returned null. TabbedPanel [" + getPath()
							+ "] ITab index [" + index + "]");

		}

		if (!panel.getId().equals(TAB_PANEL_ID)) {
			throw new WicketRuntimeException(
					"ITab.getPanel() returned a panel with invalid id ["
							+ panel.getId()
							+ "]. You must always return a panel with id equal to the provided panelId parameter. TabbedPanel ["
							+ getPath() + "] ITab index [" + index + "]");
		}

		panelContainer.replace(panel);
	}

	public final int getSelectedTab() {
		return ((Integer) getModelObject()).intValue();
	}

}
