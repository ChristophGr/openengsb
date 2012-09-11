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

import org.openengsb.core.api.Event;
import org.openengsb.domain.example.model.ExampleRequestModel;

public class CommitDataEvent extends Event {

    private ExampleRequestModel[] changedModels;
    private ExampleRequestModel[] deletedModels;

    public ExampleRequestModel[] getChangedModels() {
        return changedModels;
    }

    public void setChangedModels(ExampleRequestModel[] changedModels) {
        this.changedModels = changedModels;
    }

    public ExampleRequestModel[] getDeletedModels() {
        return deletedModels;
    }

    public void setDeletedModels(ExampleRequestModel[] deletedModels) {
        this.deletedModels = deletedModels;
    }
}
