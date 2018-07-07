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

import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Malte Fürstenau
 */
class BuildConfigPlugin implements Plugin<Project>
{

   static final String MAIN_SOURCESET = "main"
   static final String FD_SOURCE_OUTPUT = "gen/buildconfig/src"
   static final String FD_CLASS_OUTPUT = "gen/buildconfig/classes"
   static final String DEFAULT_EXTENSION_NAME = "buildConfig"
   static final String DEFAULT_CLASS_NAME = "BuildConfig"
   static final String DEFAULT_SOURCESET = MAIN_SOURCESET
   static final String DEFAULT_NAME_FIELDNAME = "NAME"
   static final String DEFAULT_VERSION_FIELDNAME = "VERSION"
   static final String DEFAULT_PACKAGENAME = "de.fuerstenau.buildconfig"

   private static final Logger LOG = LoggerFactory.getLogger (
      BuildConfigPlugin.class.getCanonicalName ())

   private Project p

   /**
    * Get the compile-{@link org.gradle.api.artifacts.Configuration }
    * corresponding to the name of given {@link BuildConfigSourceSet}.
    *
    * @param cfg BuildConfigSourceSet
    * @return corresponding <i>compile</i>-Configuration
    *
    * @exception UnknownConfigurationException is thrown if there is no
    * corresponding <i>compile</i>-{@link org.gradle.api.artifacts.Configuration }
    */
   private Configuration getCompileConfiguration (BuildConfigSourceSet cfg)
   {
      String configurationName = MAIN_SOURCESET.equals (cfg.name) ?\
            "compile" : "${cfg.name}Compile"
      try
      {
         p.configurations.getByName (configurationName)
      }
      catch (UnknownConfigurationException ex)
      {
         throw new GradleException (
                "Configuration <${configurationName}> not found.", ex)
      }
   }

   /**
    * Get the corresponding {@link org.gradle.api.tasks.SourceSet} to the
    * {@link BuildConfigSourceSet}.
    *
    * @param cfg BuildConfigSourceSet
    * @return SourceSet
    *
    * @exception UnknownDomainObjectException is thrown when there is no
    * corresponding {@link org.gradle.api.tasks.SourceSet}
    */
   private SourceSet getSourceSet (BuildConfigSourceSet cfg)
   {
      final SourceSet sourceSet
      try
      {
         sourceSet = p.convention.getPlugin (JavaPluginConvention).sourceSets.getByName (cfg.name)
      }
      catch (UnknownDomainObjectException ex)
      {
         throw new GradleException ("SourceSet <${cfg.name}> not found.", ex)
      }
      return sourceSet
   }

   private static String getTaskName (String prefix, String sourceSetName,
      String suffix)
   {
      MAIN_SOURCESET.equals (sourceSetName) ?\
            "${prefix}${suffix}" :
            "${prefix}${sourceSetName.capitalize()}${suffix}"
   }

   private GenerateBuildConfigTask createGenerateTask (Project p , BuildConfigSourceSet cfg)
   {
      final String generateTaskName = getTaskName ("generate", cfg.name, "BuildConfig")

      final GenerateBuildConfigTask generate = p.task (generateTaskName, type: GenerateBuildConfigTask) {
         /* configure generate task with values from the extension */
         packageName = cfg.packageName ?: p.group ?: DEFAULT_PACKAGENAME
         clsName = cfg.clsName ?: DEFAULT_CLASS_NAME
         appName = cfg.appName ?: p.name
         version = cfg.version ?: p.version
         useGetters = cfg.useGetters ?: false
         if (cfg.charset != null)
            charset = cfg.charset
         cfg.classFields.values ().each { ClassField cf ->
            addClassField cf
         }
      }
      return generate
   }

   private JavaCompile createCompileTask (Project p , BuildConfigSourceSet cfg, GenerateBuildConfigTask generate)
   {
      final String compileTaskName = getTaskName ("compile", cfg.name, "BuildConfig")

      final JavaCompile compile = p.task (compileTaskName, type: JavaCompile, dependsOn: generate) {
         /* configure compile task */
         classpath = p.files ()
         destinationDir = new File ("${p.buildDir}/${FD_CLASS_OUTPUT}/${cfg.name}")
         source = generate.outputDir
      }
      return compile
   }

