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


import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.annotation.ParamProp;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SUS;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;


/**
 * 
 * @author javaconsigliere
 *
 */
public class ReflectionUtil
{

	public static LogWrapper log = new LogWrapper(ReflectionUtil.class);



	public static class MethodAnnotations
	{
		private final Map<Class<? extends Annotation>, Annotation> methodAnnotations = new LinkedHashMap<>();
		public final Map<Parameter, Annotation> parametersAnnotations;
		public final Method method;
		public MethodAnnotations(Method method, Annotation[] methodAnnotations, Map<Parameter, Annotation> parametersAnnotations)
		{
			this.method = method;
			for (Annotation a : methodAnnotations)
				add(a);

			this.parametersAnnotations = parametersAnnotations;
		}

		public String toString()
		{
			return "ma:" + methodAnnotations + " pa:" + parametersAnnotations;
		}

		public Annotation[] methodAnnotations()
		{
			return methodAnnotations.values().toArray(new Annotation[0]);
		}


		public boolean matchAnnotation(Class<? extends Annotation> a)
		{
			return methodAnnotations.get(a) != null;
		}

		public synchronized MethodAnnotations add(Annotation a)
		{
			this.methodAnnotations.put(a.annotationType(), a);
			return this;
		}

	}

	public static class AnnotationMap
	{

		private final Class<?> clazz;
		private Annotation[] classAnnotations;
		private Map<Method, MethodAnnotations> methodsAnnotations = new LinkedHashMap<Method, MethodAnnotations>();
		//private Map<Class< ?extends Annotation>, Method[]> matchingMethods = new HashMap<>();

		public AnnotationMap(Class<?> c)
		{
			SUS.checkIfNulls("class can't be null", c);
			this.clazz = c;
		}

		public Class<?> getAnnotatedClass()
		{
			return clazz;
		}

		public Annotation[] getClassAnnotations()
		{
			return classAnnotations;
		}

		public Map<Method, MethodAnnotations> getMethodsAnnotations()
		{
			return methodsAnnotations;
		}

		public boolean equals(Object o)
		{
			if(o instanceof AnnotationMap)
				if(((AnnotationMap) o).getAnnotatedClass().equals(getAnnotatedClass()))
					return true;
			return false;
		}


		public Method[] matchingMethods(Class<?extends Annotation> annotationClass)
		{
			Method[] ret;

			synchronized (this)
			{

				List<Method> match = new ArrayList<>();
				for (MethodAnnotations ma : methodsAnnotations.values())
				{
					if (ma.matchAnnotation(annotationClass))
						match.add(ma.method);
				}
				ret = match.toArray(new Method[0]);
				if (ret.length == 0)
					ret = null;

			}
			return ret;
		}

		public String toString()
		{
			return clazz.getName() + "," + Arrays.toString(classAnnotations) + ":" + methodsAnnotations;
		}

		public boolean isClassAnnotatedBy(Class<? extends Annotation> c)
		{
			return getMatchingClassAnnotation(c) != null;
		}

		public boolean isMethodAnnotatedBy(Method m, Class<? extends Annotation> c)
		{
			return getMatchingMethodAnnotation(m, c) != null;
		}

		public <V extends Annotation> V  getMatchingMethodAnnotation(Method m, Class<? extends Annotation> c)
		{
			SUS.checkIfNulls("Method or class can't be null", m,c);
			MethodAnnotations methodAnnotations = methodsAnnotations.get(m);

			if(methodAnnotations != null)
			{
				return (V)methodAnnotations.methodAnnotations.get(c);
			}
			return null;
		}

		public <V extends Annotation> V  getMatchingClassAnnotation(Class<? extends Annotation> c)
		{
			SUS.checkIfNulls("Class can't be null", c);
			if(classAnnotations != null) {
				for (Annotation a : classAnnotations) {
					if (a.annotationType().equals(c))
						return (V)a;
				}
			}
			return null;
		}

	}

	private ReflectionUtil()
    {
		
	}

