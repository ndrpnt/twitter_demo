package com.weefin.twitterdemo

import java.util.concurrent.TimeUnit

import com.danielasfregola.twitter4s.entities.Tweet
import com.typesafe.scalalogging.LazyLogging
import com.weefin.twitterdemo.utils.twitter.entities.{
  Classification,
  SimpleStatus,
  SimpleUser
}
import com.weefin.twitterdemo.utils.twitter.sink.KafkaJsonProducer
import com.weefin.twitterdemo.utils.twitter.source.{
  AsyncTwitterRequest,
  KafkaJsonConsumer
}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.async.ResultFuture

import scala.util.{Failure, Success}

object ClassifyUsers extends App with LazyLogging {
  private val jobName = this.getClass.getSimpleName.split("\\$").last
  private val params = Parameters(args)
  private val env = StreamExecutionEnvironment.getExecutionEnvironment
  logger.info(s"$jobName job started")

  private val users: DataStream[SimpleUser] =
    env.addSource(consumer).flatMap(identity(_))

  private val timelines: DataStream[(SimpleUser, Seq[Tweet])] = AsyncDataStream
    .unorderedWait(users, asyncTimelineRequest, 5, TimeUnit.SECONDS, 100)
    .filter(_._2.nonEmpty)

  private val simpleTimelines: DataStream[(SimpleUser, Seq[SimpleStatus])] =
    timelines.map(t => (t._1, t._2.map(SimpleStatus(_))))

  private val classifiedUsers1
    : DataStream[(SimpleUser, Seq[Map[Classification.Label.Value, Float]])] =
    simpleTimelines.map { t =>
      (
        t._1,
        t._2
          .map(s => Classification.classify(s.hashtags: _*))
          .filterNot(_.isEmpty)
      )
    }

  private val classifiedUsers2
    : DataStream[(SimpleUser, Map[String, Float], Float)] =
    classifiedUsers1.map { t =>
      (
        t._1,
        Classification.stringify(Classification.merge(t._2: _*)),
        Math.min(1F, t._2.length / 25F)
      )
    }

  classifiedUsers2
    .map(ClassifiedSimpleUser.tupled(_))
    .addSink(producer)
  env.execute(jobName)

  private case class ClassifiedSimpleUser(user: SimpleUser,
                                          classification: Map[String, Float],
                                          confidence: Float)

  private def asyncTimelineRequest =
    new AsyncTwitterRequest[SimpleUser, (SimpleUser, Seq[Tweet])](
      params.consumerKey,
      params.consumerSecret,
      params.token,
      params.tokenSecret
    ) {
      override def timeout(
        user: SimpleUser,
        resultFuture: ResultFuture[(SimpleUser, Seq[Tweet])]
      ): Unit = {
        logger.warn(s"Get timeline for user id ${user.id}: query timed out")
        resultFuture.complete(Iterable.empty)
      }

      override def asyncInvoke(
        user: SimpleUser,
        resultFuture: ResultFuture[(SimpleUser, Seq[Tweet])]
      ): Unit =
        client
          .userTimelineForUserId(user.id)
          .map(_.data)
          .onComplete {
            case Success(tweets) =>
              logger.info(
                s"Get timeline for user id ${user.id}: received the ${tweets.length} most recent Tweets"
              )
              resultFuture.complete(Iterable((user, tweets)))
            case Failure(throwable) =>
              logger.warn(
                s"Get timeline for user id ${user.id}: received error '${throwable.getMessage}'"
              )
              resultFuture.complete(Iterable.empty)
          }
    }

  private def consumer =
    KafkaJsonConsumer[SimpleUser](
      params.consumerBootstrapServers,
      params.consumerTopicId,
      params.consumerGroupId
    )

  private def producer =
    KafkaJsonProducer[ClassifiedSimpleUser](
      params.producerBootstrapServers,
      params.producerTopicId
    )
}
