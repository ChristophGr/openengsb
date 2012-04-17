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

package org.openengsb.core.api.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.openengsb.core.api.Constants;
import org.openengsb.labs.delegation.service.Provide;

/**
 * Representation of a unique identification of connector instances.
 * 
 * A connector instance is identified by the name of the connector id, the id of the domain it represents and an
 * additional instance id.
 * 
 */
@SuppressWarnings("serial")
@Provide(alias = "ConnectorDefinition")
public class ConnectorDefinition implements Serializable {

    private static final String CONNECTOR_ID_SEPARATOR = "+";
    private String domainId;
    private String connectorId;
    private String instanceId;

    public ConnectorDefinition() {
    }

    public ConnectorDefinition(String domainId, String connectorId, String instanceId) {
        this.domainId = domainId;
        this.connectorId = connectorId;
        this.instanceId = instanceId;
    }

    public String getDomainId() {
        return this.domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getConnectorId() {
        return this.connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * returns a map-representation of the ID for use with
     * {@link org.openengsb.core.api.persistence.ConfigPersistenceService}
     */
    public Map<String, String> toMetaData() {
        Map<String, String> metaData = new HashMap<String, String>();
        metaData.put(Constants.DOMAIN_KEY, domainId);
        metaData.put(Constants.CONNECTOR_KEY, connectorId);
        metaData.put(Constants.ID_KEY, instanceId);
        return metaData;
    }

    /**
     * parses a ConnectorId object from a Map-representation used in
     * {@link org.openengsb.core.api.persistence.ConfigPersistenceService}
     */
    public static ConnectorDefinition fromMetaData(Map<String, String> metaData) {
        return new ConnectorDefinition(metaData.get(Constants.DOMAIN_KEY), metaData.get(Constants.CONNECTOR_KEY),
            metaData.get(Constants.ID_KEY));
    }

    /**
     * generates a new unique ConnectorID for the given domain and connector.
     * 
     * A {@link UUID} is used as unique string-identifier.
     */
    public static ConnectorDefinition generate(String domainId, String connectorId) {
        String instanceId = UUID.randomUUID().toString();
        return new ConnectorDefinition(domainId, connectorId, instanceId);
    }

    /**
     * parses a connectorID from a string-representation of the format:
     * 
     * "&lt;domainType&gt;+&lt;connectorType&gt;+&lt;instanceId&gt;"
     * 
     * Example: "scm+git+projectx-main-repo"
     */
    public static ConnectorDefinition fromFullId(String fullId) {
        Scanner s = new Scanner(fullId);
        s.useDelimiter("\\+");
        String domain = s.next();
        String connector = s.next();
        String instanceId = s.next();
        if (s.hasNext()) {
            s.useDelimiter("\\\n");
            instanceId += s.next();
        }
        return new ConnectorDefinition(domain, connector, instanceId);
    }

    /**
     * returns a string-representation of the ConnectorDefinition for use with the service-registry. It is also used as
     * instanceId returned by {@link org.openengsb.core.api.OpenEngSBService#getInstanceId()}
     * 
     * The resulting String can be parsed to a ConnectorId again using the {@link ConnectorDefinition#parse} method
     */
    public String toFullID() {
        return domainId + CONNECTOR_ID_SEPARATOR + connectorId + CONNECTOR_ID_SEPARATOR + instanceId;
    }

    @Override
    public String toString() {
        return toFullID();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.connectorId == null) ? 0 : this.connectorId.hashCode());
        result = prime * result + ((this.domainId == null) ? 0 : this.domainId.hashCode());
        result = prime * result + ((this.instanceId == null) ? 0 : this.instanceId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() == obj.getClass()) {
            ConnectorDefinition other = (ConnectorDefinition) obj;
            return equals(other);
        }
        if (Map.class.isInstance(obj)) {
            Map<?, ?> metadata = (Map<?, ?>) obj;
            return toMetaData().equals(metadata);
        }
        if (obj instanceof String) {
            return toFullID().equals(obj);
        }
        return false;
    }

    private boolean equals(ConnectorDefinition other) {
        if (this.connectorId == null) {
            if (other.connectorId != null) {
                return false;
            }
        } else if (!this.connectorId.equals(other.connectorId)) {
            return false;
        }
        if (this.domainId == null) {
            if (other.domainId != null) {
                return false;
            }
        } else if (!this.domainId.equals(other.domainId)) {
            return false;
        }
        if (this.instanceId == null) {
            if (other.instanceId != null) {
                return false;
            }
        } else if (!this.instanceId.equals(other.instanceId)) {
            return false;
        }
        return true;
    }

}
