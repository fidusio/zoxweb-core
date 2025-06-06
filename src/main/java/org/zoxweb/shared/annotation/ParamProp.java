package org.zoxweb.shared.annotation;

import org.zoxweb.shared.util.Const;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ParamProp {

    /**
     * Name of the parameter
     *
     * @return name
     */
    String name();

    /**
     * If optional return true
     *
     * @return
     */
    boolean optional() default false;

    /**
     * If true the whole URI
     *
     * @return true the parameter is the whole uri
     */
    boolean uri() default false;

    /**
     * Parameter input source
     *
     * @return
     */
    Const.ParamSource source() default Const.ParamSource.PATH;
}
