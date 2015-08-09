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

import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.DEFAULT_CLASS_NAME;
import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.DEFAULT_NAME_FIELDNAME;
import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.DEFAULT_PACKAGENAME;
import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.DEFAULT_SOURCESET;
import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.DEFAULT_VERSION_FIELDNAME;
import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.defaultIfNull;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Malte Fürstenau
 */
public class GenerateBuildConfigTask extends DefaultTask
{

   private static final Logger LOG = LoggerFactory.getLogger (BuildConfigPlugin.class.getCanonicalName ());

   /**
    * Delete directory recursively.
    *
    * @param dir Directory
    * @return <i>true</i> if no error occured, else <i>false</i>
    */
   private static boolean deleteDirRecursive (Path dir)
   {
      if (Files.exists (dir))
      {
         try
         {
            Files.walkFileTree (dir, new SimpleFileVisitor<Path> ()
            {
               @Override
               public FileVisitResult visitFile (Path file, BasicFileAttributes attrs) throws IOException
               {
                  Files.delete (file);
                  return FileVisitResult.CONTINUE;
               }
               
               @Override
               public FileVisitResult postVisitDirectory (Path dir, IOException exc) throws IOException
               {
                  Files.delete (dir);
                  return FileVisitResult.CONTINUE;
               }
            });
         }
         catch (IOException ex)
         {
            return false;
         }
      }
      return true;
   }

   /**
    * Deletes a directory and recreates it.
    *
    * @param outputDir Directory
    * @throws IOException thrown if I/O error occurs
    */
   private static void emptyDir (final Path outputDir) throws IOException
   {
      deleteDirRecursive (outputDir);
      Files.createDirectories (outputDir);
   }

   @Input
   private String sourceSet;

   @Input
   private String packageName;

   @Input
   private String version;

   @Input
   private String appName;

   private final Map<String, ClassField> classFields = new LinkedHashMap<> ();

   public GenerateBuildConfigTask ()
   {
      final Project p = getProject ();
      
      /* configure defaults */
      version = BuildConfigPlugin.getProjectVersion (p);
      packageName = defaultIfNull(BuildConfigPlugin.getProjectGroup (p),
              DEFAULT_PACKAGENAME);
      appName = p.getName ();
      sourceSet = DEFAULT_SOURCESET;
      
      LOG.debug ("{}: GenerateBuildConfigTask created.", getName ());
   }

   @Optional
   @Input
   public void buildConfigField (String type, String name, String value)
   {
      addClassField (type, name, value);
   }

   String getClassFieldValue (String name)
   {
      ClassField cf = classFields.get (name);
      if (cf != null)
         return cf.getValue ();
      else
         return null;
   }

   void addClassField (String type, String name, String value)
   {
      addClassField (classFields, new ClassFieldImpl (type, name, value));
   }

   void addClassField (ClassField cf)
   {
      addClassField (classFields, cf);
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

   private Map<String, ClassField> mergeClassFields ()
   {
      Map<String, ClassField> merged = new LinkedHashMap<> ();
      addClassField (merged, new ClassFieldImpl ("String", DEFAULT_VERSION_FIELDNAME, version));
      addClassField (merged, new ClassFieldImpl ("String", DEFAULT_NAME_FIELDNAME, appName));
      for (ClassField cf : classFields.values ())
         addClassField (merged, cf);
      return merged;
   }

   public String getOutputDir ()
   {
      return getOutputDirPath ().toString ();
   }

   private Path getOutputDirPath ()
   {
      return getProject ().getBuildDir ().toPath ()
              .resolve (BuildConfigPlugin.FD_SOURCE_OUTPUT)
              .resolve (defaultIfNull (sourceSet, DEFAULT_SOURCESET));
   }

   private Path getOutputFile ()
   {
      return getOutputDirPath ().resolve (DEFAULT_CLASS_NAME + ".java");
   }

   @TaskAction
   public void generateBuildConfig () throws IOException
   {
      /* base dir for sources generates by this task */
      final Path outputDir = getOutputDirPath ();
      /* buildConfig sourece file */
      final Path outputFile = getOutputFile ();

      /* merge class fields */
      Map<String, ClassField> mergedClassFields = mergeClassFields ();

      /*clear the output dir */
      emptyDir (outputDir);

      try (ClassWriter w = new ClassWriter (Files.newBufferedWriter (outputFile, Charset.forName ("UTF-8"), StandardOpenOption.CREATE)))
      {
         w.writePackage (packageName)
                 .writeClass (DEFAULT_CLASS_NAME);

         for (ClassField cf : mergedClassFields.values ())
            w.writeClassField (cf);
      }
   }

   public String getPackageName ()
   {
      return packageName;
   }

   public void setPackageName (String packagename)
   {
      this.packageName = packagename;
   }

   public String getSourceSet ()
   {
      return sourceSet;
   }

   public void setSourceSet (String sourceSet)
   {
      this.sourceSet = sourceSet;
   }

   public String getVersion ()
   {
      return version;
   }

   public void setVersion (String version)
   {
      this.version = version;
   }

   public String getAppName ()
   {
      return appName;
   }

   public void setAppName (String appname)
   {
      this.appName = appname;
   }

}
