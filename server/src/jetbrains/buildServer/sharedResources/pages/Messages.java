

package jetbrains.buildServer.sharedResources.pages;

import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.controllers.ActionMessages;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class Messages {

  public void addMessage(@NotNull final HttpServletRequest request, String message) {
    ActionMessages.getOrCreateMessages(request).addMessage(SharedResourcesPluginConstants.WEB.ACTION_MESSAGE_KEY, message);
  }

}