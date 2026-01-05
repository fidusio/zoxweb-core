package org.zoxweb.shared.task;

import org.zoxweb.shared.util.Appointment;
import org.zoxweb.shared.util.SUS;

import java.util.concurrent.Callable;

public interface CallableConsumer<V>
    extends Callable<V>, ConsumerCallback<V>
{
    final class WithSchedule<V>
    {
        private volatile ConsumerCallback<V> callback;
        private volatile Appointment appointment;

        public ConsumerCallback<V> getCallback() {
            return callback;
        }

        public WithSchedule<V> setCallback(ConsumerCallback<V> callback) {
            SUS.checkIfNull("callback is null", callback);
            this.callback = callback;
            return this;
        }

        public Appointment getAppointment() {
            return appointment;
        }

        public WithSchedule<V> setAppointment(Appointment appointment) {
            this.appointment = appointment;
            return this;
        }
    }
}
