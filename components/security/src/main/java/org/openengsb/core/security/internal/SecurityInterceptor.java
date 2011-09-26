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

package org.openengsb.core.security.internal;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.ArrayUtils;
import org.openengsb.core.api.security.SpecialAccessControlHandler;
import org.openengsb.core.api.security.annotation.SpecialAccessControl;
import org.openengsb.core.api.security.service.AccessDeniedException;
import org.openengsb.core.common.OpenEngSBCoreServices;
import org.openengsb.core.common.util.BundleAuthenticationToken;
import org.openengsb.domain.authorization.AuthorizationDomain;
import org.openengsb.domain.authorization.AuthorizationDomain.Access;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityInterceptor implements MethodInterceptor {

    private AuthorizationDomain authorizer;

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityInterceptor.class);

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        LOGGER.debug("intercepting method {}", mi.getMethod());
        if (ArrayUtils.contains(Object.class.getMethods(), mi.getMethod())) {
            LOGGER.info("is Object-method; skipping");
            return mi.proceed();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof BundleAuthenticationToken) {
            return mi.proceed();
        }
        String username = authentication.getName();
        Access decisionResult = authorizer.checkAccess(username, mi);
        if (decisionResult == Access.DENIED) {
            LOGGER.warn("Access denied because resul was {}", decisionResult);
            throw new AccessDeniedException();
        }
        if (decisionResult == Access.GRANTED) {
            return mi.proceed();
        }
        SpecialAccessControl annotation = AnnotationUtils.findAnnotation(mi.getMethod(), SpecialAccessControl.class);
        if (annotation != null) {
            SpecialAccessControlHandler service =
                (SpecialAccessControlHandler) OpenEngSBCoreServices.getServiceUtilsService().getService(
                    String.format("(controlHandler.id=%s)", annotation.value()));
            if (service.isAuthorized(username, mi)) {
                return mi.proceed();
            }
        }
        LOGGER.warn("Access denied because resul was {}", decisionResult);
        throw new AccessDeniedException();
    }

    public void setAuthorizer(AuthorizationDomain authorizer) {
        this.authorizer = authorizer;
    }

}
