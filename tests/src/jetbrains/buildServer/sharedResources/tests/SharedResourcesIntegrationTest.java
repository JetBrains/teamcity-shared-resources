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

import java.util.Map;
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior;
import jetbrains.buildServer.serverSide.RunningBuildsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.parameters.BuildParametersProvider;
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

    final BuildFeatureParametersProvider provider = new BuildFeatureParametersProvider(features);

    myProjectFeatures = new ResourceProjectFeaturesImpl();
    final Resources resources = new ResourcesImpl(myFixture.getProjectManager(), myProjectFeatures);
    final LocksStorage locksStorage = new LocksStorageImpl(myFixture.getEventDispatcher());
    final TakenLocks takenLocks = new TakenLocksImpl(locks, resources, locksStorage, features);
    final ConfigurationInspector inspector = new ConfigurationInspector(features, resources);

    final SharedResourcesAgentsFilter filter = new SharedResourcesAgentsFilter(features, locks, takenLocks, myFixture.getSingletonService(RunningBuildsManager.class), inspector);
    final SharedResourcesContextProcessor
      processor = new SharedResourcesContextProcessor(features, locks, resources, locksStorage, myFixture.getSingletonService(RunningBuildsManager.class));

    myServer.registerExtension(BuildParametersProvider.class, "tests", provider);
    myFixture.addService(filter);
    myFixture.addService(processor);
    myFixture.addService(feature);
  }

  @SuppressWarnings("SameParameterValue")
  protected Map<String, String> createInfiniteResource(final String name) {
    return CollectionsUtil.asMap(NAME, name,
                                 TYPE, ResourceType.QUOTED.name(),
                                 QUOTA, "-1",
                                 ENABLED, Boolean.toString(Boolean.TRUE));
  }

  @SuppressWarnings("SameParameterValue")
  protected Map<String, String> createReadLock(@NotNull final String resourceName) {
    return CollectionsUtil.asMap(LOCKS_FEATURE_PARAM_KEY, resourceName + "\treadLock");
  }

  @SuppressWarnings("SameParameterValue")
  protected Map<String, String> createWriteLock(@NotNull final String resourceName) {
    return CollectionsUtil.asMap(LOCKS_FEATURE_PARAM_KEY, resourceName + "\twriteLock");
  }
}
