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
package org.openengsb.core.services.internal;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.openengsb.core.api.ConnectorProvider;
import org.openengsb.core.api.VirtualConnectorProvider;
import org.openengsb.core.common.internal.Activator;
import org.openengsb.core.services.internal.virtual.FileWatcherConnectorProvider;
import org.openengsb.core.test.AbstractOsgiMockServiceTest;
import org.openengsb.core.test.NullDomain;
import org.osgi.framework.ServiceReference;

public class FileWatcherConnectorTest extends AbstractOsgiMockServiceTest {

    @Before
    public void setUp() throws Exception {
        Activator activator = new Activator();
        activator.start(bundleContext);
        createDomainProviderMock(NullDomain.class, "example");

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("connector", "filewatcher");
        FileWatcherConnectorProvider provider = new FileWatcherConnectorProvider();
        provider.setId("filewatcher");
        registerService(provider, props, VirtualConnectorProvider.class);
    }

    @Test
    public void createConnector() throws Exception {
        Collection<ServiceReference<ConnectorProvider>> serviceReferences = bundleContext.getServiceReferences(ConnectorProvider.class, "(domain=example)");
        assertThat(serviceReferences.isEmpty(), is(false));
    }
}
