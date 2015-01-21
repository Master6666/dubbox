package com.alibaba.dubbo.config.spring;

import org.springframework.beans.factory.DisposableBean;

import com.alibaba.dubbo.common.ExecutorManager;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.registry.support.AbstractRegistryFactory;

/**
 * ApplicationBean
 * @author yihaijun
 */
public class ApplicationBean extends ApplicationConfig implements
		DisposableBean {
	public synchronized void destroy() {
		AbstractRegistryFactory.destroyAll();
		ExecutorManager.destroy();
	}
}
