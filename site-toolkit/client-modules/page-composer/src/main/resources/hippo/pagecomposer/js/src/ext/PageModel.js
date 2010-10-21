Ext.namespace('Hippo.App.PageModel');

(function() {
    Hippo.App.PageModel.FactoryImpl = function() {
    };

    Hippo.App.PageModel.FactoryImpl.prototype = {
        createModel: function(element, cfg) {

            var id, name, path, type;
            if (typeof element === 'undefined' || element === null) {


            } else {
                id = element.getAttribute('hst:id');
                name = element.getAttribute('hst:name');
                path = element.getAttribute('hst:path');
                type = element.getAttribute('hst:type');
            }

            var config = {
                id:   id,
                name: name,
                path: path,
                type: type,
                element: element,

                isRoot: false,
                parentId: null,
                children: []
            };
            Ext.apply(config, cfg);

            if (config.type === 'hst:containercomponent') {
                return new Hippo.App.PageModel.Container(config);
            } else {
                return new Hippo.App.PageModel.Component(config);
            }
        },

        createRecord : function(model) {
            return new Hippo.App.PageModel.ComponentRecord(model);
        }
    };

    Hippo.App.PageModel.Factory = new Hippo.App.PageModel.FactoryImpl();

    Hippo.App.PageModel.Component = function(cfg) {
        Ext.apply(this, cfg);
    };

    Hippo.App.PageModel.Component.prototype = {
    };

    Hippo.App.PageModel.Container = Ext.extend(Hippo.App.PageModel.Component, {
        constructor: function(config) {
            Hippo.App.PageModel.Container.superclass.constructor.call(this, config);

            this.containerType = this.element.getAttribute('hst:containerType');
        }
    });

    //TODO: update this one for dropFromParent stuff
    Hippo.App.PageModel.ComponentRecord = Ext.data.Record.create([
        {name: 'id', mapping: 'id'},
        {name: 'name', mapping: 'name'},
        {name: 'type', mapping: 'type'},
        {name: 'path', mapping: 'path'},
        {name: 'parentId', mapping: 'parentId'},
        {name: 'componentClassName', mapping: 'componentClassName'},
        {name: 'template', mapping: 'template'},
        {name: 'element', mapping: 'element'}

    ]);

    Hippo.App.PageModel.ReadRecord = Ext.data.Record.create([
        {name: 'id', mapping: 'id'},
        {name: 'name', mapping: 'name'},
        {name: 'path', mapping: 'path'},
        {name: 'parentId', mapping: 'parentId'},
        {name: 'componentClassName', mapping: 'componentClassName'},
        {name: 'template', mapping: 'template'},
        {name: 'type', mapping: 'type'},
        {name: 'children', mapping: 'children'},
        {name: 'element', convert: function(v, record) {
            var element = Hippo.App.Main.findElement(record.id);
            if (element == null) {
                element = document.createElement('div');
                element.id = record.id;
                element.setAttribute('hst:id', record.id);
                element.setAttribute('hst:name', record.name);
                element.setAttribute('hst:type', record.type);
                if (record.innerHTML) {
                    element.innerHTML = record.innerHTML;
                } else {
                    var link = document.createElement('a');
                    link.setAttribute('href', '');
                    link.innerHTML = 'Click here to refresh';
                    element.appendChild(link);
                }
                element.className = 'componentContentWrapper';
            }
            return element;
        }
        }
    ]);

})();
