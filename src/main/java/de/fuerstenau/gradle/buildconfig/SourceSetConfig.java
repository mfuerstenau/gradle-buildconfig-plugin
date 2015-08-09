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
