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
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Project.AnalysisType;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.test.IsMeasure;
import org.sonar.plugins.jacococd.JaCoCoItCDSensor;
import org.sonar.plugins.jacococd.JacocoConfiguration;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JaCoCoItSensorTest {
  private static File outputDir;
  private static File jacocoExecutionData;

  private JacocoConfiguration configuration;
  private ResourcePerspectives perspectives;
  private ModuleFileSystem fileSystem;
  private PathResolver pathResolver;
  private JaCoCoItCDSensor sensor;

  @BeforeClass
  public static void setUpOutputDir() throws IOException {
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/");
    jacocoExecutionData = new File(outputDir, "jacoco.exec");

    Files.copy(TestUtils.getResource("Hello.class.toCopy"), new File(jacocoExecutionData.getParentFile(), "Hello.class"));
  }

  @Before
  public void setUp() {
    configuration = mock(JacocoConfiguration.class);
    perspectives = mock(ResourcePerspectives.class);
    fileSystem = mock(ModuleFileSystem.class);
    pathResolver = mock(PathResolver.class);
    sensor = new JaCoCoItCDSensor(configuration, perspectives, fileSystem, pathResolver);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("JaCoCoItSensor");
  }

  @Test
  public void doNotExecuteWhenReportPathNotSpecified() {
    Project project = mock(Project.class);
    when(configuration.getItReportPath()).thenReturn("");

    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void shouldExecuteIfReportPathIsDefined() {
    Project project = mock(Project.class);
    when(project.getAnalysisType()).thenReturn(AnalysisType.DYNAMIC).thenReturn(AnalysisType.REUSE_REPORTS);
    when(configuration.getItReportPath()).thenReturn("target/it-jacoco.exec");
    when(configuration.isEnabled(project)).thenReturn(true);

    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void shouldNotExecuteIfReportPathIsNotDefined() {
    Project project = mock(Project.class);
    when(project.getAnalysisType()).thenReturn(AnalysisType.DYNAMIC).thenReturn(AnalysisType.REUSE_REPORTS);
    when(configuration.getItReportPath()).thenReturn(null);
    when(configuration.isEnabled(project)).thenReturn(true);

    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void testReadExecutionData() {
    JavaFile resource = new JavaFile("org.sonar.plugins.jacoco.tests.Hello");
    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    when(context.getResource(any(Resource.class))).thenReturn(resource);
    when(fileSystem.binaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(jacocoExecutionData);

    sensor.analyse(project, context);

    verify(context, times(1)).getResource(resource);
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.IT_LINES_TO_COVER, 7.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.IT_UNCOVERED_LINES, 3.0)));
    verify(context).saveMeasure(eq(resource),
      argThat(new IsMeasure(CoreMetrics.IT_COVERAGE_LINE_HITS_DATA, "6=1;7=1;8=1;11=1;15=0;16=0;18=0")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.IT_CONDITIONS_TO_COVER, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.IT_UNCOVERED_CONDITIONS, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.IT_CONDITIONS_BY_LINE, "15=2")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE, "15=0")));
  }

  @Test
  public void doNotSaveMeasureOnResourceWhichDoesntExistInTheContext() {
    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    when(context.getResource(any(Resource.class))).thenReturn(null);
    when(fileSystem.binaryDirs()).thenReturn(ImmutableList.of(outputDir));

    sensor.analyse(project, context);

    verify(context, never()).saveMeasure(any(Resource.class), any(Measure.class));
  }
}
