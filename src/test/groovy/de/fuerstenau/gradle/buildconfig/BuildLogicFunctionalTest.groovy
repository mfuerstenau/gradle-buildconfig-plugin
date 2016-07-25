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
import spock.lang.*
import de.fuerstenau.gradle.buildconfig.TestUtil
import java.lang.reflect.Field

class BuildLogicFunctionalTest extends Specification {

   @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
   
   File buildFile
   File settingsFile
   File buildDir
   
   @Shared
   List<File> pluginClasspath
    
   def setupSpec ()
   {
      def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
      if (pluginClasspathResource == null) {
         throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
      }

      pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
      
   }
   
   def setup() {
      buildFile = testProjectDir.newFile('build.gradle')
      settingsFile = testProjectDir.newFile('settings.gradle')
      buildDir = new File (testProjectDir.root, 'build')
   }

    
   def 'buildconfig simple manual wiring' () {
      setup: 'buildscript is prepared and project built'
      /* setting the project name of the test project */
      settingsFile << "rootProject.name = 'testProject'"
      /* */
      def pluginClasspathAsFileCollcetion = 

      buildFile << """import de.fuerstenau.gradle.buildconfig.GenerateBuildConfigTask

            buildscript {
                dependencies {
                    ${TestUtil.classPathDependencyStr (pluginClasspath)}
                }
            }

            plugins {
                id 'java'
            }

            version = '1.2-SNAPSHOT'

            task generateBuildConfig (type: GenerateBuildConfigTask) {
                appName = 'SuperTrooperStarshipApp'
                version = version // same as project version
                clsName = 'MainConfig'
                packageName = 'org.sample'
                outputDir = new File (\"\${buildDir}/gen/buildconfig/src/main/\")
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
                destinationDir = new File (\"\${buildDir}/gen/buildconfig/classes/main/\")
                source = generateBuildConfig.outputDir
            }

            //compileJava.dependsOn compileBuildConfig
            sourceSets {
                main {
                    compileClasspath += compileBuildConfig.outputs.files
                    output.dir compileBuildConfig.outputs.files.first ()
                }
            }
            """
      println ("--- build.gradle ---")
      buildFile.readLines ().each { line ->
         println line
      }
        
      when:
      def result = GradleRunner.create()
              .withGradleVersion(gradleVersion)
              .withProjectDir(testProjectDir.root)
              .withArguments('clean', 'build')
              .build ()
            
      println result.output
            
      then: 'buildconfig class exists'
         /* checking the existence of the generated source */
         def buildConfigSource = new File (testProjectDir.root, 'build/gen/buildconfig/src/main/org/sample/MainConfig.java')
         buildConfigSource.exists ()
         /* checking the existence of the generated class */
         def buildConfigClass = new File (testProjectDir.root, 'build/gen/buildconfig/classes/main/org/sample/MainConfig.class')
         buildConfigClass.exists ()

      and: 'jar exists'
         def jarFile = new File (testProjectDir.root, 'build/libs/testProject-1.2-SNAPSHOT.jar')
         jarFile.exists ()

      and: 'all fields are in the class file'
         with (TestUtil.fieldsFromClass (new File (testProjectDir.root, "build/gen/buildconfig/classes/main"), 'org.sample.MainConfig')) { fields->
            fields.contains 'NAME/java.lang.String/SuperTrooperStarshipApp'
            fields.contains 'VERSION/java.lang.String/1.2-SNAPSHOT'
            fields.contains 'MY_INT/int/42'
            fields.contains 'MY_FLOAT/float/2.5345'
            fields.contains 'MY_DOUBLE/double/2.1423423'
            fields.contains 'MY_LONG/long/123'
            fields.contains 'MY_STR/java.lang.String/my string'
            fields.contains 'MY_BYTE/byte/-1'
            fields.contains 'MY_BYTEARR/[B/[-1, 10, 32]'
      }

      and: 'all fields are in the class file'
         with (TestUtil.fieldsFromJar (new File (testProjectDir.root, "build/libs/testProject-1.2-SNAPSHOT.jar"), 'org.sample.MainConfig')) { fields->
            fields.contains 'NAME/java.lang.String/SuperTrooperStarshipApp'
            fields.contains 'VERSION/java.lang.String/1.2-SNAPSHOT'
            fields.contains 'MY_INT/int/42'
            fields.contains 'MY_FLOAT/float/2.5345'
            fields.contains 'MY_DOUBLE/double/2.1423423'
            fields.contains 'MY_LONG/long/123'
            fields.contains 'MY_STR/java.lang.String/my string'
            fields.contains 'MY_BYTE/byte/-1'
            fields.contains 'MY_BYTEARR/[B/[-1, 10, 32]'
         }
      where:
         gradleVersion << ['2.9', '2.14']
   }
   
