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
 * @author Malte Fürstenau
 */
public class SourceSetConfig implements Named
{
   private static final Logger LOG = LoggerFactory.getLogger (
      SourceSetConfig.class.getCanonicalName ())

   final String name

   String version
   String clsName
   String appName
   String packageName

   private final Map<String, ClassField> classFields = new LinkedHashMap<>()

   Map<String, ClassField> getBuildConfigFields ()
   {
      return classFields
   }
    
   public void buildConfigField (String type, String name, String value)
   {
      addClassField (type, name, value)
   }
   
   void addClassField (String type, String name, String value)
   {
      addClassField (classFields, new ClassFieldImpl (type, name, value))
   }   
    
   void addClassField (Map<String, ClassField> dest, ClassField cf)
   {
      ClassField alreadyPresent = dest.get (cf.getName ())

      if (alreadyPresent != null)
      {
         LOG.debug "{}: buildConfigField <{}/{}/{}> exists, replacing with <{}/{}/{}>",
         name,
         alreadyPresent.type,
         alreadyPresent.name,
         alreadyPresent.value,
         cf.type,
         cf.name,
         cf.value
      }
      dest.put (cf.name, cf)
   }     
   
   public SourceSetConfig (String name)
   {
      this.name = name
   }
   
   @Override
   String toString ()
   {
        "name=$name, ${super.toString ()}"
   }
   
   SourceSetConfig plus (SourceSetConfig other)
   {
      SourceSetConfig ncfg = new SourceSetConfig (other.name)
      ncfg.appName = other.appName ?: this.appName
      ncfg.packageName = other.packageName ?: this.packageName
      ncfg.version = other.version ?: this.version
      ncfg.clsName = other.clsName ?: this.clsName
      other.buildConfigFields.each {
         if (it.value != null)
            ncfg.buildConfigFields.put (it.key, it.value)
      }
      this.buildConfigFields.each {
         if (it.value != null)
            ncfg.buildConfigFields.put (it.key, it.value)
      }
      return ncfg
   }
   
}
