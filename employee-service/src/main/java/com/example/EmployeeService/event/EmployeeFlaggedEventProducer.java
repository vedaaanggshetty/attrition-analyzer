package com.example.EmployeeService.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.EmployeeService.exception.EventPublicationException;

/**
 * Publishes {@link EmployeeFlaggedEvent}s to Kafka. Kept separate from
 * EmployeeController/EmployeeService so the flag flow's business logic
 * doesn't depend on Kafka's client API directly.
 */
@Component
public class EmployeeFlaggedEventProducer {

	private final KafkaTemplate<String, EmployeeFlaggedEvent> kafkaTemplate;
	private final String topic;

	public EmployeeFlaggedEventProducer(KafkaTemplate<String, EmployeeFlaggedEvent> kafkaTemplate,
			@Value("${notification.kafka.topic}") String topic) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
	}

	public void publish(EmployeeFlaggedEvent event) {
		try {
			// .get() makes the publish synchronous so a broken/unreachable broker
			// surfaces as a failure the caller can turn into a 503, rather than
			// succeeding the HTTP request while the event silently never arrives.
			kafkaTemplate.send(topic, event.employeeId(), event).get();
		} catch (Exception ex) {
			throw new EventPublicationException("Failed to publish EmployeeFlaggedEvent to Kafka", ex);
		}
	}
}
