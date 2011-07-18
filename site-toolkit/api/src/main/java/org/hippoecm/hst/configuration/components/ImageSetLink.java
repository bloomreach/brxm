package org.hippoecm.hst.configuration.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import sun.font.TrueTypeFont;

/**
 * Indicates that the annotated method returns the absolute path to the handle of an image set.
 * This annotation should only be used on public getter methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ImageSetLink {

    /**
     * The variant in the image set to use as the preview image in the CMS (i.e. the CND child node name).
     * By default an empty string is returned, which means "use the primary item". If the given variant cannot be
     * found, the primary item is used instead.
     *
     * @return the CND name of the image set variant to use as the preview image in the CMS.
     */
    String previewVariant() default "";

    /**
     * The root path of the CMS configuration to use for the image picker dialog, relative to
     * '/hippo:configuration/hippo:frontend/cms'. The default picker configuration is 'cms-pickers/images'.
     *
     * @return the root path of the CMS configuration to use for the image picker dialog, relative to
     * '/hippo:configuration/hippo:frontend/cms'.
     *
     */
    String pickerConfiguration() default "cms-pickers/images";

    /**
     * The initial UUID to use in the CMS image picker if no UUID is selected yet. Use the UUID of a folder to initially
     * open the image picker dialog that folder. Use the UUID of an image set handle to preselect that image set.
     *
     * @return the initial UUID to use in the CMS image picker.
     */
    String pickerInitialUuid() default "";

    /**
     * Whether the image picker remembers the last visited folder and image. The default is 'true'.
     *
     * @return whether the image picker remembers the last visited folder and image.
     */
    boolean pickerRemembersLastVisited() default true;

    /**
     * Types of nodes to be able to select in the CMS image picker. The default node type is 'hippogallery:imageset'.
     *
     * @return the node types to be able to select in the CMS image picker.
     */
    String[] pickerSelectableNodeTypes() default { "hippogallery:imageset" };

}
