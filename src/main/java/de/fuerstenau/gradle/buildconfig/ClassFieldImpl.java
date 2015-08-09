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

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Malte Fürstenau
 */
public final class ClassFieldImpl implements ClassField, Serializable
{

   private static final long serialVersionUID = 2508835394164784208L;

   private final String type;
   private final String name;
   private final String value;
   private final Set<String> annotations;
   private final String documentation;

   public ClassFieldImpl (String type, String name, String value)
   {
      this (type, name, value, Collections.<String>emptySet (), "");
   }

   public ClassFieldImpl (String type, String name, String value,
           Set<String> annotations, String documentation)
   {
      this.type = type;
      this.name = name;
      this.value = value;
      this.annotations = Collections.unmodifiableSet (
              new LinkedHashSet<> (annotations));
      this.documentation = documentation;
   }

   public ClassFieldImpl (ClassField classField)
   {
      this (classField.getType (), classField.getName (), classField.getValue (),
              classField.getAnnotations (), classField.getDocumentation ());
   }

   @Override

   public String getType ()
   {
      return type;
   }

   @Override

   public String getName ()
   {
      return name;
   }

   @Override

   public String getValue ()
   {
      return value;
   }

   @Override
   public String getDocumentation ()
   {
      return documentation;
   }

   @Override
   public Set<String> getAnnotations ()
   {
      return annotations;
   }

   @Override
   public int hashCode ()
   {
      int hash = 7;
      hash = 59 * hash + Objects.hashCode (this.type);
      hash = 59 * hash + Objects.hashCode (this.name);
      hash = 59 * hash + Objects.hashCode (this.value);
      hash = 59 * hash + Objects.hashCode (this.annotations);
      hash = 59 * hash + Objects.hashCode (this.documentation);
      return hash;
   }

   @Override
   public boolean equals (Object obj)
   {
      if (obj == null)
         return false;
      if (getClass () != obj.getClass ())
         return false;
      final ClassFieldImpl other = (ClassFieldImpl) obj;
      if (!Objects.equals (this.type, other.type))
         return false;
      if (!Objects.equals (this.name, other.name))
         return false;
      if (!Objects.equals (this.value, other.value))
         return false;
      if (!Objects.equals (this.annotations, other.annotations))
         return false;
      if (!Objects.equals (this.documentation, other.documentation))
         return false;
      return true;
   }
}
