/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import com.codahale.metrics.MetricRegistry
import com.google.inject.Inject
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, UpstreamErrorResponse}

import java.time.OffsetDateTime
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class MetricsReporterService @Inject() (metrics: MetricRegistry, dateTimeService: DateTimeService) {

  def withResponseTimeLogging[T](resourceName: String)(future: Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val startTime = dateTimeService.getTimeStamp

    future.andThen { case response =>
      val httpResponseCode = response match {
        case Success(_)                                => OK
        case Failure(exception: NotFoundException)     => exception.responseCode
        case Failure(exception: BadRequestException)   => exception.responseCode
        case Failure(exception: UpstreamErrorResponse) => exception.statusCode
        case Failure(_)                                => INTERNAL_SERVER_ERROR
      }
      updateResponseTimeHistogram(resourceName, httpResponseCode, startTime, dateTimeService.getTimeStamp)
    }
  }

  private def updateResponseTimeHistogram(
    resourceName: String,
    httpResponseCode: Int,
    startTimestamp: OffsetDateTime,
    endTimestamp: OffsetDateTime
  ): Unit = {
    val RESPONSE_TIMES_METRIC = "responseTimes"
    val histogramName         = s"$RESPONSE_TIMES_METRIC.$resourceName.$httpResponseCode"
    val elapsedTimeInMillis   = endTimestamp.toInstant.toEpochMilli - startTimestamp.toInstant.toEpochMilli

    metrics.histogram(histogramName).update(elapsedTimeInMillis)
  }
}
