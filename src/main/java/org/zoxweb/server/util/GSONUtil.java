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
package org.zoxweb.server.util;

import com.google.gson.*;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.stream.JsonWriter;
import org.zoxweb.server.filters.TimestampFilter;
import org.zoxweb.shared.api.APIException;
import org.zoxweb.shared.db.*;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.Const.GNVType;
import org.zoxweb.shared.util.Const.LogicalOperator;
import org.zoxweb.shared.util.ExceptionReason.Reason;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This utility class convert NVEntity Object to json and a json object to an NVEntity.
 * It uses Gson from google 
 * @author mnael
 */
public final class GSONUtil
{

//	public static final DataDecoder<byte[], NVGenericMap> NVGenericMapDecoder = new DataDecoder<byte[], NVGenericMap>() {
//		@Override
//		public NVGenericMap decode(byte[] input)
//		{
//
//		}
//	};



	public static boolean SIMPLE_FORMAT = false;
	private static final Logger log = Logger.getLogger(Const.LOGGER_NAME);

	private static final AtomicLong counter = new AtomicLong();
	
	private final static GSONUtil SINGLETON = new GSONUtil();
	
	private final static Gson DEFAULT_GSON = new GsonBuilder()
			.registerTypeAdapter(NVGenericMap.class, new NVGenericMapSerDeserializer())
			.registerTypeHierarchyAdapter(NVEntity.class, new NVEntitySerDeserializer())
			.registerTypeAdapter(Date.class, new DateSerDeserializer())
											//.registerTypeAdapter(Enum.class, new EnumSerDeserializer()
			.create();


	private final static Gson DEFAULT_GSON_NVGM_PRIMITIVE_AS_STRING = new GsonBuilder()

			.registerTypeAdapter(NVGenericMap.class, new NVGenericMapPrimitiveAsStringSerDeserializer())
			.registerTypeHierarchyAdapter(NVEntity.class, new NVEntityNVGMPrimitiveAsStringSerDeserializer())
			.registerTypeAdapter(Date.class, new DateSerDeserializer())
			//.registerTypeAdapter(Enum.class, new EnumSerDeserializer()
			.create();

	private final static Gson DEFAULT_GSON_PRETTY = new GsonBuilder()
			.registerTypeAdapter(NVGenericMap.class, new NVGenericMapSerDeserializer())
			.registerTypeHierarchyAdapter(NVEntity.class, new NVEntitySerDeserializer())
			.registerTypeAdapter(Date.class, new DateSerDeserializer())
			.setPrettyPrinting()
			//.registerTypeAdapter(Enum.class, new EnumSerDeserializer()
			.create();


	private GsonBuilder builder = null;
	
	
	public static class NVGenericMapSerDeserializer implements JsonSerializer<NVGenericMap>,JsonDeserializer<NVGenericMap>
	{

      @Override
      public JsonElement serialize(NVGenericMap src, Type typeOfSrc,
          JsonSerializationContext context) {
        // TODO Auto-generated method stub

        
        JsonTreeWriter jtw = new JsonTreeWriter();
        try {
          toJSONGenericMap(jtw, src, false, false);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
        return jtw.get();
      }

      @Override
      public NVGenericMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException
	  {
        // TODO Auto-generated method stub
        return fromJSONGenericMap((JsonObject)json, null, Base64Type.DEFAULT, false);
      }
	  
	}

//	public static class MetaTypeInterfaceSerDeserializer
//		implements JsonSerializer<MetaTypeInterface>, JsonDeserializer<MetaTypeInterface>
//	{
//
//		@Override
//		public MetaTypeInterface deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
//			System.out.println("deserialize :" + jsonElement);
//			return null;
//		}
//
//		@Override
//		public JsonElement serialize(MetaTypeInterface metaTypeInterface, Type type, JsonSerializationContext jsonSerializationContext) {
//			System.out.println("serialize :" + metaTypeInterface);
//			return null;
//		}
//	}

	public static class NVGenericMapPrimitiveAsStringSerDeserializer implements JsonSerializer<NVGenericMap>,JsonDeserializer<NVGenericMap>
	{

