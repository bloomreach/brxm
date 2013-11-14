/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
(function() {
    "use strict";

    Ext.namespace('Hippo.ChannelManager.TemplateComposer');

    Hippo.ChannelManager.TemplateComposer.IconGridView = Ext.extend(Ext.util.Observable, {

        TOOLBAR_ITEM_WIDTH: 150,

        masterTpl: new Ext.Template('<ul class="toolbar-window">',
                '<li class="scroll-left"></li>',
                '<li>',
                '<ul class="toolbar">{items}</ul>' +
                        '</li>',
                '<li class="scroll-right"></li>',
                '</ul>'),

        itemTpl: new Ext.Template('<li class="toolbar-item">',
                '<ul id="{id}" class="item">',
                '<li class="icon"><img src="{icon}"/></li>',
                '<li class="name">{name}</li>',
                '</ul>',
                '</li>'),

        defaultIconUrl: null,

        constructor: function(config) {
            Ext.apply(this, config);
            this.defaultIconUrl = config.defaultIconUrl || null;
            this.addEvents('beforerefresh', 'refresh');
            Hippo.ChannelManager.TemplateComposer.IconGridView.superclass.constructor.call(this, config);
        },

        init: function(grid) {
            this.grid = grid;
            this.initTemplates();
            this.initData(grid.store, grid.colModel);
        },

        layout: function(initial) {
            // TODO ?
        },

        processEvent: function(name, e) {
            var target, grid, row;
            target = e.getTarget();
            grid = this.grid;

            grid.fireEvent(name, e);

            row = this.findRowIndex(target);
            if (row !== false) {
                grid.fireEvent('row' + name, grid, row, e);
            } else {
                grid.fireEvent('container' + name, grid, e);
            }
        },

        /**
         * Return the index of the grid row which contains the passed HTMLElement.
         *
         * @param {HTMLElement} el The target HTMLElement
         * @return {Number} The row index, or <b>false</b> if the target element is not within a row of this GridView.
         */
        findRowIndex: function(el) {
            if (!el) {
                return false;
            }
            var item = this.fly(el).findParent(".item", 4);
            if (item) {
                return this.store.find('id', item.id);
            }
            return false;
        },

        /**
         * Focuses the specified row.
         * @param {Number} row The row index
         */
        focusRow: function(row) {
            // nothing yet
        },

        /**
         * Each GridView has its own private flyweight, accessed through this method
         */
        fly: function(el) {
            if (!this._flyweight) {
                this._flyweight = new Ext.Element.Flyweight(document.body);
            }
            this._flyweight.dom = el;
            return this._flyweight;
        },

        initTemplates: function() {
            var templates = {}, name, template;
            Ext.applyIf(templates, {
                item: this.itemTpl,
                master: this.masterTpl
            });

            for (name in templates) {
                template = templates[name];

                if (template && Ext.isFunction(template.compile) && !template.compiled) {
                    template.disableFormats = true;
                    template.compile();
                }
            }

            this.templates = templates;
        },

        initElements: function() {
            var el = Ext.get(this.grid.getGridEl().dom);

            Ext.apply(this, {
                el: el,
                mainBody: el
            });
        },

        doRenderItems: function() {
            var grid, store, rowCount, records, templates, itemTemplate, itemsBuffer, len, record, j, meta;
            if (!this.grid.store) {
                return;
            }
            grid = this.grid;
            store = grid.store;
            rowCount = store.getCount();

            if (rowCount < 1) {
                return '';
            }

            records = store.getRange(0, rowCount - 1);

            templates = this.templates;
            itemTemplate = templates.item;
            itemsBuffer = [];
            meta = {};
            len = records.length;

            //build up each items HTML
            for (j = 0; j < len; j++) {
                record = records[j];

                meta.id = record.get('id');
                meta.name = record.get('label');
                if (!meta.name || meta.name === '') {
                    meta.name = record.get('name');
                }
                meta.icon = record.get('iconURL');
                if (!meta.icon || meta.icon === '') {
                    meta.icon = this.defaultIconUrl;
                }

                if (Ext.isEmpty(meta.value)) {
                    meta.value = '&#160;';
                }

                itemsBuffer[itemsBuffer.length] = itemTemplate.apply(meta);
            }
            return itemsBuffer.join('');
        },

        doRenderBody: function() {
            return this.templates.master.apply({ items: '' });
        },

        refresh: function() {
            this.fireEvent('beforerefresh', this);
            this.grid.stopEditing(true);
            if (this.toolbarBody) {
                this.toolbarBody.setLeft('0');
                this.toolbarBody.setWidth(this.getItemsTotalWidth());
                this.toolbarBody.update(this.doRenderItems());
                this.fireEvent('refresh', this);
            }
        },

        getItemsTotalWidth: function() {
            if (!this.grid.store) {
                return 0;
            }
            return this.grid.store.getCount() * this.TOOLBAR_ITEM_WIDTH;
        },

        destroy: function() {
            var me = this,
                    grid = me.grid,
                    gridEl = grid.getGridEl(),
                    dragZone = me.dragZone,
                    splitZone = me.splitZone,
                    columnDrag = me.columnDrag,
                    columnDrop = me.columnDrop,
                    scrollToTopTask = me.scrollToTopTask,
                    columnDragData,
                    columnDragProxy;


            me.initStoreEvents(null);
            me.purgeListeners();

            delete me.innerHd;

            delete grid.container;

            if (dragZone) {
                dragZone.destroy();
            }

            Ext.dd.DDM.currentTarget = null;
            delete Ext.dd.DDM.locationCache[gridEl.id];

            Ext.EventManager.removeResizeListener(me.onWindowResize, me);
        },

        render: function() {
            this.initElements();
        },

        afterRender: function() {
            var scrollLeft, scrollRight;

            this.mainBody.dom.innerHTML = this.doRenderBody();
            this.toolbarBody = this.mainBody.child('.toolbar');

            // register scrolling handlers, maybe should move somewhere else
            scrollLeft = this.mainBody.child('.scroll-left');
            scrollLeft.on('click', function() {
                var innerLeft, outerLeft, buterLeft, to;
                innerLeft = this.toolbarBody.getLeft();
                outerLeft = this.mainBody.getLeft();
                to = this.TOOLBAR_ITEM_WIDTH;
                if (innerLeft + this.TOOLBAR_ITEM_WIDTH > outerLeft) {
                    to = outerLeft - innerLeft;
                }
                if (to > 0) {
                    this.toolbarBody.move('r', to, {duration: 0.25, easing: 'easeIn'});
                }
            }, this);

            scrollRight = this.mainBody.child('.scroll-right');
            scrollRight.on('click', function() {
                var innerRight, outerRight, to;
                innerRight = this.toolbarBody.getRight();
                outerRight = this.mainBody.getRight();
                to = this.TOOLBAR_ITEM_WIDTH;
                if (innerRight - this.TOOLBAR_ITEM_WIDTH < outerRight) {
                    to = innerRight - outerRight;
                }
                if (to > 0) {
                    this.toolbarBody.move('l', to, {duration: 0.25, easing: 'easeIn'});
                }
            }, this);

            this.grid.fireEvent('viewready', this.grid);
        },

        initData: function(newStore) {
            if (!newStore) {
                return;
            }
            var oldStore = this.store;
            if (oldStore && oldStore !== newStore) {
                oldStore.un('clear', this.onClear, this);
                oldStore.un('datachanged', this.onDataChange, this);
                oldStore.destroy();
            }
            newStore.on('clear', this.onClear, this);
            newStore.on('datachanged', this.onDataChange, this);

            this.store = newStore;
        },

        onDataChange: function() {
            this.refresh();
        },

        onClear: function() {
            this.refresh();
        },

        onRowSelect: function(row) {
        },

        onRowDeselect: function(row) {
        },

        onCellSelect: function(row, col) {
        },

        onCellDeselect: function(row, col) {
        }

    });

}());