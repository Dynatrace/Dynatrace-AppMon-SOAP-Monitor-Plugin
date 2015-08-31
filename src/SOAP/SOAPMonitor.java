package SOAP;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.dynatrace.diagnostics.pdk.*;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.util.EncodingUtil;

import com.dynatrace.diagnostics.global.Constants;

import javax.net.ssl.SSLHandshakeException;

public class SOAPMonitor implements Monitor, Migrator {

    private static final int READ_CHUNK_SIZE = 1024;
    private static final double MILLIS = 0.000001;
    private static final double SECS = 0.000000001;
    // configuration constants
    private static final String CONFIG_PROTOCOL = "protocol";
    private static final String CONFIG_PATH = "path";
    private static final String CONFIG_HTTP_PORT = "httpPort";
    private static final String CONFIG_HTTPS_PORT = "httpsPort";
    private static final String CONFIG_METHOD = "method";
    private static final String CONFIG_CONTENT_TYPE = "contentType";
    private static final String CONFIG_CHARACTER_SET = "characterSet";
    private static final String CONFIG_POST_DATA = "postData";
    private static final String CONFIG_USER_AGENT = "userAgent";
    private static final String CONFIG_HTTP_VERSION = "httpVersion";
    private static final String CONFIG_MAX_REDIRECTS = "maxRedirects";
    private static final String CONFIG_DT_TAGGING = "dtTagging";
    private static final String CONFIG_DT_TIMER_NAME = "dtTimerName";
    private static final String CONFIG_MATCH_CONTENT = "matchContent";
    private static final String CONFIG_SEARCH_STRING = "searchString";
    private static final String CONFIG_COMPARE_BYTES = "compareBytes";
    private static final String CONFIG_SERVER_AUTH = "serverAuth";
    private static final String CONFIG_SERVER_USERNAME = "serverUsername";
    private static final String CONFIG_SERVER_PASSWORD = "serverPassword";
    private static final String CONFIG_USE_PROXY = "useProxy";
    private static final String CONFIG_PROXY_HOST = "proxyHost";
    private static final String CONFIG_PROXY_PORT = "proxyPort";
    private static final String CONFIG_PROXY_AUTH = "proxyAuth";
    private static final String CONFIG_PROXY_USERNAME = "proxyUsername";
    private static final String CONFIG_PROXY_PASSWORD = "proxyPassword";
    private static final String CONFIG_IGNORE_CERTIFICATE = "ignoreCertificate";
    // measure constants
    private static final String METRIC_GROUP = "SOAP Monitor";
    private static final String MSR_HOST_REACHABLE = "HostReachable";
    private static final String MSR_HEADER_SIZE = "HeaderSize";
    private static final String MSR_FIRST_RESPONSE_DELAY = "FirstResponseDelay";
    private static final String MSR_RESPONSE_COMPLETE_TIME = "ResponseCompleteTime";
    private static final String MSR_RESPONSE_SIZE = "ResponseSize";
    private static final String MSR_THROUGHPUT = "Throughput";
    private static final String MSR_HTTP_STATUS_CODE = "HttpStatusCode";
    private static final String MSR_CONN_CLOSE_DELAY = "ConnectionCloseDelay";
    private static final String MSR_CONTENT_VERIFIED = "ContentVerified";
    private static final Logger log = Logger.getLogger(SOAPMonitor.class.getName());
    private static final String PROTOCOL_HTTPS_IGNORECERT = "https+ignorecert";
    private static final String PROTOCOL_HTTPS = "https";
    private static final String PROTOCOL_HTTP = "http";

    private enum MatchContent {

        disabled, successIfMatch, errorIfMatch, bytesMatch
    }

    private static class Config {

        URL url;
        String method;
        String postData;
        String contentType;
        String characterSet;
        String httpVersion;
        String userAgent;
        int maxRedirects;
        boolean tagging;
        boolean ignorecert;
        String timerName;
        // verification
        MatchContent matchContent;
        String searchString;
//		int minOccurrences;
//		int maxOccurrences;
        // server authentification
        boolean serverAuth;
        String serverUsername;
        String serverPassword;
        // proxy
        boolean useProxy;
        String proxyHost;
        int proxyPort;
        boolean proxyAuth;
        String proxyUsername;
        String proxyPassword;
        long compareBytes;
    }
    private Config config;
    private HttpClient httpClient;

