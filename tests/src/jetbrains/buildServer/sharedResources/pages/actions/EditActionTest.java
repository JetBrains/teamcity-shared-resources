/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.pages.actions;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.pages.Messages;
import jetbrains.buildServer.sharedResources.pages.ResourceHelper;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.util.TestFor;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = EditResourceAction.class)
public class EditActionTest extends BaseTestCase {

  private static final String RESOURCE_NAME = "NAME";
  private static final String PROJECT_ID = "PROJECT_ID";

  private Mockery m;

  /** Class under test */
  private EditResourceAction myEditResourceAction;

  private ProjectManager myProjectManager;

  private Resources myResources;

  private ResourceHelper myResourceHelper;

  private HttpServletRequest myRequest;

  private HttpServletResponse myResponse;

  private Element myAjaxResponse;

  private SProject myProject;

  private Messages myMessages;

  private ResourceFactory myResourceFactory;

  private SharedResourcesFeatures myFeatures;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myProjectManager = m.mock(ProjectManager.class);
    myResources = m.mock(Resources.class);
    myResourceHelper = m.mock(ResourceHelper.class);
    myRequest = m.mock(HttpServletRequest.class);
    myResponse = m.mock(HttpServletResponse.class);
    myAjaxResponse = m.mock(Element.class);
    myProject = m.mock(SProject.class);
    myMessages = m.mock(Messages.class);
    myFeatures = m.mock(SharedResourcesFeatures.class);
    myResourceFactory = ResourceFactory.getFactory(PROJECT_ID);
    final ConfigActionFactory configActionFactory = mockConfigActionFactory(m);
    myEditResourceAction = new EditResourceAction(myProjectManager, myResources, myResourceHelper, myFeatures, myMessages, configActionFactory);
  }

  @Test
  @TestFor (issues = "TW-29355")
  public void testPersistNameNotChanged() {
    final Resource rc = myResourceFactory.newQuotedResource(RESOURCE_NAME, 111, true);

    m.checking(new Expectations() {{
      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_OLD_RESOURCE_NAME);
      will(returnValue(RESOURCE_NAME));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
      will(returnValue(PROJECT_ID));

      oneOf(myProjectManager).findProjectById(PROJECT_ID);
      will(returnValue(myProject));

      allowing(myRequest);

      oneOf(myResourceHelper).getResourceFromRequest(PROJECT_ID, myRequest);
      will(returnValue(rc));

      allowing(myResources);

      // key part - resource is persisted even if name is not changed
      oneOf(myProject).persist(with(any(ConfigAction.class)));

      oneOf(myMessages).addMessage(myRequest, "Resource NAME was updated");

    }});
    myEditResourceAction.doProcess(myRequest, myResponse, myAjaxResponse);
  }

  @Test
  public void testUpdateLockNameInTree() {
    final String NEW_RESOURCE_NAME = "NEW_NAME";
    final Resource rcNewName = myResourceFactory.newQuotedResource(NEW_RESOURCE_NAME, 111, true);
    final SProject subProject = m.mock(SProject.class,  "sub-project") ;
    final List<SProject> projects = new ArrayList<SProject>();
    projects.add(subProject);

    final SBuildType projectBuildType = m.mock(SBuildType.class, "project-build-type");
    final SBuildType subProjectBuildType = m.mock(SBuildType.class, "subproject-build-type");

    final SharedResourcesFeature projectFeature = m.mock(SharedResourcesFeature.class, "project-feature");
    final SharedResourcesFeature subProjectFeature = m.mock(SharedResourcesFeature.class, "sub-project-feature");

    m.checking(new Expectations() {{
      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_OLD_RESOURCE_NAME);
      will(returnValue(RESOURCE_NAME));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
      will(returnValue(PROJECT_ID));

      oneOf(myProjectManager).findProjectById(PROJECT_ID);
      will(returnValue(myProject));

      allowing(myRequest);

      oneOf(myResourceHelper).getResourceFromRequest(PROJECT_ID, myRequest);
      will(returnValue(rcNewName));

      oneOf(myProject).getProjects();
      will(returnValue(projects));

      // update locks in subprojects
      oneOf(subProject).getOwnBuildTypes();
      will(returnValue(Collections.singletonList(subProjectBuildType)));
      oneOf(myFeatures).searchForFeatures(subProjectBuildType);
      will(returnValue(Collections.singletonList(subProjectFeature)));
      oneOf(subProjectFeature).updateLock(subProjectBuildType, RESOURCE_NAME, NEW_RESOURCE_NAME);
      will(returnValue(true));
      oneOf(subProject).persist(with(any(ConfigAction.class)));

      // update lock in project itself
      allowing(myProject).getOwnBuildTypes();
      will(returnValue(Collections.singletonList(projectBuildType)));
      oneOf(myFeatures).searchForFeatures(projectBuildType);
      will(returnValue(Collections.singletonList(projectFeature)));
      oneOf(projectFeature).updateLock(projectBuildType, RESOURCE_NAME, NEW_RESOURCE_NAME);
      will(returnValue(true));
      oneOf(myProject).persist(with(any(ConfigAction.class)));

      // update resource
      allowing(myResources);
      oneOf(myMessages).addMessage(myRequest, "Resource " + NEW_RESOURCE_NAME + " was updated");
    }});
    myEditResourceAction.doProcess(myRequest, myResponse, myAjaxResponse);
  }

  @Test
  @TestFor(issues = {"TW-44397", "TW-44398"})
  public void testUpdateAffectsOnlyProjectsWithUsages() throws Exception {
    final String NEW_RESOURCE_NAME = "NEW_NAME";

    final Resource newResource = myResourceFactory.newQuotedResource(NEW_RESOURCE_NAME, 111, true);

    final SProject childWithUsage = m.mock(SProject.class, "child-with-usage");
    final SProject childWithoutUsage = m.mock(SProject.class, "child-without-usage");
    final List<SProject> projects = new ArrayList<SProject>();
    projects.addAll(Arrays.asList(childWithUsage, childWithoutUsage));

    final SBuildType projectBuildType = m.mock(SBuildType.class, "project-build-type");
    final SBuildType childWithUsageType = m.mock(SBuildType.class, "child-with-usage-build-type");
    final SBuildType childWithoutUsageType = m.mock(SBuildType.class, "child-without-usage-build-type");

    final SharedResourcesFeature projectFeature = m.mock(SharedResourcesFeature.class, "project-feature");
    final SharedResourcesFeature childWithUsageFeature = m.mock(SharedResourcesFeature.class, "child-with-usage-feature");
    final SharedResourcesFeature childWithoutUsageFeature = m.mock(SharedResourcesFeature.class, "child-without-usage-feature");

    m.checking(new Expectations() {{
      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_OLD_RESOURCE_NAME);
      will(returnValue(RESOURCE_NAME));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
      will(returnValue(PROJECT_ID));

      oneOf(myProjectManager).findProjectById(PROJECT_ID);
      will(returnValue(myProject));

      allowing(myRequest);

      oneOf(myResourceHelper).getResourceFromRequest(PROJECT_ID, myRequest);
      will(returnValue(newResource));

      allowing(myResources);

      oneOf(myProject).getProjects();
      will(returnValue(projects));

      // update lock child that uses it
      oneOf(childWithUsage).getOwnBuildTypes();
      will(returnValue(Collections.singletonList(childWithUsageType)));
      oneOf(myFeatures).searchForFeatures(childWithUsageType);
      will(returnValue(Collections.singletonList(childWithUsageFeature)));
      oneOf(childWithUsageFeature).updateLock(childWithUsageType, RESOURCE_NAME, NEW_RESOURCE_NAME);
      will(returnValue(true));
      oneOf(childWithUsage).persist(with(any(ConfigAction.class)));      
      // skip persisting project without usage
      oneOf(childWithoutUsage).getOwnBuildTypes();
      will(returnValue(Collections.singletonList(childWithoutUsageType)));
      oneOf(myFeatures).searchForFeatures(childWithoutUsageType);
      will(returnValue(Collections.singletonList(childWithoutUsageFeature)));
      oneOf(childWithoutUsageFeature).updateLock(childWithoutUsageType, RESOURCE_NAME, NEW_RESOURCE_NAME);
      will(returnValue(false));
      // update lock in project itself
      allowing(myProject).getOwnBuildTypes();
      will(returnValue(Collections.singletonList(projectBuildType)));
      oneOf(myFeatures).searchForFeatures(projectBuildType);
      will(returnValue(Collections.singletonList(projectFeature)));
      oneOf(projectFeature).updateLock(projectBuildType, RESOURCE_NAME, NEW_RESOURCE_NAME);
      will(returnValue(true));
      oneOf(myProject).persist(with(any(ConfigAction.class)));
      // update resource
      allowing(myResources);
      oneOf(myMessages).addMessage(myRequest, "Resource " + NEW_RESOURCE_NAME + " was updated");
    }});
    myEditResourceAction.doProcess(myRequest, myResponse, myAjaxResponse);
  }

  @NotNull
  private ConfigActionFactory mockConfigActionFactory(@NotNull final Mockery m) {
    final ConfigActionFactory result = m.mock(ConfigActionFactory.class);
    m.checking(new Expectations() {{
      allowing(result).createAction(with(any(SProject.class)), with(any(String.class)));
      will(returnValue(m.mock(ConfigAction.class)));
    }});
    return result;
  }
}
