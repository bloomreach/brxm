package org.hippoecm.frontend.extjs;

import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.json.JSONObject;
import org.wicketstuff.js.ext.ExtComponent;
import org.wicketstuff.js.ext.util.ExtProperty;

/**
 * <p>
 * Lazy Ext components are not instantiated when rendered. Instead, we render the configuration properties
 * that should be passed to their Javascript constructor. These configuration properties are automatically registered
 * in the global <code>Hippo.LazyExtComponents</code> registry. The registration key is the Ext xtype. It is then
 * possible to instantiate such a lazy component multiple times in Javascript by retrieving its
 * configuration from the registry and passing it to Ext. Since LazyExtComponent are also CMS plugins, they can be
 * easily bootstrapped via plugin configuration node in the repository.
 * </p>
 * <p>
 * Here's a simple example of a lazy Ext component.</p>
 * <p>
 * <strong>Repository configuration:</strong>
 * <pre>
 * <?xml version="1.0" encoding="UTF-8"?>
 * <sv:node sv:name="my-lazy-component-plugin" xmlns:sv="http://www.jcp.org/jcr/sv/1.0">
 *   <sv:property sv:name="jcr:primaryType" sv:type="Name">
 *     <sv:value>frontend:plugin</sv:value>
 *   </sv:property>
 *   <sv:property sv:name="plugin.class" sv:type="String">
 *     <sv:value>com.example.MyLazyComponent</sv:value>
 *   </sv:property>
 * </sv:node>
 * </pre>
 * </p>
 * <p>
 * <strong>MyLazyComponent.java:</strong>
 * <pre>
 * @ExtClass("MyLazyComponent")
 * public class MyLazyComponent extends LazyExtComponent {
 *
 *     public MyLazyComponent(IPluginContext context, IPluginConfig config) {
 *         super("mylazycomponent", context);
 *         add(JavascriptPackageResource.getHeaderContribution(MyLazyComponent.class, "MyLazyComponent.js"));
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
 * <strong>MyLazyComponent.js</strong>:
 * <pre>
 * MyLazyComponent = Ext.extend(Ext.Panel, {
 *
 *     constructor: function(config) {
 *         alert(config.exampleProperty);
 *         MyLazyComponent.superclass.constructor.call(this, config);
 *     }
 *
 * }
 * </pre>
 * </p>
 * <p>
 * The xtype 'mylazycomponent' is passed to the Java superclass, and automatically registered with the Ext component
 * manager. Using the lazy component, like adding it to a panel, could be then done with:</strong>
 * <pre>
 * var somePanel = new Ext.Panel({
 *     items: [ Hippo.LazyExtComponents.getConfig('mylazycomponent') ]
 * });
 * </pre>
 * </p>
 */
public abstract class LazyExtComponent extends ExtComponent implements IPlugin {

    private static final String LAZY_EXT_COMPONENT_REGISTRY_CLASS = "Hippo.LazyExtComponents";

    /**
     * Always add the property 'xtype' to the configuration properties, so the registered component configuration of
     * a lazy Ext object can be passed to Ext to create a new instance of the lazy component.
     */
    @SuppressWarnings("unused")
    @ExtProperty
    private final String xtype;

    private final IPluginContext context;

    public LazyExtComponent(final String xtype, final IPluginContext context) {
        super("item");
        this.xtype = xtype;
        this.context = context;
    }

    public String getXtype() {
        return xtype;
    }

    @Override
    public void start() {
        this.context.registerService(this, LazyExtComponentRegistry.LAZY_EXT_COMPONENT_SERVICE_ID);
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
                xtype, extClass, LAZY_EXT_COMPONENT_REGISTRY_CLASS, properties.toString()));
    }

}