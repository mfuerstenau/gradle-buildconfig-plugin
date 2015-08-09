package de.fuerstenau.gradle.buildconfig;

import groovy.lang.Closure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.UnknownConfigurationException;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Malte Fürstenau
 */
public class BuildConfigPlugin implements Plugin<Project>
{

   public static final String MAIN_SOURCESET = "main";
   public static final String FD_SOURCE_OUTPUT = "buildConfigSources";
   public static final String FD_CLASS_OUTPUT = "buildConfigClasses";
   public static final String DEFAULT_EXTENSION_NAME = "buildConfig";
   public static final String DEFAULT_CLASS_NAME = "BuildConfig";
   public static final String DEFAULT_SOURCESET = MAIN_SOURCESET;
   public static final String DEFAULT_NAME_FIELDNAME = "NAME";
   public static final String DEFAULT_VERSION_FIELDNAME = "VERSION";

   private static final Logger LOG = LoggerFactory.getLogger (BuildConfigPlugin.class.getCanonicalName ());

   /**
    * Return default if value is <i>null</i>.
    *
    * @param <T> Type of value
    * @param mayBeNull Value
    * @param defaultValue Default
    * @return value or <i>null</i> if value was null
    */
   static <T> T defaultIfNull (T mayBeNull, T defaultValue)
   {
      return mayBeNull != null ? mayBeNull : defaultValue;
   }

   private static Configuration getCompileConfiguration (Project p, SourceSetConfig cfg)
   {
      Configuration res;
      final String sourceSetName = cfg.getName ();
      String configurationName;
      if (MAIN_SOURCESET.equals (sourceSetName))
         configurationName = "compile";
      else
         configurationName = sourceSetName + "Compile";

      try
      {
         res = p.getConfigurations ().getByName (configurationName);
      }
      catch (UnknownConfigurationException ex)
      {
         throw new GradleException (
                 String.format ("configuration {} not found. skipping.",
                         configurationName), ex);
      }
      return res;
   }

   private static SourceSet getSourceSet (Project p, SourceSetConfig cfg)
   {
      SourceSet res;
      try
      {
         JavaPluginConvention conv = (JavaPluginConvention) p.getConvention ()
                 .getPlugin (JavaPluginConvention.class);
         res = conv.getSourceSets ().getByName (cfg.getName ());
      }
      catch (UnknownDomainObjectException ex)
      {
         throw new GradleException (
                 String.format ("sourceSet <%s> not found. skipping.",
                         cfg.getName ()), ex);
      }
      return res;

   }

   private static String getTaskName (String prefix, String sourceSetName,
           String suffix)
   {
      if (MAIN_SOURCESET.equals (sourceSetName))
         return prefix + suffix;
      else
         return prefix + StringGroovyMethods.capitalize (sourceSetName)
                 + suffix;
   }

   @Override
   public void apply (final Project p)
   {

      /* create the configuration closure */
      p.getExtensions ().create (DEFAULT_EXTENSION_NAME, BuildConfigExtension.class, p);

      /* evaluate the configuration closure */
      p.afterEvaluate (new Closure<Void> (p)
      {
         private static final long serialVersionUID = -8313933762801809159L;

         @Override
         public Void call (Object... args)
         {
            for (final SourceSetConfig cfg : getSourceSetConfigs (p))
            {
               final Configuration compileCfg = getCompileConfiguration (p, cfg);
               final SourceSet sourceSet = getSourceSet (p, cfg);

               final String generateTaskName = getTaskName ("generate",
                       sourceSet.getName (), "BuildConfig");
               final String compileTaskName = getTaskName ("compile",
                       sourceSet.getName (), "BuildConfig");

               final GenerateBuildConfigTask generate = (GenerateBuildConfigTask) p.task (new HashMap<String, Object> ()
               {
                  private static final long serialVersionUID = 3109256773218160485L;

                  
                  {
                     put ("type", GenerateBuildConfigTask.class);
                  }
               }, generateTaskName);

               LOG.debug ("created task <{}> for sourceSet <{}>.", generateTaskName, cfg.getName ());

               /* configure generate task with values from the extension */
               generate.configure (new Closure<Void> (p)
               {
                  private static final long serialVersionUID = -8313933762801809159L;

                  @Override
                  public Void call ()
                  {
                     Object delegateObj = getDelegate ();
                     if (delegateObj instanceof GenerateBuildConfigTask)
                     {
                        GenerateBuildConfigTask task = (GenerateBuildConfigTask) delegateObj;
                        task.setPackageName (defaultIfNull (cfg.getPackageName (), getProjectGroup (p)));
                        task.setAppName (defaultIfNull (cfg.getAppName (), getProjectName (p)));
                        task.setVersion (defaultIfNull (cfg.getVersion (), getProjectVersion (p)));
                        for (ClassField cf : cfg.getBuildConfigFields ().values ())
                           task.addClassField (cf);
                     }
                     return null;
                  }
               });

               JavaCompile compile = (JavaCompile) p.task (new HashMap<String, Object> ()
               {
                  private static final long serialVersionUID = 3109256773218160485L;

                  
                  {
                     put ("type", JavaCompile.class);
                     put ("dependsOn", generate);
                  }
               }, compileTaskName);

               LOG.debug ("created compiling task <{}> for sourceSet <{}>",
                       compileTaskName,
                       cfg.getName ());

               /* configure compile task */
               compile.setClasspath (p.files ());
               compile.setDestinationDir (
                       p.getBuildDir ().toPath ().resolve (FD_CLASS_OUTPUT)
                       .resolve (cfg.getName ()).toFile ());
               compile.setSource (generate.getOutputDir ());

               /* add dependency for sourceset compile configturation */
               Dependency dep = p.getDependencies ().create (
                       compile.getOutputs ().getFiles ());
               compileCfg.getDependencies ().add (dep);

               /* also add compiled generated classes to sourceset outputs (to
                * include them in jar)*/
               sourceSet.getOutput ().dir (compile.getOutputs ().getFiles ());
            }
            return null;
         }

      });
      p.getLogger ().debug ("BuildConfigPlugin loaded.");
   }

   private static List<SourceSetConfig> getSourceSetConfigs (Project p)
   {
      final BuildConfigExtension buildconfigExt = p.getExtensions ().getByType (BuildConfigExtension.class);
      final List<SourceSetConfig> res = new ArrayList<> (buildconfigExt.getSourceSets ());
      return res;
   }

   static String getProjectVersion (Project p)
   {
      Object versionObj = p.getVersion ();
      while (versionObj instanceof Closure)
         versionObj = ((Closure) versionObj).call ();
      if (versionObj instanceof String)
         return (String) versionObj;
      return null;
   }

   static String getProjectName (Project p)
   {
      return p.getName ();
   }

   static String getProjectGroup (Project p)
   {
      Object groupObj = p.getGroup ();
      while (groupObj instanceof Closure)
         groupObj = ((Closure) groupObj).call ();
      if (groupObj instanceof String)
         return (String) groupObj;
      return null;
   }
}
