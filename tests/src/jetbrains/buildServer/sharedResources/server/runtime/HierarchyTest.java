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

package jetbrains.buildServer.sharedResources.server.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.BuildTypeSettingsFactory;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTest;
import jetbrains.buildServer.util.WaitForAssert;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class HierarchyTest extends SharedResourcesIntegrationTest {

  @Test
  public void testSingleTemplate_NoOverride() {
    // create resource 1
    final Resource rc1 = addResource(myProject, createInfiniteResource("resource1"));
    // create resource 2
    final Resource rc2 = addResource(myProject, createInfiniteResource("resource2"));
    // create template
    final BuildTypeTemplate template = myProject.createBuildTypeTemplate("myTemplate");
    // add lock to template
    final Lock templateLock = addReadLock(template, rc1);
    // create build configuration
    final SBuildType buildType = myProject.createBuildType("myBuildType");
    // create lock in build configuration
    final Lock buildTypeLock = addWriteLock(buildType, rc2);
    // attach build configuration to template
    buildType.addTemplate(template, false);
    final SFinishedBuild finishedBuild = runAndFinishSingleBuild(buildType);
    // check parameters
    final Map<String, String> params = getSharedResourceParameters(finishedBuild);
    assertLock(params, templateLock);
    assertLock(params, buildTypeLock);
    assertEquals(2, params.size());
  }

  @Test
  public void testSingleTemplate_Override() {
    final Resource rc = addResource(myProject, createInfiniteResource("resource"));
    final BuildTypeTemplate template = myProject.createBuildTypeTemplate("myTemplate");
    addReadLock(template, rc.getName());
    final SBuildType buildType = myProject.createBuildType("myBuildType");
    // create lock in build configuration
    final Lock buildTypeLock = addWriteLock(buildType, rc);
    // attach build configuration to template
    buildType.addTemplate(template, false);
    final SFinishedBuild finishedBuild = runAndFinishSingleBuild(buildType);
    final Map<String, String> params = getSharedResourceParameters(finishedBuild);
    assertLock(params, buildTypeLock);
    assertEquals(1, params.size());
  }

  @Test
  public void testSingleTemplate_Cross() {
    final Resource rc = addResource(myProject, createInfiniteResource("resource"));
    final Resource rc2 = addResource(myProject, createInfiniteResource("resource2"));
    final BuildTypeTemplate template = myProject.createBuildTypeTemplate("myTemplate");
    addReadLock(template, rc.getName());
    final Lock templateLock = addReadLock(template, rc2.getName());
    final SBuildType buildType = myProject.createBuildType("myBuildType");
    final Lock buildTypeLock = addWriteLock(buildType, rc);
    buildType.addTemplate(template, false);
    final SFinishedBuild finishedBuild = runAndFinishSingleBuild(buildType);
    final Map<String, String> params = getSharedResourceParameters(finishedBuild);
    assertLock(params, buildTypeLock);
    assertLock(params, templateLock);
    assertEquals(2, params.size());
  }

  @Test
  public void testMultipleTemplates_NoCrossNoOverride() {
    final Resource template1Resource = addResource(myProject, createInfiniteResource("template1Resource"));
    final Resource template2Resource = addResource(myProject, createInfiniteResource("template2Resource"));
    final Resource buildTypeResource = addResource(myProject, createInfiniteResource("buildTypeResource"));
    final BuildTypeTemplate template1 = myProject.createBuildTypeTemplate("template1");
    final Lock template1Lock = addReadLock(template1, template1Resource);
    final BuildTypeTemplate template2 = myProject.createBuildTypeTemplate("template2");
    final Lock template2Lock = addReadLock(template2, template2Resource);
    final SBuildType buildType = myProject.createBuildType("myBuildType");
    final Lock buildTypeLock = addWriteLock(buildType, buildTypeResource);
    buildType.addTemplate(template1, false);
    buildType.addTemplate(template2, false);
    final SFinishedBuild finishedBuild = runAndFinishSingleBuild(buildType);
    final Map<String, String> params = getSharedResourceParameters(finishedBuild);
    assertLock(params, buildTypeLock);
    assertLock(params, template1Lock);
    assertLock(params, template2Lock);
    assertEquals(3, params.size());
  }

  @Test
  public void testMultipleTemplates_OverrideInTemplates() {
    final Resource templateResource = addResource(myProject, createInfiniteResource("templateResource"));
    final Resource buildTypeResource = addResource(myProject, createInfiniteResource("buildTypeResource"));

    final BuildTypeTemplate template1 = myProject.createBuildTypeTemplate("template1");
    addReadLock(template1, templateResource);

    final BuildTypeTemplate template2 = myProject.createBuildTypeTemplate("template2");
    final Lock template2Lock = addWriteLock(template2, templateResource);

    final SBuildType buildType = myProject.createBuildType("myBuildType");

    buildType.addTemplate(template2, false);
    buildType.addTemplate(template1, false); // lowest priority

    final Lock buildTypeLock = addWriteLock(buildType, buildTypeResource);

    final SFinishedBuild finishedBuild = runAndFinishSingleBuild(buildType);
    final Map<String, String> params = getSharedResourceParameters(finishedBuild);
    assertLock(params, buildTypeLock);
    assertLock(params, template2Lock);
    assertEquals(2, params.size());
  }

  @Test
  public void testMultipleTemplates_CrossOverride() {
    final Resource resource1 = addResource(myProject, createInfiniteResource("resource1"));
    final Resource resource2 = addResource(myProject, createInfiniteResource("resource2"));
    final Resource resource3 = addResource(myProject, createInfiniteResource("resource3"));
    final Resource buildTypeResource = addResource(myProject, createInfiniteResource("buildTypeResource"));

    final BuildTypeTemplate template1 = myProject.createBuildTypeTemplate("template1");
    addReadLock(template1, resource1);
    addReadLock(template1, resource2);
    final Lock template1Lock = addReadLock(template1, resource3);

    final BuildTypeTemplate template2 = myProject.createBuildTypeTemplate("template2");
    final Lock template2Resource1Lock = addWriteLock(template2, resource1);
    final Lock template2Resource2Lock = addWriteLock(template2, resource2);

    final SBuildType buildType = myProject.createBuildType("myBuildType");

    buildType.addTemplate(template2, false);
    buildType.addTemplate(template1, false); // lowest priority

    final Lock buildTypeLock = addWriteLock(buildType, buildTypeResource);

    final SFinishedBuild finishedBuild = runAndFinishSingleBuild(buildType);
    final Map<String, String> params = getSharedResourceParameters(finishedBuild);
    
    assertEquals(4, params.size());
    assertLock(params, buildTypeLock);
    assertLock(params, template1Lock);
    assertLock(params, template2Resource1Lock);
    assertLock(params, template2Resource2Lock);
  }

  @Test
  public void testMultipleTemplates_Reorder() {
    final Resource templateResource = addResource(myProject, createInfiniteResource("templateResource"));
    final Resource buildTypeResource = addResource(myProject, createInfiniteResource("buildTypeResource"));

    final BuildTypeTemplate template1 = myProject.createBuildTypeTemplate("template1");
    final Lock template1Lock = addReadLock(template1, templateResource);

    final BuildTypeTemplate template2 = myProject.createBuildTypeTemplate("template2");
    final Lock template2Lock = addWriteLock(template2, templateResource);

    final SBuildType buildType = myProject.createBuildType("myBuildType");

    buildType.addTemplate(template2, false);
    buildType.addTemplate(template1, false); // lowest priority

    final Lock buildTypeLock = addWriteLock(buildType, buildTypeResource);

    SFinishedBuild finishedBuild = runAndFinishSingleBuild(buildType);
    Map<String, String> params = getSharedResourceParameters(finishedBuild);
    assertLock(params, buildTypeLock);
    assertLock(params, template2Lock);
    assertEquals(2, params.size());

    buildType.setTemplatesOrder(Stream.of(template1, template2).map(BuildTypeTemplate::getId).collect(Collectors.toList()));

    finishedBuild = runAndFinishSingleBuild(buildType);
    params = getSharedResourceParameters(finishedBuild);
    assertLock(params, buildTypeLock);
    assertLock(params, template1Lock);
    assertEquals(2, params.size());
  }

  @Test
  public void testDefaultTemplate() {
    final Resource resource1 = addResource(myProject, createInfiniteResource("resource1"));
    final Resource resource2 = addResource(myProject, createInfiniteResource("resource2"));

    final SBuildType buildType = myProject.createBuildType("myBuildType");
    final Lock buildTypeLock = addReadLock(buildType, resource1);

    final BuildTypeTemplate template = createBuildTypeTemplate("template");
    final Lock nonDefaultTemplateLock = addReadLock(template, resource2);

    final BuildTypeTemplate defaultTemplate = createBuildTypeTemplate("default_template");
    final Lock defaultTemplateLock = addWriteLock(defaultTemplate, resource2);

    buildType.addTemplate(template, false);

    SFinishedBuild finishedBuild = runAndFinishSingleBuild(buildType);
    Map<String, String> params = getSharedResourceParameters(finishedBuild);

    assertLock(params, buildTypeLock);
    assertLock(params, nonDefaultTemplateLock);
    assertEquals(2, params.size());

    buildType.removeTemplates(Collections.singleton(template), false);
    myProject.setDefaultTemplate(defaultTemplate);

    finishedBuild = runAndFinishSingleBuild(buildType);
    params = getSharedResourceParameters(finishedBuild);

    assertLock(params, buildTypeLock);
    assertLock(params, defaultTemplateLock);
    assertEquals(2, params.size());

  }

  @Test
  public void testEnforcedSettings_NoOverride() {
    final Resource resource = addResource(myProject, createInfiniteResource("resource"));

    final SBuildType buildType = myProject.createBuildType("myBuildType");
    addReadLock(buildType, resource);

    final BuildTypeTemplate template = createBuildTypeTemplate("template");
    final Lock templateLock = addWriteLock(template, resource);

    myProject.setEnforcedSettings(myFixture.getSingletonService(BuildTypeSettingsFactory.class).createEnforcedSettings(myProject, template.getInternalId()));

    SFinishedBuild finishedBuild = runAndFinishSingleBuild(buildType);
    Map<String, String> params = getSharedResourceParameters(finishedBuild);

    assertLock(params, templateLock);
    assertEquals(1, params.size());
  }

  private SFinishedBuild runAndFinishSingleBuild(@NotNull final SBuildType buildType) {
    // run build
    QueuedBuildEx qb = (QueuedBuildEx)buildType.addToQueue("");
    assertNotNull(qb);
    myFixture.flushQueueAndWait();

    final BuildPromotionEx buildPromotion = qb.getBuildPromotion();
    assertNotNull(buildPromotion);

    final SBuild build = buildPromotion.getAssociatedBuild();
    assertTrue(build instanceof RunningBuildEx);

    final SFinishedBuild result = finishBuild((SRunningBuild)build, false);

    new WaitForAssert() {
      @Override
      protected boolean condition() {
        return myFixture.getBuildsManager().getRunningBuilds().size() == 0;
      }
    };

    return result;
  }
}
