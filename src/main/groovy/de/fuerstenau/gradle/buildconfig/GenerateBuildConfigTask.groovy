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
import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.defaultIfNull
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
     * Deletes a directory and recreates it.
     *
     * @param outputDir Directory
     * @throws IOException thrown if I/O error occurs
     */
    private static void emptyDir (final Path outputDir) throws IOException
    {
        outputDir.deleteDir ()
        outputDir.toFile().mkdirs ()
    }

    @Input
    String sourceSet

    @Input
    String packageName

    @Input
    String version

    @Input
    String appName

    final Map<String, ClassField> classFields = new LinkedHashMap<> ()

    GenerateBuildConfigTask ()
    {
        /* configure defaults */
        version = project.version
        packageName = project.group ?: DEFAULT_PACKAGENAME
        appName = project.name
        sourceSet = DEFAULT_SOURCESET
      
        LOG.debug "{}: GenerateBuildConfigTask created", name
    }

    @Optional
    @Input
    void buildConfigField (String type, String name, String value)
    {
        addClassField (type, name, value)
    }

    String getClassFieldValue (String name)
    {
        ClassField cf = classFields.get (name)
        if (cf != null)
        return cf.getValue ()
        else
        return null
    }

    void addClassField (String type, String name, String value)
    {
        addClassField (classFields, new ClassFieldImpl (type, name, value))
    }

    void addClassField (ClassField cf)
    {
        addClassField (classFields, cf)
    }

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

    private Map<String, ClassField> mergeClassFields ()
    {
        Map<String, ClassField> merged = new LinkedHashMap<> ()
        addClassField (merged, new ClassFieldImpl ("String", DEFAULT_VERSION_FIELDNAME, version))
        addClassField (merged, new ClassFieldImpl ("String", DEFAULT_NAME_FIELDNAME, appName))
        classFields.values ().forEach { cf ->
            addClassField (merged, cf)
        }
        return merged
    }

    public String getOutputDir ()
    {
        getOutputDirPath ().toString ()
    }

    private Path getOutputDirPath ()
    {
        project.buildDir.toPath ()
            .resolve (BuildConfigPlugin.FD_SOURCE_OUTPUT)
            .resolve (sourceSet ?: DEFAULT_SOURCESET)
    }

    private Path getOutputFile ()
    {
        getOutputDirPath ().resolve (DEFAULT_CLASS_NAME + ".java")
    }

    @TaskAction
    void generateBuildConfig () throws IOException
    {
        LOG.debug "{}: GenerateBuildConfigTask executed.", name
        /* base dir for sources generates by this task */
        Path outputDir = getOutputDirPath ()
        /* buildConfig sourece file */
        Path outputFile = getOutputFile ()

        Map<String, ClassField> mergedClassFields = mergeClassFields ()
        
        /*clear the output dir */
        emptyDir (outputDir)

        new ClassWriter (
            Files.newBufferedWriter (outputFile, Charset.forName ("UTF-8"),
                StandardOpenOption.CREATE)).withCloseable { w ->
            w.writePackage (packageName).writeClass (DEFAULT_CLASS_NAME)

            mergedClassFields.values ().forEach { cf ->
                w.writeClassField cf
            }
        }
    }
}
