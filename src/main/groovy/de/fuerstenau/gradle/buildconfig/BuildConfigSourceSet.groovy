/* The MIT License (MIT)
 *
 * Copyright (c) 2015 Malte Fürstenau
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.fuerstenau.gradle.buildconfig

import org.gradle.api.Named
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Extension with configuration data per {@code SourceSet}.
 * 
 * @author Malte Fürstenau
 */
class BuildConfigSourceSet implements Named
{
   /**
    * Name (of {@code SourceSet}).
    */
   final String name

   /**
    * BuildConfig field {@code VERSION} (application version).
    */
   String version
   /**
    * BuildConfig field {@code NAME} (application name).
    */
   String appName

   /**
    * BuildConfig class name.
    */
   String clsName
   /**
    * Charset for code generation.
    */
   String charset
   /**
    * Package for BuildConfig class.
    */
   String packageName

   Map<String, ClassField> classFields = new LinkedHashMap<>()

   /**
    * Adds class field, replacing existing for class field
    * name.
    * 
    * @param type   Type of class field
    * @param type   Name of class field
    * @param value  Value of class field
    * 
    * @return       existing class field for name
    */ 
   ClassField buildConfigField (String type, String name, String value)
   {
      addClassField (classFields, new ClassFieldImpl (type, name, value))
   }
   
   /**
    * Adds class field, replacing existing for class field
    * name.
    *
    * @param type   Type of class field
    * @param type   Name of class field
    * @param value  Closure returning value of class field
    *
    * @return       existing class field for name
    */
   ClassField buildConfigField (String type, String name, Closure<String> value)
   {
      addClassField (classFields, new ClassFieldClosureImpl (type, name, value))
   }

   /**
    * Adds class field to destination map, replacing existing for class field
    * name.
    * 
    * @param target  target map
    * @param cf      class field
    * 
    * @return        existing class field for name
    */ 
   ClassField addClassField (Map<String, ClassField> target, ClassField cf)
   {
      ClassField alreadyPresent = target.get (cf.getName ())
      target.put (cf.name, cf)
      alreadyPresent
   }     
   
   /**
    * Constructor.
    * 
    * @param name name of corresponding {@code SourceSet}
    */
   BuildConfigSourceSet (String name)
   {
      this.name = name
   }
   
   /**
    * Merges this {@link BuildConfigSourceSet} instance with other.
    * <p>
    * <b>Note:</b> <i>non-null</i> values from this instance will override other
    * values.
    * 
    * @param other  {@link BuildConfigSourceSet} instance to add this instance to
    * 
    * @return       merged
    */
   BuildConfigSourceSet plus (BuildConfigSourceSet other)
   {
      BuildConfigSourceSet result = new BuildConfigSourceSet (other.name)
      result.appName = other.appName ?: this.appName
      result.charset = other.charset ?: this.charset
      result.packageName = other.packageName ?: this.packageName
      result.version = other.version ?: this.version
      result.clsName = other.clsName ?: this.clsName
      classFields.each {
         if (it.value != null)
         {
            result.classFields.put (it.key, it.value)
         }
      }
      other.classFields.each {
         if (it.value != null)
         {
            result.classFields.put (it.key, it.value)
         }
      }
      return result
   }
   
}
