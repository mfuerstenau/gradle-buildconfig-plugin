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

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.PluginManager
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskCollection
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
    
    private Configuration getCompileConfiguration (SourceSetConfig cfg)
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

    private void assertSourceSet (SourceSetConfig cfg)
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
      
        if (sourceSet == null)
        throw new GradleException ("SourceSet <${cfg.name}> not found.", ex)
    }

    private static String getTaskName (String prefix, String sourceSetName,
        String suffix)
    {
        MAIN_SOURCESET.equals (sourceSetName) ?\
            "${prefix}${suffix}" :
            "${prefix}${sourceSetName.capitalize()}${suffix}"
    }

   
    private GenerateBuildConfigTask createGenerateTask (Project p , SourceSetConfig cfg)
    {
        final String generateTaskName = getTaskName ("generate", cfg.name, "BuildConfig")
      
        final GenerateBuildConfigTask generate = p.task (generateTaskName, type: GenerateBuildConfigTask) {
            /* configure generate task with values from the extension */
            packageName = cfg.packageName ?: p.group ?: DEFAULT_PACKAGENAME
            clsName = cfg.clsName ?: DEFAULT_CLASS_NAME
            appName = cfg.appName ?: p.name
            version = cfg.version ?: p.version
            cfg.buildConfigFields.values().each { ClassField cf ->
                addClassField cf
            }
        }
        return generate
    }
   
    private JavaCompile createCompileTask (Project p , SourceSetConfig cfg, GenerateBuildConfigTask generate)
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
            getSourceSetConfigs ().each { SourceSetConfig cfg ->
                assertSourceSet (cfg)
                final Configuration compileCfg = getCompileConfiguration (cfg)

                final GenerateBuildConfigTask generate = createGenerateTask (p, cfg)
            
                generate.outputDir = p.buildDir.toPath ()
                .resolve (BuildConfigPlugin.FD_SOURCE_OUTPUT)
                .resolve (cfg.name ?: DEFAULT_SOURCESET)
                .toFile ()
            
                LOG.info ("Created task <{}> for sourceSet <{}>.", generate.name, cfg.name)

                final JavaCompile compile = createCompileTask (p, cfg, generate)
            
                LOG.info ("Created compiling task <{}> for sourceSet <{}>", compile.name, cfg.name)

                /* add dependency for sourceset compile configturation */
                compileCfg.dependencies.add (p.dependencies.create (compile.outputs.files))
            
                LOG.info ("Added task <{}> output files as dependency for configuration <{}>", compile.name, compileCfg.name)
            }
            LOG.debug "BuildConfigPlugin loaded"
        }
    }

    private List<SourceSetConfig> getSourceSetConfigs ()
    {
        BuildConfigExtension ext = p.extensions.getByType (BuildConfigExtension)
        
        List<SourceSetConfig>  res = new ArrayList<> ()
        
        if (ext.sourceSets.size () > 0)
        {
            ext.sourceSets.each { SourceSetConfig cfg ->
                res.add (ext + cfg)
            }
        }
        else
        {
            res.add(ext + new SourceSetConfig ("main"))
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
