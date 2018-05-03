/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.server;

import java.util.*;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.ResolvedSettings;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.LocksImpl;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Class {@code BuildFeatureParametersProviderTest}
 *
 * Contains tests for {@code BuildFeatureParametersProvider} class
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = BuildFeatureParametersProvider.class)
public class BuildFeatureParametersProviderTest extends BaseTestCase {

  private Mockery m;

  /** Build under test */
  private SBuild myBuild;

  /** BuildType under test */
  private SBuildType myBuildType;

  /** Resolved setting of the build type */
  @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})      // todo: add test for resolved settings
  private ResolvedSettings myResolvedSettings;

  /** SharedResources build feature descriptor */
  private SharedResourcesFeature myFeature;

  /** Build feature extractor mock*/
  private SharedResourcesFeatures myFeatures;

  /** Class under test */
  private BuildFeatureParametersProvider myBuildFeatureParametersProvider;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myBuild = m.mock(SBuild.class);
    myBuildType = m.mock(SBuildType.class);
    myFeature = m.mock(SharedResourcesFeature.class);
    myResolvedSettings = m.mock(ResolvedSettings.class);
    myFeatures = m.mock(SharedResourcesFeatures.class);

    final Locks locks = new LocksImpl();
    final LocksStorage storage = m.mock(LocksStorage.class);

    final BuildPromotionEx promo = m.mock(BuildPromotionEx.class);
    m.checking(new Expectations() {{
      oneOf(myBuild).getBuildType();
      will(returnValue(myBuildType));

      allowing(myBuild).getBuildPromotion();
      will(returnValue(promo));

      allowing(promo).isCompositeBuild();
      will(returnValue(false));
    }});


    myBuildFeatureParametersProvider = new BuildFeatureParametersProvider(myFeatures, locks, storage);
  }

  /**
   * Tests parameters provider when feature is not present
   */
  @Test
  public void testNoFeaturePresent() {
    addFeatureExpectations();

    Map<String, String> result = myBuildFeatureParametersProvider.getParameters(myBuild, false);
    assertNotNull(result);
    int size = result.size();
    assertEquals("Expected empty result. Actual size is [" + size + "]" ,0 , size);
    m.assertIsSatisfied();
  }

  /**
   * Test parameters provider when none locks are taken
   */
  @Test
  public void testEmptyParams() {
    addFeatureExpectations(myFeature);

    m.checking(new Expectations() {{
      oneOf(myFeature).getLockedResources();
      will(returnValue(Collections.emptyMap()));
    }});

    Map<String, String> result = myBuildFeatureParametersProvider.getParameters(myBuild, false);
    assertNotNull(result);
    int size = result.size();
    assertEquals("Expected empty result. Actual size is [" + size + "]" , 0, size);
    m.assertIsSatisfied();

  }

  /**
   * Test parameters provider when some locks are taken
   */
  @Test
  public void testNonEmptyParamsSomeLocks() {
    final Collection<SharedResourcesFeature> descriptors = new ArrayList<SharedResourcesFeature>() {{
      add(myFeature);
    }};

    final Map<String, Lock> locksMap = new HashMap<String, Lock>() {{
      put("lock1", new Lock("lock1", LockType.READ));
      put("lock2", new Lock("lock2", LockType.WRITE));
      put("lock3", new Lock("lock3", LockType.READ));
    }};

    m.checking(new Expectations() {{
      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(descriptors));

      oneOf(myFeature).getLockedResources();
      will(returnValue(locksMap));
    }});

    Map<String, String> result = myBuildFeatureParametersProvider.getParameters(myBuild, false);
    assertNotNull(result);
    int size = result.size();
    /* Number of locks in non empty param map */
    int numTakenLocks = 3;
    assertEquals("Wrong locks number. Expected [" + numTakenLocks + "]. Actual size is [" + size + "]" , numTakenLocks, size);
    m.assertIsSatisfied();
  }


  private void addFeatureExpectations(SharedResourcesFeature ... features) {
    final Collection<SharedResourcesFeature> descriptors = Arrays.asList(features);

    m.checking(new Expectations() {{
      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(descriptors));
    }});
  }
}
