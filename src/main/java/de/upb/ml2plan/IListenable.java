package de.upb.ml2plan;

import com.google.common.eventbus.EventBus;

public interface IListenable {

	public EventBus getEventBus();

	default void registerListener(final Object o) {
		this.getEventBus().register(o);
	}

	default void unregisterListener(final Object o) {
		this.getEventBus().unregister(o);
	}

}
