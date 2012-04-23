package org.openengsb.core.common.json;

import org.openengsb.labs.delegation.service.DelegationClassLoader;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DelegatingTypeIdResolver extends CustomTypeIdResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingTypeIdResolver.class);

    private DelegationClassLoader delegationClassLoader;

    public DelegatingTypeIdResolver() {
    }
    
    public DelegatingTypeIdResolver(BundleContext bundleContext) {
        setBundleContext(bundleContext);
    }

    @Override
    protected Class<?> doLoadClass(String name) throws ClassNotFoundException {
        try {
            return super.doLoadClass(name);
        } catch (ClassNotFoundException e) {
            LOGGER.info("could not find class {}. Try to find it using ClassProviders", name);
            return delegationClassLoader.loadClass(name);
        }
    }

    public void setBundleContext(BundleContext bundleContext) {
        delegationClassLoader = new DelegationClassLoader(bundleContext, (ClassLoader) null);
    }
}
