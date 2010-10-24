$.namespace('Hippo.PageComposer.UI');

(function() {
    if (!Hippo.PageComposer.UI.Factory) {
        var Factory = function() {
            this.objects = {};
            this.registry = {};
        };

        Factory.prototype = {
            create : function(element) {
                var die = Hippo.PageComposer.Main.die;

                var v = this.verify(element);
                if (v.type === HST.CONTAINER) {
                    var cId = $(element).attr('hst:containertype');
                    if (typeof cId === 'undefined') {
                        die('Attribute hst:containertype not found');
                    }
                    return this._create(cId, v.id, element, Hippo.PageComposer.UI.Container.Base);
                } else if (v.type === HST.CONTAINERITEM) {
                    //for now use a generic component for containerItems
                    var cId = 'Hippo.PageComposer.UI.ContainerItem.Base';
                    return this._create(cId, v.id, element, Hippo.PageComposer.UI.ContainerItem.Base);
                }
            },

            _create : function(classId, id, element, verify) {
                var die = Hippo.PageComposer.Main.die;
                if (typeof this.registry[classId] === 'undefined') {
                    die('No implementation found for classId=' + classId);
                }
                var c = new this.registry[classId](id, element);
                if(typeof verify !== 'undefined') {
                    if (!c instanceof verify) {
                        die('Instance with id ' + id + ' should be a subclass of ' + verify);
                    }
                }
                this.objects[c.id] = c;
                return c;
            },

            verify : function(element) {
                var die = Hippo.PageComposer.Main.die;

                if (typeof element === 'undefined' || element === null) {
                    die("element is undefined or null");
                }

                var el = $(element);
                var id = el.attr('hst:id');
                if (typeof id === 'undefined') {
                    die('Attribute hst:id not found on element');
                }

                if (!element.id) {
                    if(Hippo.PageComposer.Main.isDebug()) {
                        console.warn('No @id found on element, using value of hst:id instead.');
                    }
                    element.id = id;
                }

                var type = el.attr('hst:type');
                if (typeof type === 'undefined') {
                    die('Attribute hst:type not found');
                }
                return {
                    id: id,
                    type: type
                };
            },

            getById : function(id) {
                var o = this.objects[id];
                if (typeof o === 'undefined') {
                    return null;
                }
                return o;
            },

            deleteObjectRef : function(ref) {
                if (typeof this.objects[ref] !== 'undefined') {
                    delete this.objects[ref];
                }
            },

            register : function(key, value) {
                this.registry[key] = value;
            },

            isContainer : function(element) {
                var die = Hippo.PageComposer.Main.die;
                var type = $(element).attr('hst:type');
                if (typeof type === 'undefined') {
                    die('Attribute hst:type not found');
                }
                return type === HST.CONTAINER;
            }

        };

        Hippo.PageComposer.UI.Factory = new Factory();
    }
})();