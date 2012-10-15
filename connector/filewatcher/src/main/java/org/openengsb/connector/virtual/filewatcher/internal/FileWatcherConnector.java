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
package org.openengsb.connector.virtual.filewatcher.internal;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Timer;

import org.openengsb.core.api.DomainEvents;
import org.openengsb.core.api.Event;
import org.openengsb.core.common.VirtualConnector;

public class FileWatcherConnector extends VirtualConnector {

    private String watchfile;

    private DomainEvents domainEvents;

    private Method raiseMethod;

    private Class<? extends Event> eventClass;

    private Timer timer = new Timer();

    public FileWatcherConnector(String instanceId) {
        super(instanceId);
    }

    public FileWatcherConnector(String instanceId, String watchfile) {
        super(instanceId);
        setWatchfile(watchfile);
    }

    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setWatchfile(String watchfile) {
        this.watchfile = watchfile;
    }

    public void init() {
        File file = new File(watchfile);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        for (Method m : domainEvents.getClass().getMethods()) {
            if (Arrays.equals(m.getParameterTypes(), new Class<?>[]{eventClass})) {
                raiseMethod = m;
                break;
            }
        }
        if (raiseMethod == null) {
            throw new IllegalStateException("could not find correspinding raise-method for event " + eventClass.getName() + " in interface " + domainEvents.getClass().getName());
        }
        timer.schedule(new DirectoryWatcher(file) {
            @Override
            protected void onFileModified() {
                Event event = null;
                try {
                    event = eventClass.newInstance();
                    raiseMethod.invoke(domainEvents, event);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, 1000);
    }

    public void setEventClass(Class<? extends Event> eventClass) {
        this.eventClass = eventClass;
    }

    public void setDomainEvents(DomainEvents domainEvents) {
        this.domainEvents = domainEvents;
    }
}
