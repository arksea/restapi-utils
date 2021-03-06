package net.arksea.restapi.utils.influx;

import akka.dispatch.OnComplete;
import net.arksea.httpclient.asker.FuturedHttpClient;
import net.arksea.httpclient.asker.HttpResult;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by xiaohaixing on 2020/03/23.
 */
public class BasicDataSourceLogger implements DataSource {

    private static final Logger logger = LogManager.getLogger(BasicDataSourceLogger.class);
    private final FuturedHttpClient futuredHttpClient;
    private static final int timeout = 10000;
    private final AtomicLong logCount = new AtomicLong(0L);
    private final AtomicLong logSucceedCount = new AtomicLong(0L);
    private volatile String error;

    AtomicLong requestCount = new AtomicLong(0);
    AtomicLong respondTime = new AtomicLong(0);  //请求响应时间
    AtomicLong succeed = new AtomicLong(0);
    AtomicLong failed = new AtomicLong(0);

    private final String dbUrl;
    private final BasicDataSource dataSource;
    private final String dataSourceName;

    public BasicDataSourceLogger(String dataSourceName, BasicDataSource dataSource, String dbUrl, FuturedHttpClient futuredHttpClient) {
        this.dataSourceName = dataSourceName;
        this.dataSource =dataSource;
        this.futuredHttpClient = futuredHttpClient;
        this.dbUrl = dbUrl;
    }

    public double getLogSuccessRate() {
        long c = logCount.get();
        long s = logSucceedCount.get();
        if (c > 0) {
            return s*1.0 / c;
        } else {
            return 0.0;
        }
    }
    public String getLastError() {
        return error;
    }
    public long getLogCount() {
        return logCount.get();
    }

    public void succeed(long time) {
        logger.trace("Trace DataSourceLogger.succeed: time={}",respondTime,new Exception("trace"));
        respondTime.addAndGet(time);
        succeed.incrementAndGet();
    }

    public void failed(long time) {
        logger.trace("Trace DataSourceLogger.failed: time={}",respondTime,new Exception("trace"));
        respondTime.addAndGet(time);
        failed.incrementAndGet();
    }

    public void request() {
        logger.trace("Trace DataSourceLogger.request",new Exception("trace"));
        requestCount.incrementAndGet();
    }

    public void writeLogs() {
        writeLogsGetConnect();
        writeLogsPool();
    }

    public void writeLogsGetConnect() {
        StringBuilder sb = new StringBuilder();
        long req = requestCount.getAndSet(0);
        long respond0 = succeed.getAndSet(0);
        long respond1 = failed.getAndSet(0);
        long respondCount = respond0 + respond1;
        long responedTime = respondCount == 0 ? 0 : respondTime.getAndSet(0) / respondCount;
        if (req>0 || respondCount>0) {
            sb.append("getConnect").append(",group=").append(dataSourceName)
                    .append(" request=").append(req)
                    .append(",succeed=").append(respond0)
                    .append(",failed=").append(respond1)
                    .append(",responedTime=").append(responedTime)
                    .append("\n");
            String body = sb.toString();
            httpRequest(body);
        }
    }

    public void writeLogsPool() {
        StringBuilder sb = new StringBuilder();
        sb.append("dataSource").append(",group=").append(dataSourceName)
                .append(" active=").append(dataSource.getNumActive())
                .append(",idle=").append(dataSource.getNumIdle())
                .append("\n");
        String body = sb.toString();
        httpRequest(body);
    }

    private void httpRequest(String body) {
        HttpPost post = new HttpPost(dbUrl);
        logger.debug(body);
        post.setEntity(new StringEntity(body, "UTF-8"));
        logCount.incrementAndGet();
        futuredHttpClient.ask(post, "request", timeout).onComplete(
            new OnComplete<HttpResult>() {
                public void onComplete(Throwable ex, HttpResult ret) throws Throwable {
                    if (ex == null) {
                        if (ret.error == null) {
                            int code = ret.response.getStatusLine().getStatusCode();
                            if (code == 204 || code == 200) {
                                logSucceedCount.incrementAndGet();
                            } else {
                                error = ret.value;
                            }
                        } else {
                            error = ret.error.getMessage();
                        }
                    }
                }
            },futuredHttpClient.system.dispatcher());
    }

    //-----------------------------------------------------
    @Override
    public Connection getConnection() throws SQLException {
        long start = System.currentTimeMillis();
        try {
            request();
            Connection conn = dataSource.getConnection();
            succeed(System.currentTimeMillis() - start);
            return conn;
        } catch (SQLException ex) {
            failed(System.currentTimeMillis() - start);
            throw ex;
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        long start = System.currentTimeMillis();
        try {
            request();
            Connection conn = dataSource.getConnection(username, password);
            succeed(System.currentTimeMillis() - start);
            return conn;
        } catch (SQLException ex) {
            failed(System.currentTimeMillis() - start);
            throw ex;
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return dataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return dataSource.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new UnsupportedOperationException("Not supported by BasicDataSource");
    }
}
