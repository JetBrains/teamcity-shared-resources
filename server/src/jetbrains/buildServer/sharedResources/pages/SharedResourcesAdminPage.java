package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.controllers.admin.AdminPage;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.PositionConstraint;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Oleg Rybak
 */
public class SharedResourcesAdminPage extends AdminPage {

  public SharedResourcesAdminPage(@NotNull PagePlaces pagePlaces, @NotNull PluginDescriptor descriptor) {
    super(pagePlaces);
    setPluginName(SharedResourcesPluginConstants.PLUGIN_NAME);
    setIncludeUrl(descriptor.getPluginResourcesPath("/adminPage.jsp"));
    setTabTitle("Shared Resources");
    setPosition(PositionConstraint.after("clouds", "email", "jabber"));
    register();
  }

  @Override
  public boolean isAvailable(@NotNull HttpServletRequest request) {
    return true;
  }

  @NotNull
  @Override
  public String getGroup() {
    return SERVER_RELATED_GROUP;
  }
}
