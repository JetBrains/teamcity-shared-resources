/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.sharedResources.tests;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.parameters.BuildParametersProvider;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.server.*;
import jetbrains.buildServer.sharedResources.server.feature.*;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeaturesImpl;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorageImpl;
import jetbrains.buildServer.sharedResources.server.runtime.TakenLocks;
import jetbrains.buildServer.sharedResources.server.runtime.TakenLocksImpl;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.ProjectFeatureParameters.*;
import static jetbrains.buildServer.sharedResources.server.feature.FeatureParams.LOCKS_FEATURE_PARAM_KEY;

/**
 * Base class for execution of integration tests with shared resources in builds
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public abstract class SharedResourcesIntegrationTest extends BaseServerTestCase {

  protected ResourceProjectFeatures myProjectFeatures;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    registerExtension();
  }

  @NotNull
  protected static Map<String, String> getSharedResourceParameters(@NotNull final SBuild build) {
    return CollectionsUtil.filterMapByKeys(build.getParametersProvider().getAll(), data -> data.startsWith("teamcity.locks."));
  }

  private void registerExtension() {
    final PluginDescriptor descriptor = new MockServerPluginDescriptior();
    final Locks locks = new LocksImpl();
    final FeatureParams params = new FeatureParamsImpl(locks);
    final SharedResourcesBuildFeature feature = new SharedResourcesBuildFeature(descriptor, params);
    final SharedResourcesFeatureFactory factory = new SharedResourcesFeatureFactoryImpl(locks);
    final SharedResourcesFeatures features = new SharedResourcesFeaturesImpl(factory);
    final LocksStorage locksStorage = new LocksStorageImpl(myFixture.getEventDispatcher());

    final BuildFeatureParametersProvider provider = new BuildFeatureParametersProvider(features, locks, locksStorage);

    myProjectFeatures = new ResourceProjectFeaturesImpl();
    final Resources resources = new ResourcesImpl(myFixture.getProjectManager(), myProjectFeatures);

    final TakenLocks takenLocks = new TakenLocksImpl(locks, resources, locksStorage, features);
    final ConfigurationInspector inspector = new ConfigurationInspector(features, resources);

    final SharedResourcesAgentsFilter filter =
      new SharedResourcesAgentsFilter(features, locks, takenLocks, myFixture.getSingletonService(RunningBuildsManager.class), inspector, locksStorage, resources);
    final SharedResourcesContextProcessor
      processor = new SharedResourcesContextProcessor(features, locks, resources, locksStorage, myFixture.getSingletonService(RunningBuildsManager.class));

    myServer.registerExtension(BuildParametersProvider.class, "tests", provider);
    myFixture.addService(filter);
    myFixture.addService(processor);
    myFixture.addService(feature);
  }

  protected void addResource(@NotNull final SProject project, @NotNull final Map<String, String> resource) {
    myProjectFeatures.addFeature(project, resource);
  }

  @SuppressWarnings("SameParameterValue")
  protected void addWriteLock(@NotNull final SBuildType buildType, @NotNull final String resourceName) {
    buildType.addBuildFeature(SharedResourcesBuildFeature.FEATURE_TYPE, createWriteLock(resourceName));
  }

  @SuppressWarnings("SameParameterValue")
  protected void addReadLock(@NotNull final SBuildType buildType, @NotNull final String resourceName) {
    buildType.addBuildFeature(SharedResourcesBuildFeature.FEATURE_TYPE, createReadLock(resourceName));
  }

  @SuppressWarnings("SameParameterValue")
  protected void addSpecificLock(@NotNull final SBuildType buildType,
                                 @NotNull final String resourceName,
                                 @NotNull final String value) {
    buildType.addBuildFeature(SharedResourcesPluginConstants.FEATURE_TYPE, createSpecificLock(resourceName, value));
  }

  @SuppressWarnings("SameParameterValue")
  protected void addAnyLock(@NotNull final SBuildType buildType,
                            @NotNull final String resourceName) {
    buildType.addBuildFeature(SharedResourcesPluginConstants.FEATURE_TYPE, createAnyLock(resourceName));
  }

  @SuppressWarnings("SameParameterValue")
  protected Map<String, String> createInfiniteResource(final String name) {
    return CollectionsUtil.asMap(NAME, name,
                                 TYPE, ResourceType.QUOTED.name(),
                                 QUOTA, "-1",
                                 ENABLED, Boolean.toString(Boolean.TRUE));
  }

  @SuppressWarnings("SameParameterValue")
  protected Map<String, String> createCustomResource(final String name, String... values) {
    return CollectionsUtil.asMap(NAME, name,
                                 TYPE, ResourceType.CUSTOM.name(),
                                 QUOTA, "-1",
                                 ENABLED, Boolean.toString(Boolean.TRUE),
                                 VALUES, Arrays.stream(values).collect(Collectors.joining("\n")));
  }

  @SuppressWarnings("SameParameterValue")
  private Map<String, String> createReadLock(@NotNull final String resourceName) {
    return CollectionsUtil.asMap(LOCKS_FEATURE_PARAM_KEY, resourceName + "\treadLock");
  }

  @SuppressWarnings("SameParameterValue")
  private Map<String, String> createWriteLock(@NotNull final String resourceName) {
    return CollectionsUtil.asMap(LOCKS_FEATURE_PARAM_KEY, resourceName + "\twriteLock");
  }

  @SuppressWarnings("SameParameterValue")
  private Map<String, String> createSpecificLock(@NotNull final String resourceName, @NotNull final String value) {
    return CollectionsUtil.asMap(LOCKS_FEATURE_PARAM_KEY, resourceName + " readLock " + value);
  }

  @SuppressWarnings("SameParameterValue")
  private Map<String, String> createAnyLock(@NotNull final String resourceName) {
    return CollectionsUtil.asMap(LOCKS_FEATURE_PARAM_KEY, resourceName + " readLock ");
  }

  protected void enableBuildChainsProcessing() {
    try {
      final String text = SharedResourcesPluginConstants.RESOURCES_IN_CHAINS_ENABLED + "=true";
      final File myProps = createTempFile(text);
      final FileWatchingPropertiesModel myModel = new FileWatchingPropertiesModel(myProps);
      final Field field = TeamCityProperties.class.getDeclaredField("ourModel");
      field.setAccessible(true);
      field.set(TeamCityProperties.class, myModel);
      FileUtil.writeFileAndReportErrors(myProps, text);
      myModel.forceReloadProperties();
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}

