YAHOO.namespace('hippo');

YAHOO.hippo.Wireframe = {
  id: null,  
  parentId: null,
  layout: null
}

if (!YAHOO.hippo.LayoutManager) {
    
    /**
     * @description <p>Provides a singleton manager for dynamically updating on Wicket page loads and ajax events.</p>
     * @namespace YAHOO.hippo
     * @requires yahoo, dom, layout, wicketloader
     * @module layoutmanager
     * @beta
     */
    
    YAHOO.hippo.LayoutManager = {
        ROOT_ELEMENT_ID: 'ROOT_ELEMENT_ID',
        wireframes: new Array(),
        wicketLoader: new YAHOO.hippo.WicketLoader(),

        createWireframe: function(id, parentId, config) {
            var me = this;
            if(id == '') {
                id = this.ROOT_ELEMENT_ID;              
            }
            this.wireframes[id] = new YAHOO.hippo.Wireframe();
            this.wireframes[id].id = id;
            this.wireframes[id].parentId = parentId;
            
            if(id == this.ROOT_ELEMENT_ID) {
                this.wireframes[id].layout = new YAHOO.widget.Layout(config);
                function renderRoot() {
                    me.wireframes[id].layout.render();
                }
                this.wicketLoader.registerFunction(renderRoot);
                //Wicket.Event.add(window,"domready", renderRoot);
            } else if(parentId != null && this.wireframes[parentId] != null) {
                parentLayout = this.wireframes[parentId].layout;
                parentLayout.on('render', function() {
                   var el = parentLayout.getUnitByPosition('center').get('wrap');
                   config.parent = parentLayout;
                   me.wireframes[id].layout = new YAHOO.widget.Layout(id, config);
                   me.wireframes[id].layout.render();
                });
            }
        }
    };
    
    YAHOO.register("layoutmanager", YAHOO.hippo.LayoutManager, {version: "2.5.2", build: "1076"});
}
