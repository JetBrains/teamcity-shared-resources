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
import jetbrains.BuildServerCreator;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.parameters.BuildParametersProvider;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.pages.Messages;
import jetbrains.buildServer.sharedResources.pages.ResourceHelper;
import jetbrains.buildServer.sharedResources.server.*;
import jetbrains.buildServer.sharedResources.server.analysis.ResourceUsageAnalyzer;
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

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.ProjectFeatureParameters.*;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.ProjectFeatureParameters.ENABLED;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.ProjectFeatureParameters.QUOTA;
import static jetbrains.buildServer.sharedResources.server.feature.FeatureParams.LOCKS_FEATURE_PARAM_KEY;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesIntegrationTestsSupport {

  public static void apply(@NotNull final BuildServerCreator fixture) {
    final PluginDescriptor descriptor = new MockServerPluginDescriptior();
    final Locks locks = new LocksImpl();
    final FeatureParams params = new FeatureParamsImpl(locks);
    final SharedResourcesBuildFeature feature = new SharedResourcesBuildFeature(descriptor, params);
    final SharedResourcesFeatureFactory factory = new SharedResourcesFeatureFactoryImpl(locks);
    final SharedResourcesFeatures features = new SharedResourcesFeaturesImpl(factory);
    final LocksStorage locksStorage = new LocksStorageImpl(fixture.getEventDispatcher());

    final BuildUsedResourcesReport buildUsedResourcesReport = new BuildUsedResourcesReport(new UsedResourcesSerializer());

    final BuildFeatureParametersProvider provider = new BuildFeatureParametersProvider(features, locks, locksStorage);

    final ResourceProjectFeaturesImpl projectFeatures = new ResourceProjectFeaturesImpl();
    final Resources resources = new ResourcesImpl(fixture.getProjectManager(), projectFeatures);

    final TakenLocks takenLocks = new TakenLocksImpl(locks, resources, locksStorage, features);
    final ConfigurationInspector inspector = new ConfigurationInspector(features, resources);

    final SharedResourcesAgentsFilter filter =
      new SharedResourcesAgentsFilter(features, locks, takenLocks, fixture.getSingletonService(RunningBuildsManager.class), inspector, locksStorage, resources);

    final SharedResourcesContextProcessor
      processor = new SharedResourcesContextProcessor(features, locks, resources, locksStorage, fixture.getSingletonService(RunningBuildsManager.class),
                                                      buildUsedResourcesReport);

    final ResourceUsageAnalyzer analyzer = new ResourceUsageAnalyzer(resources, features);

    fixture.getServer().registerExtension(BuildParametersProvider.class, "tests", provider);
    fixture.addService(new Messages());
    fixture.addService(new ResourceHelper());
    fixture.addService(features);
    fixture.addService(projectFeatures);
    fixture.addService(buildUsedResourcesReport);
    fixture.addService(filter);
    fixture.addService(processor);
    fixture.addService(feature);
    fixture.addService(resources);
    fixture.addService(analyzer);
  }

  public static Resource addResource(@NotNull final BuildServerCreator fixture,
                                     @NotNull final SProject project,
                                     @NotNull final Map<String, String> resource) {
    return ResourceFactory.fromDescriptor(fixture.getSingletonService(ResourceProjectFeatures.class).addFeature(project, resource));
  }

  public static  Lock addWriteLock(@NotNull final BuildTypeSettings settings, @NotNull final Resource resource) {
    return addWriteLock(settings, resource.getName());
  }

  public static Lock addWriteLock(@NotNull final BuildTypeSettings settings, @NotNull final String resourceName) {
    settings.addBuildFeature(SharedResourcesBuildFeature.FEATURE_TYPE, createWriteLock(resourceName));
    return new Lock(resourceName, LockType.WRITE);
  }

  public static Lock addReadLock(@NotNull final BuildTypeSettings settings, @NotNull final Resource resource) {
    return addReadLock(settings, resource.getName());
  }

  public static Lock addReadLock(@NotNull final BuildTypeSettings settings, @NotNull final String resourceName) {
    settings.addBuildFeature(SharedResourcesBuildFeature.FEATURE_TYPE, createReadLock(resourceName));
    return new Lock(resourceName, LockType.READ);
  }

  @SuppressWarnings("UnusedReturnValue")
  public static Lock addSpecificLock(@NotNull final BuildTypeSettings settings,
                                     @NotNull final String resourceName,
                                     @NotNull final String value) {
    settings.addBuildFeature(SharedResourcesPluginConstants.FEATURE_TYPE, createSpecificLock(resourceName, value));
    return new Lock(resourceName, LockType.READ, value);
  }

  public static void addAnyLock(@NotNull final BuildTypeSettings settings,
                            @NotNull final String resourceName) {
    settings.addBuildFeature(SharedResourcesPluginConstants.FEATURE_TYPE, createAnyLock(resourceName));
  }

  public static Map<String, String> createInfiniteResource(final String name) {
    return createQuotedResource(name, -1);
  }

  @SuppressWarnings("SameParameterValue")
  public static Map<String, String> createCustomResource(final String name, String... values) {
    return CollectionsUtil.asMap(NAME, name,
                                 TYPE, ResourceType.CUSTOM.name(),
                                 QUOTA, "-1",
                                 ENABLED, Boolean.toString(Boolean.TRUE),
                                 VALUES, Arrays.stream(values).collect(Collectors.joining("\n")));
  }

  public static Map<String, String> createQuotedResource(final String name, final int quota) {
    return CollectionsUtil.asMap(NAME, name,
                                 TYPE, ResourceType.QUOTED.name(),
                                 QUOTA, Integer.toString(quota),
                                 ENABLED, Boolean.toString(Boolean.TRUE));
  }

  private static Map<String, String> createReadLock(@NotNull final String resourceName) {
    return CollectionsUtil.asMap(LOCKS_FEATURE_PARAM_KEY, resourceName + "\treadLock");
  }


  private static Map<String, String> createWriteLock(@NotNull final String resourceName) {
    return CollectionsUtil.asMap(LOCKS_FEATURE_PARAM_KEY, resourceName + "\twriteLock");
  }


  private static Map<String, String> createSpecificLock(@NotNull final String resourceName, @NotNull final String value) {
    return CollectionsUtil.asMap(LOCKS_FEATURE_PARAM_KEY, resourceName + " readLock " + value);
  }

  @SuppressWarnings("SameParameterValue")
  private static Map<String, String> createAnyLock(@NotNull final String resourceName) {
    return CollectionsUtil.asMap(LOCKS_FEATURE_PARAM_KEY, resourceName + " readLock ");
  }


  public static void assertLock(@NotNull final Map<String, String> params,
                                  @NotNull final Lock lock) {
    assertLock(params, lock.getName(), lock.getType(), lock.getValue());

  }

  private static void assertLock(@NotNull final Map<String, String> params,
                                 @NotNull final String name,
                                 @NotNull final LockType lockType,
                                 @Nullable final String value) {
    String key = "teamcity.locks." + lockType.getName() + "." + name;
    assertTrue("Resulting build parameters do not contain required key [" + key + "]", params.containsKey(key));
    if (value != null) {
      assertEquals("Expected lock value [" + value + "], got [" + params.get(key) + "]", value, params.get(key));
    }
  }

  @NotNull
  public static Map<String, String> getSharedResourceParameters(@NotNull final SBuild build) {
    return CollectionsUtil.filterMapByKeys(build.getParametersProvider().getAll(), data -> data.startsWith("teamcity.locks."));
  }
}
