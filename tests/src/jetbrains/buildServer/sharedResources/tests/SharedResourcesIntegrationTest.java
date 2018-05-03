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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.parameters.BuildParametersProvider;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.server.*;
import jetbrains.buildServer.sharedResources.server.feature.*;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeaturesImpl;
import jetbrains.buildServer.sharedResources.server.report.BuildUsedResourcesReport;
import jetbrains.buildServer.sharedResources.server.report.UsedResourcesSerializer;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorageImpl;
import jetbrains.buildServer.sharedResources.server.runtime.TakenLocks;
import jetbrains.buildServer.sharedResources.server.runtime.TakenLocksImpl;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.BeforeMethod;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.ProjectFeatureParameters.*;
import static jetbrains.buildServer.sharedResources.server.feature.FeatureParams.LOCKS_FEATURE_PARAM_KEY;

/**
 * Base class for execution of integration tests with shared resources in builds
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public abstract class SharedResourcesIntegrationTest extends BaseServerTestCase {

  private ResourceProjectFeatures myProjectFeatures;

  protected BuildUsedResourcesReport myBuildUsedResourcesReport;

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
    myBuildUsedResourcesReport = new BuildUsedResourcesReport(new UsedResourcesSerializer());

    final BuildFeatureParametersProvider provider = new BuildFeatureParametersProvider(features, locks, locksStorage);

    myProjectFeatures = new ResourceProjectFeaturesImpl();
    final Resources resources = new ResourcesImpl(myFixture.getProjectManager(), myProjectFeatures);

    final TakenLocks takenLocks = new TakenLocksImpl(locks, resources, locksStorage, features);
    final ConfigurationInspector inspector = new ConfigurationInspector(features, resources);

    final SharedResourcesAgentsFilter filter =
      new SharedResourcesAgentsFilter(features, locks, takenLocks, myFixture.getSingletonService(RunningBuildsManager.class), inspector, locksStorage, resources);
    final SharedResourcesContextProcessor
      processor = new SharedResourcesContextProcessor(features, locks, resources, locksStorage, myFixture.getSingletonService(RunningBuildsManager.class),
                                                      myBuildUsedResourcesReport);

    myServer.registerExtension(BuildParametersProvider.class, "tests", provider);
    myFixture.addService(filter);
    myFixture.addService(processor);
    myFixture.addService(feature);
  }

  protected Resource addResource(@NotNull final SProject project, @NotNull final Map<String, String> resource) {
    return ResourceFactory.fromDescriptor(myProjectFeatures.addFeature(project, resource));
  }

  protected Lock addWriteLock(@NotNull final BuildTypeSettings settings, @NotNull final Resource resource) {
    return addWriteLock(settings, resource.getName());
  }

  @SuppressWarnings("SameParameterValue")
  protected Lock addWriteLock(@NotNull final BuildTypeSettings settings, @NotNull final String resourceName) {
    settings.addBuildFeature(SharedResourcesBuildFeature.FEATURE_TYPE, createWriteLock(resourceName));
    return new Lock(resourceName, LockType.WRITE);
  }

  protected Lock addReadLock(@NotNull final BuildTypeSettings settings, @NotNull final Resource resource) {
    return addReadLock(settings, resource.getName());
  }

  @SuppressWarnings("SameParameterValue")
  protected Lock addReadLock(@NotNull final BuildTypeSettings settings, @NotNull final String resourceName) {
    settings.addBuildFeature(SharedResourcesBuildFeature.FEATURE_TYPE, createReadLock(resourceName));
    return new Lock(resourceName, LockType.READ);
  }

  protected Lock addSpecificLock(@NotNull final BuildTypeSettings settings, @NotNull final Resource resource, String value) {
    return addSpecificLock(settings, resource.getName(), value);
  }

  @SuppressWarnings("SameParameterValue")
  protected Lock addSpecificLock(@NotNull final BuildTypeSettings settings,
                                 @NotNull final String resourceName,
                                 @NotNull final String value) {
    settings.addBuildFeature(SharedResourcesPluginConstants.FEATURE_TYPE, createSpecificLock(resourceName, value));
    return new Lock(resourceName, LockType.READ, value);
  }

  @SuppressWarnings("SameParameterValue")
  protected void addAnyLock(@NotNull final BuildTypeSettings settings,
                            @NotNull final String resourceName) {
    settings.addBuildFeature(SharedResourcesPluginConstants.FEATURE_TYPE, createAnyLock(resourceName));
  }

  @SuppressWarnings("SameParameterValue")
  protected Map<String, String> createInfiniteResource(final String name) {
    return createQuotedResource(name, -1);
  }

  @SuppressWarnings("SameParameterValue")
  protected Map<String, String> createCustomResource(final String name, String... values) {
    return CollectionsUtil.asMap(NAME, name,
                                 TYPE, ResourceType.CUSTOM.name(),
                                 QUOTA, "-1",
                                 ENABLED, Boolean.toString(Boolean.TRUE),
                                 VALUES, Arrays.stream(values).collect(Collectors.joining("\n")));
  }

  protected Map<String, String> createQuotedResource(final String name, final int quota) {
    return CollectionsUtil.asMap(NAME, name,
                                 TYPE, ResourceType.QUOTED.name(),
                                 QUOTA, Integer.toString(quota),
                                 ENABLED, Boolean.toString(Boolean.TRUE));
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


  protected final void assertLock(@NotNull final Map<String, String> params,
                                  @NotNull final Lock lock) {
    assertLock(params, lock.getName(), lock.getType(), lock.getValue());

  }

  protected final void assertLock(@NotNull final Map<String, String> params,
                                  @NotNull final String name,
                                  @NotNull final LockType lockType) {
    assertLock(params, name, lockType, null);
  }

  private void assertLock(@NotNull final Map<String, String> params,
                          @NotNull final String name,
                          @NotNull final LockType lockType,
                          @Nullable final String value) {
    String key = "teamcity.locks." + lockType.getName() + "." + name;
    assertTrue("Resulting build parameters do not contain required key [" + key + "]", params.containsKey(key));
    if (value != null) {
      assertEquals("Expected lock value [" + value + "], got [" + params.get(key) + "]", value, params.get(key));
    }
  }

}

