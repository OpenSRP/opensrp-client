package org.ei.opensrp.clientandeventmodel;

import org.codehaus.jackson.annotate.JsonProperty;

public abstract class MotechBaseDataObject {
    @JsonProperty
    protected String type;

    protected MotechBaseDataObject() {
        this.type = this.getClass().getSimpleName();
    }

    protected MotechBaseDataObject(String type) {
        this.type = type;
    }

    private static final long serialVersionUID = 1L;

}
