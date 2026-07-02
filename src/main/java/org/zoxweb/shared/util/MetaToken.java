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

/**
 * This enum contains meta parameters.
 * @author mzebib
 *
 */
public enum MetaToken
        implements GetName {

    ATTRIBUTES("attributes") // 	Attributes list or collection.
    ,

    BROKER_GUID("broker_guid") // GUID of broker
    ,

    CANONICAL_ID("canonical_id") // Canonical ID
    ,

    CLASS_ID("class_id") //	Class ID
    ,

    CLASS_TYPE("class_type") // 	Class type of the attribute.
    ,

    COLLECTION_NAME("collection_name") // 	The collection name can be table name for SQL databases or collection name for NoSQL databases.
    ,

    DESCRIPTION("description") //	Description of the attribute.
    ,

    DOMAIN_ID("domain_id") // Domain ID
    ,

    ENUMS("enums") // Enums
    ,

    ENUM_TYPE("enum_type") // enum type (enum class name)
    ,

    GUID(Const.GUID) // Global ID of the attributes
    ,

    IGNORE_CASE("ignore_case") // Ignore case
    ,

    IS_ARRAY("is_array") //	Array property of the attribute.
    ,

    IS_FIXED("is_fixed") // 	Fixed property of the attribute.
    ,

    JSON_CONTENT("json_content") // Json content to wrap a json object
    ,

    LOGICAL_OPERATOR("logical_operator") // LogicalOperator
    ,

    META_TYPE("meta_type") // meta type
    ,

    NAME("name") // 	Name of the attribute.
    ,

    PERMISSION_GUID("permission_guid"),

    RECURSIVE("recursive") //	Recursive
    ,
    REFERENCE_GUID(Const.REFERENCE_GUID),

    REFERENCE_ID("reference_id") // 	Reference ID of the attribute to be deprecated replaced by GUID.
    ,
    REFERENCE_TYPE("reference_type"),

    RELATIONAL_OPERATOR("relational_operator") // RelationOperator
    ,
    RESOURCE_GUID(Const.RESOURCE_GUID),
    RESOURCE_TYPE("resource_type"),

    ROLE_GROUP_GUID("role_group_guid"),

    ROLE_GUID("role_guid"),

    STATIC("static") // is static
    ,
    SUBJECT_GUID(Const.SUBJECT_GUID) // Subject GUID
    ,

    SUBJECT_ID(Const.SUBJECT_ID), // SubjectID token

    UNIT("unit"),

    VALUE("value") // 	Value of the attribute.
    ,
    VALUES("values") //Values
    ,
    VALUE_FILTER("value_filter") // 	Value filter of the attribute.
    ,

    ;

    private final String name;

    MetaToken(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }


    public static boolean isPrimitiveArray(NVBase<?> nvb) {
        return nvb instanceof NVStringList || nvb instanceof NVIntList ||
                nvb instanceof NVLongList || nvb instanceof NVFloatList ||
                nvb instanceof NVDoubleList || nvb instanceof NVEnumList;
    }

    public static boolean isNVEntityArray(NVBase<?> nvb) {
        return nvb instanceof NVEntityReferenceList || nvb instanceof NVEntityGetNameMap || nvb instanceof NVEntityReferenceIDMap;
    }

    public static boolean isArrayValuesString(NVBase<?> nvb) {
        return nvb instanceof NVPairList || nvb instanceof NVPairGetNameMap || nvb instanceof NVGetNameValueList;
    }

}