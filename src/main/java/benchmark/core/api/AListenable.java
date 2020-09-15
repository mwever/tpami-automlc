package benchmark.core.api;

import com.google.common.eventbus.EventBus;

public abstract class AListenable implements IListenable {

	private EventBus eventBus = new EventBus();

	@Override
	public EventBus getEventBus() {
		return this.eventBus;
	}

}
