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

package jetbrains.buildServer.sharedResources.server;

import java.util.Map;
import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.BuildTypeTemplate;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.impl.ProjectEx;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.analysis.FindUsagesResult;
import jetbrains.buildServer.sharedResources.server.analysis.ResourceUsageAnalyzer;
import jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTestsSupport.*;

public class FindUsagesTest extends SharedResourcesIntegrationTest {

  private ResourceUsageAnalyzer myAnalyzer;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    myAnalyzer = myFixture.getSingletonService(ResourceUsageAnalyzer.class);
  }

  @Test
  public void testFindUsedResources_noResources() {
    final SProject project = myFixture.createProject("ProjectNoResources");
    assertEquals(0, myAnalyzer.findUsedResources(project).size());
  }

  @Test
  public void testFindUsedResources_No_Override() {
    final ProjectEx top = myFixture.createProject("TOP");
    final ProjectEx child = myFixture.createProject("child", top);
    final ProjectEx grandChild = myFixture.createProject("grandchild", child);

    final Resource topRc = addResource(myFixture, top, createInfiniteResource("top_rc"));
    final Resource childRc = addResource(myFixture, child, createInfiniteResource("child_rc"));
    final Resource grandchildRc = addResource(myFixture, grandChild, createInfiniteResource("grandChild_rc"));

    final BuildTypeEx topBt = top.createBuildType("top_bt", "top_bt");
    final BuildTypeEx childBt = child.createBuildType("child_bt", "child_bt");
    final BuildTypeEx grandChildBt = grandChild.createBuildType("grandchild_bt", "grandchild_bt");

    Map<String, Resource> result = myAnalyzer.findUsedResources(top);
    assertEquals(0, result.size());

    addReadLock(topBt, topRc);
    result = myAnalyzer.findUsedResources(top);
    assertEquals(1, result.size());
    assertEquals(topRc, result.get(topRc.getName()));

    addReadLock(childBt, topRc);
    result = myAnalyzer.findUsedResources(top);
    assertEquals(1, result.size());
    assertEquals(topRc, result.get(topRc.getName()));

    addReadLock(childBt, childRc);
    result = myAnalyzer.findUsedResources(child);
    assertEquals(2, result.size());
    assertEquals(topRc, result.get(topRc.getName()));
    assertEquals(childRc, result.get(childRc.getName()));

    addReadLock(grandChildBt, grandchildRc);
    addReadLock(grandChildBt, childRc);
    addReadLock(grandChildBt, topRc);
    result = myAnalyzer.findUsedResources(grandChild);
    assertEquals(3, result.size());
    assertEquals(topRc, result.get(topRc.getName()));
    assertEquals(childRc, result.get(childRc.getName()));
    assertEquals(grandchildRc, result.get(grandchildRc.getName()));
  }

  @Test
  public void testFindUsedResources_Override() {
    final ProjectEx top = myFixture.createProject("TOP");
    final ProjectEx child = myFixture.createProject("child", top);

    final Resource resource = addResource(myFixture, top, createInfiniteResource("resource"));

    final BuildTypeEx topBt = top.createBuildType("top_bt", "top_bt");
    final BuildTypeEx childBt = child.createBuildType("child_bt", "child_bt");

    Map<String, Resource> result = myAnalyzer.findUsedResources(top);
    assertEquals(0, result.size());

    addReadLock(topBt, resource);
    result = myAnalyzer.findUsedResources(top);
    assertEquals(1, result.size());
    assertEquals(resource, result.get(resource.getName()));

    // add read lock to child build type on root resource. check on child level
    addReadLock(childBt, resource);
    result = myAnalyzer.findUsedResources(child);
    assertEquals(1, result.size());
    assertEquals(resource, result.get(resource.getName()));

    // override resource on child level. check resource differ
    final Resource childResource = addResource(myFixture, child, createInfiniteResource("resource"));
    result = myAnalyzer.findUsedResources(child);
    assertEquals(1, result.size());
    assertEquals(childResource, result.get(childResource.getName()));
  }

  @Test
  public void testFindUsages_BuildTypes_Templates() {
    final ProjectEx top = myFixture.createProject("TOP");
    final ProjectEx child = myFixture.createProject("child", top);
    final Resource resource = addResource(myFixture, top, createInfiniteResource("resource"));

    final BuildTypeEx topBt = top.createBuildType("top_bt", "top_bt");
    final BuildTypeEx childBt = child.createBuildType("child_bt", "child_bt");

    final BuildTypeTemplate topBtt = top.createBuildTypeTemplate("top_btt");
    final BuildTypeTemplate childBtt = child.createBuildTypeTemplate("child_btt");

    addReadLock(topBt, resource);
    FindUsagesResult result = myAnalyzer.findUsages(top, resource);
    assertEquals(1, result.getBuildTypes().keySet().size());
    assertContains(result.getBuildTypes().keySet(), topBt);
    assertEquals(1, result.getTotal());

    addReadLock(childBt, resource);
    result = myAnalyzer.findUsages(top, resource);
    assertEquals(2, result.getBuildTypes().keySet().size());
    assertContains(result.getBuildTypes().keySet(), topBt, childBt);
    assertEquals(2, result.getTotal());

    addWriteLock(topBtt, resource);
    result = myAnalyzer.findUsages(top, resource);
    assertEquals(1, result.getTemplates().keySet().size());
    assertContains(result.getTemplates().keySet(), topBtt);
    assertEquals(3, result.getTotal());

    addWriteLock(childBtt, resource);
    result = myAnalyzer.findUsages(top, resource);
    assertEquals(2, result.getTemplates().keySet().size());
    assertContains(result.getTemplates().keySet(), topBtt, childBtt);
    assertEquals(4, result.getTotal());

    // override

    addResource(myFixture, child, createInfiniteResource("resource"));
    result = myAnalyzer.findUsages(top, resource);
    assertEquals(1, result.getBuildTypes().size());
    assertEquals(1, result.getTemplates().size());
    assertEquals(2, result.getTotal());
  }

}
