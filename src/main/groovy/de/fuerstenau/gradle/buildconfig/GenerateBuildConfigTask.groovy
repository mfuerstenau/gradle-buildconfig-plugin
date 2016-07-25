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

import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.DEFAULT_CLASS_NAME
import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.DEFAULT_NAME_FIELDNAME
import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.DEFAULT_PACKAGENAME
import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.DEFAULT_SOURCESET
import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.DEFAULT_VERSION_FIELDNAME
import java.nio.charset.Charset
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Malte Fürstenau
 */
class GenerateBuildConfigTask extends DefaultTask
{
   private static final Logger LOG = LoggerFactory.getLogger (
      BuildConfigPlugin.canonicalName)

   /**
    * Default charset for generated class: UTF-8.
    */
   public static final String DEFAULT_CHARSET = 'UTF-8'
   /**
    * Deletes a directory and recreates it.
    *
    * @param outputDir Directory
    * @throws IOException thrown if I/O error occurs
    */
   private static void emptyDir (final File outputDir) throws IOException
   {
      outputDir.toPath ().deleteDir ()
      outputDir.mkdirs ()
   }

   @Input
   String sourceSet

   @Input
   String charset

   /**
    * Package name for the generated class.
    */ 
   @Input
   String packageName

   /**
    * Class name of generated class.
    */ 
   @Input
   String clsName

   /**
    * Value of {@code VERSION} field of generated class.
    */ 
   @Input
   String version

   /**
    * Value of {@code Name} field of generated class.
    */ 
   @Input
   String appName

    /**
     * Target directory of generated class.
     */
   @OutputDirectory
   File outputDir

   /**
    * Fields of generated class.
    */
   @Input
   final Map<String, ClassField> classFields = new LinkedHashMap<> ()

   GenerateBuildConfigTask ()
   {
      /* configure defaults */
      version = project.version
      packageName = project.group ?: DEFAULT_PACKAGENAME
      clsName = DEFAULT_CLASS_NAME
      appName = project.name
      sourceSet = DEFAULT_SOURCESET
      charset = DEFAULT_CHARSET
      
      LOG.debug "{}: GenerateBuildConfigTask created", name
   }

   /**
    * Adds a custom field to the generated class.
    * <p>
    * Types must be Java primitive types or Objects with fully qualified names
    * if not in the Java standard library.
    * <p>
    * {@code String} or {@code char} values have to be surrounded by quotes.
    * <p>
    * Example:
    * <code>{@code buildConfigField ('String', 'MY_STR', '"my value"')
    * }</code>
    * 
    * @param type Type of the field
    * @param name Name of the field
    * @param value Value of the field
    * 
    * @see #addClassField(String, String, String)
    */
   void buildConfigField (String type, String name, String value)
   {
      addClassField (classFields, new ClassFieldImpl (type, name, value))
   }

   /**
    * Gets value for class field.
    * 
    * @param name Name of class field
    * 
    * @returns Value or <i>null</i> if unknown class field
    */
   String getClassFieldValue (String name)
   {
      ClassField cf = classFields.get (name)
      if (cf != null)
      return cf.value
      else
      return null
   }

   /**
    * Adds a class field to the generated class.
    * 
    * @param cf class field
    * 
    * @see #addClassField(String, String, String)
    */   
   void addClassField (ClassField cf)
   {
      addClassField (classFields, cf)
   }

   /**
    * Adds a class field to destination map.
    * 
    * @param dest destination map
    * @param cf class field
    */   
   void addClassField (Map<String, ClassField> dest, ClassField cf)
   {
      ClassField alreadyPresent = dest.get (cf.name)

      if (alreadyPresent != null)
      {
         LOG.debug  "{}: buildConfigField <{}/{}/{}> exists, replacing with <{}/{}/{}>",
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

   /**
    * Merges default class fields with classfields from input.
    * 
    * @return merged class fields
    */
   private Map<String, ClassField> mergeClassFields ()
   {
      Map<String, ClassField> merged = new LinkedHashMap<> ()
      addClassField (merged, new ClassFieldImpl ("String", DEFAULT_VERSION_FIELDNAME, version))
      addClassField (merged, new ClassFieldImpl ("String", DEFAULT_NAME_FIELDNAME, appName))
      classFields.values ().each { cf ->
         addClassField (merged, cf)
      }
      return merged
   }

   /**
    * Returns full path of output file.
    */
   private Path getOutputFile ()
   {
      getOutputDirPath ().resolve (clsName + ".java")
   }

   @TaskAction
   void generateBuildConfig () throws IOException
   {
      LOG.debug "{}: GenerateBuildConfigTask executed.", name
      /* buildConfig sourece file */
      Path outputFile = outputDir.toPath ()
      .resolve (packageName.replaceAll ('\\.', '/'))
      .resolve (clsName + ".java")

      Map<String, ClassField> mergedClassFields = mergeClassFields ()
        
      /*clear the output dir */
      emptyDir (outputFile.parent.toFile ())

      new ClassWriter (
         Files.newBufferedWriter (outputFile, Charset.forName (charset),
            StandardOpenOption.CREATE)).withCloseable { w ->
         w.writePackage (packageName).writeClass (clsName)

         mergedClassFields.values ().each { cf ->
            w.writeClassField cf
            }
         }
      }
      }
