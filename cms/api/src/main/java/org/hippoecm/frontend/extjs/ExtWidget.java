package org.hippoecm.frontend.extjs;

import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.json.JSONObject;
import org.wicketstuff.js.ext.ExtComponent;
import org.wicketstuff.js.ext.util.ExtProperty;

/**
 * <p>
 * An Ext widget an Ext component that is not instantiated when rendered. Instead, we render the configuration properties
 * that should be passed to their Javascript constructor. These configuration properties are automatically registered
 * in the global <code>Hippo.ExtWidgets</code> registry. The registration key is the Ext xtype. It is then
 * possible to instantiate a widget multiple times in Javascript by retrieving its configuration from the registry
 * and passing it to Ext. Since ExtWidgets are also CMS plugins, they can be easily bootstrapped via plugin
 * configuration node in the repository.
 * </p>
 * <p>
 * Here's a simple example of an Ext widget.</p>
 * <p>
 * <strong>Repository configuration:</strong>
 * <pre>
 * <?xml version="1.0" encoding="UTF-8"?>
 * <sv:node sv:name="my-lazy-component-plugin" xmlns:sv="http://www.jcp.org/jcr/sv/1.0">
 *   <sv:property sv:name="jcr:primaryType" sv:type="Name">
 *     <sv:value>frontend:plugin</sv:value>
 *   </sv:property>
 *   <sv:property sv:name="plugin.class" sv:type="String">
 *     <sv:value>com.example.MyExtWidget</sv:value>
 *   </sv:property>
 * </sv:node>
 * </pre>
 * </p>
 * <p>
 * <strong>MyExtWidget.java:</strong>
 * <pre>
 * @ExtClass("MyExtWidget")
 * public class MyExtWidget extends ExtWidget {
 *
 *     public MyExtWidget(IPluginContext context, IPluginConfig config) {
 *         super("myextwidget", context);
 *         add(JavascriptPackageResource.getHeaderContribution(MyExtWidget.class, "MyExtWidget.js"));
 *     }
 *
 *     @Override
 *     protected void onRenderProperties(final JSONObject properties) throws JSONException {
 *         super.onRenderProperties(properties);
 *         properties.put("exampleProperty", "somevalue");
 *     }
 * }
 * </pre>
 * </p>
 * <p>
 * <strong>MyExtWidget.js</strong>:
 * <pre>
 * MyExtWidget = Ext.extend(Ext.Panel, {
 *
 *     constructor: function(config) {
 *         alert(config.exampleProperty);
 *         MyExtWidget.superclass.constructor.call(this, config);
 *     }
 *
 * }
 * </pre>
 * </p>
 * <p>
 * The xtype 'myextwidget' is passed to the Java superclass, and automatically registered with the Ext component
 * manager. Using the lazy component, like adding it to a panel, could be then done with:</strong>
 * <pre>
 * var somePanel = new Ext.Panel({
 *     items: [ Hippo.ExtWidgets.getConfig('myextwidget') ]
 * });
 * </pre>
 * Instantiating a widget can also be done by the registry itself:
 * <pre>
 * var myWidget = Hippo.ExtWidgets.create('myextwidget');
 * </pre>
 * </p>
 */
public abstract class ExtWidget extends ExtComponent implements IPlugin {

    private static final String EXT_WIDGET_REGISTRY_CLASS = "Hippo.ExtWidgets";

    /**
     * Always add the property 'xtype' to the configuration properties, so the registered component configuration of
     * a lazy Ext object can be passed to Ext to create a new instance of the lazy component.
     */
    @SuppressWarnings("unused")
    @ExtProperty
    private final String xtype;

    private final IPluginContext context;

    public ExtWidget(final String xtype, final IPluginContext context) {
        super("item");
        this.xtype = xtype;
        this.context = context;
    }

    public String getXType() {
        return xtype;
    }

    @Override
    public void start() {
        this.context.registerService(this, ExtWidgetRegistry.EXT_WIDGET_SERVICE_ID);
    }

    @Override
    public void stop() {
        // nothing to do
    }

    @Override
    public void buildInstantiationJs(final StringBuilder js, final String extClass, final JSONObject properties) {
        // do not instantiate the plugin, but register its xtype and add its configuration properties to the
        // lazy Ext component registry
        js.append(String.format("Ext.reg('%s', %s); %s.register(%s); ",
                xtype, extClass, EXT_WIDGET_REGISTRY_CLASS, properties.toString()));
    }

}