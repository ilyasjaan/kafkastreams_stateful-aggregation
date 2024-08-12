package uk.co.streams.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;
import uk.co.streams.events.NotificationEvent;
import uk.co.streams.events.SaleEvent;

@Component
@AllArgsConstructor
public class Processor {

    public static final TypeReference<SaleEvent> SALE_EVENT_TYPE = new TypeReference<>() {
    };
    public static final JsonDeserializer<SaleEvent> SALE_EVENT_JSON_DESERIALIZER = new JsonDeserializer<>(SALE_EVENT_TYPE);
    public static final JsonSerializer<SaleEvent> SALE_EVENT_JSON_SERIALIZER = new JsonSerializer<>(SALE_EVENT_TYPE);

    @Autowired
    public void process(StreamsBuilder builder) {
        builder
                .stream("sales-events-v1", Consumed.with(Serdes.String(), Serdes.serdeFrom(SALE_EVENT_JSON_SERIALIZER, SALE_EVENT_JSON_DESERIALIZER)))
                .groupBy((key, value) -> key, Grouped.with(Serdes.String(), Serdes.serdeFrom(SALE_EVENT_JSON_SERIALIZER, SALE_EVENT_JSON_DESERIALIZER)))
                .aggregate(
                        () -> 0L,
                        this::aggregate,
                        Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("PRODUCT_AGGREGATED_SALES")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(Serdes.Long())
                                .withCachingDisabled()
                )
                .filter((product, salesValue) -> salesValue >= 2000)
                .mapValues(NotificationEvent::new)
                .toStream()
                .peek(((key, value) -> System.out.println("Notifying Product ->" + key + " --> Sale Value=:" + value)))
                .to("notifications-events-v1", Produced.with(
                        Serdes.String(),
                        Serdes.serdeFrom(new JsonSerializer<NotificationEvent>(), new JsonDeserializer<NotificationEvent>()))
                );
    }

    private Long aggregate(String key, SaleEvent saleEvent, Long aggregate) {
        return aggregate + saleEvent.getValue();
    }
}
