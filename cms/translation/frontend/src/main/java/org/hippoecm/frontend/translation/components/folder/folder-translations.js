/*
 * Copyright 2010-2021 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

Hippo.Translation.Folder = {};

Hippo.Translation.Folder.Panel = Ext.extend(Ext.form.FormPanel, {
    renderTo: 'folder-translations',
    width: 661,
    autoheight: false,
    height: 311,
    layout: 'form',
    padding: 8,
    border: false,
    cls: 'hippo-folder-translations',

    constructor: function(config) {
        var hasSiblings, self;

        this.folder = config.folder;
        this.loader = config.loader;
        this.locator = config.locator;
        this.root = config.root;
        this.resources = config.resources;
        this.pathRenderer = config.pathRenderer;
        this.breakLink = config.breakLink;

        this.images = new Hippo.Translation.ImageService(config.imageService);

        hasSiblings = (this.folder.siblings !== undefined);
        if (!hasSiblings) {
            this.panel = this.createSelectPanel();
        } else {
            this.panel = this.createSiblingPanel(this.folder.siblings);
        }

        if (config.items === undefined) {
            config.items = [];
        }
        self = this;
        config.items.push({
            xtype: 'fieldset',
            title: self.resources['folder-details'],
            items: [
                {
                    xtype: 'compositefield',
                    frame: true,
                    hideLabel: true,
                    items: [
                        {
                            id: 'folder-path',
                            xtype: 'displayfield',
                            height: 37,
                            value: this.pathRenderer.renderPath(self.folder.path)
                        }
                    ]
                }
            ]
        });

        config.items.push({
            xtype: 'button',
            id: 'translations-break-link',
            height: 20,
            disabled: !hasSiblings,
            icon: (hasSiblings ? self.breakLink : self.breakLinkDisabled),
            iconCls: 'hippo-t9n-breaklink',
            cls: 'x-btn-text-icon btn btn-default btn-sm' + (!hasSiblings ? ' x-btn-text-icon-not-found' : ''),
            text: (hasSiblings ? self.resources['unlink-translations'] : self.resources['link-translations']),
            listeners: {
                disable: function(button) {
                    this.setIcon(self.breakLinkDisabled);
                    this.addClass('x-btn-text-icon-not-found');
                    this.setText(self.resources['link-translations']);
                },
                enable: function(button) {
                    this.setIcon(self.breakLink);
                    this.removeClass('x-btn-text-icon-not-found');
                    this.setText(self.resources['unlink-translations']);
                },
                click: function() {
                    self.resetSelection();
                }
            }
        });

        this.translationsfield = Ext.create({
            xtype: 'fieldset',
            title: self.resources['translation-folders'],
            height: 156,
            cls: 'translation-field-set'
        });
        this.translationsfield.add(this.panel);
        config.items.push(this.translationsfield);

        Hippo.Translation.Folder.Panel.superclass.constructor.call(this, config);
    },

    initComponent: function() {

        Hippo.Translation.Folder.Panel.superclass.initComponent.call(this);

        this.addEvents('select-folder');

        // Ext does not handle form-within-form
        this.bodyCfg = {
            tag: "div",
            cls: "x-panel-body"
        };
    },

    createSelectPanel: function() {
        var self = this;
        return Ext.create({
            xtype: 'hippo-translation-folder-select',
            loader: self.loader,
            images: self.images,
            resources: self.resources,
            root: new Ext.tree.AsyncTreeNode({id: self.root.id}),
            listeners: {
                click: function(node) {
                    self.onNodeClick(node);
                }
            }
        });
    },

    createSiblingPanel: function(siblings) {
        return Ext.create({
            xtype: 'hippo-translation-folder-container',
            images: this.images,
            resources: this.resources,
            pathRenderer: this.pathRenderer,
            entries: siblings,
            width: 618
        });
    },

    resetSelection: function() {
        this.translationsfield.remove(this.panel);
        this.panel.destroy();

        this.panel = this.createSelectPanel();
        this.translationsfield.add(this.panel);

        Ext.getCmp('translations-break-link').disable();
        this.doLayout();

        this.fireEvent('select-folder', null);
    },

    onNodeClick: function(node) {
        var t9Id = node.attributes.t9id;
        if (t9Id !== undefined) {
            this.locator.getSiblings(t9Id, function(siblings) {
                Ext.getCmp('translations-break-link').enable();

                this.translationsfield.remove(this.panel);
                this.panel.destroy();

                this.panel = this.createSiblingPanel(siblings);
                this.translationsfield.add(this.panel);

                this.doLayout();

                this.fireEvent('select-folder', t9Id);
            }.createDelegate(this));
        }
    }

});

Hippo.Translation.Folder.SelectTree = Ext.extend(Ext.ux.tree.TreeGrid, {
    useArrows: true,
    enableSort: false,
    hideHeaders: false,
    enableHdMenu: false,
    enableDD: true,
    collapsible: false,
    width: 618,

    constructor: function(config) {
        this.images = config.images;
        this.resources = config.resources;

        var self = this;
        this.columns = [
            {
                header: self.resources['linked-folder'],
                dataIndex: 'text',
                width: 389
            },
            {
                header: self.resources.translations,
                dataIndex: 't9ns',
                width: 100,
                tpl: new Ext.XTemplate(
                        '<tpl if="this.isTranslated(values)">',
                        '<tpl for="t9ns">{.:this.format}{[xindex < xcount ? "&nbsp;" : ""]}</tpl>',
                        '</tpl>',
                        {
                            isTranslated: function(values) {
                                return values.t9ns !== undefined;
                            },
                            format: function(v) {
                                var url = Ext.util.Format.htmlEncode(self.images.getImage(v));
                                return '<img class="flag-md" src="' + url + '" />';
                            }
                        }
                )
            }
        ];
        Hippo.Translation.Folder.SelectTree.superclass.constructor.call(this, config);
    },

    selectNodeByT9Id: function() {
        var doSelect, ids, i, len;
        doSelect = function(node, ids) {
            var id = ids.pop();
            node.expand(false, false, function() {
                var j, len, child;
                for (j = 0, len = node.childNodes.length; j < len; j++) {
                    child = node.childNodes[j];
                    if (child.id === id) {
                        if (ids.length > 0) {
                            doSelect(child, ids);
                        } else {
                            this.getSelectionModel().select(child);
                        }
                        break;
                    }
                }
            }.createDelegate(this));
        }.createDelegate(this);
        ids = [];
        for (i = 0, len = this.selected.length; i < len; i++) {
            ids.unshift(this.selected[i].id);
        }
        doSelect(this.root, ids);
    }
});
Ext.reg('hippo-translation-folder-select', Hippo.Translation.Folder.SelectTree);

Hippo.Translation.Folder.Container = Ext.extend(Ext.Container, {
    layout: 'hbox',

    constructor: function(config) {
        var store, self;

        this.entries = config.entries || [];
        this.images = config.images;
        this.resources = config.resources;
        this.pathRenderer = config.pathRenderer;

        Hippo.Translation.Folder.Container.superclass.constructor.call(this, config);

        store = new Ext.data.JsonStore({
            fields: ['lang', 'path'],
            data: this.getData(this.entries)
        });

        self = this;
        this.list = Ext.create({
            xtype: 'listview',
            hideHeaders: true,
            width: 622,
            store: store,
            columns: [
                {
                    header: self.resources['folder-translation'],
                    dataIndex: 'path'
                }
            ]
        });
        this.add(this.list);
    },

    getData: function(entries) {
        var data, language, path, record;
        data = [];
        for (language in entries) {
            if (entries.hasOwnProperty(language)) {
                path = this.pathRenderer.renderPath(entries[language]);
                record = {
                    lang: language,
                    path: path
                };
                data.push(record);
            }
        }
        return data;
    }

});
Ext.reg('hippo-translation-folder-container', Hippo.Translation.Folder.Container);
