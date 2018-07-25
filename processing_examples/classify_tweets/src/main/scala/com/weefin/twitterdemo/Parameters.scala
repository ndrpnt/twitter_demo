package com.weefin.twitterdemo

import org.apache.flink.api.java.utils.ParameterTool

import scala.util.Try

object Parameters {
	val defaultBootstrapServers = "localhost:9092"
	
	private def throwInvalidArgs = throw new IllegalArgumentException(
		"""Invalid arguments. Usage: extract_hashtags
			| --consumer.bootstrap.servers <server1[,server2,...]>
			| --consumer.group.id <id>
			| --consumer.topic.id <id>
			| --producer.bootstrap.servers <server1[,server2,...]>
			| --producer.topic.id <id>
			| """.stripMargin)
	
	def apply(args: Array[String]): Parameters = new Parameters(args)
}

class Parameters(args: Array[String]) {
	private val params = ParameterTool.fromArgs(args)
	val producerBootstrapServers: String = params.get("producer.bootstrap.servers", Parameters.defaultBootstrapServers)
	val consumerBootstrapServers: String = params.get("consumer.bootstrap.servers", Parameters.defaultBootstrapServers)
	val consumerGroupId: String = Try(params.getRequired("consumer.group.id")).getOrElse(Parameters.throwInvalidArgs)
	val consumerTopicId: String = Try(params.getRequired("consumer.topic.id")).getOrElse(Parameters.throwInvalidArgs)
	val producerTopicId: String = Try(params.getRequired("producer.topic.id")).getOrElse(Parameters.throwInvalidArgs)
}