/*
 * Copyright (c) 2012-2023 ZoxWeb.com LLC.
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
package org.zoxweb.server.http;

import com.google.gson.annotations.SerializedName;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.util.*;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a special encoder class that take 2 input parameters HTTPMessageConfigInterface and NVGenericMap and return  HTTPMessageConfigInterface.
 * <br>The inputs:
 * <ul>
 *     <li>HTTPMessageConfigInterface is the HTTP message configuration url, http method, headers, basic content
 *     <li>NVGeneric the user parameters
 * </ul>
 * <br>The return is HTTPMessageConfigInterface with the content encoded for NVGenericMap input
 *
 *
 */
public class HTTPNVGMBiEncoder
    implements BiDataEncoder<HTTPMessageConfigInterface, NVGenericMap, HTTPMessageConfigInterface>
{


    public static final LogWrapper log = new LogWrapper(HTTPNVGMBiEncoder.class);
    // the basic configuration for the message structure
    private final NVGenericMap config;

    @SerializedName("params_map")
    private final HashMap<String, String> paramsNameMapping;

    @SerializedName("use_only_mapped_names")
    private boolean useOnlyMappedNames  = false;


    /**
     *
     * @param config the message content basic configuration
     * @param inclusionParameters if any set the of those parameters only will be encoded
     */

    public HTTPNVGMBiEncoder(NVGenericMap config, String... inclusionParameters)
    {
        this.config = config;

        if(inclusionParameters != null && inclusionParameters.length > 0)
        {
            paramsNameMapping = new HashMap<>();
            for(String toInclude : inclusionParameters)
            {
                if(toInclude != null)
                    paramsNameMapping.put(toInclude, toInclude);
            }
            useOnlyMappedNames = true;
        }
        else
        {
            paramsNameMapping = null;
        }

    }

    /**
     *
     * @param config the message content basic configuration
     * @param paramsMapName if the parameter encoded name is different from the name of the input parameters
     * @param useOnlyMappedNames if true only the parameters from paramMapName will be encoded
     */
    public HTTPNVGMBiEncoder(NVGenericMap config, Map<String,String> paramsMapName, boolean useOnlyMappedNames)
    {
        this.config = config;
        if(paramsMapName != null)
        {
            this.useOnlyMappedNames = useOnlyMappedNames;
            paramsNameMapping = new HashMap<>();
            for(Map.Entry<String, String> toInclude : paramsMapName.entrySet())
            {
                if(toInclude != null && toInclude.getKey() != null)
                    paramsNameMapping.put(toInclude.getKey(), toInclude.getValue() != null ? toInclude.getValue() : toInclude.getKey());
            }
        }
        else
        {
            paramsNameMapping = null;
        }

    }

    /**
     * This method encode the user params as json format and create the content and set it to hmci
     * @param hmci to be used to encode params and update hmci by setting its content
     * @param params to be added to the content
     * @return updated hmci
     */
    @Override
    public HTTPMessageConfigInterface encode(HTTPMessageConfigInterface hmci, NVGenericMap params)
    {
        // make a copy of encoded configuration
        // the static part of the content
        NVGenericMap content = NVGenericMap.copy(config, true);

        // do we have parameters to encode
        if (params != null)
        {
            // go through the list of parameters
            for(GetNameValue<?> parameter: params.values())
            {

                String containerName = null; // the name of containing json object where the parameter will be added
                String paramName = null; // the name of parameter excepted by the remote api
                if (paramsNameMapping != null && paramsNameMapping.containsKey(parameter.getName()))
                {


                    // we have a mapped name
                    // input parameter name "mailto" mapped to "data.profile.email"
                    String mappedName = paramsNameMapping.get(parameter.getName());
                    if(mappedName != null)
                    {
                        // extract the name email
                        paramName = SharedStringUtil.valueAfterRightToken(mappedName, ".");
                        // extract the container data.profile
                        containerName = SharedStringUtil.valueBeforeRightToken(mappedName, ".");
                        if(containerName.equals(paramName))
                        {
                            // if the name and container are equals
                            // the container is content
                            containerName = null;
                        }
                        if(log.isEnabled()) log.getLogger().info(paramName + ", " + containerName +",  " + mappedName );
                        // this might throw an exception
                        // update the parameter name mailto->email
                        ((SetName)parameter).setName(paramName);
                    }


                    if(containerName != null && content.lookup(containerName) instanceof ArrayValues)
                        // locate the parameter container data.profile and add the parameter to it
                        ((ArrayValues)content.lookup(containerName)).add(parameter);
                    else
                        // if the container not found add parameter email to the content
                        content.add(parameter);
                }
                else if(!useOnlyMappedNames())
                {
                    // if userOnlyMappedNames is not enabled add extra parameters to the content

                    // extract the name email
                    paramName = SharedStringUtil.valueAfterRightToken(parameter.getName(), ".");
                    // extract the container data.profile
                    containerName = SharedStringUtil.valueBeforeRightToken(parameter.getName(), ".");

                    ((SetName)parameter).setName(paramName);


                    if (paramName.equals(containerName))
                    {
                        // the container is the content if containerName and paramName equals
                        content.add(parameter);
                    }
                    else if(containerName != null && content.lookup(containerName) instanceof ArrayValues)
                    {
                        // add the parameter to appropriate container
                        ((ArrayValues)content.lookup(containerName)).add(parameter);
                    }
                    else
                    {
                        // add parameter
                        content.add(parameter);
                    }

                }

            }
        }

        String json = GSONUtil.toJSONDefault(content);
        if(log.isEnabled()) log.getLogger().info("Content to be sent:\n" + json);
        hmci.setContent(GSONUtil.toJSONDefault(content));
        return hmci;
    }

    public boolean useOnlyMappedNames()
    {
        return useOnlyMappedNames;
    }

}