    @Override
    public Status setup(MonitorEnvironment env) throws Exception {
        Status status = new Status(Status.StatusCode.Success);
        httpClient = new HttpClient(new SimpleHttpConnectionManager());

        try {
            Protocol.getProtocol(PROTOCOL_HTTPS_IGNORECERT);
        } catch (IllegalStateException e) {
            Protocol protocol = new Protocol(PROTOCOL_HTTPS_IGNORECERT, new EasySSLProtocolSocketFactory(), 443);
            Protocol.registerProtocol(PROTOCOL_HTTPS_IGNORECERT, protocol);
        }
        config = readConfig(env);
        return status;
    }

    @Override
    public void teardown(MonitorEnvironment env) throws Exception {
        HttpConnectionManager httpConnectionManager = httpClient.getHttpConnectionManager();
        if (httpConnectionManager instanceof SimpleHttpConnectionManager) {
            ((SimpleHttpConnectionManager) httpConnectionManager).shutdown();
        }
    }

    @Override
    public Status execute(MonitorEnvironment env) throws Exception {
        Status status = new Status();

        //in case plug-in returns PartialSuccess the hostReachable measure will always return 0
        Collection<MonitorMeasure> hostReachableMeasures = env.getMonitorMeasures(METRIC_GROUP, MSR_HOST_REACHABLE);
        if (status.getStatusCode().getBaseCode() == Status.StatusCode.Success.getBaseCode() && hostReachableMeasures != null) {
            for (MonitorMeasure measure : hostReachableMeasures) {
                measure.setValue(0);
            }
        }

        // measurement variables
        int httpStatusCode = 0;
        int headerSize = 0;
        long firstResponseTime = 0;
        long responseCompleteTime = 0;
        int inputSize = 0;
        long connectionCloseDelay = 0;
        boolean verified = false;
        long time;

        if (config.url == null || (!config.url.getProtocol().equals("http") && !config.url.getProtocol().equals("https"))) {
            status.setShortMessage("only protocols http and https are allowed.");
            status.setMessage("only protocols http and https are allowed.");
            status.setStatusCode(Status.StatusCode.PartialSuccess);
            return status;
        }

        // create a HTTP client and method
        HttpMethodBase httpMethod = createHttpMethod(config);
        if (httpMethod == null) {
            status.setMessage("Unknown HTTP method: " + config.method);
            status.setStatusCode(Status.StatusCode.ErrorInternal);
            return status;
        }

        // try to set parameters
        try {
            setHttpParameters(httpMethod, config);
        } catch (Exception ex) {
            status.setStatusCode(Status.StatusCode.ErrorInternal);
            status.setMessage("Setting HTTP client parameters failed");
            status.setShortMessage(ex == null ? "" : ex.getClass().getSimpleName());
            status.setMessage(ex == null ? "" : ex.getMessage());
            status.setException(ex);
            return status;
        }

        StringBuilder messageBuffer = new StringBuilder("URL: ");
        messageBuffer.append(config.url).append("\r\n");

        try {
            if (log.isLoggable(Level.INFO)) {
                log.info("Executing method: " + config.method + ", URI: " + httpMethod.getURI());
            }

            // connect
            time = System.nanoTime();
            httpStatusCode = httpClient.executeMethod(httpMethod);
            firstResponseTime = System.nanoTime() - time;

            // calculate header size
            headerSize = calculateHeaderSize(httpMethod.getResponseHeaders());
            if (log.isLoggable(Level.FINE)) {
                try {
                    log.log(Level.FINE, "Response is: {0}", httpMethod.getResponseBodyAsString());
                } catch (Exception e) {
                    log.info("Exception thrown on Response Body fine logging: " + e);
                }
            }
            if (log.isLoggable(Level.INFO)) {
                log.info("HTTP Status Code is: " + httpMethod.getStatusCode());
            }

            // read response data
            InputStream inputStream = httpMethod.getResponseBodyAsStream();
            if (inputStream != null) {
                int bytesRead;
                byte[] data = new byte[READ_CHUNK_SIZE];
                String charset = httpMethod.getResponseCharSet();
                StringBuilder buf = new StringBuilder();
                while ((bytesRead = inputStream.read(data)) > 0) {
                    if (config.matchContent != MatchContent.disabled && config.matchContent != MatchContent.bytesMatch) {
                        //FIXME may cause excessive memory usage
                        buf.append(EncodingUtil.getString(data, 0, bytesRead, charset));
                    }
                    inputSize += bytesRead;
                }
                responseCompleteTime = System.nanoTime() - time;

                if (config.matchContent == MatchContent.bytesMatch) {
                    verified = (inputSize == config.compareBytes);
                    if (!verified) {
                        messageBuffer.append("Expected ").append(config.compareBytes).append(" bytes, but was ").append(inputSize).append(" bytes");
                    }
                } else if (config.matchContent != MatchContent.disabled) {
                    try {
                        boolean found = buf.toString().contains(config.searchString);
                        if (config.matchContent == MatchContent.successIfMatch) {
                            verified = found;
                            if (!verified) {
                                messageBuffer.append("Expected string \"").append(config.searchString).append("\" didn't match.");
                            }
                        } else { // error if match
                            verified = !found;
                            if (!verified) {
                                messageBuffer.append("Expected string \"").append(config.searchString).append("\" matched.");
                            }
                        }
                    } catch (Exception ex) {
                        messageBuffer.append("Verifying the response content failed");
                        status.setException(ex);
                        if (log.isLoggable(Level.SEVERE)) {
                            log.severe(status.getMessage() + ": " + ex);
                        }
                        status.setStatusCode(Status.StatusCode.PartialSuccess);
                        status.setMessage(messageBuffer.toString());
                        return status;
                    }
                }
                connectionCloseDelay = System.nanoTime();
            } // end read response
        } catch (HttpException httpe) {
            status.setException(httpe);
            status.setStatusCode(Status.StatusCode.PartialSuccess);
            status.setShortMessage(httpe == null ? "" : httpe.getClass().getSimpleName());
            messageBuffer.append(httpe == null ? "" : httpe.getMessage());
            if (log.isLoggable(Level.SEVERE)) {
                log.severe("Requesting URL " + httpMethod.getURI() + " caused exception: " + httpe);
            }
        } catch (ConnectException ce) {
            status.setException(ce);
            status.setStatusCode(Status.StatusCode.PartialSuccess);
            status.setShortMessage(ce == null ? "" : ce.getClass().getSimpleName());
            messageBuffer.append(ce == null ? "" : ce.getMessage());
        } catch (SSLHandshakeException e) {
            status.setException(e);
            status.setStatusCode(Status.StatusCode.PartialSuccess);
            status.setShortMessage(e == null ? "" : e.getClass().getSimpleName());
            messageBuffer.append("SSL handshake failed, this may be caused by an incorrect certificate. Check 'Disable certificate validation' to override this.\n");
            messageBuffer.append(e == null ? "" : e.getMessage());
        } catch (IOException ioe) {
            status.setException(ioe);
            status.setStatusCode(Status.StatusCode.PartialSuccess);
            status.setShortMessage(ioe == null ? "" : ioe.getClass().getSimpleName());
            messageBuffer.append(ioe == null ? "" : ioe.getMessage());
            if (log.isLoggable(Level.SEVERE)) {
                log.severe("Requesting URL " + httpMethod.getURI() + " caused exception: " + ioe);
            }
        } catch (IllegalArgumentException iae) {
            status.setException(iae);
            status.setStatusCode(Status.StatusCode.ErrorInternal);
            status.setShortMessage(iae == null ? "" : iae.getClass().getSimpleName());
            messageBuffer.append(iae == null ? "" : iae.getMessage());
            if (log.isLoggable(Level.SEVERE)) {
                log.severe("Requesting URL " + httpMethod.getURI() + " caused exception: " + iae);
            }
        } finally {
            // always release the connection
            httpMethod.releaseConnection();
            if (connectionCloseDelay > 0) {
                connectionCloseDelay = System.nanoTime() - connectionCloseDelay;
            }
        }

        // calculate and set the measurements
        Collection<MonitorMeasure> measures = null;
        measures = env.getMonitorMeasures(METRIC_GROUP, MSR_HOST_REACHABLE);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Measure collection size MSR_HOST_REACHABLE is " + measures.size());
        }
        if (status.getStatusCode().getBaseCode() == Status.StatusCode.Success.getBaseCode() && (measures = env.getMonitorMeasures(METRIC_GROUP, MSR_HOST_REACHABLE)) != null) {
            for (MonitorMeasure measure : measures) {
                measure.setValue(httpStatusCode > 0 ? 1 : 0);
            }
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine("Measure collection size after httpstatuscode is " + measures.size());
        }
        if (status.getStatusCode().getCode() == Status.StatusCode.Success.getCode()) {
            if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_HEADER_SIZE)) != null) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Measure collection size after httpstatuscode is " + measures.size());
                }

                for (MonitorMeasure measure : measures) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("Header Size is " + headerSize);
                    }
                    measure.setValue(headerSize);
                }

            }
            if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_FIRST_RESPONSE_DELAY)) != null) {
                double firstResponseTimeMillis = firstResponseTime * MILLIS;
                for (MonitorMeasure measure : measures) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("firstResponseTimeMillis is " + firstResponseTimeMillis);
                    }
                    measure.setValue(firstResponseTimeMillis);
                }

            }
            if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_RESPONSE_COMPLETE_TIME)) != null) {
                double responseCompleteTimeMillis = responseCompleteTime * MILLIS;
                for (MonitorMeasure measure : measures) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("responseCompleteTimeMillis is " + responseCompleteTimeMillis);
                    }
                    measure.setValue(responseCompleteTimeMillis);
                }

            }
            if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_RESPONSE_SIZE)) != null) {
                for (MonitorMeasure measure : measures) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("inputSize is " + inputSize);
                    }
                    measure.setValue(inputSize);
                }

            }
            if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_THROUGHPUT)) != null) {
                double throughput = 0;
                if (responseCompleteTime > 0) {
                    double responseCompleteTimeSecs = responseCompleteTime * SECS;
                    double contentSizeKibiByte = inputSize / 1024.0;
                    throughput = contentSizeKibiByte / responseCompleteTimeSecs;
                }
                for (MonitorMeasure measure : measures) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("throughput is " + throughput);
                    }
                    measure.setValue(throughput);
                }

            }
            if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_HTTP_STATUS_CODE)) != null) {
                for (MonitorMeasure measure : measures) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("httpStatusCode is " + httpStatusCode);
                    }
                    measure.setValue(httpStatusCode);
                }

            }
            if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_CONN_CLOSE_DELAY)) != null) {
                double connectionCloseDelayMillis = connectionCloseDelay * MILLIS;
                for (MonitorMeasure measure : measures) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("connectionCloseDelayMillis is " + connectionCloseDelayMillis);
                    }
                    measure.setValue(connectionCloseDelayMillis);
                }

            }
            if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_CONTENT_VERIFIED)) != null) {
                for (MonitorMeasure measure : measures) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("verified is " + verified);
                    }
                    measure.setValue(verified ? 1 : 0);
                }

            }
        }

        status.setMessage(messageBuffer.toString());
        return status;
    }

    private Config readConfig(MonitorEnvironment env) throws MalformedURLException {
        Config config = new Config();

        String protocol = env.getConfigString(CONFIG_PROTOCOL);
        int port;
        if (protocol != null && protocol.contains("https")) {
            port = env.getConfigLong(CONFIG_HTTPS_PORT).intValue();
            protocol = PROTOCOL_HTTPS;
        } else {
            port = env.getConfigLong(CONFIG_HTTP_PORT).intValue();
            protocol = PROTOCOL_HTTP;
        }
        String path = fixPath(env.getConfigString(CONFIG_PATH));
        config.ignorecert = env.getConfigBoolean(CONFIG_IGNORE_CERTIFICATE);
        config.url = new URL(protocol, env.getHost().getAddress(), port, path);
        //config.method = env.getConfigString(CONFIG_METHOD) == null ? "GET" : env.getConfigString(CONFIG_METHOD).toUpperCase();
        config.method = "POST";
        config.postData = env.getConfigString(CONFIG_POST_DATA);
        config.contentType = env.getConfigString(CONFIG_CONTENT_TYPE);
        config.characterSet = env.getConfigString(CONFIG_CHARACTER_SET);
        config.httpVersion = env.getConfigString(CONFIG_HTTP_VERSION);
        config.userAgent = env.getConfigString(CONFIG_USER_AGENT);
        config.tagging = env.getConfigBoolean(CONFIG_DT_TAGGING) == null ? false : env.getConfigBoolean(CONFIG_DT_TAGGING);
        if (config.tagging) {
            config.timerName = env.getConfigString(CONFIG_DT_TIMER_NAME) == null ? "" : env.getConfigString(CONFIG_DT_TIMER_NAME);
        }
        config.maxRedirects = env.getConfigLong(CONFIG_MAX_REDIRECTS) == null ? 0 : env.getConfigLong(CONFIG_MAX_REDIRECTS).intValue();

        String matchContent = env.getConfigString(CONFIG_MATCH_CONTENT);
        if ("Success if match".equals(matchContent)) {
            config.matchContent = MatchContent.successIfMatch;
        } else if ("Error if match".equals(matchContent)) {
            config.matchContent = MatchContent.errorIfMatch;
        } else if ("Expected size in bytes".equals(matchContent)) {
            config.matchContent = MatchContent.bytesMatch;
        } else {
            config.matchContent = MatchContent.disabled;
        }
        config.searchString = env.getConfigString(CONFIG_SEARCH_STRING) == null ? "" : env.getConfigString(CONFIG_SEARCH_STRING);
        config.compareBytes = env.getConfigLong(CONFIG_COMPARE_BYTES) == null ? 0 : env.getConfigLong(CONFIG_COMPARE_BYTES);

        config.serverAuth = env.getConfigBoolean(CONFIG_SERVER_AUTH) == null ? false : env.getConfigBoolean(CONFIG_SERVER_AUTH);
        if (config.serverAuth) {
            config.serverUsername = env.getConfigString(CONFIG_SERVER_USERNAME);
            config.serverPassword = env.getConfigPassword(CONFIG_SERVER_PASSWORD);
        }

        config.useProxy = env.getConfigBoolean(CONFIG_USE_PROXY) == null ? false : env.getConfigBoolean(CONFIG_USE_PROXY);
        if (config.useProxy) {
            config.proxyHost = env.getConfigString(CONFIG_PROXY_HOST);
            config.proxyPort = env.getConfigLong(CONFIG_PROXY_PORT) == null ? 0 : env.getConfigLong(CONFIG_PROXY_PORT).intValue();
        }
        config.proxyAuth = env.getConfigBoolean(CONFIG_PROXY_AUTH) == null ? false : env.getConfigBoolean(CONFIG_PROXY_AUTH);
        if (config.proxyAuth) {
            config.proxyUsername = env.getConfigString(CONFIG_PROXY_USERNAME);
            config.proxyPassword = env.getConfigPassword(CONFIG_PROXY_PASSWORD);
        }
        return config;
    }

    private String fixPath(String path) {
        if (path == null) {
            return "/";
        }
        if (!path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }

    private HttpMethodBase createHttpMethod(Config config) {
        String url = config.url.toString();

        if (config.ignorecert && url.startsWith(PROTOCOL_HTTPS)) {
            url = PROTOCOL_HTTPS_IGNORECERT + url.substring(PROTOCOL_HTTPS.length());
        }

        HttpMethodBase httpMethod = null;
        if ("GET".equals(config.method)) {
            httpMethod = new GetMethod(url);
        } else if ("HEAD".equals(config.method)) {
            httpMethod = new HeadMethod(url);
        } else if ("POST".equals(config.method)) {
            httpMethod = new PostMethod(url);
            // set the POST data
            if (config.postData != null && config.postData.length() > 0) {
                try {
                    //StringRequestEntity requestEntity = new StringRequestEntity(config.postData, "application/soap+xml", "UTF-8");
                    StringRequestEntity requestEntity = new StringRequestEntity(config.postData, config.contentType, config.characterSet);
                    ((PostMethod) httpMethod).setRequestEntity(requestEntity);
                    if (log.isLoggable(Level.INFO)) {
                        log.info("request url is " + httpMethod.getPath());
                        log.info("requestEntity content is " + requestEntity.getContent());
                        log.info("requestEntity content type is " + requestEntity.getContentType());
                    }

                } catch (UnsupportedEncodingException uee) {
                    if (log.isLoggable(Level.WARNING)) {
                        log.warning("Encoding POST data failed: " + uee);
                    }
                }
            }
        }
        return httpMethod;
    }

    private void setHttpParameters(HttpMethodBase httpMethod, Config config) throws URIException, IllegalStateException {
        HttpVersion httpVersion = HttpVersion.HTTP_1_1;
        try {
            httpVersion = HttpVersion.parse("HTTP/" + config.httpVersion);
        } catch (Exception ex) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning("Parsing httpVersion failed, using default: " + HttpVersion.HTTP_1_1);
            }
        }
        httpClient.getParams().setParameter(HttpClientParams.PROTOCOL_VERSION, httpVersion);
        httpClient.getParams().setParameter(HttpClientParams.USER_AGENT, config.userAgent);
        httpClient.getParams().setParameter(HttpClientParams.MAX_REDIRECTS, config.maxRedirects);

        // set server authentication credentials
        if (config.serverAuth) {
            URI uri = httpMethod.getURI();
            String host = uri.getHost();
            int port = uri.getPort();
            if (port <= 0) {
                Protocol protocol = Protocol.getProtocol(uri.getScheme());
                port = protocol.getDefaultPort();
            }
//			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(config.serverUsername, config.serverPassword);
            NTCredentials credentials = new NTCredentials(config.serverUsername, config.serverPassword, host, host);
            httpClient.getState().setCredentials(new AuthScope(host, port, AuthScope.ANY_REALM), credentials);
        }

        // set proxy and credentials
        if (config.useProxy) {
            httpClient.getHostConfiguration().setProxy(config.proxyHost, config.proxyPort);

            if (config.proxyAuth) {
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(config.proxyUsername, config.proxyPassword);
                httpClient.getState().setProxyCredentials(new AuthScope(config.proxyHost, config.proxyPort, AuthScope.ANY_REALM), credentials);
            }
        }

        // set dynaTrace tagging header (only timer name)
        if (config.tagging) {
            httpMethod.addRequestHeader(Constants.HEADER_DYNATRACE, "NA=" + config.timerName);
        }

        // use a custom retry handler
        HttpMethodRetryHandler retryHandler = new HttpMethodRetryHandler() {

            @Override
            public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
                // we don't want to retry
                return false;
            }
        };
        httpMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);

        boolean followRedirects = true;
        if (config.maxRedirects > 0) {
            followRedirects = true;
        } else {
            followRedirects = false;
        }

        httpMethod.setFollowRedirects(followRedirects);

        //httpMethod.setFollowRedirects((config.maxRedirects > 0));
    }

    private int calculateHeaderSize(Header[] headers) {
        int headerLength = 0;
        for (Header header : headers) {
            headerLength += header.getName().getBytes().length;
            headerLength += header.getValue().getBytes().length;
        }
        return headerLength;
    }

    @Override
    public void migrate(PropertyContainer properties, int major, int minor, int micro, String qualifier) {
        //JLT-41859: change protocol value from http:// and https:// to http and https
        Property prop = properties.getProperty(CONFIG_PROTOCOL);
        if (prop != null) {
            if (prop.getValue() != null && prop.getValue().contains("https")) {
                prop.setValue("https");
            } else {
                prop.setValue("http");
            }
        }
    }
}
