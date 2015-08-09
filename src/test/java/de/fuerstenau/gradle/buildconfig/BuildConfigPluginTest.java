package de.fuerstenau.gradle.buildconfig;

import static de.fuerstenau.gradle.buildconfig.BuildConfigPlugin.DEFAULT_EXTENSION_NAME;
import groovy.lang.Closure;
import static org.assertj.core.api.Assertions.assertThat;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Malte Fürstenau
 */
public class BuildConfigPluginTest
{

   public BuildConfigPluginTest ()
   {
   }

   @BeforeClass
   public static void setUpClass ()
   {
   }

   @AfterClass
   public static void tearDownClass ()
   {
   }

   @Before
   public void setUp ()
   {
   }

   @After
   public void tearDown ()
   {
   }

   @Test
   public void greeterPluginAddsGreetingTaskToProject ()
   {
      Project p = ProjectBuilder.builder ().build ();
      p.getPluginManager ().apply ("java");
      p.getPluginManager ().apply ("de.fuerstenau.buildconfig");
      
      Object extObj = p.getExtensions ().findByName (DEFAULT_EXTENSION_NAME);
      
      assertThat (extObj).isInstanceOf (BuildConfigExtension.class);
      
      if (extObj instanceof BuildConfigExtension)
      {
         BuildConfigExtension ext = (BuildConfigExtension) extObj;
         ext.sourceSets (new Closure<Void> (p) {

            @Override
            public Void call ()
            {
               Object delegateObj = getDelegate();
               if (delegateObj instanceof NamedDomainObjectContainer)
               {
                  @SuppressWarnings("unchecked")
                  NamedDomainObjectContainer<SourceSetConfig> delegate =
                          (NamedDomainObjectContainer<SourceSetConfig>) delegateObj;
                  SourceSetConfig config = new SourceSetConfig ("main");
                  config.setAppName ("testName");
                  config.setVersion ("testVersion");
                  config.setPackageName ("testPackage");
                  config.buildConfigField ("String", "TEST1", "testString");
                  delegate.add (config);
               }
               return null;
            }
         });
         
      }
      
   }
}
