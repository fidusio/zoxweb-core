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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NVConfigEntityLocal is an implementation of the NVConfigEntity interface
 * that can only be within the domain that it was created within.
 * The properties of this class are not exportable.
 *
 * @author mnael
 */
@SuppressWarnings("serial")
public class NVConfigEntityPortable
        extends NVConfigPortable
        implements NVConfigEntity {

    @Override
    public String toString() {
        return "NVConfigEntityLocal [byReference=" + byReference
                + ", attrList=" + fastMap + ", displayList=" + displayList
                + ", attributesValidationRequired="
                + attributesValidationRequired + ", domainID=" + domainID
                + ", getAttributes()=" + getAttributes()
                + ", getDisplayAttributes()=" + getDisplayAttributes()
                + ", getReferencedNVConfigEntity()="
                + getReferencedNVConfigEntity() + ", isReferenced()="
                + isReferenced() + ", getMetaType()=" + getMetaType()
                + ", isAttributesValidationRequired()="
                + isAttributesValidationRequired() + ", getGUID()="
                + getGUID() + ", getDomainID()="
                + getDomainID() + ", toCanonicalID()=" + toCanonicalID()
                + ", toString()=" + super.toString() + ", isUnique()="
                + isUnique() + ", getName()=" + getName()
                + ", getDescription()=" + getDescription() + ", isMandatory()="
                + isMandatory() + ", isEditable()=" + isEditable()
                + ", getDisplayName()=" + getDisplayName()
                + ", getValueFilter()=" + getValueFilter() + ", isArray()="
                + isArray() + ", getMetaTypeBase()=" + getMetaTypeBase()
                + ", isEnum()=" + isEnum() + ", isHidden()=" + isHidden()
                + ", getClass()=" + getClass() + ", hashCode()=" + hashCode()
                + "]";
    }

    private transient NVConfigEntity byReference = null;
    private transient List<NVConfig> attrList = null;
    private transient List<NVConfig> displayList = null;
    private transient Map<String, NVConfig> fastMap = null;
    private transient boolean attributesValidationRequired;
    private transient ArrayType arrayType = null;
    //private transient boolean fullyInitialized = false;
    private transient String domainID = null;
    //private transient String guid = null;


    private transient String guid = null;

    /**
     * This is the default constructor which instantiates
     * the parent class, NVConfigLocal.
     */
    public NVConfigEntityPortable() {
        super();
    }

    /**
     *
     * This constructor instantiates NVConfigLocal based on set values for the
     * following properties: name, description, display name, mandatory,
     * editable, class type, attributes list, and display attributes list.
     *
     * @param name
     * @param description
     * @param displayName
     * @param man
     * @param edit
     * @param type
     * @param attrList
     * @param displayList
     * @param attributesValidationRequired
     */
    public NVConfigEntityPortable(String name,
                                  String description,
                                  String displayName,
                                  boolean man,
                                  boolean edit,
                                  boolean isUnique,
                                  boolean isHidden,
                                  Class<?> type,
                                  List<NVConfig> attrList,
                                  List<NVConfig> displayList,
                                  boolean attributesValidationRequired,
                                  NVConfigEntity superClass) {
        super(name, description, displayName, man, edit, isUnique, isHidden, false, type, null);

        if (name == null && type != null) {
            setName(type.getName());
        }

        if (superClass != null) {
            attrList = SharedUtil.mergeMeta(attrList, superClass.getAttributes());
        }

        setAttributes(attrList);
        setDisplayAttributes(displayList);
        setAttributesValidationRequired(attributesValidationRequired);
    }


//	public NVConfigEntityLocal(	String name, 
//								String  description,
//								String displayName,
//								boolean man,
//								boolean edit,
//								boolean isUnique,
//								Class<?> type,
//								ArrayList<NVConfig> attrList,
//								ArrayList<NVConfig> displayList,
//								boolean attributesValidationRequired,
//								NVConfigEntity superClass)
//	{
//		this(name, description, displayName, man, edit, isUnique, false, type, attrList, displayList, attributesValidationRequired, superClass);
//	}


    /**
     * This constructor instantiates NVConfigLocal based on set values for the
     * following properties: name, description, display name, mandatory,
     * editable, class type, attributes list, and display attributes list.
     *
     * @param name
     * @param description
     * @param displayName
     * @param man
     * @param edit
     * @param type
     * @param at
     */
    public NVConfigEntityPortable(String name,
                                  String description,
                                  String displayName,
                                  boolean man,
                                  boolean edit,
                                  boolean embedded,
                                  Class<?> type,
                                  ArrayType at) {
        this(name, description, displayName, man, edit, false, false, type, null, null, false, null);
        if (at != null && at != ArrayType.NOT_ARRAY) {
            setArray(true);
            setArrayType(at);
        } else if (isArray()) {
            setArrayType(ArrayType.LIST);
        } else {
            setArrayType(ArrayType.NOT_ARRAY);
        }
        this.embedded = embedded;

    }

    /**
     *
     * @param name
     * @param description
     * @param displayName
     * @param man
     * @param edit
     * @param byRef
     */
//	public NVConfigEntityLocal(String name,
//							   String  description,
//							   String displayName,
//							   boolean man,
//							   boolean edit,
//							   NVConfigEntity byRef)
//	{
//		super();
//		setReferencedNVConfigEntity(byRef);
//		setName(name);
//		setDescription(description);
//		setDisplayName(displayName);
//		setMandatory(man);
//		setEditable(edit);
//	}
    public NVConfigEntityPortable(String name,
                                  String description,
                                  String displayName,
                                  boolean man,
                                  boolean edit,
                                  boolean unique,
                                  boolean hidden,
                                  NVConfigEntity byRef,
                                  boolean embedded,
                                  ArrayType arrayType) {
        super();
        setReferencedNVConfigEntity(byRef);
        setName(name);
        setDescription(description);
        setDisplayName(displayName);
        setMandatory(man);
        setEditable(edit);
        setUnique(unique);
        setHidden(hidden);
        setArrayType(arrayType);
        setArray(!(getArrayType() == ArrayType.NOT_ARRAY));
        this.embedded = embedded;
    }


    /**
     * Returns the NVConfig array list of
     * attributes.
     */
    public List<NVConfig> getAttributes() {
        if (byReference != null) {
            return byReference.getAttributes();
        }

        return attrList;

    }


//	public List<NVConfig> getAttributes() 
//	{
//		if (byReference != null)
//		{
//			return byReference.getAttributes();
//		}
//		
//		return new ArrayList<NVConfig>(fastMap.values());
//	}


    /**
     * Sets the NVConfig array list
     * of attributes.
     *
     * @param attrList
     */
    public synchronized void setAttributes(List<NVConfig> attrList) {
        this.attrList = attrList;
        fastMap = new LinkedHashMap<String, NVConfig>();


        if (this.attrList != null) {
            ArrayList<NVConfig> toPurge = new ArrayList<NVConfig>();
            for (NVConfig nvc : this.attrList) {
                String attrName = nvc.getName().toLowerCase();

                NVConfig nvcToPurge = fastMap.get(attrName);
                if (nvcToPurge != null) {
                    //throw new IllegalArgumentException("Attribute name " + attrName +" exist more than once");
                    if (nvc.getName().equalsIgnoreCase(nvcToPurge.getName()) && nvc.getClass().equals(nvcToPurge.getClass()) && nvc.isArray() == nvcToPurge.isArray())
                        toPurge.add(fastMap.get(attrName));
                    else
                        throw new IllegalArgumentException("Attribute name " + attrName + " exist more than once can't be replaced");
                }


                fastMap.put(attrName, nvc);
            }

            for (NVConfig nvcToPurge : toPurge) {
                this.attrList.remove(nvcToPurge);
            }
        }

    }


//	public synchronized void setAttributes(List<NVConfig> attrList)
//	{
//		
//		fastMap = new LinkedHashMap<String, NVConfig>();
//		
//		
//		if (attrList != null)
//		{
//			//ArrayList<NVConfig> toPurge = new ArrayList<NVConfig>();
//			for(NVConfig nvc : attrList)
//			{
//				String attrName = nvc.getName().toLowerCase();
//				
//				NVConfig nvcToPurge = fastMap.get(attrName);
//				if (nvcToPurge != null)
//				{
//					//throw new IllegalArgumentException("Attribute name " + attrName +" exist more than once");
//					if ( !(nvc.getName().equalsIgnoreCase(nvcToPurge.getName()) && nvc.getClass().equals(nvcToPurge.getClass()) && nvc.isArray() == nvcToPurge.isArray()))
//					{	
//						throw new IllegalArgumentException("Attribute name " + attrName +" exist more than once can't be replaced");
//					}
//					
//					
//				}
//				
//				fastMap.put(attrName, nvc);
//				
//			}
//
//			
//		}
//		
//	}

    /**
     * Returns the specified list of attributes
     * to be displayed.
     */
    public List<NVConfig> getDisplayAttributes() {
        if (byReference != null) {
            return byReference.getAttributes();
        }

        return displayList;
    }


    /**
     * Sets the specified list of attributes
     * to be displayed.
     *
     * @param displayList
     */
    public void setDisplayAttributes(List<NVConfig> displayList) {
        this.displayList = displayList;
    }

    /**
     * Looks up the matching NVConfig
     * by name.
     *
     * @param name
     */
    public NVConfig lookup(String name) {
        if (byReference != null) {
            //System.out.println("By Reference " + name);
            return byReference.lookup(name);
        }

        NVConfig ret = fastMap.get(name.toLowerCase());

        if (ret == null && name.indexOf('.') != -1) {
            String subNames[] = name.split("\\.");
            NVConfigEntity nvcs = this;

            for (int i = 0; i < subNames.length; i++) {
                ret = nvcs.lookup(subNames[i]);

                if (ret != null && ret instanceof NVConfigEntity) {
                    nvcs = (NVConfigEntity) ret;
                } else if (i + 1 < subNames.length) {
                    return null;
                }
            }
        }

        return ret;

//		ArrayList<NVConfig> temp = getAttributes();
//		
//		for (NVConfig nvc: temp)
//		{
//			if ( nvc.getName().equalsIgnoreCase(name))
//			{
//				return nvc;
//			}
//		}
//		return null;
    }

    /**
     * Looks up the matching NVConfig
     * by enum value.
     *
     * @param e
     */
    public NVConfig lookup(Enum<?> e) {
        if (e != null) {
            return lookup(e.name());
        }

        return null;
    }

/*

// do NOT DELETE
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		else if(o instanceof NVConfigEntity)
		{
			NVConfigEntity oNCE = (NVConfigEntity)o;
			if (toCanonicalID().equals(oNCE.toCanonicalID()) &&
				getMetaType().equals(oNCE.getMetaType()) &&
				isArray() == oNCE.isArray() &&
			    getArrayType() == oNCE.getArrayType())
			{
				return true;
			}

		}
		return false;
	}

 */


//	/**
//	 * This method checks if meta initialization is complete.
//	 * 
//	 */
//	public boolean isMetaInitComplete() 
//	{
//		return fullyInitialized;
//	}
//
//	/**
//	 * This method sets the status of the meta initialization;
//	 * true if the meta initialization is complete or false 
//	 * if not complete.
//	 * @param isFullyInitialized
//	 */
//	public synchronized void setMetaInitComplete(boolean isFullyInitialized)
//	{
//		if ( fullyInitialized)
//			throw new IllegalArgumentException( "Meta setting must can not occure more than once");
//			
//		this.fullyInitialized = isFullyInitialized;
//	}

    /**
     * @param nvce
     * @see org.zoxweb.shared.util.NVConfigEntity#setReferencedNVConfigEntity(org.zoxweb.shared.util.NVConfigEntity)
     */
    @Override
    public synchronized void setReferencedNVConfigEntity(NVConfigEntity nvce) {
        byReference = nvce;
    }

    /**
     * @see org.zoxweb.shared.util.NVConfigEntity#getReferencedNVConfigEntity()
     */
    @Override
    public NVConfigEntity getReferencedNVConfigEntity() {
        return byReference;
    }

    /**
     * @see org.zoxweb.shared.util.NVConfigEntity#isReferenced()
     */
    @Override
    public boolean isReferenced() {
        return (byReference != null);
    }

    /**
     *
     */
    public Class<?> getMetaType() {
        if (byReference != null) {
            return byReference.getMetaType();
        }

        return metaType;
    }

    /**
     *
     */
    public boolean isAttributesValidationRequired() {
        return attributesValidationRequired;
    }

    /**
     *
     */
    public void setAttributesValidationRequired(boolean attributesValidationRequired) {
        this.attributesValidationRequired = attributesValidationRequired;
    }

    /**
     * @see org.zoxweb.shared.util.DomainID#getDomainID()
     */
    @Override
    public String getDomainID() {
        return domainID;
    }

    /**
     * @see org.zoxweb.shared.util.DomainID#setDomainID(java.lang.Object)
     */
    @Override
    public void setDomainID(String domainid) {
        this.domainID = domainid;
    }

    /**
     * @see org.zoxweb.shared.util.CanonicalID#toCanonicalID()
     */
    @Override
    public String toCanonicalID() {
        if (!SUS.isEmpty(getDomainID())) {
            return SharedUtil.toCanonicalID('.', getDomainID(), getName());
        }

        return getName();
    }

    /**
     * @see org.zoxweb.shared.util.ReferenceID#getReferenceID()
     */
    @Override
    public String getReferenceID() {
        return guid;
    }

    /**
     * @see org.zoxweb.shared.util.ReferenceID#setReferenceID(java.lang.Object)
     */
    @Override
    public void setReferenceID(String id) {
        guid = id;
    }

    /**
     * @see org.zoxweb.shared.util.NVConfigEntity#getArrayType()
     */
    @Override
    public ArrayType getArrayType() {
        if (arrayType == null) {
            if (isArray()) {
                return ArrayType.LIST;
            } else {
                return ArrayType.NOT_ARRAY;
            }
        }

        return arrayType;
    }

    /**
     * @see org.zoxweb.shared.util.NVConfigEntity#setArrayType(org.zoxweb.shared.util.NVConfigEntity.ArrayType)
     */
    @Override
    public void setArrayType(ArrayType at) {
        if (at == null) {
            at = ArrayType.NOT_ARRAY;
        }

        arrayType = at;
    }


    /**
     * @param gid the global identifier of the object uuid is string format.
     */
    @Override
    public void setGUID(String gid) {
       setReferenceID(gid);
    }

    /**
     * @return the global identifier of the object
     */
    @Override
    public String getGUID() {
        return getReferenceID();
    }
}