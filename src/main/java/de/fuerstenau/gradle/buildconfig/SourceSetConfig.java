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
package de.fuerstenau.gradle.buildconfig;

import java.util.LinkedHashMap;
import java.util.Map;
import org.gradle.api.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Malte Fürstenau
 */
public class SourceSetConfig implements Named
{
   private static final Logger LOG = LoggerFactory.getLogger (SourceSetConfig.class.getCanonicalName ());

   private final String name;
   private String version;
   private String appName;
   private String packageName;
   private final Map<String, ClassField> classFields = new LinkedHashMap<>();

   public SourceSetConfig (String name)
   {
      this.name = name;
   }

   Map<String, ClassField> getBuildConfigFields ()
   {
      return classFields;
   }
   
   
   public void buildConfigField (String type, String name, String value)
   {
      addClassField (type, name, value);
   }
   
   void addClassField (String type, String name, String value)
   {
      addClassField (classFields, new ClassFieldImpl (type, name, value));
   }   
   private void addClassField (Map<String, ClassField> dest, ClassField cf)
   {
      ClassField alreadyPresent = dest.get (cf.getName ());

      if (alreadyPresent != null)
      {
         LOG.debug (
                 "{}: buildConfigField <{}/{}/{}> exists, replacing with <{}/{}/{}>",
                 getName (),
                 alreadyPresent.getType (),
                 alreadyPresent.getName (),
                 alreadyPresent.getValue (),
                 cf.getType (),
                 cf.getName (),
                 cf.getValue ()
         );
      }
      dest.put (cf.getName (), cf);
   }

   @Override
   public String getName ()
   {
      return name;
   }

   public String getAppName ()
   {
      return appName;
   }

   public void setAppName (String appname)
   {
      this.appName = appname;
   }

   public String getVersion ()
   {
      return version;
   }

   public void setVersion (String version)
   {
      this.version = version;
   }

   public String getPackageName ()
   {
      return packageName;
   }

   public void setPackageName (String packageName)
   {
      this.packageName = packageName;
   }
}
