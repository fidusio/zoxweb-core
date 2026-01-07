package org.zoxweb.shared.task;

import org.zoxweb.shared.util.Appointment;
import org.zoxweb.shared.util.SUS;

public final class ScheduledAttachment<V> {
    private volatile V attachment;
    private volatile Appointment appointment;

    public V attachment() {
        return attachment;
    }

    public ScheduledAttachment<V> attach(V attachment) {
        SUS.checkIfNull("callback is null", attachment);
        this.attachment = attachment;
        return this;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public ScheduledAttachment<V> setAppointment(Appointment appointment) {
        this.appointment = appointment;
        return this;
    }
}
