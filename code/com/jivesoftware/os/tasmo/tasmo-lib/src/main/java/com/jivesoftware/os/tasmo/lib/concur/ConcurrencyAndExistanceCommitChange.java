package com.jivesoftware.os.tasmo.lib.concur;

import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import com.jivesoftware.os.tasmo.id.ObjectId;
import com.jivesoftware.os.tasmo.id.TenantIdAndCentricId;
import com.jivesoftware.os.tasmo.lib.process.existence.ExistenceStore;
import com.jivesoftware.os.tasmo.lib.write.CommitChange;
import com.jivesoftware.os.tasmo.lib.write.CommitChangeException;
import com.jivesoftware.os.tasmo.lib.write.PathId;
import com.jivesoftware.os.tasmo.lib.write.ViewFieldChange;
import com.jivesoftware.os.tasmo.reference.lib.ReferenceWithTimestamp;
import com.jivesoftware.os.tasmo.reference.lib.concur.ConcurrencyStore;
import com.jivesoftware.os.tasmo.reference.lib.concur.ConcurrencyStore.FieldVersion;
import com.jivesoftware.os.tasmo.reference.lib.concur.PathModifiedOutFromUnderneathMeException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author jonathan
 */
public class ConcurrencyAndExistanceCommitChange implements CommitChange {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ConcurrencyStore concurrencyStore;
    private final ExistenceStore existenceStore;
    private final CommitChange commitChange;

    public ConcurrencyAndExistanceCommitChange(ConcurrencyStore concurrencyStore,
            ExistenceStore existenceStore,
            CommitChange commitChange) {
        this.concurrencyStore = concurrencyStore;
        this.existenceStore = existenceStore;
        this.commitChange = commitChange;
    }

    @Override
    public void commitChange(TenantIdAndCentricId tenantIdAndCentricId, List<ViewFieldChange> changes) throws CommitChangeException {

        Set<ObjectId> check = new HashSet<>();
        for (ViewFieldChange change : changes) {
            for (PathId ref : change.getModelPathInstanceIds()) {
                check.add(ref.getObjectId());
            }
            for (ReferenceWithTimestamp ref : change.getModelPathVersions()) {
                check.add(ref.getObjectId());
            }
        }

        Set<ObjectId> existence = existenceStore.getExistence(tenantIdAndCentricId.getTenantId(), check);

        List<ViewFieldChange> acceptableChanges = new ArrayList<>();
        for (ViewFieldChange fieldChange : changes) {
            Set<ObjectId> ids = new HashSet<>();
            for (PathId ref : fieldChange.getModelPathInstanceIds()) {
                ids.add(ref.getObjectId());
            }
            for (ReferenceWithTimestamp ref : fieldChange.getModelPathVersions()) {
                ids.add(ref.getObjectId());
            }

            if (fieldChange.getType() == ViewFieldChange.ViewFieldChangeType.remove || existence.containsAll(ids)) {
                acceptableChanges.add(fieldChange);
            } else {
                traceLogging(ids, existence, fieldChange);
            }
        }

        commitChange.commitChange(tenantIdAndCentricId, acceptableChanges);

        // TODO re-write to use batching!
        for (ViewFieldChange fieldChange : acceptableChanges) {
            if (fieldChange.getType() == ViewFieldChange.ViewFieldChangeType.add) {
                List<FieldVersion> expected = new ArrayList<>();
                List<ReferenceWithTimestamp> versions = fieldChange.getModelPathVersions();
                for (ReferenceWithTimestamp version : versions) {
                    if (version != null) {
                        expected.add(new FieldVersion(version.getObjectId(), version.getFieldName(), version.getTimestamp()));
                    }
                }
                List<FieldVersion> was = concurrencyStore.checkIfModified(tenantIdAndCentricId.getTenantId(), expected);
                if (expected != was) {
                    PathModifiedOutFromUnderneathMeException pmofume = new PathModifiedOutFromUnderneathMeException(expected, was);
                    //pmofume.printStackTrace();
                    throw pmofume;
                }
            }
        }
    }

    private void traceLogging(Set<ObjectId> ids, Set<ObjectId> existence, ViewFieldChange fieldChange) {
        if (LOG.isTraceEnabled()) {
            StringBuilder msg = new StringBuilder().append(" existence:");

            String sep = "";
            for (ObjectId id : ids) {
                msg.append(sep);
                msg.append(id).append(existence.contains(id) ? " exists" : " does not exist");
                sep = ",";
            }
            msg.append(" change:").append(fieldChange);

            LOG.trace("WRITE BLOCKED DUE TO LACK OF EXISTANCE:" + msg.toString());
        }
    }
}