package com.cloud.hypervisor.kvm.resource;

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.xen.resource.CitrixResourceBase;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;
import com.cloud.vm.State;

public class KvmDummyResourceBase extends ServerResourceBase implements ServerResource {
	private String _zoneId;
	private String _podId;
	private String _guid;
	private String _agentIp;
	private String _clusterId;
	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StartupCommand[] initialize() {
		StartupRoutingCommand cmd = new StartupRoutingCommand(0, 0, 0, 0, null, Hypervisor.HypervisorType.KVM, new HashMap<String, String>(), new HashMap<String, State>());
		cmd.setDataCenter(_zoneId);
		cmd.setPod(_podId);
		cmd.setGuid(_guid);
		cmd.setName(_agentIp);
		cmd.setPrivateIpAddress(_agentIp);
		cmd.setStorageIpAddress(_agentIp);
		cmd.setVersion(KvmDummyResourceBase.class.getPackage().getImplementationVersion());
		cmd.setCluster(_clusterId);
		return new StartupCommand[] { cmd };
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Answer executeRequest(Command cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getDefaultScriptsDir() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
		_zoneId = (String)params.get("zone");
		_podId = (String)params.get("pod");
		_guid = (String)params.get("guid");
		_agentIp = (String)params.get("agentIp");
		_clusterId = (String)params.get("cluster");
		return true;
	}
}
