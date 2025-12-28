/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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

import java.util.Comparator;

/**
 * This class compares NVEntity objects based on the applicable
 * data type.
 * @author mzebib
 *
 */
public class NVEntityComparator
        implements Comparator<NVEntity> {
    /**
     * This constructor instantiates NVEntityComparator based on NVConfig parameter.
     */
    //private final NVConfigEntity nvceParent;
    private final NVConfig nvc;
    private final boolean reverseOrder;
    private String[] canonicalNames = null;

    public NVEntityComparator(NVConfig nvc) {
        this(nvc, false);
    }

    public NVEntityComparator(NVConfig nvc, boolean reverseOrder) {
        this.nvc = nvc;
        this.reverseOrder = reverseOrder;
    }

//	public NVEntityComparator(NVConfig nvc, NVConfigEntity nvceParent, boolean reverseOrder)
//	{
//		this.nvceParent = nvceParent;
//		this.nvc = nvc;
//		this.reverseOrder = reverseOrder;
//	}

    public NVEntityComparator(String canonicalID, NVConfigEntity nvceParent) {
        this(canonicalID, nvceParent, false);
    }

    public NVEntityComparator(String canonicalID, NVConfigEntity nvceParent, boolean reverseOrder) {
        //this.nvceParent = nvceParent;
        this.reverseOrder = reverseOrder;

        NVConfig tempNVC = nvceParent;
        canonicalNames = canonicalID.split("[.]");

        for (int i = 0; i < canonicalNames.length; i++) {
            tempNVC = ((NVConfigEntity) tempNVC).lookup(canonicalNames[i]);

            if (tempNVC == null) {
                //throw new IllegalArgumentException("Invalid canonical ID: " + canonicalID + " Associated NVConfig not found.");
            }
        }

        nvc = tempNVC;

    }

    /**
     * This method is used to compare two objects and returns
     * the comparison value between the two objects whether one
     * is equal to, greater than, or less than the other object.
     * @param o1
     * @param o2
     */
    public int compare(NVEntity o1, NVEntity o2) {
        int val = 0;

        if (o1 != null && o2 == null) {
            val = +1;
        } else if (o1 == null && o2 != null) {
            val = -1;
        } else if (o1 != null && o2 != null) {
            NVBase<?> nvb1 = lookupNVB(o1);
            NVBase<?> nvb2 = lookupNVB(o2);

            Comparator<Object> c = NVConfigComparators.lookupComparator(lookupNVC(o1, o2));

            if (c != null && nvb1 != null && nvb2 != null) {
                val = c.compare(nvb1.getValue(), nvb2.getValue());
            } else if (nvb1 != null && nvb2 == null) {
                val = +1;
            } else if (nvb1 == null && nvb2 != null) {
                val = -1;
            }
        }

        if (reverseOrder) {
            return -val;
        }

        return val;
    }

    private NVBase<?> lookupNVB(NVEntity nve) {
        if (canonicalNames != null) {
            NVBase<?> temp = null;

            for (String name : canonicalNames) {
                temp = nve.lookup(name);

                if (temp != null && temp instanceof NVEntityReference) {
                    nve = (NVEntity) temp.getValue();
                } else if (temp == null) {
                    return null;
                }
            }

            return temp;
        }


        return nve.lookup(nvc.getName());
    }

    private NVConfig lookupNVC(NVEntity nve1, NVEntity nve2) {
        if (nvc != null) {
            return nvc;
        }

        NVConfig temp = null;

        if (nve1 != null) {
            NVEntity nve = nve1;
            NVBase<?> nvb = null;

            for (String name : canonicalNames) {
                nvb = nve.lookup(name);

                if (nvb == null) {
                    break;
                }

                if (nvb instanceof NVEntityReference) {
                    nve = (NVEntity) nvb.getValue();
                }
            }

            if (nvb != null) {
                temp = ((NVConfigEntity) nve.getNVConfig()).lookup(nvb.getName());
            }
        }

        if (temp == null && nve2 != null) {
            NVEntity nve = nve1;
            NVBase<?> nvb = null;

            for (String name : canonicalNames) {
                nvb = nve.lookup(name);

                if (nvb == null) {
                    break;
                }

                if (nvb instanceof NVEntityReference) {
                    nve = (NVEntity) nvb.getValue();
                }
            }

            if (nvb != null) {
                temp = ((NVConfigEntity) nve.getNVConfig()).lookup(nvb.getName());
            }
        }

        return temp;
    }

}
