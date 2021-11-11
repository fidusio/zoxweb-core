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
package org.zoxweb;

import java.lang.reflect.Field;
import java.util.logging.SimpleFormatter;

import org.zoxweb.server.util.ReflectionUtil;

public class SimpleFormatterOverride {

  public static void main(String[] args) {

    try {
      Class<?> simpleFormatter = SimpleFormatter.class;
      Field field = simpleFormatter.getDeclaredField("format");
      field.setAccessible(true);
      System.out.println("" + field);
      //System.out.println("" + field.get(null));

//			Field modifiersField = Field.class.getDeclaredField( "modifiers" );
//            modifiersField.setAccessible( true );
//            int oldModifier = field.getModifiers();
//            modifiersField.setInt( field, field.getModifiers() & ~Modifier.FINAL );
//           
//			//field.setInt( field, field.getModifiers() & ~Modifier.FINAL );
//			field.set(null, "marwan");
//			modifiersField.setInt( field, oldModifier);

      System.out
          .println("" + ReflectionUtil.updateFinalStatic(simpleFormatter, "format", "mario taza"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
