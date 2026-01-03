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

# MySQL Fuzzy Search Examples

If you want to implement fuzzy search functionality on product data stored in MySQL, here are some useful SQL commands and techniques:

## 1. Basic LIKE Pattern Matching

Search for products with names containing a specific substring:

```sql
-- Find products with 'product' in the name (case-insensitive)
SELECT * FROM products 
WHERE product_name LIKE '%product%';

-- Find products starting with 'prod'
SELECT * FROM products 
WHERE product_name LIKE 'prod%';

-- Find products ending with '1'
SELECT * FROM products 
WHERE product_name LIKE '%1';
```

## 2. Multiple Pattern Matching with OR

Search for products matching multiple patterns:

```sql
-- Find products matching any of the patterns
SELECT * FROM products 
WHERE product_name LIKE '%product1%' 
   OR product_name LIKE '%product2%' 
   OR product_name LIKE '%product5%';
```

## 3. FULLTEXT Search (Recommended for Large Datasets)

For better performance with large datasets, use FULLTEXT indexes:

```sql
-- Create a FULLTEXT index on the product_name column
ALTER TABLE products 
ADD FULLTEXT INDEX ft_product_name (product_name);

-- Natural language search
SELECT *, MATCH(product_name) AGAINST('product1') AS relevance
FROM products 
WHERE MATCH(product_name) AGAINST('product1' IN NATURAL LANGUAGE MODE)
ORDER BY relevance DESC;

-- Boolean mode search (supports wildcards and operators)
SELECT * FROM products 
WHERE MATCH(product_name) AGAINST('+product* -test' IN BOOLEAN MODE);
```

## 4. Phonetic Matching with SOUNDEX

Find products with similar-sounding names:

```sql
-- Find products that sound like 'product'
SELECT * FROM products 
WHERE SOUNDEX(product_name) = SOUNDEX('product');
```

## 5. Levenshtein Distance (Requires Custom Function)

For advanced fuzzy matching based on edit distance, you can create a stored function:

```sql
-- Example query using Levenshtein distance (function must be defined first)
SELECT product_name, LEVENSHTEIN(product_name, 'product1') AS distance
FROM products
WHERE LEVENSHTEIN(product_name, 'product1') <= 3
ORDER BY distance;
```

## 6. Regular Expression Search (MySQL 8.0+)

Use REGEXP for pattern-based searches:

```sql
-- Find products matching a regular expression pattern
SELECT * FROM products 
WHERE product_name REGEXP 'product[0-9]+';

-- Case-insensitive regex search
SELECT * FROM products 
WHERE product_name REGEXP BINARY 'product[1-5]';
```

## 7. Weighted Relevance Search

Combine multiple criteria for better relevance:

```sql
SELECT 
    product_name,
    (CASE 
        WHEN product_name = 'product1' THEN 100
        WHEN product_name LIKE 'product1%' THEN 80
        WHEN product_name LIKE '%product1' THEN 60
        WHEN product_name LIKE '%product1%' THEN 40
        ELSE 0
    END) AS relevance_score
FROM products
WHERE product_name LIKE '%product1%'
ORDER BY relevance_score DESC;
```

## Best Practices

- **Use FULLTEXT indexes** for large datasets with text search requirements
- **Add indexes** on columns frequently used in WHERE clauses
- **Use EXPLAIN** to analyze query performance
- **Consider caching** frequently searched results
- **Validate input** to prevent SQL injection attacks when using dynamic queries
