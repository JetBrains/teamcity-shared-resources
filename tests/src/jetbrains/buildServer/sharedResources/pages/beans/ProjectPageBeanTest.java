/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.pages.beans;

import java.util.Map;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTest;
import org.testng.annotations.Test;

import static jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTestsSupport.addResource;
import static jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTestsSupport.createInfiniteResource;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ProjectPageBeanTest extends SharedResourcesIntegrationTest {

  @Test
  public void testOverrides() {
    // top project
    final SProject top = myFixture.createProject("top");
    // resource that will be overridden in the bottom of the hierarchy
    final Resource topToBottom = addResource(myFixture, top, createInfiniteResource("topToBottom"));
    // resource that will be overridden somewhere in the middle of the hierarchy
    final Resource topToMiddle = addResource(myFixture, top, createInfiniteResource("topToMiddle"));
    // resource that will ve overridden 2 times
    final Resource topMiddleBottom = addResource(myFixture, top, createInfiniteResource("topMiddleBottom"));
    // resource that will not be overridden
    addResource(myFixture, top, createInfiniteResource("topToNone"));
    // 1st project without resources
    final SProject midEmpty1 = top.createProject("midEmpty1", "midEmpty1");
    // mid project with some resources
    final SProject mid = midEmpty1.createProject("mid", "mid");
    // override resource
    final Resource topToMiddleOverride = addResource(myFixture, mid, createInfiniteResource("topToMiddle"));
    final Resource topMiddleBottomOverrideMid = addResource(myFixture, mid, createInfiniteResource("topMiddleBottom"));
    // 2nd project without resources
    final SProject midEmpty2 = mid.createProject("midEmpty2", "midEmpty2");
    // bottom project
    final SProject bottom = midEmpty2.createProject("bottom", "bottom");
    // override another resource
    final Resource topToBottomOverride = addResource(myFixture, bottom, createInfiniteResource("topToBottom"));
    final Resource topMiddleBottomOverrideBottom = addResource(myFixture, bottom, createInfiniteResource("topMiddleBottom"));

    final BeansFactory factory = myFixture.getSingletonService(BeansFactory.class);
    final ProjectPageBean result = factory.createProjectPageBean(bottom);
    final Map<String, Resource> overridesMap = result.getOverridesMap();
    assertEquals(4, overridesMap.size());
    assertEquals(overridesMap.get(topToMiddle.getId()), topToMiddleOverride);
    assertEquals(overridesMap.get(topToBottom.getId()), topToBottomOverride);
    assertEquals(overridesMap.get(topMiddleBottom.getId()), topMiddleBottomOverrideBottom);
    assertEquals(overridesMap.get(topMiddleBottomOverrideMid.getId()), topMiddleBottomOverrideBottom);
  }
}
