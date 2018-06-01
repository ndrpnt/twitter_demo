# Flink/Kafka demo
## Installation
Require Zookeeper, Kafka & Flink
```bash
mkdir platform
cd platform
wget http://mirrors.standaloneinstaller.com/apache/zookeeper/zookeeper-3.4.12/zookeeper-3.4.12.tar.gz
wget http://mirror.ibcp.fr/pub/apache/kafka/1.1.0/kafka_2.11-1.1.0.tgz
wget http://apache.mediamirrors.org/flink/flink-1.5.0/flink-1.5.0-bin-scala_2.11.tgz
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.2.4.tar.gz
wget https://artifacts.elastic.co/downloads/kibana/kibana-6.2.4-linux-x86_64.tar.gz # Linux
wget https://artifacts.elastic.co/downloads/kibana/kibana-6.2.4-darwin-x86_64.tar.gz # Mac
tar -xzf zookeeper-3.4.12.tar.gz
tar -xzf kafka_2.11-1.1.0.tgz
tar -xzf flink-1.5.0-bin-scala_2.11.tgz
tar -xzf elasticsearch-6.2.4.tar.gz
tar -xzf kibana-6.2.4-linux-x86_64.tar.gz # Linux
tar -xzf kibana-6.2.4-darwin-x86_64.tar.gz # Mac
```
## Configuration
### Zookeeper
```bash
mv zookeeper-3.4.12/conf/zoo_sample.cfg zookeeper-3.4.12/conf/zoo.cfg
mkdir zookeeper-3.4.12/data
```
In `zookeeper-3.4.12/conf/zoo.cfg` set `dataDir` to `/<zookeeper_home_directory>/data`
### Kafka
In `kafka_2.11-1.1.0/config/server.properties` set:
- `listeners` to `PLAINTEXT://:9092` (commented by default)
- `log.dirs` to `/<kafka_home_directory>/kafka-logs`

Create a new topic named `streaming.twitter.statuses`
```bash
./platform/kafka_2.11-1.1.0/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 
--partitions 1 --topic "streaming.twitter.statuses"
```
## Start
```bash
./zookeeper-3.4.12/bin/zkServer.sh start zoo.cfg
./kafka_2.11-1.1.0/bin/kafka-server-start.sh kafka_2.11-1.1.0/config/server.properties
./flink-1.5.0/bin/start-cluster.sh
./elasticsearch-6.2.4/bin/elasticsearch
./kibana-6.2.4-darwin-x86_64/bin/kibana
```
## Stop
```bash
./flink-1.5.0/bin/stop-cluster.sh
./kafka_2.11-1.1.0/bin/kafka-server-stop.sh
./zookeeper-3.4.12/bin/zkServer.sh stop
```