package uk.co.streams

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.kafka.test.utils.KafkaTestUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.time.Duration

@SpringBootTest(classes = StreamsSpringBootApplication)
@EmbeddedKafka(
        partitions = 3,
        brokerProperties = ["listeners=PLAINTEXT://localhost:9092", "port=9092"]
)
class EndToEndComponentSpec extends Specification{

    @TempDir
    @Shared
    Path stateDir

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker

    KafkaMessageListenerContainer<String, String> container
    Queue<ConsumerRecord<String, String>> records
    Producer<String, String> producer

    def setupSpec() {
        System.out.println("Setting State Directory To="+stateDir.toAbsolutePath().toString())
        System.setProperty("spring.kafka.streams.properties..state.dir", stateDir.toAbsolutePath().toString())
    }

    def setup() {
        startTestConsumerForNotificationEvents()
        initProducerTemplate()
    }

    def "should generate notification event for sale beyond given threshold for a product"() {
        given:
        def events = testSaleEvents()

        when:
        events.stream().forEach(input -> {
            def inputJson = new JsonSlurper().parseText(input)
            producer.send(new ProducerRecord("sales-events-v1", inputJson["product"], input))
        });

        then:
        Awaitility
                .await()
                .timeout(Duration.ofSeconds(30))
                .pollDelay(Duration.ofMillis(100))
                .until(() -> {
                    return records.size() == 2
                })
        records.stream()
                .filter({record -> ["product1", "product2"].contains(record.key())})
                .map({record -> new JsonSlurper().parseText(record.value())})
                .filter({
                    json -> json["value"] >= 2000
                })
                .findAll().size() == 2
    }

    List<String> testSaleEvents() throws IOException {
        List<Object> list = new JsonSlurper().parse(new File(new ClassPathResource("input.json").getURI()))
        list.collect({input ->
            new JsonBuilder(input).toString()
        })
    }

    def initProducerTemplate() {
        // set up the Kafka producer properties
        Map<String, Object> senderProperties =
                KafkaTestUtils.producerProps(embeddedKafkaBroker.getBrokersAsString())
        senderProperties.put("key.serializer", StringSerializer)
        senderProperties.put("value.serializer", StringSerializer)

        // create a Kafka producer factory
        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(
                senderProperties)

        // create a Kafka producer
        producer = producerFactory.createProducer();
    }

    def startTestConsumerForNotificationEvents() {
        // set up the Kafka consumer properties
        Map<String, Object> consumerProperties =
                KafkaTestUtils.consumerProps("sender", "true",
                        embeddedKafkaBroker)
        consumerProperties.put("key.deserializer", StringDeserializer)
        consumerProperties.put("value.deserializer", StringDeserializer)

        // create a Kafka consumer factory
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<String, String>(
                consumerProperties)

        // set the topic that needs to be consumed
        ContainerProperties containerProperties = new ContainerProperties("notifications-events-v1")

        // create a Kafka MessageListenerContainer
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties)

        // create a thread safe queue to store the received message
        records = new LinkedList<>()
        // setup a Kafka message listener
        container.setupMessageListener(new MessageListener<String, String>() {
                    void onMessage(ConsumerRecord<String, String> record) {
                        System.out.println("Test Consumer Received Key="+record.key())
                        System.out.println("Test Consumer Received Value="+record.value())
                        records.add(record)
                    }
                })

        // start the container and underlying message listener
        container.start()

        // wait until the container has the required number of assigned partitions
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic())
    }

}