   def 'buildconfig empty closure' () {
      setup: 'buildscript is prepared'
      /* setting the project name of the test project */
      settingsFile << "rootProject.name = 'testProject'"
      /* */
      def pluginClasspathAsFileCollcetion = 

      buildFile << """import de.fuerstenau.gradle.buildconfig.GenerateBuildConfigTask

            plugins {
                id 'java'
                id 'de.fuerstenau.buildconfig'
            }

            buildConfig {
            }
            """
      println ("--- build.gradle ---")
      buildFile.readLines ().each { line ->
         println line
      }
        
      when: 'running tasks [clean build]'
      def result = GradleRunner.create()
              .withGradleVersion(gradleVersion)
              .withPluginClasspath(pluginClasspath)
              .withProjectDir(testProjectDir.root)
              .withArguments('clean', 'build')
              .build ()
            
      println result.output
            
      then: 'buildconfig source code exists'
         /* checking the existence of the generated source */
         def buildConfigSource = new File (testProjectDir.root, 'build/gen/buildconfig/src/main/de/fuerstenau/buildconfig/BuildConfig.java')
         buildConfigSource.exists ()
      and: 'buildconfig class file exists'
      /* checking the existence of the generated class */
         def buildConfigClass = new File (testProjectDir.root, 'build/gen/buildconfig/classes/main/de/fuerstenau/buildconfig/BuildConfig.class')
         buildConfigClass.exists ()

      and: 'project jar file exists'
         def jarFile = new File (testProjectDir.root, 'build/libs/testProject.jar')
         jarFile.exists ()

      and: 'buildconfig in class file has correct fields'
         with (TestUtil.fieldsFromClass (new File (testProjectDir.root, "build/gen/buildconfig/classes/main"), 'de.fuerstenau.buildconfig.BuildConfig')) { fields->
            fields.contains 'VERSION/java.lang.String/unspecified'
            fields.contains 'NAME/java.lang.String/testProject'
      }

      and: 'buildconfig in jar file has correct fields'
         with (TestUtil.fieldsFromJar (new File (testProjectDir.root, "build/libs/testProject.jar"), 'de.fuerstenau.buildconfig.BuildConfig')) { fields->
            fields.contains 'VERSION/java.lang.String/unspecified'
            fields.contains 'NAME/java.lang.String/testProject'
         }
      where:
         gradleVersion << ['2.9', '2.14']
   }
    