		@Override
		public JsonElement serialize(NVGenericMap src, Type typeOfSrc,
									 JsonSerializationContext context) {
			// TODO Auto-generated method stub


			JsonTreeWriter jtw = new JsonTreeWriter();
			try {
				toJSONGenericMap(jtw, src, false, false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return jtw.get();
		}

		@Override
		public NVGenericMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException
		{
			// TODO Auto-generated method stub
			return fromJSONGenericMap((JsonObject)json, null, Base64Type.DEFAULT, true);
		}

	}
	
	
	
	public static class DateSerDeserializer
			implements JsonSerializer<Date>,JsonDeserializer<Date>
	{

      @Override
      public JsonElement serialize(Date src, Type typeOfSrc,
          JsonSerializationContext context) {
        // TODO Auto-generated method stub
       
        
        return new JsonPrimitive(DateUtil.DEFAULT_GMT_MILLIS.format(src));
      }

      @Override
      public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException
			{
        // TODO Auto-generated method stub
        JsonPrimitive jp = (JsonPrimitive)json;
        if(jp.isNumber())
          return new Date(jp.getAsLong());
        
        try
		{
			if(jp.isString() && !SUS.isEmpty(jp.getAsString()))
          		return DateUtil.DEFAULT_GMT_MILLIS.parse(jp.getAsString());
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          //e.printStackTrace();
		  log.info(jp + " Exception: " + e);
        }
        return null;
      }
      
	}

	public static class EnumSerDeserializer
			implements JsonSerializer<Enum<? extends Enum<?>>>,JsonDeserializer<Enum<? extends Enum<?>>>
	{

		@Override
		public JsonElement serialize(Enum <?>src, Type typeOfSrc, JsonSerializationContext context) {
			// TODO Auto-generated method stub

			return new JsonPrimitive(src.name());
		}

		@Override
		public Enum<?> deserialize(JsonElement json, Type typeOf, JsonDeserializationContext context)
				throws JsonParseException
		{

			// TODO Auto-generated method stub
			JsonPrimitive jp = (JsonPrimitive)json;
			if(jp.isString())
			{
				try {
					Enum<?>[] enums = (Enum<?>[]) Class.forName(typeOf.getTypeName()).getEnumConstants();
					return SharedUtil.lookupEnum(jp.getAsString(), enums);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return null;
		}

	}


	public static long getJSONDefaultCount()
	{
		return counter.get();
	}

	public static <T> T fromJSONDefault(byte[] json, Class<T> classOfT)
	{
		counter.incrementAndGet();
		return DEFAULT_GSON.fromJson(SharedStringUtil.toString(json), classOfT);
	}

	public static <T> T fromJSONDefault(String json, Class<T> classOfT)
	{
		return fromJSONDefault(json, classOfT, false);
	}

	public static <T> T fromJSONDefault(String json, Class<T> classOfT, boolean primitiveAsString)
	{
		counter.incrementAndGet();
		T ret = primitiveAsString ? DEFAULT_GSON_NVGM_PRIMITIVE_AS_STRING.fromJson(json, classOfT) : DEFAULT_GSON.fromJson(json, classOfT);
		return ret;
	}


	public static String toJSONDefault(Object o)
	{
		return toJSONDefault(o, false);
	}

	public static String toJSONDefault(Object o, boolean pretty)
	{
		counter.incrementAndGet();
		if(pretty)
			return DEFAULT_GSON_PRETTY.toJson(o);

		return DEFAULT_GSON.toJson(o);
	}

	

	public static class NVEntitySerDeserializer implements JsonSerializer<NVEntity>,JsonDeserializer<NVEntity>
	{

		@Override
		public JsonElement serialize(NVEntity src, Type typeOfSrc, JsonSerializationContext context)
		{
			JsonTreeWriter jtw = new JsonTreeWriter();
			try
			{
				toJSON(jtw, src.getClass(), src, false, true, null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return jtw.get();
		}

		@Override
		public NVEntity deserialize(JsonElement json, Type typeOfT,	JsonDeserializationContext context)
				throws JsonParseException
		{
			// TODO Auto-generated method stub
			return fromJSON((JsonObject)json, typeOfT, Base64Type.DEFAULT, false);
		}

	}


	public static class NVEntityNVGMPrimitiveAsStringSerDeserializer implements JsonSerializer<NVEntity>,JsonDeserializer<NVEntity>
	{

		@Override
		public JsonElement serialize(NVEntity src, Type typeOfSrc, JsonSerializationContext context)
		{
			JsonTreeWriter jtw = new JsonTreeWriter();
			try
			{
				toJSON(jtw, src.getClass(), src, false, true, null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return jtw.get();
		}

		@Override
		public NVEntity deserialize(JsonElement json, Type typeOfT,	JsonDeserializationContext context)
				throws JsonParseException
		{
			// TODO Auto-generated method stub
			return fromJSON((JsonObject)json, typeOfT, Base64Type.DEFAULT, true);
		}

	}



	
	private GSONUtil()
    {
		builder = new GsonBuilder();
		builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
		builder.setPrettyPrinting();
		log.info("Created");
	}
	
	public static Gson create(boolean pretty)
    {
		if (pretty)
		{
			return SINGLETON.builder.create();
		}
		else
        {
			return new Gson();
		}
	}


	public static String toJSONSimple(Object obj)
			throws IOException
	{
		if(obj instanceof NVEntity)
		{
			return toJSON((NVEntity) obj, true, false, false);
		}

		return toJSONDefault(obj);
	}

	public static String toJSON(NVEntity nve, boolean indent) 
        throws IOException
    {
		return toJSON(nve, indent, true, true);
	}
	
	
	public static String toJSON(NVEntity nve, boolean indent, boolean printNull, boolean printClassType) 
	        throws IOException
	{
		return toJSON(nve, indent, printNull, printClassType, null);
	}
	
	public static List<NVEntity> fromJSONArray(String json, Base64Type b64Type)
	{
		List<NVEntity> ret = new ArrayList<NVEntity>();
		JsonElement je =  JsonParser.parseString(json);
		
		if (je instanceof JsonArray)
		{
			JsonArray ja = (JsonArray) je;
			for (int i = 0; i < ja.size(); i++)
			{
				JsonObject jo = (JsonObject) ja.get(i);
				ret.add(fromJSON(jo, null, b64Type, false));
			}
			
		}
		
		return ret;
	}

	public static List<NVGenericMap> fromJSONGenericMapArray(String json, Base64Type b64Type)
	{
		List<NVGenericMap> ret = new ArrayList<NVGenericMap>();
		JsonElement je =  JsonParser.parseString(json);
		if (je instanceof JsonArray)
		{
			JsonArray ja = (JsonArray) je;
			for (int i = 0; i < ja.size(); i++)
			{
				JsonObject jo = (JsonObject) ja.get(i);
				ret.add(fromJSONGenericMap(jo, null, b64Type, false));
			}
		}
		return ret;
	}
	
	
	public static String toJSONArray(List<NVEntity> list, boolean indent, boolean printNull, Base64Type b64Type)
		throws IOException
	{
		return toJSONArray(list.toArray(new NVEntity[list.size()]), indent, printNull, b64Type);
	}
	
	public static String toJSONArray(NVEntity[] nves, boolean indent, boolean printNull, Base64Type b64Type)
		 throws IOException
	{
		StringWriter sw = new StringWriter();
		JsonWriter writer = new JsonWriter(sw);
		writer.setSerializeNulls(true);
		writer.setHtmlSafe(true);
		
		if (indent)
			writer.setIndent("  ");
		else
			writer.setIndent("");
		writer.beginArray();
		for (NVEntity nve: nves)
		{
			if (nve != null)
			{
				toJSON(writer, nve.getClass(), nve, printNull, true, b64Type);
			}
		}
		writer.endArray();
		writer.close();
		return sw.toString();
	}
	
	public static String toJSON(NVEntity nve, boolean indent, boolean printNull, boolean printClassType, Base64Type b64Type) 
        throws IOException
    {
		StringWriter sw = new StringWriter();
		JsonWriter writer = new JsonWriter(sw);
		writer.setSerializeNulls(true);
		writer.setHtmlSafe(true);
		
		if (indent)
			writer.setIndent("  ");
		else
			writer.setIndent("");
		
		toJSON(writer, nve.getClass(), nve, printNull, printClassType, b64Type);
		
		writer.close();
		
		return sw.toString();
	}
	
	
	public static String toJSONWrapper(String wrapName, 
									   NVEntity nve, 
									   boolean indent, 
									   boolean printNull, 
									   boolean printClassType,
									   Base64Type b64Type) 
        throws IOException
    {
		StringWriter sw = new StringWriter();
		JsonWriter writer = new JsonWriter(sw);
		writer.setSerializeNulls(true);
		writer.setHtmlSafe(true);
		
		if (indent)
			writer.setIndent("  ");
		else
			writer.setIndent("");
		
		writer.beginObject();
		writer.name(wrapName);
		toJSON(writer, nve.getClass(), nve, printNull, printClassType, b64Type);
		writer.endObject();
		writer.close();
		
		return sw.toString();
	}
	
	public static byte[] toObjectHash(String mdAlgo, Object obj) throws NoSuchAlgorithmException
	{
		return toJSONHash(mdAlgo, create(false).toJson(obj));
	}
	
	public static byte[] toNVEntityHash(String mdAlgo, NVEntity nve, Base64Type b64Type) throws NoSuchAlgorithmException, IOException
	{
		return toJSONHash(mdAlgo, toJSON(nve, false, false, true, b64Type));
	}
	
	
	public static byte[] toJSONHash(String mdAlgo, String json) throws NoSuchAlgorithmException
	{
		MessageDigest md = MessageDigest.getInstance(mdAlgo);
		return md.digest(SharedStringUtil.getBytes(json));
	}
	
	public static QueryRequest fromQueryRequest(String json)
    {
		JsonElement je = JsonParser.parseString(json);
		QueryRequest ret = null;

		if (je instanceof JsonObject)
		{
			ret = new QueryRequest();
			JsonObject jo = (JsonObject) je;
			ret.setCanonicalID(jo.get(MetaToken.CANONICAL_ID.getName()).getAsString());
			JsonElement batchSize = jo.get("batch_size");

			if (batchSize != null)
			{
				ret.setBatchSize(batchSize.getAsInt());
			}
			
			JsonArray jaFNs = (JsonArray) jo.get("field_names");

			if (jaFNs != null)
			{
				List<String> fieldNames = new ArrayList<String>();
				
				for (int i = 0; i < jaFNs.size(); i++)
				{
					fieldNames.add(jaFNs.get(i).getAsString());
				}
				
				ret.setFieldNames(fieldNames);
			}
			
			JsonArray jaQuery = (JsonArray) jo.get("query");
			if (jaQuery != null)
			{
				List<QueryMarker> qms = new ArrayList<QueryMarker>();
				for (int i = 0; i < jaQuery.size(); i++)
				{
					// get the query marker
					JsonObject joQM = (JsonObject) jaQuery.get(i);
					QueryMarker qm = null;
					
					JsonPrimitive lo = (JsonPrimitive) joQM.get(MetaToken.LOGICAL_OPERATOR.getName());
					
					if (lo != null)
					{
						qm = Const.LogicalOperator.valueOf(lo.getAsString());
					}
					else
                    {
						Const.RelationalOperator ro = null;
						
						JsonPrimitive jpRO = (JsonPrimitive) joQM.get(MetaToken.RELATIONAL_OPERATOR.getName());
						
						if (jpRO != null)
						{
							ro = Const.RelationalOperator.valueOf(jpRO.getAsString());
						}
						
						String name = null;
						JsonElement value = null;
						
						Set<Map.Entry<String, JsonElement>> allParams = joQM.entrySet();
						
						for (Map.Entry<String, JsonElement> e : allParams)
						{
							if (!e.getKey().equals(MetaToken.RELATIONAL_OPERATOR.getName()))
							{
								name = e.getKey();
								value = e.getValue();
								break;
							}
						}
						
						// try to guess the type
						if (value.isJsonPrimitive())
						{
							JsonPrimitive jp = (JsonPrimitive) value;
							
							if (jp.isString())
							{
								qm = new QueryMatchString(ro, jp.getAsString(), name);
							}
							else if (jp.isNumber())
							{
								qm = new QueryMatchLong(ro, jp.getAsLong(), name);
							}
						}
					}
					
					if (qm != null)
					{
						qms.add(qm);
					}
				}
				
				ret.setQuery(qms);
			}
		}
		
		return ret;
	}
	
	private static String toJSONValue(Enum <?> e)
    {
		if (e == null)
		{
			return null;
		}
		
		return e.name();
	}
	
	@SuppressWarnings("unchecked")
	private static JsonWriter toJSON(JsonWriter writer, Class<? extends NVEntity> clazz, NVEntity nve, boolean printNull, boolean printClassType, Base64Type b64Type) 
        throws IOException
    {

		NVConfigEntity nvce = (NVConfigEntity) nve.getNVConfig();
		
		writer.beginObject();
		
		if (clazz!= null  && clazz != nve.getClass() || (clazz != null && printClassType))
		{
			writer.name(MetaToken.CLASS_TYPE.getName()).value(nve.getClass().getName());
		}
		else if (nvce.getMetaType().isInterface() || Modifier.isAbstract(nvce.getMetaType().getModifiers()))
		{
			writer.name(MetaToken.CLASS_TYPE.getName()).value(nve.getClass().getName());
		}
	
		List <NVConfig> attributes = nvce.getAttributes();
		
		for (int i = 0; i < attributes.size(); i++)
		{
			NVConfig nvc = attributes.get(i);
			//Class<?> type = nvc.getMetaType();
			
			if (!printNull)
			{
				Object tempObj = nve.lookupValue(nvc);
				if (tempObj == null || 
					(tempObj instanceof List && ((List<?>)tempObj).size() == 0) ||
					(tempObj instanceof Map && ((Map<?,?>)tempObj).size() == 0))
				{
					continue;
				}
			}
			
			if (nvc.isArray())
			{
				writer.name(nvc.getName());
				
				if (byte[].class.equals(nvc.getMetaType()))
				{
					byte[] value = nve.lookupValue(nvc);				
					writer.value(value != null ?  new String(SharedBase64.encode(b64Type, value)) : null);
				}
				else
                {
					writer.beginArray();
					
					if (nvc.isEnum())
					{
						List<Enum<?>> eAll = nve.lookupValue(nvc);
						
						for (Enum<?> e: eAll)
						{
							writer.value(toJSONValue(e));
						}
					}
					else if (nvc.getMetaTypeBase() == String.class)
					{
						ArrayValues<GetNameValue<String>> tempArray = (ArrayValues<GetNameValue<String>>) nve.lookup(nvc.getName());
						
						for (GetNameValue<String> nvp : tempArray.values())
						{
							toJSON(writer, nvp, true, printNull);
						}
//						
//						if (!nvc.isUnique())
//						{
//							List<NVPair> all = nve.lookupValue(nvc);
//							for ( NVPair nvp : all)
//							{
//								toJSON( writer, nvp, true, printNull);
//							}
//						}
//						else
//						{
//							NVPairGetNameMap nvpm = (NVPairGetNameMap) nve.lookup(nvc.getName());
//							for (NVPair nvp : nvpm.getValue().values())
//							{
//								toJSON( writer, nvp, true, printNull);
//							}
//						}
					}
					else if (nvc.getMetaTypeBase() == Long.class)
					{
						List<Long> values = nve.lookupValue(nvc);
						
						for (long v : values)
						{
							writer.value(v);
						}
					}

					else if (nvc.getMetaTypeBase() == Integer.class)
					{
						List<Integer> values = nve.lookupValue(nvc);
						
						for (Integer v : values)
						{
							writer.value(v);
						}
					}
					else if (nvc.getMetaTypeBase() == Float.class)
					{
						List<Float> values = nve.lookupValue(nvc);
						
						for (Float v : values)
						{
							writer.value(v);
						}
					}
					else if (nvc.getMetaTypeBase() == Double.class)
					{
						List<Double> values = nve.lookupValue(nvc);
						
						for (Double v : values)
						{
							writer.value(v);
						}
					}
					else if (nvc.getMetaTypeBase() == Boolean.class)
					{
						List<Boolean> values = nve.lookupValue(nvc);
						
						for (boolean b : values)
						{
							writer.value(b);
						}
					}
					else if (nvc instanceof NVConfigEntity)
					{
						ArrayValues<NVEntity> tempArray = (ArrayValues<NVEntity>) nve.lookup(nvc.getName());
						
						for (NVEntity value : tempArray.values())
						{
							toJSON( writer, (Class<? extends NVEntity>) nvc.getMetaTypeBase(), value, printNull, printClassType, b64Type);
						}						
						
//						if (!nvc.isUnique())
//						{
//							List<NVEntity> values = nve.lookupValue(nvc);
//							for ( NVEntity value : values)
//							{
//								toJSON( writer, (Class<? extends NVEntity>) nvc.getMetaTypeBase(), value, printNull);
//							}
//						}
//						else
//						{
//							Map<GetName, NVEntity> values = nve.lookupValue(nvc);
//							for ( NVEntity value : values.values())
//							{
//								toJSON( writer, (Class<? extends NVEntity>) nvc.getMetaTypeBase(), value, printNull);
//							}
//						}
					}
					else if (nvc.getMetaTypeBase() == Date.class)
					{
						List<Long> values = nve.lookupValue(nvc);
						
						for (long v : values)
						{
							writer.value(v);
						}
					}
					else if (nvc.getMetaTypeBase() == BigDecimal.class)
					{
						List<BigDecimal> values = nve.lookupValue(nvc);
						
						for (BigDecimal v : values)
						{
							writer.value(v);
						}
					}
					
					writer.endArray();
				}
			}
			else
            {
				if (nvc.isEnum())
				{
					if (nvc.getMetaTypeBase().isAssignableFrom(DynamicEnumMap.class))
					{
						writer.name(nvc.getName()).value((String) nve.lookupValue(nvc));
					}
					else
                    {
						Enum<?> e = nve.lookupValue(nvc);
						writer.name( nvc.getName()).value( toJSONValue(e));
					}
				}
				else if (nvc.getMetaTypeBase() == String.class)
				{
					toJSON(writer, (NVPair)nve.lookup(nvc.getName()), false, printNull);
					//writer.name( nvc.getName()).value((String)nve.lookupValue(nvc));
				}
				else if (nvc.getMetaTypeBase() == Long.class )
				{
					if ((long)nve.lookupValue(nvc) != 0)
					{
						writer.name( nvc.getName()).value((long) nve.lookupValue(nvc));
					}
				}
				else if (nvc.getMetaTypeBase() == Integer.class)
				{
					if ((int) nve.lookupValue(nvc) != 0)
					{
						writer.name( nvc.getName()).value((int) nve.lookupValue(nvc));
					}
				}
				else if (nvc.getMetaTypeBase() == Double.class)
				{
					if ((Double) nve.lookupValue(nvc) != 0)
					{
						writer.name( nvc.getName()).value((double) nve.lookupValue(nvc));
					}
				}
				else if (nvc.getMetaTypeBase() == Float.class)
				{
					if ((Float)nve.lookupValue(nvc) != 0)
					{
						writer.name( nvc.getName()).value((float) nve.lookupValue(nvc));
					}
				}
				else if (nvc.getMetaTypeBase() == Boolean.class) {
					if (printNull || (boolean) nve.lookupValue(nvc))
						writer.name(nvc.getName()).value((boolean) nve.lookupValue(nvc));
				}
				else if (nvc.getMetaTypeBase() == Date.class)
				{

					if (nve.lookupValue(nvc) instanceof Date)
					{
						writer.name(nvc.getName()).value(DateUtil.DEFAULT_GMT_MILLIS.format(nve.lookupValue(nvc)));
					}
					else if ((long) nve.lookupValue(nvc) != 0)
					{
						//writer.name( nvc.getName()).value((long)nve.lookupValue(nvc));
						writer.name(nvc.getName()).value(DateUtil.DEFAULT_GMT_MILLIS.format(new Date((long)nve.lookupValue(nvc))));
					}
				}
				else if (nvc.getMetaTypeBase() == BigDecimal.class)
				{
					if (nve.lookupValue(nvc) != null)
					{
						writer.name(nvc.getName()).value((BigDecimal) nve.lookupValue(nvc));
					}
				}
				else if (nvc.getMetaTypeBase() == Number.class)
				{
					if (nve.lookupValue(nvc) != null)
					{
						writer.name(nvc.getName()).value((Number) nve.lookupValue(nvc));
					}
				}
				else if (nvc instanceof NVConfigEntity)
				{
					NVEntity tempNVE = nve.lookupValue(nvc);
					// we need to write the class type if the current object is derived from nvc.getClass()
					// this is we important to accurately rebuild the object
					
					if (tempNVE != null)
					{
						writer.name(nvc.getName());
//						if (tempNVE instanceof GetNVGenericMap)
//						{
//							toJSONGenericMap(writer, ((GetNVGenericMap) tempNVE).getProperties(), printNull, printClassType);
//						}
//						else
						toJSON( writer,  (Class<? extends NVEntity>) ((NVConfigEntity) nvc).getMetaType(), (NVEntity)nve.lookupValue(nvc), printNull, printClassType, b64Type);
					}
					else if (printNull)
					{
						writer.name(nvc.getName());
						writer.nullValue();
					}
				}
				else if (NVGenericMap.class.equals(nvc.getMetaTypeBase()))
				{
					writer.name(nvc.getName());
					toJSONGenericMap(writer, (NVGenericMap)nve.lookup(nvc),  printNull, printClassType);
				}
				else if (NVStringList.class.equals(nvc.getMetaTypeBase()))
				{
				  writer.name(nvc.getName());
				  writer.beginArray();
				  NVStringList tempNVSL = nve.lookup(nvc);
				  for (String str : tempNVSL.getValue())
				  {
				    writer.value(str);
				  }
				  writer.endArray();
				}
				else if (NVStringSet.class.equals(nvc.getMetaTypeBase()))
				{
					writer.name(nvc.getName());
					writer.beginArray();
					NVStringSet tempNVSL = nve.lookup(nvc);
					for (String str : tempNVSL.getValue())
					{
						writer.value(str);
					}
					writer.endArray();
				}
			}
		}
	
		writer.endObject();
		
		return writer;
	}

	public static String toJSONGenericMap(NVGenericMap nvgm, boolean indent, boolean printNull, boolean printClassType) throws IOException
	{
		StringWriter sw = new StringWriter();
		JsonWriter writer = new JsonWriter(sw);
		writer.setSerializeNulls(true);
		writer.setHtmlSafe(true);
		if (indent)
			writer.setIndent("  ");
		else
			writer.setIndent("");
		toJSONGenericMap(writer, nvgm,  printNull, printClassType);
		writer.close();
		return sw.toString();
	}
	
	private static JsonWriter toJSONGenericMap(JsonWriter writer, NVGenericMap nvgm,  boolean printNull, boolean printClassType) throws IOException
	{
		writer.beginObject();
		GetNameValue<?>[] values = nvgm.values();
		for (GetNameValue<?> gnv : values)
		{
			toJSONGenericMap(writer, gnv, printNull, printClassType);
		}
		writer.endObject();
		return writer;
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static JsonWriter toJSONGenericMap(JsonWriter writer, GetNameValue<?> gnv,  boolean printNull, boolean printClassType) throws IOException
	{if (gnv instanceof NVPair &&
				((NVPair) gnv).getValueFilter() != null &&
				((NVPair) gnv).getValueFilter() != FilterType.CLEAR)
			printNull = true;

		if (gnv.getValue() == null && !printNull)
			return writer;

		String name = gnv.getName();

		if (gnv instanceof NVBoolean)
		{
			if (!printNull && !(Boolean)gnv.getValue())
			{
				return writer;
			}
			writer.name(name).value((Boolean)gnv.getValue());
		}
		else if (gnv instanceof NVEnum)
		{

			Enum<?> enumValue = (Enum<?>) gnv.getValue();
			if (enumValue == null)
				return writer;

			if (SIMPLE_FORMAT)
				writer.name(name).value(((Enum<?>)gnv.getValue()).name());
			else
			{
				writer.name(name).beginObject()
						.name(MetaToken.VALUE.getName()).value(enumValue.name())
						.name(MetaToken.ENUM_TYPE.getName()).value(enumValue.getClass().getName())
						.endObject();
			}
		}
		else if (gnv instanceof  NVInt || gnv instanceof NVLong || gnv instanceof NVFloat || gnv instanceof NVDouble)
		{
			Number value = (Number) gnv.getValue();
			if (!printNull && value.doubleValue()==0)
			{
				return writer;
			}
			writer.name(name).value((Number)gnv.getValue());
		}
		else if (gnv instanceof NVPair &&
				((NVPair)gnv).getValueFilter() != null &&
				((NVPair)gnv).getValueFilter() != FilterType.CLEAR)
		{
			//writer.name(name).value((String) gnv.getValue());
			writer.name(name).beginObject()
					.name(MetaToken.VALUE.getName()).value((String)gnv.getValue())
					.name(MetaToken.VALUE_FILTER.getName()).value(((NVPair)gnv).getValueFilter().toCanonicalID())
					.endObject();
			//toJSON(writer, (NVPair)gnv, true, printNull);
		}
		else if (gnv.getValue() instanceof String)
		{
			writer.name(name).value((String)gnv.getValue());
		}
		else if (gnv instanceof NVBlob)
		{
			writer.name(name).value(SharedBase64.encodeWrappedAsString((byte[]) gnv.getValue()));
		}
		else if (gnv instanceof NVEntityReference)
		{
			writer.name(name);
			toJSON(writer, ((NVEntity)gnv.getValue()).getClass(), (NVEntity)gnv.getValue(), printNull, printClassType, Base64Type.URL);
		}
		else if (gnv instanceof NVGenericMap)
		{
			writer.name(name);
			toJSONGenericMap(writer, (NVGenericMap)gnv,  printNull, printClassType);
		}
		else if (gnv instanceof ArrayValues)
		{
			writer.name(gnv.getName());
			writer.beginArray();
			ArrayValues<?> av = (ArrayValues<?>) gnv;
			for (Object localGNV : av.values())
			{
				if(localGNV instanceof GetNameValue)
				{

					if (((GetNameValue) localGNV).getValue() instanceof String)
					{
						toJSON(writer, (GetNameValue<String>)localGNV, true, printNull);
					}
					else
					{
						writer.beginObject();
						toJSONGenericMap(writer, (GetNameValue<?>) localGNV, printNull, printClassType);
						writer.endObject();
					}
				}
				if (localGNV instanceof NVEntity)
				{
					//writer.beginObject();


					toJSON(writer,  ((NVEntity)localGNV).getClass(), (NVEntity) localGNV, printNull, printClassType, Base64Type.URL);

					//writer.endObject();
				}
				else
				{
					if (localGNV instanceof Number)
					{
						writer.value((Number)localGNV);
					}
					else if (localGNV instanceof Boolean)
					{
						writer.value((Boolean) localGNV);
					}

					//writer.value(localGNV);
				}
			}
			writer.endArray();
		}
		else if (gnv instanceof ValueUnit)
		{
			if (SIMPLE_FORMAT)
			{
				writer.name(gnv.getName());
				writer.value(gnv.getValue() + ((ValueUnit) gnv).getSeparator() + ((ValueUnit) gnv).getUnit());
			}
			else
			{
				writer.name(name).beginObject();
				if(gnv.getValue() instanceof Number)
					writer.name(MetaToken.VALUE.getName()).value((Number)gnv.getValue());
				else if(gnv.getValue() instanceof String)
					writer.name(MetaToken.VALUE.getName()).value((String)gnv.getValue());
				else if(gnv.getValue() instanceof Boolean)
					writer.name(MetaToken.VALUE.getName()).value((Boolean)gnv.getValue());
				else if(gnv.getValue() instanceof Enum)
					writer.name(MetaToken.VALUE.getName()).value(((Enum)gnv.getValue()).name());
				else
					writer.name(MetaToken.VALUE.getName()).value(""+gnv.getValue());
				writer.name(MetaToken.UNIT.getName()).value(""+ ((ValueUnit<?, ?>) gnv).getUnit());
				writer.endObject();
			}
		}

		//else if (gnv instanceof NVIntList || gnv instanceof NVLongList || gnv instanceof NVFloatList || gnv instanceof NVDoubleList)
		else if (MetaToken.isPrimitiveArray((NVBase<?>) gnv))
		{
			writer.name(gnv.getName());
			writer.beginArray();
			List<?> values = (List<?>) gnv.getValue();
			for (Object val : values)
			{
				if (val != null) {
					if (val instanceof Number)
						writer.value((Number) val);
					else if (val instanceof Enum) {
						writer.value(((Enum) val).name());
					}
					else if (val instanceof String)
						writer.value((String) val);
				}
			}
			writer.endArray();
		}
		else if (gnv instanceof NVGenericMapList)
		{
			writer.name(gnv.getName());
			writer.beginArray();
			List<NVGenericMap> values = (List<NVGenericMap>) gnv.getValue();

			for (NVGenericMap val : values)
			{
				toJSONGenericMap(writer, val, printNull, printClassType);
			}

			writer.endArray();
		}

		
		
	
		
		return writer;
	}




	public static NVGenericMap fromJSONGenericMap(byte[] data)
	{
		return fromJSONGenericMap(data, null, Base64Type.URL);
	}

	public static NVGenericMap fromJSONGenericMap(byte[] data, NVConfigEntity nvce, Base64Type btype)
	{
		return fromJSONGenericMap(SharedStringUtil.toString(data), nvce, btype);
	}

	public static NVGenericMap fromJSONGenericMap(byte[] data, NVConfigEntity nvce, Base64Type btype, boolean nvgPrimitiveAsString)
	{
		return fromJSONGenericMap(SharedStringUtil.toString(data), nvce, btype, nvgPrimitiveAsString);
	}

	
	public static NVGenericMap fromJSONGenericMap(String json, NVConfigEntity nvce, Base64Type btype)
	{
		return fromJSONGenericMap(json, nvce, btype, false);
	}


	public static NVGenericMap fromJSONGenericMap(String json, NVConfigEntity nvce, Base64Type btype, boolean nvgPrimitiveAsString)
	{
		JsonElement je = JsonParser.parseString(json);

		if (je instanceof JsonObject)
		{
			return fromJSONGenericMap((JsonObject)je, nvce, btype, nvgPrimitiveAsString);
		}

		return null;
	}
	
	private static NVGenericMap fromJSONGenericMap(JsonObject je, NVConfigEntity nvce, Base64Type b64Type, boolean nvgmPrimitiveAsString)
			throws APIException, AccessException
	{
			NVGenericMap ret = new NVGenericMap();

			Iterator<Map.Entry<String, JsonElement>> iterator = je.entrySet().iterator();
			while(iterator.hasNext())
			{
				Map.Entry<String, JsonElement> element = iterator.next();
				if (element.getKey().equals(MetaToken.CLASS_TYPE.getName()))
					continue;
				JsonElement jne = element.getValue();
				if (jne.isJsonArray())
				{
					JsonArray ja = jne.getAsJsonArray();
					NVBase<?> nvb = guessNVBaseArray(ja);
					if (nvb != null)
					{
						nvb.setName(element.getKey());
						ret.add(nvb);
						for (int i = 0; i < ja.size(); i++)
						{
							if (nvb instanceof NVPairList)
							{
								((NVPairList)nvb).add(toNVPair((JsonObject) ja.get(i)));
							}
							else if (nvb instanceof NVIntList)
							{
								((NVIntList)nvb).getValue().add(ja.get(i).getAsInt());
							}
							else if (nvb instanceof NVLongList)
							{
								((NVLongList)nvb).getValue().add(ja.get(i).getAsLong());
							}
							else if (nvb instanceof NVFloatList)
							{
								((NVFloatList)nvb).getValue().add(ja.get(i).getAsFloat());
							}
							else if (nvb instanceof NVDoubleList)
							{
								((NVDoubleList)nvb).getValue().add(ja.get(i).getAsDouble());
							}
							else if (nvb instanceof NVStringList)
							{
								((NVStringList)nvb).getValue().add(ja.get(i).getAsString());
							}
							else if (nvb instanceof NVStringSet)
							{
								((NVStringSet)nvb).getValue().add(ja.get(i).getAsString());
							}
							else if (nvb instanceof NVGenericMapList)
							{
								((NVGenericMapList)nvb).add(fromJSONGenericMap((JsonObject)ja.get(i), null, b64Type, nvgmPrimitiveAsString));
							}
						}
					}
					else
                    {
                        log.info("Array guess failed " + jne);
                    }
					
					
				}
				else if (jne.isJsonPrimitive())
				{
					ret.add(guessPrimitive(element.getKey(), nvce != null ? nvce.lookup(element.getKey()) : null, (JsonPrimitive) jne, nvgmPrimitiveAsString));
				}
				else if (jne.isJsonObject())
				{
					GetNameValue<?> nvpMaybe = guessNVPairEnum(element.getKey(), (JsonObject) jne);
					if (nvpMaybe != null)
					{
						ret.add(nvpMaybe);
					}
					else {
						try {
							ret.add(new NVEntityReference(element.getKey(),
									(NVEntity) fromJSON(jne.getAsJsonObject(), null, b64Type, nvgmPrimitiveAsString)));
						} catch (Exception e) {
							NVGenericMap toAdd = fromJSONGenericMap(jne.getAsJsonObject(), null, b64Type, nvgmPrimitiveAsString);
							toAdd.setName(element.getKey());
							ret.add(toAdd);
						}
					}
				}
			}
			
			return ret;
			
	
		
	}
	
	private static NVBase<?> guessNVBaseArray(JsonArray ja)
	{
		NVBase<?> ret = null;
		
		GNVType guess = null;
		for (int i=0; i < ja.size(); i++)
		{	
			JsonElement je = ja.get(i);
			
			if (je.isJsonObject())
			{
				// could an NVEntity or NVPairList or NVGenericMap
				// nvpair
				JsonObject jo  = je.getAsJsonObject();
				if (jo.size() == 1)
				{
					
					if (ret == null)
					{
						return new NVPairList(null, new ArrayList<NVPair>());
					}
				}
				
				if (jo.size()>1)
				{
					return new NVGenericMapList();
					
				}
			}
			else if (je.isJsonPrimitive())
			{
				if (je.getAsJsonPrimitive().isString())
				{
					// must be fixed
					//break;
					return  new NVStringList();
				}
				
				GNVType gnv = GNVType.toGNVType(je.getAsNumber());
				if (gnv != null)
				{
					if (guess == null)
					{
						guess = gnv;
					}
					else
					{
						switch(gnv)
						{
						
						case NVDOUBLE:
							if (guess == GNVType.NVINT || guess == GNVType.NVLONG || guess == GNVType.NVFLOAT)
							{
								guess = gnv;
							}
							break;
						case NVFLOAT:
							if (guess == GNVType.NVINT || guess == GNVType.NVLONG)
							{
								guess = gnv;
							}
							break;
						case NVINT:
							break;
						case NVLONG:
							if (guess == GNVType.NVINT)
							{
								guess = gnv;
							}
							break;
						default:
							break;
						
						}
					}
				}
			}
		}
		
		if (ret == null && guess != null)
		{
			switch(guess)
			{
			
			case NVDOUBLE:
				ret = new NVDoubleList(null, new ArrayList<Double>());
				break;
			case NVFLOAT:
				ret = new NVFloatList(null, new ArrayList<Float>());
				break;
			case NVINT:
				ret = new NVIntList(null, new ArrayList<Integer>());
				break;
			case NVLONG:
				ret = new NVLongList(null, new ArrayList<Long>());
				break;
			default:
				break;
			
			}
		}
		
		
		return ret;
	}

	private static GetNameValue<?> guessNVPairEnum(String name, JsonObject jo)
	{

		JsonElement joType = jo.get(MetaToken.VALUE_FILTER.getName());
		if (joType != null)
		{
			// we have potential NVPAIR
			switch (jo.size())
			{
				case 1:
				{

					FilterType vf = SharedUtil.lookupEnum(joType.getAsString(), FilterType.values());
					if(vf != null)
						return new NVPair(name, null, vf);
				}
				break;
				case 2:
				{
					JsonElement jeValue = jo.get(MetaToken.VALUE.getName());
					if(jeValue != null)
					{
						FilterType vf = SharedUtil.lookupEnum(joType.getAsString(), FilterType.values());

						if(vf != null)
							return new NVPair(name, jeValue.isJsonNull() ? null : jeValue.getAsString(), vf);
					}
				}
				break;
			}
		}
		joType = jo.get(MetaToken.ENUM_TYPE.getName());
		if (joType != null)
		{
			// we have a potential enum type
			switch (jo.size())
			{
				case 2:
				{
					JsonElement jeValue = jo.get(MetaToken.VALUE.getName());
					if(jeValue != null && !jeValue.isJsonNull())
					{
						try {
							Class<Enum<?>> enumType = (Class<Enum<?>>) Class.forName(joType.getAsString());
							if (enumType.isEnum())
							{
								Enum<?> enumValue = SharedUtil.lookupEnum(jeValue.getAsString(), enumType.getEnumConstants());
								return new NVEnum(name, enumValue);
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				break;
			}

		}



		return null;

	}
	
	private static NVBase<?> guessPrimitive(String name, NVConfig nvc, JsonPrimitive jp, boolean stringAsValue)
	{

		GNVType gnvType = nvc != null ? GNVType.toGNVType(nvc) : null;
		
		if (gnvType == null)
		{
			GNVTypeName tn = GNVType.toGNVTypeName(':', name);
			if (tn != null)
			{
				gnvType = tn.getType();
				name = tn.getName();
			}
		}
		
		if (gnvType != null)
		{
			switch(gnvType)
			{
			case NVBLOB:
				try
				{
					byte value[] = SharedBase64.decode(Base64Type.URL, jp.getAsString());
					return new NVBlob(name, value);
				}
				catch(Exception e)
				{
					
				}
				break;
			case NVBOOLEAN:
				return new NVBoolean(name, jp.getAsBoolean());
			case NVDOUBLE:
				return new NVDouble(name, jp.getAsDouble());
			case NVFLOAT:
				return new NVFloat(name, jp.getAsFloat());
			case NVINT:
				return new NVInt(name, jp.getAsInt());
			case NVLONG:
				return new NVLong(name, jp.getAsLong());
			
			}
		}
		
		
		
		
		if (jp.isBoolean())
		{
			return new NVBoolean(name, jp.getAsBoolean());
		}
		else if (jp.isNumber())
		{
			// if there is no dots it should be a 
			//if (jp.getAsString().indexOf(".") == -1)

			try
			{
				Number number = SharedUtil.parseNumber(jp.getAsString());
				return SharedUtil.numberToNVBase(name, number);

			}
			catch(NumberFormatException e)
			{
				e.printStackTrace();
			}


			try
			{
				return new NVDouble(name, jp.getAsDouble());
			}
			catch(NumberFormatException e)
			{
				e.printStackTrace();
			}

		}
		else if (jp.isString())
		{
			try
			{
				byte value[] = SharedBase64.decodeWrappedAsString(jp.getAsString());
				return new NVBlob(name, value);
			}
			catch(Exception e)
			{
				
			}

			// if true we will not guess the string value
			// like parsing a long or guessing a long value as date
			//
			if(stringAsValue)
			{
				return new NVPair(name, jp.getAsString());
			}


			try
			{
				Long.parseLong(jp.getAsString());
			}
			catch(Exception e)
			{
				if (TimestampFilter.SINGLETON.isValid(jp.getAsString()))
				{
					try
					{
						return new NVLong(name, TimestampFilter.SINGLETON.validate(jp.getAsString()));
					}
					catch (Exception ex){}
				}
			}
			// this the last step we have a string
			return new NVPair(name, jp.getAsString());
		}
		
		return null;
		
	}
	
	private static JsonWriter toJSON(JsonWriter writer, GetNameValue<String> nvp, boolean isObject, boolean printNull)
        throws IOException
    {

		if (nvp != null && (printNull || nvp.getValue() != null))
		{
			String referenceID = null;
			ValueFilter<String, String> vf = null;
			
			if (nvp instanceof NVPair)
			{
				 referenceID = ((NVPair)nvp).getReferenceID();
				 vf = ((NVPair)nvp).getValueFilter();
			}
			
			//if ( object && isObject)
			if (isObject)
			{
				//writer.name(nvp.getName());
				writer.beginObject();
			
				if (referenceID != null)
				{
					writer.name(MetaToken.REFERENCE_ID.getName()).value(referenceID);
				}
			
				if (nvp.getName() == null)
				{
					writer.name(MetaToken.NAME.getName()).value(nvp.getName());
					writer.name(MetaToken.VALUE.getName()).value(nvp.getValue());
				}
				else
                {
					writer.name(nvp.getName()).value(nvp.getValue());
				}
					
				if (vf != null && FilterType.CLEAR != vf)
				{
					writer.name(MetaToken.VALUE_FILTER.getName()).value(vf.toCanonicalID());
				}
				
				writer.endObject();
			}
			else
            {
				writer.name(nvp.getName()).value(nvp.getValue());
			}
			
		}
		
		return writer;
	}	
	
	public static <V extends NVEntity> V fromJSON(String json) 
        throws  APIException
    {
		return fromJSON(json, null, null);
	}
	
	
	public static <V extends NVEntity> V fromJSON(byte[] json) 
	        throws  APIException
    {
		return fromJSON(SharedStringUtil.toString(json), null, null);
	}
	
	public static <V extends NVEntity> V fromJSON(byte[] json, Base64Type b64t) 
	        throws  APIException
    {
		return fromJSON(SharedStringUtil.toString(json), null, b64t);
	}
	
	
	public static Map<String, ?> fromJSONMap(String json, Base64Type b64Type) 
        throws APIException
    {
		Map<String, Object> ret = new LinkedHashMap<String, Object>();
		
		JsonElement je = JsonParser.parseString(json);
		
		log.log(Level.FINE, "JSONElement created from json (String): " + je);
		
		if (je instanceof JsonObject)
		{
			JsonObject jo = (JsonObject) je;
			
			for (Entry<String, JsonElement> element : jo.entrySet())
			{
				if (!element.getValue().isJsonNull())
				{
					if (element.getValue().isJsonArray())
					{
						List<Object> list = new ArrayList<Object>();
						
						JsonArray jsonArray = element.getValue().getAsJsonArray();
						
						for (int i = 0; i < jsonArray.size(); i++)
						{
							if (jsonArray.get(i).isJsonObject())
							{
								NVEntity nve = fromJSON(jsonArray.get(i).getAsJsonObject(), null, b64Type, false);
								list.add(nve);
							}
							else if (jsonArray.get(i).isJsonPrimitive())
							{
								JsonPrimitive jsonPrimitive = jsonArray.get(i).getAsJsonPrimitive();
								
								if (jsonPrimitive.isString())
								{
									list.add(jsonArray.get(i).getAsString());
								}
								else if (jsonPrimitive.isBoolean())
								{
									list.add(jsonArray.get(i).getAsBoolean());
								}
							}
						}

						ret.put(element.getKey(), list);
					}
					else if (element.getValue().isJsonObject())
					{
						NVEntity nve = fromJSON(element.getValue().getAsJsonObject(), null, b64Type, false);
						ret.put(element.getKey(), nve);
					}
					else if (element.getValue().isJsonPrimitive())
					{
						JsonPrimitive jsonPrimitive = element.getValue().getAsJsonPrimitive();
						
						if (jsonPrimitive.isString())
						{
							ret.put(element.getKey(), jsonPrimitive.getAsString());
						}
						else if (jsonPrimitive.isBoolean())
						{
							ret.put(element.getKey(), jsonPrimitive.getAsBoolean());
						}
					}
				}
				else
				    {
					ret.put(element.getKey(), null);
				}
			}
		}
		
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static String toJSONMap(Map<String, ?> map, Base64Type b64Type) 
        throws IOException
    {
		StringWriter sw = new StringWriter();
		JsonWriter writer = new JsonWriter(sw);
		writer.setSerializeNulls(true);
		writer.setHtmlSafe(true);
		writer.setIndent("  ");
		
		writer.beginObject();

		if (map != null && map.size() > 0)
		{
			for (Entry<String, ?> entry : map.entrySet())
			{
				Object value = entry.getValue();
				
				writer.name(entry.getKey());
				
				if (value != null)
				{
					if (value instanceof List)
					{
						List<?> list = (List<?>) value;
						
						writer.beginArray();
						
						for (Object val : list)
						{
							if (val instanceof NVEntity)
							{
								toJSON(writer, (Class<? extends NVEntity>) val.getClass(), (NVEntity) val, false, true, b64Type);
							}
							else if (val instanceof String)
							{
								writer.value((String) val);
							}
							else if (val instanceof Boolean)
							{
								writer.value((boolean) val);
							}
						}
						
						writer.endArray();
					}
					else if (value instanceof NVEntity)
					{
						toJSON(writer, (Class<? extends NVEntity>) value.getClass(), (NVEntity) value, false, true, b64Type);
					}
					else if (value instanceof String)
					{
						writer.value((String) value);
					}
					else if (value instanceof Boolean)
					{
						writer.value((boolean) value);
					}
				}
				else
                {
				    writer.nullValue();
				}
			}
		}
		
		writer.endObject();
		writer.close();
		
		return sw.toString();
	}
	
	
	public static <V extends NVEntity> V fromJSON(String json, Class<? extends NVEntity> clazz) 
	        throws  AccessException, APIException
	{
		return fromJSON(json, clazz, null);
	}
	
	@SuppressWarnings("unchecked")
	public static <V extends NVEntity> V fromJSON(String json, Class<? extends NVEntity> clazz, Base64Type b64Type) 
        throws AccessException, APIException
    {
		JsonElement je = JsonParser.parseString(json);
		
		if (je instanceof JsonObject)
		{
			return (V) fromJSON((JsonObject)je, clazz, b64Type, false);
		}
		
		return null;
	}
	
	
	public static <V extends NVEntity> V fromJSON(Reader json)
	{
	  return fromJSON(json, null, null); 
	}
	public static <V extends NVEntity> V fromJSON(Reader json, Class<? extends NVEntity> clazz)
    {
      return fromJSON(json, clazz, null); 
    }
	
	@SuppressWarnings("unchecked")
	public static <V extends NVEntity> V fromJSON(Reader json, Class<? extends NVEntity> clazz, Base64Type b64Type) 
        throws AccessException, APIException
    {
        JsonElement je = JsonParser.parseReader(json);
        
        if (je instanceof JsonObject)
        {
            return (V) fromJSON((JsonObject)je, clazz, b64Type, false);
        }
        
        return null;
    }


	private static NVEntity fromJSON(JsonObject jo, Type typeOf, Base64Type b64Type, boolean nvgmPrimitiveAsString)
			throws AccessException, APIException
	{
		Class<? extends NVEntity> clazz = null;
		try
		{
			clazz = (Class<? extends NVEntity>) Class.forName(typeOf.getTypeName());
		}
		catch(Exception e)
		{}

		return fromJSON(jo, clazz, b64Type, nvgmPrimitiveAsString);

	}

	@SuppressWarnings("unchecked")
	private static NVEntity fromJSON(JsonObject jo, Class<? extends NVEntity> clazz, Base64Type b64Type, boolean nvgmPrimitiveAsString)
        throws AccessException, APIException
    {

		// check if the jo has class name setup
		// before creating the new instance
		JsonElement classType = jo.get(MetaToken.CLASS_TYPE.getName());

		if (classType != null)
		{
			if (!classType.isJsonNull())
			{
				
				try
				{
					clazz = (Class<? extends NVEntity>) Class.forName(classType.getAsString());
				} 
				catch (ClassNotFoundException e) 
				{
					// TODO Auto-generated catch block
					//e.printStackTrace();
					throw new APIException(e.getMessage(), Reason.NOT_FOUND);
				}
			}
		}
		
		NVEntity nve = null;
		
		try
        {
			try
			{
				nve = clazz.getDeclaredConstructor().newInstance();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				throw new APIException(e.getMessage(), Reason.NOT_FOUND);
			}
		}
		catch(InstantiationException | InvocationTargetException | NoSuchMethodException ie )
        {
		    ie.printStackTrace();
			log.info("Error class:" + clazz);
			log.info("" + jo.toString());
			throw new APIException(ie.getMessage(), Reason.NOT_FOUND);
//			if (ie instanceof InstantiationException)
//				throw (InstantiationException)ie;
//			else 
//				throw new InstantiationException(ie.getMessage());
			
		}
		catch(SecurityException ie)
		{
			throw new AccessException(ie.getMessage(), Reason.ACCESS_DENIED);
		}
		
		if (jo.get(MetaToken.REFERENCE_ID.getName()) != null && !jo.get(MetaToken.REFERENCE_ID.getName()).isJsonNull())
		{
			nve.setReferenceID(jo.get(MetaToken.REFERENCE_ID.getName()).getAsString());
		}	
		
		NVConfigEntity mcEntity = (NVConfigEntity) nve.getNVConfig();
	
		List<NVConfig> nvconfigs = mcEntity.getAttributes();

		for (NVConfig nvc: nvconfigs)
		{
			Class<?> metaType = nvc.getMetaType();
			JsonElement je = jo.get(nvc.getName());
			
			if (je != null && !je.isJsonNull())
			{
				NVBase<?> nvb = nve.lookup(nvc.getName());
				
				if (nvc.isArray())
				{
					//if ( nvb instanceof NVBase<List<NVEntity>>)
					
					//if ( NVEntity.class.isAssignableFrom( metaType.getComponentType()))
					if (NVEntity.class.isAssignableFrom(nvc.getMetaTypeBase()))
					{
						ArrayValues<NVEntity> tempArray = (ArrayValues<NVEntity>) nvb;
						JsonArray jsonArray = je.getAsJsonArray();
						for (int i = 0; i< jsonArray.size(); i++)
						{
							JsonObject jobj = jsonArray.get(i).getAsJsonObject();
//							try
							{
								tempArray.add(fromJSON(jobj, (Class<? extends NVEntity>) nvc.getMetaTypeBase(), b64Type, nvgmPrimitiveAsString));
							}
//							catch (InstantiationException ie)
//							{
//								log.info("nvc:" + nvc.getName() + ":" + nvc.getMetaTypeBase());
//								throw ie;
//							}
							//nvl.getValue().add( toNVPair( jobj));		
						}
					}
					// enum must be checked first
					else if (metaType.getComponentType().isEnum())
					{
						JsonArray jsonArray = je.getAsJsonArray();
						NVBase<List<Enum<?>>> nel = (NVBase<List<Enum<?>>>) nvb;
						
						for (int i = 0; i< jsonArray.size(); i++)
						{
							String jobj = jsonArray.get(i).getAsString();
							nel.getValue().add(SharedUtil.enumValue(metaType.getComponentType(), jobj));
						}
					}
					else if (String[].class.equals(metaType))
					{
						JsonArray jsonArray = je.getAsJsonArray();
						ArrayValues<NVPair> nvpm = (ArrayValues<NVPair>) nvb;
						
						for (int i = 0; i< jsonArray.size(); i++)
						{
							JsonObject jobj = jsonArray.get(i).getAsJsonObject();
							nvpm.add(toNVPair( jobj));	
						}
					}
					else if (Long[].class.equals(metaType))
					{
						JsonArray jsonArray = je.getAsJsonArray();
						NVBase<ArrayList<Long>> nval = (NVBase<ArrayList<Long>>) nvb;
						
						for (int i = 0; i< jsonArray.size(); i++)
						{
							nval.getValue().add( jsonArray.get(i).getAsLong());
						}	
					}
					else if (byte[].class.equals(metaType))
					{
						String byteArray64 = je.getAsString();
						
						if (byteArray64 != null)
						{
							nve.setValue(nvc, SharedBase64.decode(b64Type,  SharedStringUtil.getBytes(byteArray64)));
						}
					}
					else if (Integer[].class.equals(metaType))
					{
						JsonArray jsonArray = je.getAsJsonArray();
						NVBase<ArrayList<Integer>> nval = (NVBase<ArrayList<Integer>>) nvb;
						
						for (int i = 0; i< jsonArray.size(); i++)
						{
							nval.getValue().add((int) jsonArray.get(i).getAsLong());
						}	
					}
					else if (Float[].class.equals(metaType))
					{
						JsonArray jsonArray = je.getAsJsonArray();
						NVBase<ArrayList<Float>> nval = (NVBase<ArrayList<Float>>) nvb;
						
						for (int i = 0; i< jsonArray.size(); i++)
						{
							nval.getValue().add((float) jsonArray.get(i).getAsDouble());
						}	
					}
					else if (Double[].class.equals(metaType))
					{
						JsonArray jsonArray = je.getAsJsonArray();
						NVBase<ArrayList<Double>> nval = (NVBase<ArrayList<Double>>) nvb;
						
						for (int i = 0; i < jsonArray.size(); i++)
						{
							nval.getValue().add(jsonArray.get(i).getAsDouble());
						}	
					}
					else if (Date[].class.equals(metaType))
					{
						JsonArray jsonArray = je.getAsJsonArray();
						NVBase<ArrayList<Long>> nval = (NVBase<ArrayList<Long>>) nvb;
						
						for (int i = 0; i< jsonArray.size(); i++)
						{
							JsonPrimitive jp = (JsonPrimitive) jsonArray.get(i);
							long tempDate = 0;
							
							if (jp.isString() && nvc.getValueFilter() != null)
							{
								tempDate = (Long) nvc.getValueFilter().validate(jp.getAsString());
							}
							else
                            {
								tempDate = jp.getAsLong();
							}
							
							nval.getValue().add(tempDate);
						}	
					}
					else if (BigDecimal[].class.equals(metaType))
					{
						JsonArray jsonArray = je.getAsJsonArray();
						NVBase<ArrayList<BigDecimal>> nval = (NVBase<ArrayList<BigDecimal>>) nvb;
						
						for (int i = 0; i < jsonArray.size(); i++)
						{
							nval.getValue().add(jsonArray.get(i).getAsBigDecimal());
						}	
					}
					
				}
				else
                {
				    // not array
					if (nvc instanceof NVConfigEntity)
					{
						if (!(je instanceof JsonNull))
						{
							((NVBase<NVEntity>) nvb).setValue(fromJSON(je.getAsJsonObject(), (Class<? extends NVEntity>) nvc.getMetaType(), b64Type, nvgmPrimitiveAsString));
						}
					}
					else if (NVGenericMap.class.equals(metaType))
					{
						
						if (!(je instanceof JsonNull))
						{
							NVGenericMap nvgm = fromJSONGenericMap(je.getAsJsonObject(), null, b64Type, nvgmPrimitiveAsString);
							((NVGenericMap)nve.lookup(nvc)).add(nvgm.values(), true);
							
						}
					
					}
					else if (NVStringList.class.equals(metaType))
                    {
                        JsonArray jsonArray = je.getAsJsonArray();
                        NVStringList nval = (NVStringList) nvb;
                        
                        for (int i = 0; i < jsonArray.size(); i++)
                        {
                            nval.getValue().add(jsonArray.get(i).getAsString());
                        }   
                    }
					else if (NVStringSet.class.equals(metaType))
					{
						JsonArray jsonArray = je.getAsJsonArray();
						NVStringSet nval = (NVStringSet) nvb;

						for (int i = 0; i < jsonArray.size(); i++)
						{
							nval.getValue().add(jsonArray.get(i).getAsString());
						}
					}

					else if (nvc.isEnum())
					{
						if (!(je instanceof JsonNull))
						{
//							if (metaType.isAssignableFrom( DynamicEnumMap.class))
//							{
//								
//								((NVDynamicEnum)nvb).setValue(je.getAsString());
//							}
//							else
							{
								((NVBase<Enum<?>>)nvb).setValue(SharedUtil.enumValue(metaType, je.getAsString()));
							}
						}
					}
					else if (String.class.equals(metaType))
					{
						if (!(je instanceof JsonNull))
						{
							((NVPair) nvb).setValue(je.getAsString());
						}
					}
					else if (Long.class.equals(metaType))
					{
						((NVBase<Long>) nvb).setValue(je.getAsLong());
					}
					else if (Boolean.class.equals(metaType))
					{
						 ((NVBase<Boolean>) nvb).setValue(je.getAsBoolean());
					}
					else if ( Integer.class.equals(metaType))
					{
						 ((NVBase<Integer>) nvb).setValue((int)je.getAsLong());
					}
					else if (Float.class.equals(metaType))
					{
						 ((NVBase<Float>) nvb).setValue((float)je.getAsDouble());
					}
					else if (Double.class.equals(metaType))
					{
						 ((NVBase<Double>) nvb).setValue(je.getAsDouble());
					}
					else if (Date.class.equals(metaType))
					{
						JsonPrimitive jp = (JsonPrimitive) je;

						if (jp.isString())
						{
							if (nvc.getValueFilter() != null)
								((NVBase<Long>) nvb).setValue((Long) nvc.getValueFilter().validate(jp.getAsString()));
							else
								((NVBase<Long>) nvb).setValue(TimestampFilter.SINGLETON.validate(jp.getAsString()));
						}
						
						else
						{
							((NVBase<Long>) nvb).setValue(jp.getAsLong());
						}
					}
					else if (BigDecimal.class.equals(metaType))
					{
						((NVBase<BigDecimal>) nvb).setValue(je.getAsBigDecimal());
					}
					else if (Number.class.equals(metaType))
					{
						((NVBase<Number>) nvb).setValue(SharedUtil.parseNumber(je.getAsString()));
					}
				}
			}
				
		}
		
		
		if (nve instanceof SubjectID)
		{
			((SubjectID<?>) nve).getSubjectID();
		}
		return nve;
	}
	
	private static NVPair toNVPair(JsonObject jo)
    {
		NVPair nvp = new NVPair();
	
		if (jo.get(MetaToken.NAME.getName())!= null && !jo.get(MetaToken.NAME.getName()).isJsonNull())
		{
			nvp.setName(jo.get(MetaToken.NAME.getName()).getAsString());
		}
		
		if (jo.get(MetaToken.VALUE.getName()) != null && !jo.get(MetaToken.VALUE.getName()).isJsonNull())
		{
			nvp.setValue(jo.get(MetaToken.VALUE.getName()).getAsString());
		}
		
		if (jo.get(MetaToken.REFERENCE_ID.getName()) != null)
		{
			nvp.setReferenceID(jo.get(MetaToken.REFERENCE_ID.getName()).getAsString());
		}
		
		if (jo.get(MetaToken.VALUE_FILTER.getName()) != null)
		{
			ValueFilter<String, String> vf = (FilterType) SharedUtil.enumValue(FilterType.class, jo.get(MetaToken.VALUE_FILTER.getName()).getAsString());
			
			if (vf == null)
			{
				vf = DynamicEnumMapManager.SINGLETON.lookup(jo.get(MetaToken.VALUE_FILTER.getName()).getAsString());
			}
			
			if (vf != null)
			{
				nvp.setValueFilter(vf);
			}
		}
		
		if (nvp.getName() == null && nvp.getValue() == null)
		{
			// we might have "name" : "value"
			Iterator<Entry<String, JsonElement>> it = jo.entrySet().iterator();
			while (it.hasNext())
            {
				Entry<String, JsonElement> nv = it.next();
				String name = nv.getKey();

				if (!MetaToken.REFERENCE_ID.getName().equals(name) && !MetaToken.VALUE_FILTER.getName().equals(name))
				{
					nvp.setName(name);

					if (!nv.getValue().isJsonNull())
					{
						nvp.setValue(nv.getValue().getAsString());
					}
					
					break;
				}
			}
		}
		
		return nvp;
	}
	
	public static String toJSONs(List<? extends NVEntity> list, boolean indent, boolean printNull, Base64Type b64Type)
        throws IOException
    {
		StringBuilder sb = new StringBuilder();
		
		for (NVEntity nve : list)
		{
			if (sb.length() > 0)
			{
				sb.append('\n');
			}
			
			sb.append(toJSON(nve, indent, printNull, true, b64Type));
		}
		
		return sb.toString();
	}
	
	public static String toJSONValues(NVEntity[] list, boolean indent, boolean printNull, boolean printClass, Base64Type b64Type) 
        throws IOException
    {
		StringWriter sw = new StringWriter();
		JsonWriter writer = new JsonWriter( sw);
		writer.setSerializeNulls(true);
		writer.setHtmlSafe(true);

		if (indent)
			writer.setIndent("  ");
		else
			writer.setIndent("");

		writer.beginObject();
		writer.name(MetaToken.VALUES.getName());
		writer.beginArray();
		
		for (NVEntity nve : list)
		{
			if (nve != null)
			toJSON(writer, nve.getClass(), nve, printNull, true, b64Type);
		}
		
		writer.endArray();
		writer.endObject();
		writer.close();
		
		return sw.toString();
	}
	
	public static List<NVEntity> fromJSONValues(String json, Base64Type b64Type) 
        throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
    {
		JsonElement je = JsonParser.parseString(json);
		
		if (je instanceof JsonObject)
		{
			List<NVEntity> ret = new ArrayList<NVEntity>();
		
			JsonArray ja = 	(JsonArray) ((JsonObject) je).get(MetaToken.VALUES.getName());
			
			for (int i = 0; i < ja.size(); i++)
			{
				ret.add(fromJSON((JsonObject)ja.get(i), null, b64Type, false));
			}
			
			return ret;
		}
		
		return null;
	}

	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <V extends NVEntity> List<V> fromJSONs(String json, Base64Type b64Type, Class<? extends NVEntity>... classes)
    {
		List<V> ret = new ArrayList<V>();
		
		List<CharSequence> tokens = SharedStringUtil.parseGroup(json, "{", "}", true);
		
		for (CharSequence token : tokens)
		{
			for (Class<? extends NVEntity> c : classes)
			{
				try
                {
					NVEntity nve = fromJSON((String) token, c, b64Type);
					ret.add((V) nve);
				}
				catch (Exception e)
                {
					
				}
			}
		}
		
		return ret;
	}
	
	public static DynamicEnumMap fromJSONDynamicEnumMap(String json)
        throws InstantiationException, IllegalAccessException
    {
		DynamicEnumMap ret = new DynamicEnumMap();
	
		JsonElement je = JsonParser.parseString(json);

		if (je instanceof JsonObject)
		{
			JsonObject jo = (JsonObject) je;
			
			if (jo.get(MetaToken.REFERENCE_ID.getName()) != null
                    && !jo.get(MetaToken.REFERENCE_ID.getName()).isJsonNull())
			{
				ret.setReferenceID(jo.get(MetaToken.REFERENCE_ID.getName()).getAsString());
			}
			
			if (jo.get(MetaToken.SUBJECT_GUID.getName()) != null
                    && !jo.get(MetaToken.SUBJECT_GUID.getName()).isJsonNull())
			{
				ret.setSubjectGUID(jo.get(MetaToken.SUBJECT_GUID.getName()).getAsString());
			}
			
			if (jo.get(MetaToken.ACCOUNT_ID.getName()) != null
                    && !jo.get(MetaToken.ACCOUNT_ID.getName()).isJsonNull())
			{
				ret.setAccountID(jo.get(MetaToken.ACCOUNT_ID.getName()).getAsString());
			}
			
			if (jo.get(MetaToken.NAME.getName()) != null
                    && !jo.get(MetaToken.NAME.getName()).isJsonNull())
			{
				ret.setName(jo.get(MetaToken.NAME.getName()).getAsString());
			}
			
			if (jo.get(MetaToken.DESCRIPTION.getName()) != null
                    && !jo.get(MetaToken.DESCRIPTION.getName()).isJsonNull())
			{
				ret.setDescription(jo.get(MetaToken.DESCRIPTION.getName()).getAsString());
			}
			
			if (jo.get(MetaToken.IS_FIXED.getName()) != null
                    && !jo.get(MetaToken.IS_FIXED.getName()).isJsonNull())
			{
				ret.setFixed(jo.get(MetaToken.IS_FIXED.getName()).getAsBoolean());
			}

			if (jo.get(MetaToken.STATIC.getName()) != null
                    && !jo.get(MetaToken.STATIC.getName()).isJsonNull())
			{
				ret.setStatic(jo.get(MetaToken.STATIC.getName()).getAsBoolean());
			}
			
			if (jo.get(MetaToken.IGNORE_CASE.getName()) != null
                    && !jo.get(MetaToken.IGNORE_CASE.getName()).isJsonNull())
			{
				ret.setStatic(jo.get(MetaToken.IGNORE_CASE.getName()).getAsBoolean());
			}
			
			if (jo.get(MetaToken.VALUE.getName()) != null && !jo.get(MetaToken.VALUE.getName()).isJsonNull())
			{
				List<NVPair> list = new ArrayList<NVPair>();
				JsonArray jsonArray = jo.getAsJsonArray(MetaToken.VALUE.getName());
				
				for (int i = 0; i< jsonArray.size(); i++)
				{
					list.add(toNVPair(jsonArray.get(i).getAsJsonObject()));
				}
				
				ret.setValue(list);
			}
		}
		
		return ret;
	}
	
	public static String toJSONDynamicEnumMap(DynamicEnumMap dem)
        throws IOException
    {
		StringWriter sw = new StringWriter();
		JsonWriter writer = new JsonWriter(sw);
		writer.setSerializeNulls(true);
		writer.setHtmlSafe(true);
		writer.setIndent("  ");

		toJSONDynamicEnumMap(writer, dem);
		
		writer.close();
		
		return sw.toString();
	}
	
	private static JsonWriter toJSONDynamicEnumMap(JsonWriter writer, DynamicEnumMap dem) 
        throws IOException
    {
		writer.beginObject();
		
		if (dem != null)
		{
			writer.name(MetaToken.REFERENCE_ID.getName()).value(dem.getReferenceID());
			writer.name(MetaToken.SUBJECT_GUID.getName()).value(dem.getSubjectGUID());
			writer.name(MetaToken.ACCOUNT_ID.getName()).value(dem.getAccountID());
			writer.name(MetaToken.NAME.getName()).value(dem.getName());
			writer.name(MetaToken.DESCRIPTION.getName()).value(dem.getDescription());
			writer.name(MetaToken.IS_FIXED.getName()).value(dem.isFixed());
			writer.name(MetaToken.STATIC.getName()).value(dem.isStatic());
			writer.name(MetaToken.IGNORE_CASE.getName()).value(dem.isIgnoreCase());
			
			writer.name(MetaToken.VALUE.getName());
			writer.beginArray();
			
			for (NVPair nvp : dem.getValue())
			{
				toJSON(writer, nvp, true, true);
			}
			
			writer.endArray();
		}
		
		writer.endObject();	
		
		return writer;
	}
	
	public static List<DynamicEnumMap> fromJSONDynamicEnumMapList(String json)
        throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
    {
		JsonElement je = JsonParser.parseString(json);
		
		if (je instanceof JsonObject)
		{
			List<DynamicEnumMap> ret = new ArrayList<DynamicEnumMap>();
		
			JsonArray ja = 	(JsonArray) ((JsonObject) je).get(MetaToken.VALUES.getName());
			
			for (int i = 0; i < ja.size(); i++)
			{
				ret.add(fromJSONDynamicEnumMap(ja.get(i).toString()));
			}
			
			return ret;
		}
		
		return null;
	}
	
	public static String toJSONQuery(QueryRequest qr)
    {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(MetaToken.CANONICAL_ID.getName(), qr.getCanonicalID());
				
		jsonObject.addProperty("batch_size", qr.getBatchSize());
		
		if (qr.getFieldNames() != null)
		{
			JsonArray ja = new JsonArray();

			for (String fn : qr.getFieldNames())
			{
				if (!SUS.isEmpty(fn))
				{
					ja.add(fn);
				}
			}
			
			jsonObject.add("field_names", ja);
		}
		
		if (qr.getQuery() != null)
		{
			JsonArray ja = new JsonArray();
			
			for (QueryMarker qm : qr.getQuery())
			{
				if (qm != null)
				{
					JsonObject qmJSON = new JsonObject();

					if (qm instanceof GetNameValue)
					{
						if (qm instanceof QueryMatch)
						{
							QueryMatch<?> qMatch = (QueryMatch<?>) qm;
							Object value = qMatch.getValue();
							
							if (value instanceof Number)
							{
								qmJSON.addProperty(qMatch.getName(),(Number)value);
							}
							else if (value instanceof String)
							{
								qmJSON.addProperty(qMatch.getName(), (String) value);
							}
							else if (value instanceof Enum)
							{
								qmJSON.addProperty(qMatch.getName(), ((Enum<?>) value).name());
							}
							
							if (qMatch.getOperator() != null)
							{
                                qmJSON.addProperty(MetaToken.RELATIONAL_OPERATOR.getName(), qMatch.getOperator().name());
                            }
						}
					}
					else if (qm instanceof LogicalOperator)
					{
						qmJSON.addProperty(MetaToken.LOGICAL_OPERATOR.getName(), ((LogicalOperator)qm).name());
					}
					
					ja.add(qmJSON);
				}
			}
			
			jsonObject.add("query", ja);
		}

		return jsonObject.toString();
	}

	public static String toJSONDynamicEnumMapList(List<DynamicEnumMap> list)
        throws IOException
    {
		StringWriter sw = new StringWriter();
		JsonWriter writer = new JsonWriter( sw);
		writer.setSerializeNulls(true);
		writer.setHtmlSafe(true);
		writer.setIndent("  ");
		
		writer.beginObject();
		writer.name(MetaToken.VALUES.getName());
		writer.beginArray();
		
		for (DynamicEnumMap dem : list)
		{
			if (dem != null)
			{
				toJSONDynamicEnumMap(writer, dem);
			}
		}
		
		writer.endArray();
		writer.endObject();
		writer.close();
		
		return sw.toString();
	}
	
}