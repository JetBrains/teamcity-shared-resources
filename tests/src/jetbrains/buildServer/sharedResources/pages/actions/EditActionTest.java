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
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.pages.Messages;
import jetbrains.buildServer.sharedResources.pages.ResourceHelper;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.util.TestFor;
import org.jdom.Element;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    final SharedResourcesFeatures features = m.mock(SharedResourcesFeatures.class);
    myProjectManager = m.mock(ProjectManager.class);
    myResources = m.mock(Resources.class);
    myResourceHelper = m.mock(ResourceHelper.class);
    myRequest = m.mock(HttpServletRequest.class);
    myResponse = m.mock(HttpServletResponse.class);
    myAjaxResponse = m.mock(Element.class);
    myProject = m.mock(SProject.class);
    myMessages = m.mock(Messages.class);
    myEditResourceAction = new EditResourceAction(myProjectManager, myResources, myResourceHelper, features, myMessages);

  }

  @Test
  @TestFor (issues = "TW-29355")
  public void testPersistNameNotChanged() {
    final Resource rc = ResourceFactory.newQuotedResource(RESOURCE_NAME, 111, true);

    m.checking(new Expectations() {{
      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_OLD_RESOURCE_NAME);
      will(returnValue(RESOURCE_NAME));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
      will(returnValue(PROJECT_ID));

      oneOf(myProjectManager).findProjectById(PROJECT_ID);
      will(returnValue(myProject));

      allowing(myRequest);

      oneOf(myResourceHelper).getResourceFromRequest(myRequest);
      will(returnValue(rc));

      allowing(myResources);

      // key part - resource is persisted even if name is not changed
      oneOf(myProject).persist();

      oneOf(myMessages).addMessage(myRequest, "Resource NAME was updated");

    }});
    myEditResourceAction.doProcess(myRequest, myResponse, myAjaxResponse);
  }
}
