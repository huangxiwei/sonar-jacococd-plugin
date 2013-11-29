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

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.test.IsMeasure;
import org.sonar.plugins.jacococd.JaCoCoCDOverallSensor;
import org.sonar.plugins.jacococd.JacocoConfiguration;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class JaCoCoOverallSensorTest {

  private JacocoConfiguration configuration;
  private SensorContext context;
  private ModuleFileSystem fileSystem;
  private PathResolver pathResolver;
  private Project project;
  private ResourcePerspectives perspectives;
  private JaCoCoCDOverallSensor sensor;

  @Before
  public void before(){
    configuration = mock(JacocoConfiguration.class);
    context = mock(SensorContext.class);
    fileSystem = mock(ModuleFileSystem.class);
    pathResolver = mock(PathResolver.class);
    project = mock(Project.class);
    perspectives = mock(ResourcePerspectives.class);
    sensor = new JaCoCoCDOverallSensor(configuration, perspectives, fileSystem, pathResolver);
  }

  @Test
  public void should_execute_if_report_path_is_set() {
    Project project = mock(Project.class);
    when(configuration.getItReportPath()).thenReturn("target/it-jacoco.exec");
    when(configuration.isEnabled(project)).thenReturn(true);

    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void do_not_execute_when_report_path_not_specified() {
    Project project = mock(Project.class);
    when(configuration.getItReportPath()).thenReturn("");

    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_save_measures() throws IOException {
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    Files.copy(TestUtils.getResource("HelloWorld.class.toCopy"), new File(outputDir, "HelloWorld.class"));

    JavaFile resource = new JavaFile("com.sonar.coverages.HelloWorld");

    when(context.getResource(any(Resource.class))).thenReturn(resource);
    when(configuration.getReportPath()).thenReturn("ut.exec");
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(fileSystem.binaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.exec"));
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(new File(outputDir, "it.exec"));
    when(pathResolver.relativeFile(any(File.class), eq(new File("target/sonar/jacoco-overall.exec").getAbsolutePath()))).thenReturn(new File("target/sonar/jacoco-overall.exec"));
    when(fileSystem.workingDir()).thenReturn(new File("target/sonar"));

    sensor.analyse(project, context);

    verify(context, times(1)).getResource(resource);
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_LINES_TO_COVER, 12.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_LINES, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, "3=1;6=1;7=1;10=1;11=1;14=1;15=1;17=1;18=1;20=1;23=0;24=0")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_CONDITIONS_TO_COVER, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, 0.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_CONDITIONS_BY_LINE, "14=2")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE, (String) null)));
  }

  @Test
  public void should_no_save_measures_when_it_report_is_not_found() throws IOException {
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");

    when(configuration.getReportPath()).thenReturn("ut.exec");
    when(configuration.getItReportPath()).thenReturn("it.not.found.exec");

    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.exec"));
    when(pathResolver.relativeFile(any(File.class), eq("it.not.found.exec"))).thenReturn(new File(outputDir, "it.not.found.exec"));

    sensor.analyse(project, context);

    verifyZeroInteractions(context);
  }

  @Test
  public void should_no_save_measures_when_ut_report_is_not_found() throws IOException {
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");

    when(configuration.getReportPath()).thenReturn("ut.not.found.exec");
    when(configuration.getItReportPath()).thenReturn("it.exec");

    when(pathResolver.relativeFile(any(File.class), eq("ut.not.found.exec"))).thenReturn(new File(outputDir, "ut.not.found.exec"));
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(new File(outputDir, "it.not.found.exec"));

    sensor.analyse(project, context);

    verifyZeroInteractions(context);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("JaCoCoOverallSensor");
  }

}
