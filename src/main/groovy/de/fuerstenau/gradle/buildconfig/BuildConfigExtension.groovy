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
 * Extension for build config configuration.
 * <p>
 * The extension holds the general config for all source sets. If any values
 * are present and no source sets are given (in the extension closure) the
 * {@code main} source set is added to be configured with the general values.
 * <p>
 * Any source set given in the {@code sourceSets} closure is configure with the
 * general properties and after that with the specific properties.
 * <p>
 * If any other than the {@code main} source set is given and not the
 * {@code main} source set then the {@code main} source set is not added
 * automatically.
 * 
 * <i>Example with the general config is applied to source set build configs
 * for source sets {@code main} and {@code otherconfig}. The {@code otherconfig}
 * has its {@code version} overridden.</i>

 * <pre>{@code
 * buildConfig {
 *    clsName = "buildConfigClassNameForAllSourceSets"
 *    packageName = "buildConfigPackageNameForAllSourceSets"
 *    version = "buildConfigVersionForAllSourceSets"
 *    appName = "buildConfigAppNameForAllSourceSets"
 *    buildConfigField "String" "STRING_FIELD_FOR_ALL_SOURCESETS" "myValue"
 *    buildConfigField "boolean" "BOOLEAN_FIELD_FOR_ALL_SOURCESETS" "true"
 *    sourceSets {
 *       main
 *       otherconfig {
 *          version = "testVersion"
 *       }
 *    }
 * }}</pre>
 * 
 * <i>Example with only {@code main} config. No values are given, therefore the
 * defaults are used.</i>
 * 
 * <pre>{@code
 * buildConfig {
 *    sourceSets {
 *       main
 *    }
 * }}</pre>
 *
 * <i>Example with no build config tasks generated at all. If any value was set
 * in the closure the BuildConfig for source set {@code main} would be added.</i>
 * 
 * <pre>{@code
 * buildConfig {
 * }}</pre>
 * @author Malte Fürstenau
 */
class BuildConfigExtension extends SourceSetConfig
{
    final NamedDomainObjectContainer<SourceSetConfig> sourceSets

    BuildConfigExtension (Project project)
    {
        super ("extension")
        sourceSets = project.container(SourceSetConfig)
    }

    void sourceSets (Closure<Void> c)
    {
        sourceSets.configure (c)
    }
    
    @Override
    String toString ()
    {
        "${super.toString ()}, sourceSets=$sourceSets"
    }
}
