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
package org.zoxweb.shared.util;

/**
 * This utility class includes methods used to manage NVConfigMap.
 * @author mzebib
 */
public class NVConfigMapUtil {
    /**
     * This constructor is declared private to prevent instantiation of this class.
     */
    private NVConfigMapUtil() {
    }

    /**
     * Returns the display based on NVConfig and NVConfigNameMap inputs.
     * @param nvc
     * @param nvcnm
     * @return display string
     */
    public static String toDisplayName(NVConfig nvc, NVConfigNameMap nvcnm) {
        if (nvc == null) {
            return null;
        }

        if (nvcnm != null) {
            if (nvc.getName() != null && nvc.getName().equals(nvcnm.getName())) {
                if (nvcnm.getDisplayName() != null) {
                    return nvcnm.getDisplayName();
                }
            }
        }

        if (nvc.getDisplayName() != null) {
            return nvc.getDisplayName();
        }

        return nvc.getName();
    }

    /**
     * Returns the display name based on NVConfig and NVConfigAttributesMap inputs.
     * @param nvc
     * @param map
     * @return display name
     */
    public static String toDisplayName(NVConfig nvc, NVConfigAttributesMap map) {
        if (nvc == null) {
            return null;
        }

        NVConfigNameMap nvcnm = null;

        if (map != null) {
            nvcnm = map.lookup(nvc.getName());
        }

        return toDisplayName(nvc, nvcnm);
    }

    /**
     * This method returns the display name based on NVEntity and NVConfigAttributesMap inputs.
     * @param nve
     * @param map
     * @return toString
     */
    public static String toString(NVEntity nve, NVConfigAttributesMap map) {
        return toString(nve, map, false);
    }

    /**
     * This method returns the display name based on the inputs: NVEntity, NVConfigAttributesMap,
     * and whether to add the display name.
     * @param nve
     * @param map
     * @param addDisplayName
     * @return toString
     */
    @SuppressWarnings("rawtypes")
    public static String toString(NVEntity nve, NVConfigAttributesMap map, boolean addDisplayName) {
        if (nve == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        if (map != null) {
            if (map.getAttribuesMap() != null) {
                for (int i = 0; i < map.getAttribuesMap().size(); i++) {
                    NVConfigNameMap nvConfigNameMap = map.getAttribuesMap().get(i);
                    Object value = nve.lookupValue(nvConfigNameMap.getName());

                    String tempValue = "" + value;


                    if (value == null || tempValue.length() == 0) {
                        continue;
                    }

                    if (sb.length() > 0)
                        sb.append(",");

                    sb.append(addDisplayName ? (nvConfigNameMap.getDisplayName() + ":" + value) : (value));
                }
            }

        } else if (nve instanceof RenderableValue) {
            return "" + ((RenderableValue) nve).toValue();
        } else if (nve.getAttributes() != null) {
            NVConfigEntity config = (NVConfigEntity) nve.getNVConfig();

            if (config.getDisplayAttributes() == null || config.getDisplayAttributes().size() == 0) {
                for (int i = 0; i < config.getAttributes().size(); i++) {
                    NVConfig confAttr = config.getAttributes().get(i);
                    NVBase<?> nvp = nve.lookup(confAttr.getName());

                    Object value = nvp.getValue();

                    String tempValue = "" + value;

                    if (confAttr.isHidden() || nvp.getValue() == null || tempValue.length() == 0) {
                        continue;
                    }

                    if (nvp != null && nvp.getValue() != null) {
                        if (sb.length() > 0)
                            sb.append(",");

                        sb.append(addDisplayName ? (nvp.getName() + ":" + nvp.getValue()) : (nvp.getValue()));
                    }
                }
            } else {
                for (int i = 0; i < config.getDisplayAttributes().size(); i++) {
                    NVConfig confAttr = config.getDisplayAttributes().get(i);
                    NVBase<?> nvp = nve.lookup(confAttr.getName());

                    Object value = nvp.getValue();

                    String tempValue = "" + value;

                    if (confAttr.isHidden() || nvp.getValue() == null || tempValue.length() == 0) {
                        continue;
                    }

                    if (nvp != null && nvp.getValue() != null) {
                        if (sb.length() > 0)
                            sb.append(",");

                        sb.append(addDisplayName ? (nvp.getName() + ":" + nvp.getValue()) : (nvp.getValue()));

                    }
                }
            }
        }

        return sb.toString();
    }

}
