/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.tasmo.lib.process.traversal;

import com.jivesoftware.os.tasmo.id.Id;
import com.jivesoftware.os.tasmo.id.ObjectId;
import com.jivesoftware.os.tasmo.id.TenantIdAndCentricId;
import com.jivesoftware.os.tasmo.lib.write.PathId;
import com.jivesoftware.os.tasmo.model.process.WrittenEvent;
import com.jivesoftware.os.tasmo.model.process.WrittenInstance;
import java.util.Objects;

/**
 *
 */
public class TraverseViewValueWriter implements StepTraverser {

    private final String viewIdFieldName;
    private final String viewClassName;
    private final String modelPathId;

    public TraverseViewValueWriter(String viewIdFieldName, String viewClassName, String modelPathId) {
        this.viewIdFieldName = viewIdFieldName;
        this.viewClassName = viewClassName;
        this.modelPathId = modelPathId;
    }

    @Override
    public void process(final TenantIdAndCentricId tenantIdAndCentricId,
            PathTraversalContext context,
            PathId pathId,
            StepStream streamTo) throws Exception {

        ObjectId objectId = pathId.getObjectId();
        Id viewId = buildAlternateViewId(context.getWrittenEvent());
        if (viewId == null) {
            viewId = objectId.getId();
        }

        context.writeViewFields(viewClassName, modelPathId, viewId);
    }

    protected Id buildAlternateViewId(WrittenEvent writtenEvent) throws IllegalStateException {
        Id alternateViewId = null;
        if (viewIdFieldName != null) {
            WrittenInstance payload = writtenEvent.getWrittenInstance();

            if (payload.hasField(viewIdFieldName)) {
                try {
                    // We are currently only supporting one ref, but with light change we could support a list of refs. Should we?
                    ObjectId objectId = payload.getReferenceFieldValue(viewIdFieldName);
                    if (objectId != null) {
                        alternateViewId = objectId.getId();
                    }
                    alternateViewId = writtenEvent.getWrittenInstance().getIdFieldValue(viewIdFieldName);
                } catch (Exception x) {
                    throw new IllegalStateException("instanceClassName:" + payload.getInstanceId().getClassName() + " requires that field:"
                            + viewIdFieldName
                            + " always be present and of type ObjectId. Please check that you have marked this field as mandatory.", x);
                }
            } else {
                throw new IllegalStateException("instanceClassName:" + payload.getInstanceId().getClassName() + " requires that field:"
                        + viewIdFieldName
                        + " always be present and of type ObjectId. Please check that you have marked this field as mandatory.");
            }
        }
        return alternateViewId;
    }

    @Override
    public String toString() {
        return "TraverseViewValueWriter{" + "viewIdFieldName=" + viewIdFieldName + ", viewClassName=" + viewClassName + ", modelPathId=" + modelPathId + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.viewIdFieldName);
        hash = 89 * hash + Objects.hashCode(this.viewClassName);
        hash = 89 * hash + Objects.hashCode(this.modelPathId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TraverseViewValueWriter other = (TraverseViewValueWriter) obj;
        if (!Objects.equals(this.viewIdFieldName, other.viewIdFieldName)) {
            return false;
        }
        if (!Objects.equals(this.viewClassName, other.viewClassName)) {
            return false;
        }
        if (!Objects.equals(this.modelPathId, other.modelPathId)) {
            return false;
        }
        return true;
    }

}
