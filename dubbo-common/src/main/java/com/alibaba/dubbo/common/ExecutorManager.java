package com.alibaba.dubbo.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

/**
 * 管理所有静态ScheduledThreadPoolExecutor,ExecutorService等这方面的实例
 * @author yihaijun
 */
public class ExecutorManager {
	protected static final Logger logger = LoggerFactory
			.getLogger(ExecutorManager.class);
	private static final java.util.Hashtable<String, ScheduledThreadPoolExecutor> scheduledThreadPoolExecutorSet = new java.util.Hashtable<String, ScheduledThreadPoolExecutor>();
	private static final java.util.Hashtable<String, ExecutorService> executorServiceSet = new java.util.Hashtable<String, ExecutorService>();
	public static boolean destroyed = false;

	public static void put(String keyName,
			ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
		scheduledThreadPoolExecutorSet
				.put(keyName, scheduledThreadPoolExecutor);
	}

	public static void put(String keyName, ExecutorService executorService) {
		executorServiceSet.put(keyName, executorService);
	}

	public static synchronized void destroy() {
		destroyed = true;
		// try {
		// throw new Exception("ExecutorManager test destroy()...");
		// } catch (Exception e) {
		// e.printStackTrace();
		// if (logger.isInfoEnabled()) {
		// logger.info("ExecutorManager shutdown.",e);
		// }
		// }
		java.util.Iterator<String> it = null;
		it = scheduledThreadPoolExecutorSet.keySet().iterator();
		while (it.hasNext()) {
			ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = scheduledThreadPoolExecutorSet
					.get(it.next());
			scheduledThreadPoolExecutor.shutdown();
		}
		scheduledThreadPoolExecutorSet.clear();

		it = executorServiceSet.keySet().iterator();
		while (it.hasNext()) {
			ExecutorService executorService = executorServiceSet.get(it.next());
			executorService.shutdown();
		}
		executorServiceSet.clear();
	}
}
