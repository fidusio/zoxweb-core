/*
 * Copyright (c) 2012-Aug 19, 2014 ZoxWeb.com LLC.
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
	implements GetName
{
	// 	Name of the attribute.
	NAME("name"),
	//	Description of the attribute.
	DESCRIPTION("description"),
	// 	Value of the attribute.
	VALUE("value"),
	// 	Reference ID of the attribute.
	REFERENCE_ID("reference_id"),
	// Global ID of the attributes
	GLOBAL_ID("global_id"),
	// 	Value filter of the attribute.
	VALUE_FILTER("value_filter"),
	// 	Attributes list or collection.
	ATTRIBUTES("attributes"),
	// 	Class type of the attribute.
	CLASS_TYPE("class_type"),
	// 	Fixed property of the attribute.
	IS_FIXED("is_fixed"),
	// 	The collection name can be table name for SQL databases or collection name for NoSQL databases.
	COLLECTION_NAME("collection_name"),
	// Json content to wrap a json object
	JSON_CONTENT("json_content"),
	// Canonical ID
	CANONICAL_ID("canonical_id"),
	// Domain ID
	DOMAIN_ID("domain_id"),
	// Account ID
	ACCOUNT_ID("account_id"),
	// User ID
	USER_ID("user_id"),
	//	Array property of the attribute.
	IS_ARRAY("is_array"),
	// RelationOperator
	RELATIONAL_OPERATOR("relational_operator"),
	// LogicalOperator
	LOGICAL_OPERATOR("logical_operator"),
	//Values
	VALUES("values"),
	// Enums
	ENUMS("enums"),
	// Ignore case 
	IGNORE_CASE("ignore_case"),
	// is static
	STATIC("static"),
	//	Class ID
	CLASS_ID("class_id"),
	//	Recursive
	RECRUSIVE("recursive"),
	
	
	;
	
	private String name;
	
	MetaToken(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

}