   def 'buildconfig empty closure with some properties set' () {
      setup: 'buildscript is prepared and project built'
      /* setting the project name of the test project */
      settingsFile << "rootProject.name = 'testProject'"
      /* */
      def pluginClasspathAsFileCollcetion = 

      buildFile << """import de.fuerstenau.gradle.buildconfig.GenerateBuildConfigTask

            plugins {
                id 'java'
                id 'de.fuerstenau.buildconfig'
            }

            version = '1.3.3'
            group = 'org.sample'

            buildConfig {
               clsName = 'MyConfig'
            }

            """
      println ("--- build.gradle ---")
      buildFile.readLines ().each { line ->
         println line
      }
        
      when:
      def result = GradleRunner.create()
              .withGradleVersion(gradleVersion)
              .withPluginClasspath(pluginClasspath)
              .withProjectDir(testProjectDir.root)
              .withArguments('clean', 'build')
              .build ()
            
      println result.output
            
      then: 'buildconfig class exists'
         /* checking the existence of the generated source */
         def buildConfigSource = new File (testProjectDir.root, 'build/gen/buildconfig/src/main/org/sample/MyConfig.java')
         buildConfigSource.exists ()
         /* checking the existence of the generated class */
         def buildConfigClass = new File (testProjectDir.root, 'build/gen/buildconfig/classes/main/org/sample/MyConfig.class')
         buildConfigClass.exists ()

      and: 'jar exists'
         def jarFile = new File (testProjectDir.root, 'build/libs/testProject-1.3.3.jar')
         jarFile.exists ()

      and: 'all fields are in the class file'
         with (TestUtil.fieldsFromClass (new File (testProjectDir.root, "build/gen/buildconfig/classes/main"), 'org.sample.MyConfig')) { fields->
            fields.contains 'VERSION/java.lang.String/1.3.3'
            fields.contains 'NAME/java.lang.String/testProject'
      }

      and: 'all fields are in the class file'
         with (TestUtil.fieldsFromJar (new File (testProjectDir.root, "build/libs/testProject-1.3.3.jar"), 'org.sample.MyConfig')) { fields->
            fields.contains 'VERSION/java.lang.String/1.3.3'
            fields.contains 'NAME/java.lang.String/testProject'
         }
      where:
         gradleVersion << ['2.9', '2.14']
   }
    
   def 'buildconfig empty closure with some properties set and two sourcesets' () {
      setup: 'buildscript is prepared and project built'
      /* setting the project name of the test project */
      settingsFile << "rootProject.name = 'testProject'"
      /* */
      def pluginClasspathAsFileCollcetion = 

      buildFile << """import de.fuerstenau.gradle.buildconfig.GenerateBuildConfigTask

            plugins {
                id 'java'
                id 'de.fuerstenau.buildconfig'
            }

            sourceSets {
               other
            }

            task otherJar (type: Jar) {
               baseName = baseName + "-other"
               from sourceSets.other.output
            }

            artifacts {
               archives otherJar
            }
            version = '1.3.3'
            group = 'org.sample'

            buildConfig {
               sourceSets {
                  other
               }
            }

            """
      println ("--- build.gradle ---")
      buildFile.readLines ().each { line ->
         println line
      }
        
      when:
      def result = GradleRunner.create()
              .withGradleVersion(gradleVersion)
              .withPluginClasspath(pluginClasspath)
              .withProjectDir(testProjectDir.root)
              .withArguments('clean', 'build')
              .build ()
            
      println result.output
            
      then: 'buildconfig class exists'
         /* checking the existence of the generated source */
         def buildConfigSourceBlub = new File (testProjectDir.root, 'build/gen/buildconfig/src/other/org/sample/BuildConfig.java')
         buildConfigSourceBlub.exists ()
         /* checking the existence of the generated class */
         def buildConfigClassBlub = new File (testProjectDir.root, 'build/gen/buildconfig/classes/other/org/sample/BuildConfig.class')
         buildConfigClassBlub.exists ()
         /* checking the existence of the generated source */
         def buildConfigSourceMain = new File (testProjectDir.root, 'build/gen/buildconfig/src/main/org/sample/BuildConfig.java')
         !buildConfigSourceMain.exists ()
         /* checking the existence of the generated class */
         def buildConfigClassMain = new File (testProjectDir.root, 'build/gen/buildconfig/classes/main/org/sample/BuildConfig.class')
         !buildConfigClassMain.exists ()

      and: 'jar exists'
         def jarFile = new File (testProjectDir.root, 'build/libs/testProject-1.3.3.jar')
         jarFile.exists ()

      and: 'all fields are in the class file'
         with (TestUtil.fieldsFromClass (new File (testProjectDir.root, "build/gen/buildconfig/classes/other"), 'org.sample.BuildConfig')) { fields->
            fields.contains 'VERSION/java.lang.String/1.3.3'
            fields.contains 'NAME/java.lang.String/testProject'
      }

      and: 'all fields are in the class file'
         with (TestUtil.fieldsFromJar (new File (testProjectDir.root, "build/libs/testProject-other-1.3.3.jar"), 'org.sample.BuildConfig')) { fields->
            fields.contains 'VERSION/java.lang.String/1.3.3'
            fields.contains 'NAME/java.lang.String/testProject'
         }
      where:
         gradleVersion << ['2.9', '2.14']
   }
    
