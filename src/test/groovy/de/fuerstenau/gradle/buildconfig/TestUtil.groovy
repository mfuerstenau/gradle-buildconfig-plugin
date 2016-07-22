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

/**
 *
 * @author Nuffe
 */
final class TestUtil
{
   static List<String> getFields (Class<?> clazz)
   {
      clazz.declaredFields.collect {
            "${it.name}/${it.type.name}/${it.get (null)}".toString ()
      }
   }
   
   static List<String> fieldsFromJar (File jarFile, String fullyQualifiedClass)
   {
      URL url = urlForJar (jarFile)  
      Class<?> clazz = loadClass (url, fullyQualifiedClass)
      getFields (clazz)
   }
    
   static List<String> fieldsFromClass (File classesDir, String fullyQualifiedClass)
   {
      URL url = urlForClasses (classesDir)  
      Class<?> clazz = loadClass (url, fullyQualifiedClass)
      getFields (clazz)
   }
    
   static URL urlForJar (File jarFile)
   {
      new URL("jar", "", jarFile.toURL ().toString () + "!/")
   }

   static URL urlForClasses (File classesDir)
   {
      classesDir.toURL ()
   }
    
   static Class<?> loadClass (URL url, String fullyQualifiedClass)
   {
      ClassLoader cl = new URLClassLoader(url);
      cl.loadClass(fullyQualifiedClass);
   }
    
   static void triggerGC() throws InterruptedException {
      System.out.println("\n-- Starting GC");
      System.gc();
      Thread.sleep(100);
      System.out.println("-- End of GC\n");
   }

   static String classPathDependencyStr (List<File> classpath)
   {
      return 'classpath files (' + classpath
      .findAll ({ !(it.toString ().contains(".wrapper") || it.toString ().contains (".gradle")) })
      .collect({ "'${it.toString ().replace ('\\', '/')}'"}).join (', ') + ')'
   }
}

