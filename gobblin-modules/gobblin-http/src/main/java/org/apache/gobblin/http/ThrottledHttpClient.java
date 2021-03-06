package org.apache.gobblin.http;

import java.io.IOException;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.codahale.metrics.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.gobblin.async.Callback;
import org.apache.gobblin.broker.gobblin_scopes.GobblinScopeTypes;
import org.apache.gobblin.broker.iface.NotConfiguredException;
import org.apache.gobblin.broker.iface.SharedResourcesBroker;
import org.apache.gobblin.metrics.MetricContext;
import org.apache.gobblin.metrics.broker.MetricContextFactory;
import org.apache.gobblin.metrics.broker.MetricContextKey;
import org.apache.gobblin.util.http.HttpLimiterKey;
import org.apache.gobblin.util.limiter.Limiter;
import org.apache.gobblin.util.limiter.broker.SharedLimiterFactory;


/**
 * A {@link HttpClient} for throttling calls to the underlying TX operation using the input
 * {@link Limiter}.
 */
@Slf4j
public abstract class ThrottledHttpClient<RQ, RP> implements HttpClient<RQ, RP>  {

  protected final Limiter limiter;
  protected final SharedResourcesBroker<GobblinScopeTypes> broker;

  @Getter
  private final Timer sendTimer;
  private final MetricContext metricContext;

  public ThrottledHttpClient (SharedResourcesBroker<GobblinScopeTypes> broker, String limiterKey) {
    this.broker = broker;
    try {
      this.limiter = broker.getSharedResource(new SharedLimiterFactory<>(), new HttpLimiterKey(limiterKey));
      this.metricContext = broker.getSharedResource(new MetricContextFactory<>(), new MetricContextKey());
      this.sendTimer = this.metricContext.timer(limiterKey);
    } catch (NotConfiguredException e) {
      log.error ("Limiter cannot be initialized due to exception " + ExceptionUtils.getFullStackTrace(e));
      throw new RuntimeException(e);
    }
  }

  public final RP sendRequest(RQ request) throws IOException {
    final Timer.Context context = sendTimer.time();
    try {
      if (limiter.acquirePermits(1) != null) {
        log.debug ("Acquired permits successfully");
        return sendRequestImpl (request);
      } else {
        throw new IOException ("Acquired permits return null");
      }
    } catch (InterruptedException e) {
      throw new IOException("Throttling is interrupted");
    } finally {
      context.stop();
    }
  }

  public final void sendAsyncRequest(RQ request, Callback<RP> callback) throws IOException {
    final Timer.Context context = sendTimer.time();
    try {
      if (limiter.acquirePermits(1) != null) {
        log.debug ("Acquired permits successfully");
        sendAsyncRequestImpl (request, callback);
      } else {
        throw new IOException ("Acquired permits return null");
      }
    } catch (InterruptedException e) {
      throw new IOException("Throttling is interrupted");
    } finally {
      context.stop();
    }
  }

  public abstract RP sendRequestImpl (RQ request) throws IOException;

  public abstract void sendAsyncRequestImpl (RQ request, Callback<RP> callback) throws IOException;
}