   def 'buildconfig more complex closure' () {
      setup: 'buildscript is prepared and project built'
      /* setting the project name of the test project */
      settingsFile << "rootProject.name = 'testProject'"
      /* */
      def pluginClasspathAsFileCollcetion = 

      buildFile << """import de.fuerstenau.gradle.buildconfig.GenerateBuildConfigTask

            plugins {
                id 'java'
                id 'de.fuerstenau.buildconfig'
            }

            version = '1.2-SNAPSHOT'

            buildConfig {
                appName = 'SuperTrooperStarshipApp'
                version = version
                clsName = 'MainConfig'
                packageName = 'org.sample'
                buildConfigField 'int', 'MY_INT', '42'
                buildConfigField 'float', 'MY_FLOAT', '2.5345f'
                buildConfigField 'double', 'MY_DOUBLE', '2.1423423'
                buildConfigField 'long', 'MY_LONG', '123L'
                buildConfigField 'String', 'MY_STR', 'my string'
                buildConfigField 'byte', 'MY_BYTE', '(byte) 0xff'
                buildConfigField 'byte[]', 'MY_BYTEARR', '{ (byte) 0xff, (byte) 0xa, (byte) 0x20 }'
            }
            """
      println ("--- build.gradle ---")
      buildFile.readLines ().each { line ->
         println line
      }
        
      when:
      def result = GradleRunner.create()
              .withGradleVersion(gradleVersion)
              .withPluginClasspath(pluginClasspath)
              .withProjectDir(testProjectDir.root)
              .withArguments('clean', 'build')
              .build ()
            
      println result.output
            
      then: 'buildconfig class exists'
         /* checking the existence of the generated source */
         def buildConfigSource = new File (testProjectDir.root, 'build/gen/buildconfig/src/main/org/sample/MainConfig.java')
         buildConfigSource.exists ()
         /* checking the existence of the generated class */
         def buildConfigClass = new File (testProjectDir.root, 'build/gen/buildconfig/classes/main/org/sample/MainConfig.class')
         buildConfigClass.exists ()

      and: 'jar exists'
         def jarFile = new File (testProjectDir.root, 'build/libs/testProject-1.2-SNAPSHOT.jar')
         jarFile.exists ()

      and: 'all fields are in the class file'
         with (TestUtil.fieldsFromClass (new File (testProjectDir.root, "build/gen/buildconfig/classes/main"), 'org.sample.MainConfig')) { fields->
            fields.contains 'NAME/java.lang.String/SuperTrooperStarshipApp'
            fields.contains 'VERSION/java.lang.String/1.2-SNAPSHOT'
            fields.contains 'MY_INT/int/42'
            fields.contains 'MY_FLOAT/float/2.5345'
            fields.contains 'MY_DOUBLE/double/2.1423423'
            fields.contains 'MY_LONG/long/123'
            fields.contains 'MY_STR/java.lang.String/my string'
            fields.contains 'MY_BYTE/byte/-1'
            fields.contains 'MY_BYTEARR/[B/[-1, 10, 32]'
      }

      and: 'all fields are in the class file'
         with (TestUtil.fieldsFromJar (new File (testProjectDir.root, "build/libs/testProject-1.2-SNAPSHOT.jar"), 'org.sample.MainConfig')) { fields->
            fields.contains 'NAME/java.lang.String/SuperTrooperStarshipApp'
            fields.contains 'VERSION/java.lang.String/1.2-SNAPSHOT'
            fields.contains 'MY_INT/int/42'
            fields.contains 'MY_FLOAT/float/2.5345'
            fields.contains 'MY_DOUBLE/double/2.1423423'
            fields.contains 'MY_LONG/long/123'
            fields.contains 'MY_STR/java.lang.String/my string'
            fields.contains 'MY_BYTE/byte/-1'
            fields.contains 'MY_BYTEARR/[B/[-1, 10, 32]'
         }
      where:
         gradleVersion << ['2.9', '2.14']
   }
    
   def cleanup ()
   {
      TestUtil.triggerGC ()
   }
}