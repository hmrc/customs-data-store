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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.http.Status
import uk.gov.hmrc.http._
import utils.SpecBase
import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MetricsReporterServiceSpec extends SpecBase {

  "withResponseTimeLogging" should {

    "log successful call metrics" in new Setup {
      await {
        metricsReporterService.withResponseTimeLogging("foo") {
          Future.successful("OK")
        }
      }

      verify(mockRegistry).histogram("responseTimes.foo.200")
      verify(mockHistogram).update(elapsedTimeInMillis)
    }

    "log default error during call metrics" in new Setup {
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

    "log not found call metrics" in new Setup {
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

    "log bad request error call metrics" in new Setup {
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

    "log 5xx error call metrics" in new Setup {
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

    "log 4xx error call metrics" in new Setup {
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

  trait Setup {
    val startTimestamp: OffsetDateTime = OffsetDateTime.parse("2018-11-09T17:15:30+01:00")
    val endTimestamp: OffsetDateTime   = OffsetDateTime.parse("2018-11-09T17:15:35+01:00")
    val elapsedTimeInMillis            = 5000L

    val mockDateTimeService: DateTimeService = mock[DateTimeService]
    val mockHistogram: Histogram             = mock[Histogram]
    val mockRegistry: MetricRegistry         = mock[MetricRegistry]

    when(mockDateTimeService.getTimeStamp)
      .thenReturn(startTimestamp)
      .thenReturn(endTimestamp)

    when(mockRegistry.histogram(any())).thenReturn(mockHistogram)

    val metricsReporterService = new MetricsReporterService(mockRegistry, mockDateTimeService)
  }

}
