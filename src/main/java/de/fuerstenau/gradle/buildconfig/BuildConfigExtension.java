
package de.fuerstenau.gradle.buildconfig;

import groovy.lang.Closure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

/**
 * @author Malte Fürstenau
 */
public class BuildConfigExtension
{
   private final NamedDomainObjectContainer<SourceSetConfig> sourceSets;

   public BuildConfigExtension (Project project)
   {
      sourceSets = project.container(SourceSetConfig.class);
   }

   public NamedDomainObjectContainer<SourceSetConfig> getSourceSets ()
   {
      return sourceSets;
   }
   
   public void sourceSets (Closure<Void> c)
   {
      sourceSets.configure (c);
   }
}
