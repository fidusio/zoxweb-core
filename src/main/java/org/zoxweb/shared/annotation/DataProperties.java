package org.zoxweb.shared.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;




@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataProperties 
{
	
	
	Class<?> inputFormat();
	/**
	 * Specify the class type of the data
	 * @return
	 */
 	Class<?> dataType();
 	/**
 	 * If true the data is mandatory
 	 * @return
 	 */
 	boolean  dataRequired() default true;
 	/**
 	 * If true an attempt will be made to convert the data in advance
 	 * @return
 	 */
 	boolean  dataAutoConvert() default true;
	
}