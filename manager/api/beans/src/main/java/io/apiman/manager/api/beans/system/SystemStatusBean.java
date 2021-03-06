/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.manager.api.beans.system;

import java.io.Serializable;

import org.jboss.errai.common.client.api.annotations.Portable;

/**
 * A simple bean used to return system status information.
 *
 * @author eric.wittmann@redhat.com
 */
@Portable
public class SystemStatusBean implements Serializable {
    
    private static final long serialVersionUID = -7099876167335105162L;
    
    private boolean up;
    private String version;

    /**
     * Constructor.
     */
    public SystemStatusBean() {
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the up
     */
    public boolean isUp() {
        return up;
    }

    /**
     * @param up the up to set
     */
    public void setUp(boolean up) {
        this.up = up;
    }

}
