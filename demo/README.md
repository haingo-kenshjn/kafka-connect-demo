
Start local posgresql if dont have any connection

```
docker-compose -f ./docker-composs-db.yml up
```

Edit kafka broker to company broker, and point to local keyTab file.

Example:

```
#spring.cloud.stream.kafka.binder.brokers=localhost:9092
#spring.cloud.stream.kafka.binder.autoCreateTopics=true


#https://cloud.spring.io/spring-cloud-stream-binder-kafka/spring-cloud-stream-binder-kafka.html#_example_security_configuration
spring.cloud.stream.kafka.binder.brokers=localhost:9092
spring.cloud.stream.kafka.binder.autoCreateTopics=false
spring.cloud.stream.kafka.binder.configuration.security.protocol=SASL_PLAINTEXT
spring.cloud.stream.kafka.binder.jaas.options.useKeyTab=true
spring.cloud.stream.kafka.binder.jaas.options.storeKey=true
spring.cloud.stream.kafka.binder.jaas.options.keyTab=/etc/security/keytabs/kafka_client.keytab

Uncommnent and edit if kafka have principal user
#spring.cloud.stream.kafka.binder.jaas.options.principal=kafka-client-1@EXAMPLE.COM
```


Build project
```
mvn clean install
```

Run project

```
mvn spring-boot:run
```
