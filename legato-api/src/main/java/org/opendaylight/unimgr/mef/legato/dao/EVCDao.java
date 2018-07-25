/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.legato.dao;

import java.util.List;
/**
 * @author santanu.de@xoriant.com
 */

public class EVCDao {

    private String evcId;
    private Integer maxFrameSize;
    private String connectionType;
    private String svcType;
    private List<String> uniList;

    public String getEvcId() {
        return evcId;
    }

    public void setEvcId(String evcId) {
        this.evcId = evcId;
    }

    public Integer getMaxFrameSize() {
        return maxFrameSize;
    }

    public void setMaxFrameSize(Integer maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getSvcType() {
        return svcType;
    }

    public void setSvcType(String svcType) {
        this.svcType = svcType;
    }

    public List<String> getUniList() {
        return uniList;
    }

    public void setUniList(List<String> uniList) {
        this.uniList = uniList;
    }

}
