/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.http.jetty;

import java.util.concurrent.TimeUnit;

import com.alibaba.dubbo.remoting.http.servlet.ServletManager;

import org.apache.log4j.MDC;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.log.Log;
import org.mortbay.log.StdErrLog;
import org.mortbay.thread.QueuedThreadPool;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.servlet.DispatcherServlet;
import com.alibaba.dubbo.remoting.http.support.AbstractHttpServer;

public class JettyHttpServer extends AbstractHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(JettyHttpServer.class);

    private Server              server;

    private URL url;

    public JettyHttpServer(URL url, final HttpHandler handler){
        super(url, handler);
		if(MDC.get("appName") == null || MDC.get("appName").equals("")){
			String message = "LOGHELPER STACKTRACE:[MDC.get(appName) is null or \"\"]";
		    try {
					throw new Throwable("SimulationException");
			} catch (Throwable e1) {
				if(logger.isTraceEnabled()){
					logger.trace(message,e1);
				}else if(logger.isDebugEnabled()){
					logger.debug(message);
				}
			}
			MDC.put("appName", url.getAddress().replace(":", "-"));
		}
        // modified by lishen
        this.url = url;
        // TODO we should leave this setting to slf4j
        Log.setLog(new StdErrLog());
        Log.getLog().setDebugEnabled(false);

        DispatcherServlet.addHttpHandler(url.getPort(), handler);

        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setDaemon(true);
        threadPool.setMaxThreads(threads);
        threadPool.setMinThreads(threads);

        int cores = url.getParameter(Constants.CORE_THREADS_KEY, Constants.DEFAULT_CORE_THREADS);
        int maxThreads = url.getParameter(Constants.THREADS_KEY, Integer.MAX_VALUE);
//        int queues = url.getParameter(Constants.QUEUES_KEY, Constants.DEFAULT_QUEUES);
        int keepAliveTime = url.getParameter(Constants.KEEP_ALIVE_TIME_KEY, Constants.DEFAULT_ALIVE);
//        threadPoolExecutor.setCorePoolSize(cores);
//        threadPoolExecutor.setMaximumPoolSize(maxThreads);
//        BlockingQueue<Runnable> workQueue = queues == 0 ? new SynchronousQueue<Runnable>() : 
//			(queues < 0 ? new LinkedBlockingQueue<Runnable>() 
//					: new LinkedBlockingQueue<Runnable>(queues));
//        threadPoolExecutor.setKeepAliveTime(keepAliveTime,TimeUnit.MILLISECONDS);
        com.alibaba.dubbo.common.utils.LogHelper.stackTrace(logger,"reset Now QueuedThreadPool:("+cores + "," + threads + "," + keepAliveTime+",..)");
        threadPool.setMaxThreads(maxThreads);
        threadPool.setMinThreads(cores);
        threadPool.setMaxIdleTimeMs(keepAliveTime*3);

        
        SelectChannelConnector connector = new SelectChannelConnector();
        if (! url.isAnyHost() && NetUtils.isValidLocalHost(url.getHost())) {
            connector.setHost(url.getHost());
        }
        connector.setPort(url.getPort());

        server = new Server();
        server.setThreadPool(threadPool);
        server.addConnector(connector);

        ServletHandler servletHandler = new ServletHandler();
        ServletHolder servletHolder = servletHandler.addServletWithMapping(DispatcherServlet.class, "/*");
        servletHolder.setInitOrder(2);

        // modified by lishen
        // dubbo's original impl can't support the use of ServletContext
//        server.addHandler(servletHandler);
        // TODO Context.SESSIONS is the best option here?
        Context context = new Context(server, "/", Context.SESSIONS);
        context.setServletHandler(servletHandler);
        ServletManager.getInstance().addServletContext(url.getPort(), context.getServletContext());

        try {
            try {
        		if(MDC.get("appName") == null || MDC.get("appName").equals("")){
					MDC.put("appName", url.getAddress().replace(":", "-"));
				}
			} catch (Throwable e) {
			}
			server.start();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start jetty server on " + url.getAddress() + ", cause: "
                                            + e.getMessage(), e);
        }
    }

    public void close() {
        super.close();

        // modified by lishen
        ServletManager.getInstance().removeServletContext(url.getPort());

        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

}