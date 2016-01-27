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

import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.internal.project.AbstractProject
import org.gradle.api.internal.project.ProjectStateInternal
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.testfixtures.ProjectBuilder
import static org.assertj.core.api.Assertions.assertThat
/**
 *
 */
class BuildConfigPluginTest {

   public BuildConfigPluginTest() {
   }

   @BeforeClass
   public static void setUpClass() {
   }

   @AfterClass
   public static void tearDownClass() {
   }

   @Before
   public void setUp() {
   }

   @Test
   public void testPlugin() {
      Project project = ProjectBuilder.builder().withName ("testProject").build()
      project.pluginManager.apply 'java'
      project.pluginManager.apply 'de.fuerstenau.buildconfig'
        
      project.sourceSets {
         testSet1
      }

      project.buildConfig {
         version = "meineVersion"
         packageName = "de.testpackage"
      }
        
      ProjectStateInternal projectState = new ProjectStateInternal();
      projectState.executed();
      ProjectEvaluationListener evaluationListener = ((AbstractProject) project).getProjectEvaluationBroadcaster();
      evaluationListener.afterEvaluate(project, projectState);
      
      println "tasks: $project.tasks"
        
      GenerateBuildConfigTask gTask = project.tasks.getByName "generateBuildConfig"

      assertThat (gTask.appName).isEqualTo ("testProject") // Default: Projektname
      assertThat (gTask.version).isEqualTo ("meineVersion")
      assertThat (gTask.packageName).isEqualTo ("de.testpackage")
      assertThat (gTask.clsName).isEqualTo ("BuildConfig") // Default: BuildConfig
      assertThat (gTask.classFields).isEmpty ()

      JavaCompile cTask = project.tasks.getByName "compileBuildConfig"
      
      final Set<Object> cDeps = cTask.getDependsOn()
      
      /* compileBuildConfigTask depends on generateBuildConfigTask */
      assertThat (cDeps.any {Object o ->
         o == gTask
      }).isTrue ();
      
      /* compileBuildConfigTask depends on source files */
      assertThat (cDeps.any {Object o ->
         o in FileCollection && (o as FileCollection).source == (cTask.inputs.inputFiles + cTask.inputs.sourceFiles).source
      }).isTrue ();
     
      /* compile configuration depends on generated classes output dir, resolved */
      assertThat (project.configurations.getByName ("compile").dependencies.any { Dependency dep ->
         dep in FileCollectionDependency && (dep as FileCollectionDependency).resolve ().any { File f ->
            f.path.endsWith ('\\buildConfigClasses\\main')
         }
      }).isTrue ()
   }
      
   @After
   public void tearDown() {
   }
}
