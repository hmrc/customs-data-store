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

import com.codahale.metrics.{Histogram, MetricRegistry}
import com.kenshoo.play.metrics.Metrics
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.http.Status
import uk.gov.hmrc.http._
import utils.SpecBase

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class MetricsReporterServiceSpec extends SpecBase {

  val mockDateTimeService = mock[DateTimeService]
  val startTimestamp = OffsetDateTime.parse("2018-11-09T17:15:30+01:00")
  val endTimestamp = OffsetDateTime.parse("2018-11-09T17:15:35+01:00")
  val elapsedTimeInMillis = 5000L // endTimestamp - startTimestamp

  when(mockDateTimeService.getTimeStamp)
    .thenReturn(startTimestamp)
    .thenReturn(endTimestamp)

  val mockHistogram = mock[Histogram]

  val mockRegistry = mock[MetricRegistry]
  when(mockRegistry.histogram(any())).thenReturn(mockHistogram)

  val mockMetrics = mock[Metrics]
  when(mockMetrics.defaultRegistry).thenReturn(mockRegistry)

  val metricsReporterService = new MetricsReporterService(mockMetrics, mockDateTimeService)

  "MetricsReporterService" should {

    "withResponseTimeLogging" should {

        "log successful call metrics" in {
          await {
            metricsReporterService.withResponseTimeLogging("foo") {
              Future.successful("OK")
            }
          }
          verify(mockRegistry).histogram("responseTimes.foo.200")
          verify(mockHistogram).update(elapsedTimeInMillis)
        }

        "log default error during call metrics" in {
          assertThrows[InternalServerException] {
            await {
              metricsReporterService.withResponseTimeLogging("bar") {
                Future.failed(new InternalServerException("boom"))
              }
            }
          }
          verify(mockRegistry).histogram("responseTimes.bar.500")
          verify(mockHistogram).update(elapsedTimeInMillis)
        }

        "log not found call metrics" in {
          assertThrows[NotFoundException] {
            await {
              metricsReporterService.withResponseTimeLogging("bar") {
                Future.failed(new NotFoundException("boom"))
              }
            }
          }
          verify(mockRegistry).histogram("responseTimes.bar.404")
          verify(mockHistogram).update(elapsedTimeInMillis)
        }

        "log bad request error call metrics" in {
          assertThrows[BadRequestException] {
            await {
              metricsReporterService.withResponseTimeLogging("bar") {
                Future.failed(new BadRequestException("boom"))
              }
            }
          }
          verify(mockRegistry).histogram("responseTimes.bar.400")
          verify(mockHistogram).update(elapsedTimeInMillis)
        }

        "log 5xx error call metrics" in {
          assertThrows[UpstreamErrorResponse] {
            await {
              metricsReporterService.withResponseTimeLogging("bar") {
                Future.failed(UpstreamErrorResponse("boom", Status.SERVICE_UNAVAILABLE, Status.NOT_IMPLEMENTED))
              }
            }
          }
          verify(mockRegistry).histogram("responseTimes.bar.503")
          verify(mockHistogram).update(elapsedTimeInMillis)
        }

        "log 4xx error call metrics" in {
          assertThrows[UpstreamErrorResponse] {
            await {
              metricsReporterService.withResponseTimeLogging("bar") {
                Future.failed(UpstreamErrorResponse("boom", Status.FORBIDDEN, Status.NOT_IMPLEMENTED))
              }
            }
          }
          verify(mockRegistry).histogram("responseTimes.bar.403")
          verify(mockHistogram).update(elapsedTimeInMillis)
        }

    }

  }
}
