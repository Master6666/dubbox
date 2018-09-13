package com.alibaba.dubbo.remoting.zookeeper.zkclient;

import java.util.List;

import com.alibaba.dubbo.common.Constants;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.Watcher.Event.KeeperState;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.alibaba.dubbo.remoting.zookeeper.ChildListener;
import com.alibaba.dubbo.remoting.zookeeper.StateListener;
import com.alibaba.dubbo.remoting.zookeeper.support.AbstractZookeeperClient;

public class ZkclientZookeeperClient extends AbstractZookeeperClient<IZkChildListener> {

//	private final ZkClient client;
	private ZkClient client;
	private final Object clientLock = new Object();
	private final URL url;
	private volatile KeeperState state = KeeperState.SyncConnected;

	public ZkclientZookeeperClient(URL url) {
		super(url);
		this.url=url;
		boolean check=url.getParameter(Constants.CHECK_KEY,"true").equalsIgnoreCase("true");
		try {
			client = null;
			createZkClient();
		} catch (Throwable e) {
			e.printStackTrace();
			LogHelper.stackTrace(logger,"createZkClient Throwable.("+url.toFullString()+")",e);
			if(check){
				throw new RuntimeException(e);	
			}
		}
	}

	private void createZkClient(){
		synchronized (clientLock) {
			if(client!=null){
				return;
			}
			client = new ZkClient(url.getBackupAddress(), url.getParameter(
					Constants.SESSION_TIMEOUT_KEY,
					Constants.DEFAULT_SESSION_TIMEOUT), url.getParameter(
					Constants.TIMEOUT_KEY,
					Constants.DEFAULT_REGISTRY_CONNECT_TIMEOUT));

			LogHelper.stackTrace(logger,"url.getParameter(username)="+url.getParameter("username")+"("+url.toFullString()+")");
			//zookeeper://yhjtest2:WdYRScFdDUw0zwjJa+ZC5/oQViU=@172.16.28.123:2181/com.alibaba.dubbo.registry.RegistryService?application=tisson-fingerprint-service&check=true&dubbo=2.8.4-dd-orange-eagle&group=yhjtest2&interface=com.alibaba.dubbo.registry.RegistryService&organization=tisson&owner=admin&pid=2245&timestamp=1536829455008)], dubbo version: 2.8.4-dd-orange-eagle, current host: 127.0.0.1
			String locaUserFromPrefix = "zookeeper://";
			String digest="dzdb:!Q2w3E$r5";
			if(url.toFullString().startsWith(locaUserFromPrefix) && url.toFullString().indexOf("@")> locaUserFromPrefix.length()+1  && url.toFullString().indexOf(":", locaUserFromPrefix.length()+1) >0 && url.toFullString().indexOf(":", locaUserFromPrefix.length()+1) <url.toFullString().indexOf("@")){
				digest=url.toFullString().substring(locaUserFromPrefix.length(),url.toFullString().indexOf("@"));
			}
			LogHelper.stackTrace(logger,"digest="+digest);
			client.addAuthInfo("digest",digest.getBytes());
			client.subscribeStateChanges(new IZkStateListener() {
			public void handleStateChanged(KeeperState state) throws Exception {
				ZkclientZookeeperClient.this.state = state;
				if (state == KeeperState.Disconnected) {
					stateChanged(StateListener.DISCONNECTED);
				} else if (state == KeeperState.SyncConnected) {
					stateChanged(StateListener.CONNECTED);
				}
			}
			public void handleNewSession() throws Exception {
				stateChanged(StateListener.RECONNECTED);
			}
			@Override
			public void handleSessionEstablishmentError(Throwable arg0)
					throws Exception {
			}
		});
		}		
	}

	
	public void createPersistent(String path) {
		try {
			synchronized (clientLock) {
				createZkClient();
				LogHelper.stackTrace(logger,"client.createPersistent("+path+") begin...");
				client.createPersistent(path, true);
			}
		} catch (ZkNodeExistsException e) {
		}
	}

	public void createEphemeral(String path) {
		try {
			synchronized (clientLock) {
				createZkClient();
				LogHelper.stackTrace(logger,"client.createEphemeral("+path+") begin...");
				client.createEphemeral(path);
			}
		} catch (ZkNodeExistsException e) {
		}
	}

	public void delete(String path) {
		try {
			synchronized (clientLock) {
				createZkClient();
				client.delete(path);
			}
		} catch (ZkNoNodeException e) {
		}
	}

	public List<String> getChildren(String path) {
		try {
			synchronized (clientLock) {
				createZkClient();
				return client.getChildren(path);
			}
        } catch (ZkNoNodeException e) {
            return null;
        }
	}

	public boolean isConnected() {
		return state == KeeperState.SyncConnected;
	}

	public void doClose() {
		synchronized (clientLock) {
			createZkClient();
			client.close();
		}
	}

	public IZkChildListener createTargetChildListener(String path, final ChildListener listener) {
		synchronized (clientLock) {
			createZkClient();
			return new IZkChildListener() {
				public void handleChildChange(String parentPath, List<String> currentChilds)
						throws Exception {
					listener.childChanged(parentPath, currentChilds);
				}
			};
		}
	}

	public List<String> addTargetChildListener(String path, final IZkChildListener listener) {
		synchronized (clientLock) {
			createZkClient();
			return client.subscribeChildChanges(path, listener);
		}
	}

	public void removeTargetChildListener(String path, IZkChildListener listener) {
		synchronized (clientLock) {
			createZkClient();
			client.unsubscribeChildChanges(path,  listener);
		}
	}
}
