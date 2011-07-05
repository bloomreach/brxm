package org.hippoecm.hst.configuration.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DocumentLink {

    /**
     * Specifies the node type of the document to be searched for.
     * @return the document type String
     */
    String docType() default ""; //Document type is only used when a DOCUMENT type is used.

    /**
     * @return  specifies whether to show a link to create a new document of the type as specified by the docType
     */
    boolean allowCreation() default false;

    /**
     * @return the relative path of the folder where the document is created
     */
    String docLocation() default "";

}
