# Kafka Streams Stateful Aggregation Part 1 (Example and Q&A)
This guide is in relation to the contents covered in PART 1 of the series.

We have below articles in this series:
 - [Kafka Streams Stateful Aggregation Part 1 (Example and Q&A)](https://medium.com/@MalikMIlyas/kafka-streams-state-stores-part-1-stateful-aggregation-with-example-40bf5a9aafdf)
 - [Kafka Streams Stateful Aggregation Part 2 (How to retain/rebuild state on restarts)](https://medium.com/@MalikMIlyas/kafka-streams-stateful-aggregation-part-2-how-to-retain-rebuild-state-on-restarts-37ec3fedd996)
 - Kafka Streams - Stateful Aggregation - Part 3 (Production ready deployment)
 - Kafka Streams Stateful Aggregation Part 4 (Standby Replicas)

# Technologies Used
 - Java 17
 - Spring Boot 3.0.0
 - Spring Cloud 2020.0.2
 - EmbeddedKafka/Spock for component test

# How to test the example application.
We can see the application working in three ways

 - Execute component test available in code. 
 - Run application as docker container locally and manually publish sales events.

We will see both options here.

## Execute component test (EndToEndComponentSpec)
```shell
    ./gradlew clean test --tests *EndToEndComponentSpec
```
OR simply run using IDE. 

## Run application as docker container locally and manually publish sales events

### Build docker image

```shell
    ./gradlew clean build && docker build -t kafka-streams-statestores-example:latest .
```

### Step 2:: spin up kafka instance and service using docker-compose

```shell
    docker-compose up
```

### Step 3:: Verify Kafka topics
For this we can go inside the kafka container that we just started. 
First list the container by using the below command and then use that ID in next command to get in.

```shell

   docker exec -ti local_kafka_docker /bin/bash
   
   # Try listing topics
   /usr/bin/kafka-topics --list --bootstrap-server localhost:29092
   
   # It should show the output as below
   __consumer_offsets
   notifications-events-v1
   sales-events-v1
   spring-boot-streams-PRODUCT_AGGREGATED_SALES-changelog
   spring-boot-streams-PRODUCT_AGGREGATED_SALES-repartition

```

### Step 4:: Register a notification consumer to see notification events (as per example)

```shell
  docker exec -ti local_kafka_docker /bin/bash
    
  /usr/bin/kafka-console-consumer \
    --bootstrap-server localhost:29092 \
    --topic notifications-events-v1 \
    --from-beginning
```

### Step 5:: Publish messages to the sales-events-v1 topic to see aggregation
```shell

  docker exec -ti local_kafka_docker /bin/bash
  
  # init producer    
  /usr/bin/kafka-console-producer \
    --bootstrap-server localhost:29092 \
    --topic sales-events-v1 \
    --property "parse.key=true" \
     --property "key.separator=:" 
     
  # once connected simple paste JSON EVENT and enter   
  
  > product2:{ "product": "product2", "value": 1000 } 
  > product1:{ "product": "product1", "value": 1500 } 
  > product1:{ "product": "product1", "value": 1000 } 
  > product2:{ "product": "product2", "value": 1300 } 
  > product5:{ "product": "product5", "value": 1000 } 
  
  # Keep checking consumer window
```
