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

package org.openengsb.ui.admin.serviceListPanel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.openengsb.core.api.AliveState;
import org.openengsb.core.api.Domain;
import org.openengsb.core.api.OsgiUtilsService;
import org.openengsb.core.api.security.annotation.SecurityAttribute;
import org.ops4j.pax.wicket.api.PaxWicketBean;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SecurityAttribute(key = "org.openengsb.ui.component", value = "SERVICE_USER")
@SuppressWarnings("serial")
public class ServiceListPanel extends Panel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceListPanel.class);

    private class ServiceEntry implements Comparable<ServiceEntry>, Serializable {
        private Dictionary<String, Object> properties = new Hashtable<String, Object>();
        private AliveState aliveState;

        private String getInstanceId() {
            return (String) properties.get("id");
        }

        @Override
        public int compareTo(ServiceEntry o) {
            if (aliveState != o.aliveState) {
                return aliveState.compareTo(o.aliveState);
            }
            return getInstanceId().compareTo(o.getInstanceId());
        }
    }

    @PaxWicketBean
    private OsgiUtilsService serviceUtils;

    private class ServiceEntryListModel extends LoadableDetachableModel<List<ServiceEntry>> {
        @Override
        protected List<ServiceEntry> load() {
            List<ServiceReference> listServiceReferences = serviceUtils.listServiceReferences(Domain.class);
            List<ServiceEntry> result = new ArrayList<ServiceListPanel.ServiceEntry>();
            for (ServiceReference ref : listServiceReferences) {
                ServiceEntry entry = new ServiceEntry();
                for (String key : ref.getPropertyKeys()) {
                    entry.properties.put(key, ref.getProperty(key));
                }
                Domain service = serviceUtils.getService(Domain.class, ref);
                try {
                    entry.aliveState = service.getAliveState();
                } catch (Exception e) {
                    LOGGER.error("Couldn't load service entry " + ref, e);
                    entry.aliveState = AliveState.OFFLINE;
                }
                result.add(entry);
            }
            return result;
        }
    }

    public ServiceListPanel(String id) {
        super(id);
        WebMarkupContainer container = new WebMarkupContainer("serviceListContainer");
        container.add(new ListView<ServiceEntry>("serviceListView", new ServiceEntryListModel()) {
            @Override
            protected void populateItem(final ListItem<ServiceEntry> item) {
                item.add(new Label("service.name", item.getModelObject().getInstanceId()));
                item.add(new Label("service.state", item.getModelObject().aliveState.name()));
            }
        });
        add(container);
    }

}
