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

package org.openengsb.domain.example.event;

import javax.xml.bind.annotation.XmlType;

import org.openengsb.core.api.Event;
import org.openengsb.domain.example.model.ExampleRequestModel;

@XmlType(namespace = "http://example.domain.openengsb.org/")
public class LogEvent extends Event {

    private String message;
    private String level;
    private ExampleRequestModel model;

    public LogEvent() {
        super("LogEvent");
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public ExampleRequestModel getModel() {
        return model;
    }

    public void setModel(ExampleRequestModel model) {
        this.model = model;
    }

}
