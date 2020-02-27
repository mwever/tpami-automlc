package ai.libs.hyperopt.util;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class EventForwarder {

	private final EventBus source;
	private final EventBus target;

	public EventForwarder(final EventBus source, final EventBus target) {
		this.source = source;
		this.target = target;
		this.source.register(this);
	}

	public void cancel() {
		this.source.unregister(this);
	}

	@Subscribe
	public void forward(final Object event) {
		this.target.post(event);
	}

}
