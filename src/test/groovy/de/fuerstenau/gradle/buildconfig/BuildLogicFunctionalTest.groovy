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

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import java.lang.reflect.Field

class BuildLogicFunctionalTest extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    File settingsFile
    File genClassesDir
    File genSourcesDir
    File libsDir
    File srcFile
    String packageName = 'org.sample'
    String buildConfigName = 'MainConfig'
    String testProjectName = 'testProject'
    String testProjectVersion = '1.2-SNAPSHOT'
    List<File> pluginClasspath

    String sourceSetSourcesDir (String sourceSet)
    {
        new File (genSourcesDir, sourceSet).toString ().replace ('\\', '/')
    }

    String sourceSetClassesDir (String sourceSet)
    {
        new File (genClassesDir, sourceSet).toString ().replace ('\\', '/')
    }
    
    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        settingsFile = testProjectDir.newFile('settings.gradle')
        libsDir = new File (testProjectDir.root, 'build/libs')
        genSourcesDir = new File (testProjectDir.root, BuildConfigPlugin.FD_SOURCE_OUTPUT)
        genClassesDir = new File (testProjectDir.root, BuildConfigPlugin.FD_CLASS_OUTPUT)

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
    }

    static String classPathDependencyStr (List<File> classpath)
    {
        return 'classpath files (' + classpath
            .findAll ({ !(it.toString ().contains(".wrapper") || it.toString ().contains (".gradle")) })
            .collect({ "'${it.toString ().replace ('\\', '/')}'"}).join (', ') + ')'
    }
    
    def 'test buildconfig simple manual wiring' () {
        given: 'buildscript is prepared and project built'
            /* setting the project name of the test project */
            settingsFile << "rootProject.name = '${testProjectName}'"
            /* */
            def pluginClasspathAsFileCollcetion = 

            buildFile << """import de.fuerstenau.gradle.buildconfig.GenerateBuildConfigTask

            buildscript {
                dependencies {
                    ${classPathDependencyStr (pluginClasspath)}
                }
            }

            plugins {
                id 'java'
            }

            version = '${testProjectVersion}'

            sourceCompatibility = '1.7'
            [compileJava, compileTestJava]*.options*.encoding = 'UTF-8'

            task generateBuildConfig (type: GenerateBuildConfigTask) {
                appName = 'SuperTrooperStarshipApp'
                version = version // same as project version
                clsName = '${buildConfigName}'
                packageName = '${packageName}'
                outputDir = new File ('${sourceSetSourcesDir ('main')}')
                buildConfigField 'int', 'MY_INT', '42'
                buildConfigField 'float', 'MY_FLOAT', '2.5345f'
                buildConfigField 'double', 'MY_DOUBLE', '2.1423423'
                buildConfigField 'long', 'MY_LONG', '123L'
                buildConfigField 'String', 'MY_STR', 'my string'
                buildConfigField 'byte', 'MY_BYTE', '(byte) 0xff'
                buildConfigField 'byte[]', 'MY_BYTEARR', '{ (byte) 0xff, (byte) 0xa, (byte) 0x20 }'
            }

            task compileBuildConfig(type:JavaCompile, dependsOn: generateBuildConfig) {
                classpath = files ()
                destinationDir = new File ('${sourceSetClassesDir('main')}')
                source = generateBuildConfig.outputDir
            }

            compileJava.dependsOn compileBuildConfig
            sourceSets {
                main {
                    compileClasspath += compileBuildConfig.outputs.files
                    output.dir compileBuildConfig.outputs.files.first ()
                }
            }
            """
        
        when:
            def result = GradleRunner.create()
            .withPluginClasspath(pluginClasspath)
            .withProjectDir(testProjectDir.root)
            .withArguments('build')
            .build ()
            
            println result.output
            
        then: 'buildconfig class exists'
            buildConfigClassFileExists ('main')
        then: 'all fields are in the class file'
            checkFields (fieldsFromClassFile (new File (genClassesDir, 'main').toString (), 'org.sample.MainConfig'))
        then: 'all fields are in the class file (in the jar file)'
            checkFields (fieldsFromJar (new File (libsDir, "${testProjectName}-${testProjectVersion}.jar").toString (), 'org.sample.MainConfig'))
    }
    
    def cleanup ()
    {
       triggerGC ()
    }
    
    def checkFields (def fields) {
        def expected = [
            'VERSION/java.lang.String/1.2-SNAPSHOT',
            'NAME/java.lang.String/SuperTrooperStarshipApp',
            'MY_INT/int/42',
            'MY_FLOAT/float/2.5345',
            'MY_DOUBLE/double/2.1423423',
            'MY_LONG/long/123',
            'MY_STR/java.lang.String/my string',
            'MY_BYTE/byte/-1',
            'MY_BYTEARR/[B/[-1, 10, 32]'
        ]
        fields.intersect (expected).size () == expected.size ()
    }
    
    def buildConfigClassFileExists (String sourceSet)
    {
        new File (sourceSetClassesDir (sourceSet), packageName.replace ('.', '/') + '/' + buildConfigName + '.class').exists ()
    }
    
    List<String> getFields (Class<?> clazz)
    {
       clazz.declaredFields.collect {
            "${it.name}/${it.type.name}/${it.get (null)}"
        }
    }
   
    List<String> fieldsFromJar (String jarFile, String fullyQualifiedClass)
    {
      URL url = urlForJar (jarFile)  
      Class<?> clazz = loadClass (url, fullyQualifiedClass)
      getFields (clazz)
    }
    
    List<String> fieldsFromClassFile (String classesDir, String fullyQualifiedClass)
    {
      URL url = urlForClasses (classesDir)  
      Class<?> clazz = loadClass (url, fullyQualifiedClass)
      getFields (clazz)
    }
    
    URL urlForJar (String jarFile)
    {
        new URL("jar", "", new File(jarFile).toURL ().toString () + "!/")
    }

   URL urlForClasses (String classesDir)
    {
        new File(classesDir).toURL ()
    }
    
    Class<?> loadClass (URL url, String fullyQualifiedClass)
    {
        ClassLoader cl = new URLClassLoader(url);
        cl.loadClass(fullyQualifiedClass);
    }
    
    private static void triggerGC() throws InterruptedException {
        System.out.println("\n-- Starting GC");
        System.gc();
        Thread.sleep(100);
        System.out.println("-- End of GC\n");
    }   
}