	public static Object updateFinalStatic(Class<?> clazz, String fieldName, Object newValue) 
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
	
		Field field = clazz.getDeclaredField(fieldName);
		boolean fieldAccessible = field.isAccessible();
		field.setAccessible(true);
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		boolean modifierAccessible = modifiersField.isAccessible();
		modifiersField.setAccessible(true);
		int oldModifier = field.getModifiers();
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
		   
		//field.setInt( field, field.getModifiers() & ~Modifier.FINAL );
		field.set(null, newValue);
		modifiersField.setInt(field, oldModifier);
		
		Object ret =  field.get(null);
		field.setAccessible(fieldAccessible);
		modifiersField.setAccessible(modifierAccessible);
		
		return ret;
   }


	
	
	
	
	
	public static <T extends Annotation> T getAnnotationFromMethod(Method m, Class<T> t)
	{
		return m.getAnnotation(t);
	}
	
	public static <T extends Annotation>  Map<Method, T> scanMethodsAnnotation(Class<?> c, Class<T> t)
	{
		Map<Method, T> ret = new HashMap<Method, T>();
		
		for(Method m : c.getMethods())
		{
			T a = m.getAnnotation(t);
			if (a != null)
			{
				ret.put(m, a);
			}
		}
		
		return ret;
	}

	public static AnnotationMap scanClassAnnotations(Class<?> c, Class<? extends Annotation>...annotationTypes)
	{
		AnnotationMap ret = new AnnotationMap(c);
		ret.classAnnotations =  matchAnnotations(c.getAnnotations(), annotationTypes);
		Method[] methods = c.getMethods();


		for(Method method : methods)
		{
			if(Modifier.isPublic(method.getModifiers())) {
				Annotation[] methodAnnotations = matchAnnotations(method.getDeclaredAnnotations(), annotationTypes);
				Map<Parameter, Annotation> parametersAnnotations = matchAnnotations(method.getParameters(), annotationTypes);

				if(methodAnnotations != null) {
					MethodAnnotations ma = new MethodAnnotations(method, methodAnnotations, parametersAnnotations);
					ret.methodsAnnotations.put(method, ma);
				}
			}
		}


		if(ret.classAnnotations != null || ret.methodsAnnotations.size() > 0)
			return ret;

		return null;
	}


	public static boolean isMethodAnnotatedAs(Method m, Class<? extends Annotation>...annotationTypes)
	{
		if(annotationTypes != null && annotationTypes.length > 0)
		{
			for(Class<? extends Annotation> c : annotationTypes)
			{
				if(!isTypeMatchingAnyAnnotation(c, m.getAnnotations()))
				{
					return  false;
				}
			}
			return true;
		}
		return false;
	}

	public static boolean isClassAnnotatedAs(Class<?> clazz, Class<? extends Annotation>...annotationTypes)
	{
		if(annotationTypes != null && annotationTypes.length > 0)
		{

			for(Class<? extends Annotation> c : annotationTypes)
			{
				if(!isTypeMatchingAnyAnnotation(c, clazz.getAnnotations()))
				{
					return  false;
				}
			}
			return true;
		}
		return false;
	}


	public static Field findField(Class<?> clazz, Class<?> fieldClassType, JMod...modifiers)
	{
		Field[] fields = clazz.getDeclaredFields();
		int mod = 0;
		if(modifiers != null)
		{
			mod = JMod.toModifier(modifiers);
		}

		for (Field field : fields)
		{

			if (mod != 0)
			{
				if (field.getModifiers() != mod)
					continue;
			}

			if (fieldClassType.isAssignableFrom(field.getType()))
				return field;
		}
		return null;
	}


	public static <V> V getValueFromField(Class<?> clazz, Class<?> fieldClassType, JMod...modifiers)
			throws IllegalAccessException
	{
		Field match = findField(clazz, fieldClassType, modifiers);

		return match != null ? (V) match.get(null) : null;
	}

	public static boolean isParameterAnnotatedAs(Parameter p, Class<? extends Annotation>...annotationTypes)
	{
		if(annotationTypes != null && annotationTypes.length > 0)
		{
			for(Class<? extends Annotation> c : annotationTypes)
			{
				if(!isTypeMatchingAnyAnnotation(c, p.getAnnotations()))
				{
					return  false;
				}
			}
			return true;
		}
		return false;
	}

	public static boolean areAllMethodParametersAnnotatedAs(Method m, Class<? extends Annotation>...annotationTypes)
	{
		if(annotationTypes != null && annotationTypes.length > 0)
		{
			Parameter[] parameters = m.getParameters();
			if(parameters!= null && parameters.length > 0)
			{
				for(Parameter p : parameters)
				{
					if(!isParameterAnnotatedAs(p, annotationTypes))
					{
						return false;
					}
				}
				return true;
			}
		}


		return false;
	}

	public static Annotation[] matchAnnotations(Annotation[] annotations, Class<? extends Annotation>...annotationTypes)
	{
		Set<Annotation> match = new HashSet<Annotation>();
		for(Annotation a : annotations)
		{
			for(Class<?> c : annotationTypes){
				if (a.annotationType().equals(c))
				{
					match.add(a);
					break;
				}
			}
		}
		if(match.size() > 0)
			return match.toArray(new Annotation[0]);
		return null;
	}

	public static boolean isAnnotationMatchingAnyType(Annotation a, Class<? extends Annotation>...annotationTypes)
	{
		for(Class<?> c : annotationTypes)
		{
			if(c.isAssignableFrom(a.annotationType()))
				return true;
		}

		return false;
	}

	public static boolean isTypeMatchingAnyAnnotation(Class<? extends Annotation> aType, Annotation... annotations)
	{
		for(Annotation a : annotations)
		{
			if(aType.isAssignableFrom(a.annotationType()))
				return true;
		}
		return false;
	}


	public static Map<Parameter, Annotation> matchAnnotations(Parameter[] parameters, Class<? extends Annotation>...annotationTypes)
	{
		Map<Parameter, Annotation> match = new LinkedHashMap<Parameter, Annotation>();
		if (parameters != null && parameters.length > 0) {
			for (Parameter p : parameters) {
				List<Annotation> paAnnotations = new ArrayList<Annotation>();
				for (Annotation pa : p.getAnnotations()) {
					for (Class<?> c : annotationTypes) {
						if (pa.annotationType().equals(c)) {
							paAnnotations.add(pa);
							break;
						}
					}
				}
				if (paAnnotations.size() > 0)
				{
					match.put(p, paAnnotations.get(0));
				}
			}
			if (match.size() > 0)
				return match;
		}
		return null;
	}


	public static boolean hasMethod(Object instance, Method method)
	{
		SUS.checkIfNulls("Instance or method can't be null", instance, method);
		return hasMethod(instance.getClass(), method);
	}

	public static boolean hasMethod(Class<?> clazz, Method method)
	{
		SUS.checkIfNulls("Instance or method can't be null", clazz, method);
		for(Method m : clazz.getMethods())
		{
			if (method == method)
				return true;
		}
		return false;
	}


	public static int parameterIndex(Method m, int startIndex, Class<?> parameterType)
	{

		Parameter[] parameters = m .getParameters();
		if(parameters.length != 0)
		{
			if (startIndex < 0 || startIndex > parameters.length)
			{
				throw  new IndexOutOfBoundsException(parameters.length + " startIndex " + parameters.length);
			}
			for(int i = startIndex; i < parameters.length; i++)
			{
				Class<?> toCheck = parameters[i].getType();
				if(toCheck.isPrimitive() && !parameterType.isPrimitive())
				{
					toCheck = Const.wrap(toCheck);
				}

				if (toCheck.isAssignableFrom(parameterType))
					return i;
			}

		}
		return -1;
	}

	public static boolean doesMethodSupportParameters(boolean strict, Method m, Class<?> ...parameterTypes)
	{

		Parameter[] methodParameter = m.getParameters();

		if (parameterTypes.length > methodParameter.length && strict)
		{
			return false;
		}

		int[] indexMatches = new int[methodParameter.length];
		Arrays.fill(indexMatches, -1);

		int fromIndex = 0;
		for (int i = 0; i < parameterTypes.length; i++)
		{
			int index = parameterIndex(m, fromIndex, parameterTypes[i]);
			if (index == -1)
			{
				if (strict)
					return false;
			}
			else
			{
				if (indexMatches[index] == -1) {
					indexMatches[index] = i;
					fromIndex = 0;
				}
				else
				{
					fromIndex = index + 1;
					i--;
				}
			}
		}



		if(strict)
		{

			for (int val : indexMatches)
				if (val == -1)
					return false;
		}
		else
		{
			int matchCount = 0;
			for(int i = 0; i < indexMatches.length; i++)
			{
				if(indexMatches[i] != -1)
					matchCount++;
			}


			if(indexMatches.length > 0 && matchCount == 0)
				return false;
		}

		return true;
	}


	public Method matchMethod(Class<?> clazz, String methodName, Class<?> ...parameters) throws NoSuchMethodException {
		return clazz.getMethod(methodName, parameters);
	}


	public static Object invokeMethod(Object source, ReflectionUtil.MethodAnnotations methodAnnotations, Map<String, Object> incomingData)
			throws InvocationTargetException, IllegalAccessException
	{
		Object result = null;
		List<Object> parameterValues = new ArrayList<Object>();
		Parameter[] parameters = methodAnnotations.method.getParameters();
		Object[] values = new Object[parameters.length];

		for(int i =0; i < values.length; i++)
		{
			ParamProp pp = (ParamProp) methodAnnotations.parametersAnnotations.get(parameters[i]);
			Object tempValue = incomingData.get(pp.name());
			if (tempValue != null && tempValue instanceof List)
			{
				List<?> list = (List<?>) tempValue;
				if(list.size() > 0)
				{
					tempValue = ArrayCopier.copy(parameters[i].getType(), list.toArray());
				}
				else
					tempValue = null;
			}
			values[i] = tempValue;
		}


		//log.info("" +  methodAnnotations.method + " " + Arrays.toString(values));
		result = methodAnnotations.method.invoke(source, values);

		return result;
	}



	public static Object[] arrangeMethodParameters(boolean strict, Method method, Object... inputValues)
	{
		Object[] values = new Object[method.getParameters().length];
		if(inputValues.length > values.length && strict)
			throw new IndexOutOfBoundsException("Too many parameters " + inputValues.length + " > " +values.length);

		Arrays.fill(values, null);
		int fromIndex = 0;
		for(int i = 0; i < inputValues.length; i++)
		{
			if(inputValues[i] != null)
			{
				int mappedIndex = parameterIndex(method, fromIndex , inputValues[i].getClass());

				if(mappedIndex != -1)
				{
					if(values[mappedIndex] == null)
					{
						values[mappedIndex] = inputValues[i];
						fromIndex = 0;

					}
					else
					{
						fromIndex = mappedIndex + 1;
						i--;
					}

				}
				else if (strict)
					throw new IllegalArgumentException("Parameter not found " + inputValues[i]);
			}
		}
		return values;

	}


	public static Object invokeMethod(boolean strict, Object source, Method method , Object... inputValues)
			throws InvocationTargetException, IllegalAccessException
	{
		return method.invoke(source, arrangeMethodParameters(strict, method, inputValues));
	}

	public static <T> T createBean(String className)
			throws
			ClassNotFoundException,
			NoSuchMethodException,
			IllegalAccessException,
			InvocationTargetException,
			InstantiationException
	{
		return createBean(Class.forName(className));
	}

	public static <T> T createBean(Class<?> clazz)
			throws
			NoSuchMethodException,
			IllegalAccessException,
			InvocationTargetException,
			InstantiationException
	{
		return (T)clazz.getDeclaredConstructor().newInstance();
	}

}