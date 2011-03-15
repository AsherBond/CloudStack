package com.cloud.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.DetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=DeploymentPlanner.class)
public class FirstFitPlanner extends PlannerBase implements DeploymentPlanner {
	private static final Logger s_logger = Logger.getLogger(FirstFitPlanner.class);
	@Inject private HostDao _hostDao;
	@Inject private CapacityDao _capacityDao;
	@Inject private DataCenterDao _dcDao;
	@Inject private HostPodDao _podDao;
	@Inject private ClusterDao _clusterDao;
	@Inject DetailsDao _hostDetailsDao = null;
	@Inject GuestOSDao _guestOSDao = null; 
    @Inject GuestOSCategoryDao _guestOSCategoryDao = null;
    @Inject CapacityManager _capacityMgr;
    @Inject ConfigurationDao _configDao;
	
	@Override
	public DeployDestination plan(VirtualMachineProfile vmProfile,
			DeploymentPlan plan, ExcludeList avoid)
			throws InsufficientServerCapacityException {
	    String _allocationAlgorithm = _configDao.getValue(Config.VmAllocationAlgorithm.key());
		VirtualMachine vm = vmProfile.getVirtualMachine();
		ServiceOffering offering = vmProfile.getServiceOffering();
		DataCenter dc = _dcDao.findById(vm.getDataCenterId());
		int cpu_requested = offering.getCpu() * offering.getSpeed();
		long ram_requested = offering.getRamSize() * 1024L * 1024L;
		
		s_logger.info("try to allocate a host from dc:" + plan.getDataCenterId() + ", pod:" + plan.getPodId() + ",cluster:" + plan.getClusterId() +
				", requested cpu: " + cpu_requested + ", requested ram: " + ram_requested);
		if (vm.getLastHostId() != null) {
			if (s_logger.isDebugEnabled()) {
				s_logger.debug("This VM has last host_id specified, trying to choose the same host: " +vm.getLastHostId());
			}
			HostVO host = _hostDao.findById(vm.getLastHostId());
			
			if (host != null && host.getStatus() == Status.Up) {
				boolean canDepployToLastHost = deployToHost(host, cpu_requested, ram_requested, true, avoid);
				if (canDepployToLastHost) {
					Pod pod = _podDao.findById(vm.getPodId());
					Cluster cluster = _clusterDao.findById(host.getClusterId());
					return new DeployDestination(dc, pod, cluster, host);
				}else{
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("The last host of this VM does not have enough capacity");
					}
				}
			}else{
				if(host == null){
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("The last host of this VM cannot be found");
					}
				}else{
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("The last host of this VM is not UP, host status is: "+host.getStatus().name());
					}
				}
			}
			if (s_logger.isDebugEnabled()) {
				s_logger.debug("Cannot choose the last host to deploy this VM ");
			}
		}
		
		/*Go through all the pods/clusters under zone*/
		List<HostPodVO> pods = null;
		if (plan.getPodId() != null) {
			if (s_logger.isDebugEnabled()) {
				s_logger.debug("Searching for hosts only under specified Pod: "+ plan.getPodId());
			}
			HostPodVO pod = _podDao.findById(plan.getPodId());
			if (pod != null && dc.getId() == pod.getDataCenterId()) {
				pods = new ArrayList<HostPodVO>(1);
				pods.add(pod);
			} else {
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Can't enforce the pod selector");
				}
				return null;
			}
		} 
		
		if (pods == null)
			pods = _podDao.listByDataCenterId(plan.getDataCenterId());
		
		if (_allocationAlgorithm != null && _allocationAlgorithm.equalsIgnoreCase("random")) {
		    Collections.shuffle(pods);
		}
		if (s_logger.isDebugEnabled()) {
			s_logger.debug("List of pods to be considered: "+ pods);
		}
		for (HostPodVO hostPod : pods) {
			if (avoid.shouldAvoid(hostPod)) {
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Pod: " + hostPod.getId() + " is in the avoid set, skipping this pod");
				}
				continue;
			}
			
			
			List<ClusterVO> clusters = null;
			if (plan.getClusterId() != null) {
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Searching for hosts only under specified Cluster: "+ plan.getClusterId());
				}
				ClusterVO cluster = _clusterDao.findById(plan.getClusterId());
				if (cluster != null && hostPod.getId() == cluster.getPodId()) {
					clusters = new ArrayList<ClusterVO>(1);
					clusters.add(cluster);
				} else {
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("Can't enforce the cluster selector");
					}
					return null;
				}
			} 
			
			if (clusters == null) {			
				clusters = _clusterDao.listByPodId(hostPod.getId());
			}
			
			if (_allocationAlgorithm != null && _allocationAlgorithm.equalsIgnoreCase("random")) {
                Collections.shuffle(clusters);
            }
			if (s_logger.isDebugEnabled()) {
				s_logger.debug("List of clusters to be considered under this podId: "+ hostPod.getId() + ", clusters: "+clusters);
			}
			for (ClusterVO clusterVO : clusters) {
				if (avoid.shouldAvoid(clusterVO)) {
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("Cluster: " +clusterVO.getId() + " is in the avoid set, skipping this cluster");
					}
					continue;
				}
				
				if (clusterVO.getHypervisorType() != vmProfile.getHypervisorType()) {
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("Cluster: "+clusterVO.getId() + " has HyperVisorType that does not match the VM, skipping this cluster");
					}
					avoid.addCluster(clusterVO.getId());
					continue;
				}
				
				List<HostVO> hosts = _hostDao.listBy(Host.Type.Routing, clusterVO.getId(), hostPod.getId(), dc.getId());
				if (_allocationAlgorithm != null && _allocationAlgorithm.equalsIgnoreCase("random")) {
				    Collections.shuffle(hosts);
				}
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("List of hosts to be considered under this cluster: "+ clusterVO.getId() + ", hosts: "+hosts);
				}
				 // We will try to reorder the host lists such that we give priority to hosts that have
		        // the minimums to support a VM's requirements
		        hosts = prioritizeHosts(vmProfile.getTemplate(), hosts);
		        
		        if (s_logger.isDebugEnabled()) {
		            s_logger.debug("Found " + hosts.size() + " hosts for allocation after prioritization: "+ hosts);
		        }

		        if (s_logger.isDebugEnabled()) {
		            s_logger.debug("Looking for speed=" + cpu_requested + "Mhz, Ram=" + ram_requested);
		        }
				
				for (HostVO hostVO : hosts) {																				
					boolean canDeployToHost = deployToHost(hostVO, cpu_requested, ram_requested, false, avoid);
					if (canDeployToHost) {
						Pod pod = _podDao.findById(hostPod.getId());
						Cluster cluster = _clusterDao.findById(clusterVO.getId());
						Host host = _hostDao.findById(hostVO.getId());
						DeployDestination dest = new DeployDestination(dc, pod, cluster, host);
						s_logger.info("Returning DeployDestination: "+dest);
						return dest;
					}
					avoid.addHost(hostVO.getId());
				}
				avoid.addCluster(clusterVO.getId());
			}
			avoid.addPod(hostPod.getId());
		}
		
		s_logger.info("Could not find a DeployDestination for the VM");
		return null;
	}

	
	@Override
	public boolean check(VirtualMachineProfile vm, DeploymentPlan plan,
			DeployDestination dest, ExcludeList exclude) {
		// TODO Auto-generated method stub
		return false;
	}
	  
    @DB
	protected boolean deployToHost(HostVO host, Integer cpu, long ram, boolean fromLastHost, ExcludeList avoid) {		
    	if (avoid.shouldAvoid(host)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Host name: " + host.getName() + ", hostId: "+ host.getId() +" is in avoid set, skipping this and trying other available hosts");
            }    		
			return false;
    	}

    	return _capacityMgr.checkIfHostHasCapacity(host.getId(), cpu, ram, fromLastHost);
	}
    
    protected List<HostVO> prioritizeHosts(VirtualMachineTemplate template, List<HostVO> hosts) {
    	if (template == null) {
    		return hosts;
    	}
    	
    	// Determine the guest OS category of the template
    	String templateGuestOSCategory = getTemplateGuestOSCategory(template);
    	
    	List<HostVO> prioritizedHosts = new ArrayList<HostVO>();
    	
    	// If a template requires HVM and a host doesn't support HVM, remove it from consideration
    	List<HostVO> hostsToCheck = new ArrayList<HostVO>();
    	if (template.isRequiresHvm()) {
    		for (HostVO host : hosts) {
    			if (hostSupportsHVM(host)) {
    				hostsToCheck.add(host);
    			}else{
    				if (s_logger.isDebugEnabled()) {
    					s_logger.debug("Template requires HVM, this host doe not support HVM, skipping the host");
    				}
   	    		}
    		}
    	} else {
    		hostsToCheck.addAll(hosts);
    	}
    	
    	// If a host is tagged with the same guest OS category as the template, move it to a high priority list
    	// If a host is tagged with a different guest OS category than the template, move it to a low priority list
    	List<HostVO> highPriorityHosts = new ArrayList<HostVO>();
    	List<HostVO> lowPriorityHosts = new ArrayList<HostVO>();
    	for (HostVO host : hostsToCheck) {
    		String hostGuestOSCategory = getHostGuestOSCategory(host);
    		if (hostGuestOSCategory == null) {
    			if (s_logger.isDebugEnabled()) {
    				s_logger.debug("No hostGuestOSCategory found, skipping the host");
    			}
    			continue;
    		} else if (templateGuestOSCategory.equals(hostGuestOSCategory)) {
    			highPriorityHosts.add(host);
    		} else {
    			lowPriorityHosts.add(host);
    		}
    	}
    	
    	hostsToCheck.removeAll(highPriorityHosts);
    	hostsToCheck.removeAll(lowPriorityHosts);
    	
    	// Prioritize the remaining hosts by HVM capability
    	for (HostVO host : hostsToCheck) {
    		if (!template.isRequiresHvm() && !hostSupportsHVM(host)) {
    			// Host and template both do not support hvm, put it as first consideration
    			prioritizedHosts.add(0, host);
    		} else {
    			// Template doesn't require hvm, but the machine supports it, make it last for consideration
    			prioritizedHosts.add(host);
    		}
    	}
    	
    	// Merge the lists
    	prioritizedHosts.addAll(0, highPriorityHosts);
    	prioritizedHosts.addAll(lowPriorityHosts);
    	
    	return prioritizedHosts;
    }
    
    protected boolean hostSupportsHVM(HostVO host) {
    	// Determine host capabilities
		String caps = host.getCapabilities();
		
		if (caps != null) {
            String[] tokens = caps.split(",");
            for (String token : tokens) {
            	if (token.contains("hvm")) {
            	    return true;
            	}
            }
		}
		
		return false;
    }
    
    protected String getHostGuestOSCategory(HostVO host) {
		DetailVO hostDetail = _hostDetailsDao.findDetail(host.getId(), "guest.os.category.id");
		if (hostDetail != null) {
			String guestOSCategoryIdString = hostDetail.getValue();
			long guestOSCategoryId;
			
			try {
				guestOSCategoryId = Long.parseLong(guestOSCategoryIdString);
			} catch (Exception e) {
				return null;
			}
			
			GuestOSCategoryVO guestOSCategory = _guestOSCategoryDao.findById(guestOSCategoryId);
			
			if (guestOSCategory != null) {
				return guestOSCategory.getName();
			} else {
				return null;
			}
		} else {
			return null;
		}
    }
    
    protected String getTemplateGuestOSCategory(VirtualMachineTemplate template) {
    	long guestOSId = template.getGuestOSId();
    	GuestOSVO guestOS = _guestOSDao.findById(guestOSId);
    	long guestOSCategoryId = guestOS.getCategoryId();
    	GuestOSCategoryVO guestOSCategory = _guestOSCategoryDao.findById(guestOSCategoryId);
    	return guestOSCategory.getName();
    }
}
