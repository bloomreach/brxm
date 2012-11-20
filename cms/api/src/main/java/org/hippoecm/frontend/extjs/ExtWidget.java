package org.hippoecm.frontend.extjs;

import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.json.JSONObject;
import org.wicketstuff.js.ext.ExtObservable;
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
 * <sv:node sv:name="my-ext-widget" xmlns:sv="http://www.jcp.org/jcr/sv/1.0">
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
 * manager.</p>
 * <p>
 * An Ext widget can also be registered from Javascript alone:
 * <strong>MyExtWidget.js</strong>:
 * <pre>
 * MyExtWidget = ...
 * Hippo.ExtWidgets.register('myextwidget', MyExtWidget);
 * </pre>
 * </p>
 * <p>
 * Using the widget, like adding it to a panel, could be then done with:</strong>
 * <pre>
 * var somePanel = new Ext.Panel({
 *     items: [ Hippo.ExtWidgets.getConfig('myextwidget') ]
 * });
 * </pre>
 * Instantiating a widget can also be done by the registry itself:
 * <pre>
 * var myWidget = Hippo.ExtWidgets.create('myextwidget');
 * </pre>
 * It is also possible to provide additional configuration when instantiating a widget:
 * <pre>
 * var myWidget = Hippo.ExtWidgets.create('myextwidget', {
 *     someproperty: 'foo'
 * });
 * </pre>
 * </p>
 */
public abstract class ExtWidget extends ExtObservable implements IPlugin {

    private static final String EXT_WIDGET_REGISTRY_CLASS = "Hippo.ExtWidgets";

    /**
     * Always add the property 'xtype' to the configuration properties, so the registered component configuration of
     * an Ext widget can be passed to Ext to create a new instance of the Ext widget.
     */
    @SuppressWarnings("unused")
    @ExtProperty
    private final String xtype;

    private final IPluginContext context;

    public ExtWidget(final String xtype, final IPluginContext context) {
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
        // Ext widgets can be instantiated multiple times. ExtJS expects a globally unique ID per object. We therefore
        // remove the generated Wicket ID from the widget's configuration, otherwise each instantiated object will reuse
        // that same ID.
        properties.remove("id");

        // do not instantiate the plugin, but register its configuration properties and class in the Ext widget registry
        js.append(String.format("%s.register(%s, %s); ", EXT_WIDGET_REGISTRY_CLASS, properties.toString(), extClass));
    }

}