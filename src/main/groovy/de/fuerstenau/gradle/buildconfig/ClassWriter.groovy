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
 * @author Malte Fürstenau
 */
class ClassWriter extends Writer
{

   private final Writer delegate
   private boolean isClass
   
   private String pkg
   private String cls

   ClassWriter (Writer delegate, String pkg, String cls)
   {
      this.delegate = delegate
      this.pkg = pkg
      this.cls = cls
   }

   ClassWriter writePackage (String pkg) throws IOException
   {
      if (isClass)
         throw new IllegalStateException ("cannot write package if class is already written")
      delegate.write ("""package ${pkg};

import java.util.List;
import java.lang.reflect.Field;

""")
      this
   }

   ClassWriter writeClass (String cls) throws IOException
   {
      if (isClass)
         throw new IllegalStateException ("cannot write class if class is already written")
      StringBuilder sb = StringBuilder.newInstance()
      sb << "/** DO NOT EDIT. GENERATED CODE */\npublic final class ${cls}\n{\n"
      sb << "   private ${cls} () { /*. no instance */ }\n\n"
      delegate.write (sb.toString ())
      isClass = true
      this
   }

   ClassWriter writeClassField (ClassField cf) throws IOException
   {
      if (!isClass)
         throw new IllegalStateException ("cannot write class field if class is not written")
      StringBuilder sb = StringBuilder.newInstance ()
      if (cf.getDocumentation () != null && !cf.getDocumentation ().isEmpty ())
         sb << "   ${cf.documentation}\n"
      cf.annotations.each { annot -> sb << "   ${annot}\n" }
      sb << "   public static final ${cf.type} ${cf.name} = "
      switch (cf.type)
      {
         case "String":
           sb << "\"${cf.value}\""
           break
         case "char":
           sb << "'${cf.value}'"
           break
         default:
             sb << cf.value
      }
      sb << ";\n\n"
      delegate.write (sb.toString ())
      this
   }

   @Override
   public void write (char[] cbuf, int off, int len) throws IOException
   {
      delegate.write (cbuf, off, len)
   }

   @Override
   public void flush () throws IOException
   {
      delegate.flush ()
   }

   @Override
   public void close () throws IOException
   {
      if (isClass)
      {
         delegate.write("""   public interface PrettyPrinter
   {
      public void print(Field[] field);
   }

   public static void prettyPrint(PrettyPrinter printer)
   {
      printer.print(${pkg}.${cls}.class.getDeclaredFields());
   }

   public static void prettyPrint()
   {
      prettyPrint((fields) ->
      {
         for (final Field field : fields)
            try
            {
               System.out.println(field.getName() + ": " + field.get(null));\n\
            }
            catch (IllegalArgumentException | IllegalAccessException ex)
            {
            }
      });
   }""")
         delegate.write ("}\n")
      }
      delegate.close ()
   }

}
