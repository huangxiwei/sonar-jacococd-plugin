/*
 * Sonar Java
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.jacococd;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jacoco.core.runtime.AgentOptions;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.plugins.java.api.JavaSettings;

import com.google.common.base.Joiner;
@Properties({
	@Property(
			key=JacocoCDConfiguration.REPORT_PATH_PROPERTY,
			defaultValue=JacocoCDConfiguration.REPORT_PATH_DEFAULT_VALUE,
			category=JacocoCDConfiguration.JACOCOCD_CATEGORY,
			name = "File with execution data",
	        description = "Path (absolute or relative) to the file with execution data.",
	        project = true, 
	        module = true
			),
	@Property(
			key=JacocoCDConfiguration.IT_REPORT_PATH_PROPERTY,
			defaultValue=JacocoCDConfiguration.IT_REPORT_PATH_DEFAULT_VALUE,
			category=JacocoCDConfiguration.JACOCOCD_CATEGORY,
			name = "File with execution data for integration tests",
		    description = "Path (absolute or relative) to the file with execution data.",
	        project = true, 
	        module = true
			),		
	@Property(
			key=JacocoCDConfiguration.INCLUDES_PROPERTY,
			multiValues = true,
			category=JacocoCDConfiguration.JACOCOCD_CATEGORY,
			name = "Includes",
		    description = "A list of class names that should be included in execution analysis (see wildcards)." 
		    	+ " Except for performance optimization or technical corner cases this option is normally not required.",
		        project = true, 
		        module = true
			),	
	@Property(
			key=JacocoCDConfiguration.EXCLUDES_PROPERTY,
			
			multiValues = true,
			defaultValue=JacocoCDConfiguration.EXCLUDES_DEFAULT_VALUE,
			category=JacocoCDConfiguration.JACOCOCD_CATEGORY,
			name = "Excludes",
		    description = "A list of class names that should be excluded in execution analysis (see wildcards)." 
		    	+ " Except for performance optimization or technical corner cases this option is normally not required.",
		        project = true, 
		        module = true
			),				
	@Property(
			key=JacocoCDConfiguration.EXCLCLASSLOADER_PROPERTY,
			
			multiValues = true,
			category=JacocoCDConfiguration.JACOCOCD_CATEGORY,
			name = "Excluded class loaders",
		    description = "A list of class loader names that should be excluded from execution analysis (see wildcards)." +
			          " This option might be required in case of special frameworks that conflict with JaCoCo code" +
			          " instrumentation, in particular class loaders that do not have access to the Java runtime classes.",
				        project = true, 
				        module = true
			),				

	@Property(
			key=JacocoCDConfiguration.ANT_TARGETS_PROPERTY,
			
			multiValues = true,
			defaultValue=JacocoCDConfiguration.ANT_TARGETS_DEFAULT_VALUE,
			category=CoreProperties.CATEGORY_JAVA,
			name = "Ant targets",
		    description ="Comma separated list of Ant targets for execution of tests.",
	        project = true, 
	        module = true
			)		
}
)
public class JacocoCDConfiguration implements BatchExtension {
  public static final String JACOCOCD_CATEGORY="JaCoCoCD";
  public static final String REPORT_PATH_PROPERTY = "sonar.jacococd.reportPath";
  public static final String REPORT_PATH_DEFAULT_VALUE = "target/jacoco.exec";
  public static final String IT_REPORT_PATH_PROPERTY = "sonar.jacococd.itReportPath";
  public static final String IT_REPORT_PATH_DEFAULT_VALUE = "";
  public static final String INCLUDES_PROPERTY = "sonar.jacococd.includes";
  public static final String EXCLUDES_PROPERTY = "sonar.jacococd.excludes";

  /**
   * Hibernate uses Javassist to modify entity classes and without exclusion of such classes from JaCoCo exception might be thrown:
   * <pre>
   * Javassist Enhancement failed: org.sonar.api.profiles.Alert
   * java.lang.VerifyError: (class: org/sonar/api/profiles/Alert_$$_javassist_3, method: <clinit> signature: ()V) Illegal local variable number
   * </pre>
   */
  public static final String EXCLUDES_DEFAULT_VALUE = "*_javassist_*";
  public static final String EXCLCLASSLOADER_PROPERTY = "sonar.jacococd.exclclassloader";
  public static final String ANT_TARGETS_PROPERTY = "sonar.jacococd.antTargets";
  public static final String ANT_TARGETS_DEFAULT_VALUE = "";

  private Settings settings;
  private JavaSettings javaSettings;
  private JaCoCoCDAgentDownloader downloader;

  public JacocoCDConfiguration(Settings settings, JaCoCoCDAgentDownloader downloader, JavaSettings javaSettings) {
    this.settings = settings;
    this.downloader = downloader;
    this.javaSettings = javaSettings;
  }

  public boolean isEnabled(Project project) {
    return Java.KEY.equals(project.getLanguageKey()) &&
      project.getAnalysisType().isDynamic(true) &&
      JaCoCoUtils.PLUGIN_KEY.equals(javaSettings.getEnabledCoveragePlugin());
  }

  public String getReportPath() {
    return settings.getString(REPORT_PATH_PROPERTY);
  }

  public String getItReportPath() {
    return settings.getString(IT_REPORT_PATH_PROPERTY);
  }

  public String getJvmArgument() {
    AgentOptions options = new AgentOptions();
    options.setDestfile(getReportPath());
    String includes = Joiner.on(':').join(settings.getStringArray(INCLUDES_PROPERTY));
    if (StringUtils.isNotBlank(includes)) {
      options.setIncludes(includes);
    }
    String excludes = Joiner.on(':').join(settings.getStringArray(EXCLUDES_PROPERTY));
    if (StringUtils.isNotBlank(excludes)) {
      options.setExcludes(excludes);
    }
    String exclclassloader = Joiner.on(':').join(settings.getStringArray(EXCLCLASSLOADER_PROPERTY));
    if (StringUtils.isNotBlank(exclclassloader)) {
      options.setExclClassloader(exclclassloader);
    }
    return options.getVMArgument(downloader.getAgentJarFile());
  }

  public String[] getAntTargets() {
    return settings.getStringArray(ANT_TARGETS_PROPERTY);
  }

  public String getExcludes() {
    return settings.getString(EXCLUDES_PROPERTY);
  }

  public static List<PropertyDefinition> getPropertyDefinitions() {

    List<PropertyDefinition> list = new ArrayList<PropertyDefinition>();
    
    
    return list;
      
  }

}