   @Override
   void apply (Project p)
   {
      this.p = p
      p.apply plugin: 'java'

      /* create the configuration closure */
      p.extensions.create (DEFAULT_EXTENSION_NAME, BuildConfigExtension, p)

      /* evaluate the configuration closure */
      p.afterEvaluate {
         getBuildConfigSourceSets ().each { BuildConfigSourceSet cfg ->
            SourceSet sourceSet = getSourceSet (cfg)
            final Configuration compileCfg = getCompileConfiguration (cfg)

            final GenerateBuildConfigTask generate = createGenerateTask (p, cfg)

            generate.outputDir = p.buildDir.toPath ()
            .resolve (BuildConfigPlugin.FD_SOURCE_OUTPUT)
            .resolve (cfg.name ?: DEFAULT_SOURCESET)
            .toFile ()

            LOG.info ("Created task <{}> for sourceSet <{}>.", generate.name, cfg.name)

            final JavaCompile compile = createCompileTask (p, cfg, generate)

            LOG.info ("Created compiling task <{}> for sourceSet <{}>", compile.name, cfg.name)

            if (p.plugins.hasPlugin ('org.gradle.eclipse'))
            {
               LOG.debug ('Eclipse plugin is found. Make compile dependend on eclipse task.')
               try {
                  compile.dependsOn 'eclipseClasspath'
               } catch (UnknownTaskException ex) {
                  LOG.warn ('Probed for eclipse task but none is defined even though EclipsePlugin is found.')
               }
            }
            /* workaround for Eclipse, running eclipse task after will add this to classpath,
             * since Gradle 3.0 this has to be wrapped into ConfigurableFileCollection */
            FileCollection compiledClasses = p.files (compile.outputs.files.filter { f ->
               !f.name.endsWith ('dependency-cache')
            })

            /* this is no longer possible by gradle 3.0 */
            compiledClasses.builtBy compile
            /*
             * add dependency for sourceset compile configturation */
            /* previously, we'd add the files via dependency:
             * compileCfg.dependencies.add (p.dependencies.create (compiledClasses))
             */
            sourceSet.compileClasspath += compiledClasses
            /* add to classpath, but not the dependency-cache */
             compiledClasses.each { f ->
                 sourceSet.output.dir f
             }

            /*
             * previously the jar would be manipulated via:
             * def jarTaskName = p.convention.getPlugin (JavaPluginConvention).sourceSets.getByName (cfg.name).getJarTaskName()
             *        if (jarTaskName) {
             *           Jar jar = (Jar) p.tasks[jarTaskName]
             *           jar.configure {
             *           from compile.outputs
             *        }
             * }
             */

            LOG.info ("Added task <{}> output files as dependency for configuration <{}>", compile.name, compileCfg.name)

            if (p.plugins.hasPlugin ('org.gradle.idea'))
            {
               LOG.debug ('IDEA plugin is found.')
               Configuration providedCfg = p.configurations.create(getTaskName('provided', cfg.name, 'BuildConfig'))
               providedCfg.dependencies.add (p.dependencies.create (compiledClasses))
               p.idea.module.scopes.PROVIDED.plus += [ providedCfg ]
            }
         }


         LOG.debug "BuildConfigPlugin loaded"
      }
   }

   private List<BuildConfigSourceSet> getBuildConfigSourceSets ()
   {
      BuildConfigExtension ext = p.extensions.getByType (BuildConfigExtension)

      List<BuildConfigSourceSet>  res = new ArrayList<> ()

      if (ext.sourceSets.size () > 0)
      {
         ext.sourceSets.each { BuildConfigSourceSet cfg ->
            res.add (ext + cfg)
         }
      }
      else
      {
         res.add(ext + new BuildConfigSourceSet ("main"))
      }
      return res
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
