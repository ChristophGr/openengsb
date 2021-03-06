/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openengsb.ui.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.openengsb.core.api.security.SecurityAttributeManager;
import org.openengsb.core.api.security.annotation.SecurityAttribute;
import org.openengsb.core.api.security.annotation.SecurityAttributes;
import org.openengsb.core.api.security.model.Authentication;
import org.openengsb.core.api.security.model.SecurityAttributeEntry;
import org.openengsb.core.common.OpenEngSBCoreServices;
import org.openengsb.core.common.util.SpringSecurityContext;
import org.openengsb.domain.authorization.AuthorizationDomain;
import org.openengsb.domain.authorization.AuthorizationDomain.Access;
import org.openengsb.ui.api.UIAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class DomainAuthorizationStrategy implements IAuthorizationStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainAuthorizationStrategy.class);

    private AuthorizationDomain authorizer = OpenEngSBCoreServices.getWiringService().getDomainEndpoint(
        AuthorizationDomain.class, "authorization-root");

    @Override
    public boolean isActionAuthorized(Component arg0, Action arg1) {
        List<SecurityAttributeEntry> attributeList = Lists.newArrayList();
        if (hasSecurityAnnotation(arg0.getClass())) {
            attributeList.addAll(getSecurityAttributes(arg0.getClass()));
        }
        Collection<SecurityAttributeEntry> runtimeAttributes = SecurityAttributeManager.getAttribute(arg0);
        if (runtimeAttributes != null) {
            attributeList.addAll(runtimeAttributes);
        }
        if (attributeList.isEmpty()) {
            return true;
        }

        Authentication authentication = getAuthenticatedUser();
        if (authentication == null) {
            return false;
        }
        String user = authentication.getUsername();
        UIAction secureAction =
            new UIAction(attributeList, arg1.getName(), ImmutableMap.of("component", (Object) arg0));

        Access checkAccess = authorizer.checkAccess(user, secureAction);
        if (checkAccess != Access.GRANTED) {
            LOGGER.warn("User {} was denied action {} on component {}", new Object[]{ user, arg1.toString(),
                arg0.getClass().getName() });
        }
        return checkAccess == Access.GRANTED;
    }

    @Override
    public <T extends Component> boolean isInstantiationAuthorized(Class<T> componentClass) {
        if (!hasSecurityAnnotation(componentClass)) {
            return true;
        }
        Authentication authentication = getAuthenticatedUser();
        if (authentication == null) {
            return false;
        }
        String user = authentication.getUsername();

        LOGGER.trace("security-attribute-annotation present on {}", componentClass);

        return authorizer.checkAccess(user, new UIAction(getSecurityAttributes(componentClass))) == Access.GRANTED;
    }

    private static Authentication getAuthenticatedUser() {
        return SpringSecurityContext.unwrapToken(SecurityContextHolder.getContext().getAuthentication());
    }

    private boolean hasSecurityAnnotation(Class<? extends Component> class1) {
        return class1.isAnnotationPresent(SecurityAttribute.class)
                || class1.isAnnotationPresent(SecurityAttributes.class);
    }

    private Collection<SecurityAttributeEntry> getSecurityAttributes(Class<? extends Component> componentClass) {
        SecurityAttribute annotation = componentClass.getAnnotation(SecurityAttribute.class);
        if (annotation != null) {
            return Arrays.asList(convertAnnotationToEntry(annotation));
        }
        SecurityAttributes annotation2 = componentClass.getAnnotation(SecurityAttributes.class);
        if (annotation2 != null) {
            Collection<SecurityAttributeEntry> result = Lists.newArrayList();
            for (SecurityAttribute a : annotation2.value()) {
                result.add(convertAnnotationToEntry(a));
            }
            return result;
        }
        return null;
    }

    private SecurityAttributeEntry convertAnnotationToEntry(SecurityAttribute annotation) {
        return new SecurityAttributeEntry(annotation.key(), annotation.value());
    }

    public static final void registerComponent(Component component, SecurityAttributeEntry... securityAttributes) {
        registerComponent(component, Arrays.asList(securityAttributes));
    }

    public static final void registerComponent(Component component,
            Collection<SecurityAttributeEntry> securityAttributes) {
        SecurityAttributeManager.storeAttribute(component, securityAttributes);
    }

    public void setAuthorizer(AuthorizationDomain authorizer) {
        this.authorizer = authorizer;
    }
}
