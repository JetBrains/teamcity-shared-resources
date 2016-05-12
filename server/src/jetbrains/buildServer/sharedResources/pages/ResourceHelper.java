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

package jetbrains.buildServer.sharedResources.pages;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Function;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.*;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ResourceHelper {

  private final Logger LOG = Logger.getInstance(ResourceHelper.class.getName());

  @Nullable
  public Resource getResourceFromRequest(@NotNull final HttpServletRequest request) {
    final String resourceName = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME);
    final ResourceType resourceType = ResourceType.fromString(request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_TYPE));
    Resource resource = null;
    if (ResourceType.QUOTED.equals(resourceType)) {
      final String resourceQuota = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_QUOTA);
      if (resourceQuota != null && !"".equals(resourceQuota)) { // we have quoted resource
        try {
          int quota = Integer.parseInt(resourceQuota);
          resource = ResourceFactory.newQuotedResource(resourceName, quota, true);
        } catch (IllegalArgumentException e) {
          LOG.warn("Illegal argument supplied in quota for resource [" + resourceName + "]");
        }
      } else {
        resource = ResourceFactory.newInfiniteResource(resourceName, true);
      }
    } else if (ResourceType.CUSTOM.equals(resourceType)) {
      final String values = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_VALUES);
      final List<String> strings = StringUtil.split(values, true, '\r', '\n');
      resource = ResourceFactory.newCustomResource(resourceName, strings, true);
    }
    return resource;
  }

  @NotNull
  public Resource getResourceInState(@NotNull final Resource resource, final boolean state) {
    Resource result;
    final ResourceType resourceType = resource.getType();
    if (ResourceType.QUOTED.equals(resourceType)) {
      final QuotedResource qr = (QuotedResource) resource;
      if (qr.isInfinite()) {
        result = ResourceFactory.newInfiniteResource(resource.getName(), state);
      } else {
        result = ResourceFactory.newQuotedResource(resource.getName(), qr.getQuota(), state);
      }
    } else {
      final CustomResource cr = (CustomResource) resource;
      result = ResourceFactory.newCustomResource(resource.getName(), cr.getValues(), state);
    }
    return result;
  }

  @NotNull
  public static String formatLocksList(@NotNull final Collection<Lock> invalidLocks) {
    return StringUtil.join(invalidLocks, new Function<Lock, String>() {
      @Override
      public String fun(Lock lock) {
        return lock.getName();
      }
    }, ", ");
  }
}

