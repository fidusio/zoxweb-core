/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.annotation;


import org.zoxweb.shared.http.HTTPConst;
import org.zoxweb.shared.http.HTTPMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author javaconsigliere@gmail.com
 * This annotation is a generic binding between Servlet or WebService handler and a class defition
 * The implementing class be a bean and published via the EndPointsConfig.json file
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EndPointProp {
    /**
     * This is simplified way to represent a list of usr as one string comma separated
     * Ex: "/system/info,/system/info/detailed"
     * This represents a list of 2 uris:
     * <ol>
     *     <li>/system/info</li>
     *     <li>/system/info/detailed</li>
     * </ol>
     * Note: There a limitation here which is the exclusion of comma from the uri definition which is actually
     * a good practice
     * @return comma separated uris
     */
    String uris();


    /**
     * List of HTTPMethod to be called GET=HTTPMethod.GET, POST=HTTPMethod.POST ...
     * @return array of HTTPMethod
     */
    HTTPMethod[] methods() default {};

    String iContentType() default HTTPConst.APPLICATION_JSON + "; charset=utf-8";

    String oContentType() default HTTPConst.APPLICATION_JSON + "; charset=utf-8";

    /**
     * Name of the handler or servlet depending on the server used
     * @return name of the handle
     */
    String name();

    /**
     * Describe the handler
     * @return description of the handle
     */
    String description() default "";

}
