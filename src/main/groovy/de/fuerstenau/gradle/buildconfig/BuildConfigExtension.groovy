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

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

/**
 * Extension for <i>BuildConfig</i> configuration.
 * <p>
 * Example:
 * <pre>
 * {@code
 * buildConfig &lbrace;
 *    appName &equals; &apos;The Appname&apos;
 *    sourceSets &lbrace;
 *       main
 *       otherSourceSet &lbrace;
 *          appName &equals; &apos;Overridden Appname&apos;
 *          buildConfigField &apos;String&apos;&comma; &apos;UNIQUE&lowbar;FIELD&apos;&comma; &apos;some unique value&apos;
 *       &rbrace;
 *    &rbrace;
 * &rbrace;
 * }
 * </pre>
 * 
 * @author Malte Fürstenau
 */
class BuildConfigExtension extends BuildConfigSourceSet
{
   /**
    * {@link BuildConfigSourceSet} per {@code SourceSet}.
    */
   final NamedDomainObjectContainer<BuildConfigSourceSet> sourceSets

   /**
    * Constructor.
    */ 
   BuildConfigExtension (Project project)
   {
      super ("extension")
      sourceSets = project.container(BuildConfigSourceSet)
   }

   /**
    * Allows for configuration per {@code SourceSet}.
    * <p>
    * <b>Note:</b> Configuration from rootis inherited and can be
    * overriden if defined below.
    * <p>
    * Example:
    * <pre>
    * {@code
    * buildconfig &lbrace;
    *    sourceSets &lbrace;
    *       main &lbrace;
    *          &sol;&ast; configure per SourceSet  &ast;&sol;
    *       &rbrace;
    *    &rbrace;
    * &rbrace;
    * }
    * </pre>
    * or with multiple {@link org.gradle.api.tasks.SourceSet}s
    * <pre>
    * {@code
    * buildconfig &lbrace;
    *    sourceSets &lbrace;
    *       main
    *       otherSourceSet &lbrace;
    *          &sol;&ast; configure per SourceSet &ast;&sol;
    *       &rbrace;
    *       yetAnotherSourceSet
    *    &rbrace;
    * &rbrace;
    * }
    * </pre>
    */
   void sourceSets (Closure<Void> c)
   {
      sourceSets.configure (c)
   }
}
