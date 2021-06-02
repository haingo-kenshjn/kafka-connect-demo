# kafka-connect-demo

Edit docker-compose.yml zookeeper volumn to local machine
```
    volumes:
      - /Users/hngo/Documents/dev/volumes/zookeeper/log/version-2/:/var/lib/zookeeper/log/version-2
      - /Users/hngo/Documents/dev/volumes/zookeeper/data/version-2/:/var/lib/zookeeper/data/version-2
```

Start zookeeper first

```
docker-compose up zookeeper
```

then start whole docker

```
docker-compose up
```

Kakfa connect UI: http://localhost:8001/
DB admin UI: http://localhost:8080/

Create source connector for csv file
```
{
  "connector.class": "com.github.jcustenborder.kafka.connect.spooldir.SpoolDirCsvSourceConnector",
  "csv.first.row.as.header": "true",
  "input.file.pattern": ".*\\.csv",
  "finished.path": "/tmp/kafka-connect/data/processed",
  "tasks.max": "1",
  "topic": "csv_spooldir_03",
  "halt.on.error": "false",
  "error.path": "/tmp/kafka-connect/data/error",
  "input.path": "/tmp/kafka-connect/data/unprocessed",
  "schema.generation.key.fields": "id",
  "schema.generation.enabled": "true"
}
```
Create sink connector for posgressql

```
{
  "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",
  "connection.password": "sample",
  "topics": "csv_spooldir_03",
  "tasks.max": "1",
  "transforms.insertuuid.uuid.field.name": "uuid",
  "transforms": "insertuuid",
  "key.converter.schemas.enable": "true",
  "delete.enabled": "true",
  "transforms.InsertField.type": "org.apache.kafka.connect.transforms.InsertField$Value",
  "auto.evolve": "true",
  "connection.user": "sample",
  "value.converter.schemas.enable": "true",
  "auto.create": "true",
  "connection.url": "jdbc:postgresql://db:5432/sample",
  "insert.mode": "upsert",
  "pk.mode": "record_key",
  "pk.fields": "id",
  "transforms.insertuuid.type": "com.github.cjmatta.kafka.connect.smt.InsertUuid$Value"
}
```
