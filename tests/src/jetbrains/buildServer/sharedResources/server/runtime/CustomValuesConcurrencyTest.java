/*
 * Copyright 2000-2025 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.server.runtime;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTest;
import jetbrains.buildServer.util.ThreadUtil;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.getReservedResourceAttributeKey;
import static jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTestsSupport.*;
import static jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTestsSupport.addReadLock;

public class CustomValuesConcurrencyTest extends SharedResourcesIntegrationTest {

  @Test(invocationCount = 3)
  public void custom_resource_concurrency() throws InterruptedException {
    final SProject resourceProj = myFixture.createProject("resourceProject");
    final Resource resource = addResource(myFixture, resourceProj, createCustomResource("database", "1", "2"));

    int numBuildTypes = 50;

    for (int i=0; i<numBuildTypes; i++) {
      SBuildType bt = resourceProj.createBuildType("bt" + i, "bt" + i);
      addReadLock(bt, resource);
    }

    myFixture.createEnabledAgent("Gradle");
    myFixture.createEnabledAgent("Gradle");
    assertEquals(3, myFixture.getBuildAgentManager().getRegisteredAgents().size());

    AtomicBoolean wrongResourceValue = new AtomicBoolean(false);
    Map<String, Long> lockedValues = new ConcurrentHashMap<>();
    myFixture.getEventDispatcher().addListener(new BuildServerAdapter() {
      @Override
      public void changesLoaded(@NotNull final SRunningBuild build) {
        String val = ((BuildPromotionEx)build.getBuildPromotion()).getAttribute(getReservedResourceAttributeKey(resource.getId())).toString();
        final Long curBuildId = lockedValues.put(val, build.getBuildId());
        if (curBuildId != null && myFixture.getBuildsManager().findRunningBuildById(curBuildId) != null) {
          wrongResourceValue.set(true);
          System.out.println("The value " + val + " is already locked by a running build: " + curBuildId);
        }
      }

      @Override
      public void beforeBuildFinish(@NotNull final SRunningBuild build) {
        String val = ((BuildPromotionEx)build.getBuildPromotion()).getAttribute(getReservedResourceAttributeKey(resource.getId())).toString();
        if (!lockedValues.remove(val, build.getBuildId())) {
          wrongResourceValue.set(true);
          Long newBuildId = lockedValues.get(val);
          System.out.println("The value " + val + " was occupied by some other build with id " + newBuildId + " while our build " + build.getBuildId() + " was still running");
        }
      }
    });

    for (SBuildType bt: resourceProj.getBuildTypes()) {
      bt.addToQueue("");
    }

    enableDebug("jetbrains.buildServer.sharedResources.server.runtime");

    runAsync(1, () -> {
      while (!myFixture.getBuildQueue().isQueueEmpty()) {
        myServer.flushQueue();
      }
    }, () -> {
      while (!myFixture.getBuildQueue().isQueueEmpty() || !myFixture.getBuildsManager().getRunningBuilds().isEmpty()) {
        for (SRunningBuild rb: myFixture.getBuildsManager().getRunningBuilds()) {
          if (rb.isStartedOnAgent()) {
            ((RunningBuildEx)rb).finishImmediately(new Date(), false);
          }
        }
      }
    });

    finishAllBuilds();

    assertFalse(wrongResourceValue.get());
  }
}
