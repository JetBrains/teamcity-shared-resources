/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = {SharedResourcesFeatureFactory.class, SharedResourcesFeatureFactoryImpl.class})
public class SharedResourcesFeatureFactoryImplTest extends BaseTestCase {

  private Mockery m;

  private Locks myLocks;

  private SBuildFeatureDescriptor myBuildFeatureDescriptor;

  private SharedResourcesFeatureFactory myFactory;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myLocks = m.mock(Locks.class);
    myBuildFeatureDescriptor = m.mock(SBuildFeatureDescriptor.class);
    myFactory = new SharedResourcesFeatureFactoryImpl(myLocks);
  }

  @Test
  public void testCreateFeature() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(myBuildFeatureDescriptor);
      will(returnValue(new HashMap<String, Lock>()));
    }});

    final SharedResourcesFeature feature = myFactory.createFeature(myBuildFeatureDescriptor);
    assertNotNull(feature);
  }
}
