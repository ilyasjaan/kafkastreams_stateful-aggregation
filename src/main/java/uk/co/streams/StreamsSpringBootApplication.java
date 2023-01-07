package uk.co.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.TopicBuilder;

@SpringBootApplication
@EnableKafkaStreams
public class StreamsSpringBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamsSpringBootApplication.class, args);
	}

	@Bean
	NewTopic salesEvents() {
		return TopicBuilder.name("sales-events-v1").partitions(3).replicas(1).build();
	}

	@Bean
	NewTopic notificationsEvents() {
		return TopicBuilder.name("notifications-events-v1").partitions(3).replicas(1).build();
	}

	@Bean
	ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

}
