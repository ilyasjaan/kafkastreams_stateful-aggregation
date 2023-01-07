# Kafka Streams Stateful Aggregtion Part 1 (Example and Q&A)
This guide is in relation to the contents covered in PART 1 of the series.

We have below articles in this series:
 - **Kafka Streams Stateful Aggregation Part 1 (Example and Q&A)**
 - Kafka Streams Stateful Aggregation Part 2 (how to retain/rebuild state on restarts)
 - Kafka Streams Stateful Aggregation Part 3 (standby replicas)

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
    docker ps - a
```

```shell

   docker exec -it CONTAINER_ID /bin/bash
   
   # Try listing topics
   /usr/bin/kafka-topics --list --bootstrap-server localhost:29092
   
   # It should show the output as below
   __consumer_offsets
   notifications-events-v1
   sales-events-v1
   spring-boot-streams-KSTREAM-AGGREGATE-STATE-STORE-0000000002-changelog
   spring-boot-streams-KSTREAM-AGGREGATE-STATE-STORE-0000000002-repartition

```

### Step 4:: Register a notification consumer to see notification events (as per example)

```shell
  docker exec -it CONTAINER_ID /bin/bash
    
  /usr/bin/kafka-console-consumer \
    --bootstrap-server localhost:29092 \
    --topic sales-events-v1 \
    --from-beginning
```

### Step 5:: Publish messages to the sales-events-v1 topic to see aggregation
```shell

   docker exec -it CONTAINER_ID /bin/bash
  
  # init producer    
  /usr/bin/kafka-console-producer \
    --bootstrap-server localhost:29092 \
    --topic sales-events-v1 \
    --property "parse.key=true" \
     --property "key.separator=:" 
     
  # once connected simple paste JSON EVENT and enter   
  
  > { "product": "product2", "value": 1000 } 
  > { "product": "product1", "value": 1500 } 
  > { "product": "product1", "value": 1000 } 
  > { "product": "product2", "value": 1300 } 
  > { "product": "product5", "value": 1000 } 
  
  # Keep checking consumer window
```
