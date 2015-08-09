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

import java.io.IOException;
import java.io.Writer;

/**
 * @author Malte Fürstenau
 */
public class ClassWriter extends Writer
{

   private final Writer delegate;
   private boolean isClass;

   public ClassWriter (Writer delegate)
   {
      this.delegate = delegate;
   }

   public ClassWriter writePackage (String pkg) throws IOException
   {
      if (isClass)
         throw new IllegalStateException ("cannot write package if class is already written");
      delegate.write ("package " + pkg + ";\n\n");
      return this;
   }

   public ClassWriter writeClass (String cls) throws IOException
   {
      if (isClass)
         throw new IllegalStateException ("cannot write class if class is already written");
      StringBuilder sb = new StringBuilder ();
      sb.append ("/** DO NOT EDIT. GENERATED CODE */\npublic final class ")
              .append (cls)
              .append ("\n{\n   private ")
              .append (cls)
              .append (" () { /*. no instance */ }\n\n");
      delegate.write (sb.toString ());
      isClass = true;
      return this;
   }

   public ClassWriter writeClassField (ClassField cf) throws IOException
   {
      if (!isClass)
         throw new IllegalStateException ("cannot write class field if class is not written");
      StringBuilder sb = new StringBuilder ();
      if (cf.getDocumentation () != null && !cf.getDocumentation ().isEmpty ())
         sb.append ("   ")
                 .append (cf.getDocumentation ())
                 .append ('\n');
      for (String annotation : cf.getAnnotations ())
         sb.append ("   ")
                 .append (annotation)
                 .append ('\n');
      sb.append ("   public static final ")
              .append (cf.getType ())
              .append (' ')
              .append (cf.getName ())
              .append (" = ");

      if ("String".equals (cf.getType ()))
         sb.append ('"')
                 .append (cf.getValue ())
                 .append ('"');
      else if ("char".equals (cf.getType ()))
         sb.append ('\'')
                 .append (cf.getValue ())
                 .append ('\'');
      else
         sb.append (cf.getValue ());
      sb.append (';')
              .append ('\n')
              .append ('\n');
      delegate.write (sb.toString ());
      return this;
   }

   @Override
   public void write (char[] cbuf, int off, int len) throws IOException
   {
      delegate.write (cbuf, off, len);
   }

   @Override
   public void flush () throws IOException
   {
      delegate.flush ();
   }

   @Override
   public void close () throws IOException
   {
      if (isClass)
         delegate.write ("}\n");
      delegate.close ();
   }

}
