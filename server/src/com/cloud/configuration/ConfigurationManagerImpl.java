/**
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.configuration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.acl.SecurityChecker;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.CreateCfgCmd;
import com.cloud.api.commands.CreateDiskOfferingCmd;
import com.cloud.api.commands.CreatePodCmd;
import com.cloud.api.commands.CreateServiceOfferingCmd;
import com.cloud.api.commands.CreateVlanIpRangeCmd;
import com.cloud.api.commands.CreateZoneCmd;
import com.cloud.api.commands.DeleteDiskOfferingCmd;
import com.cloud.api.commands.DeletePodCmd;
import com.cloud.api.commands.DeleteServiceOfferingCmd;
import com.cloud.api.commands.DeleteVlanIpRangeCmd;
import com.cloud.api.commands.DeleteZoneCmd;
import com.cloud.api.commands.UpdateCfgCmd;
import com.cloud.api.commands.UpdateDiskOfferingCmd;
import com.cloud.api.commands.UpdatePodCmd;
import com.cloud.api.commands.UpdateServiceOfferingCmd;
import com.cloud.api.commands.UpdateZoneCmd;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.DataCenterNetworkType;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.dc.dao.DataCenterLinkLocalIpAddressDaoImpl;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.State;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value={ConfigurationManager.class, ConfigurationService.class})
public class ConfigurationManagerImpl implements ConfigurationManager, ConfigurationService {
    public static final Logger s_logger = Logger.getLogger(ConfigurationManagerImpl.class.getName());

	String _name;
	@Inject ConfigurationDao _configDao;
	@Inject HostPodDao _podDao;
	@Inject AccountVlanMapDao _accountVlanMapDao;
	@Inject PodVlanMapDao _podVlanMapDao;
	@Inject DataCenterDao _zoneDao;
	@Inject DomainRouterDao _domrDao;
	@Inject DomainDao _domainDao;
	@Inject ServiceOfferingDao _serviceOfferingDao;
	@Inject DiskOfferingDao _diskOfferingDao;
	@Inject VlanDao _vlanDao;
	@Inject IPAddressDao _publicIpAddressDao;
	@Inject DataCenterIpAddressDao _privateIpAddressDao;
	@Inject VMInstanceDao _vmInstanceDao;
	@Inject AccountDao _accountDao;
	@Inject EventDao _eventDao;
	@Inject UserDao _userDao;
	@Inject ConsoleProxyDao _consoleDao;
	@Inject SecondaryStorageVmDao _secStorageDao;
    @Inject AccountManager _accountMgr;
    @Inject NetworkManager _networkMgr;
	@Inject(adapter=SecurityChecker.class)
    Adapters<SecurityChecker> _secChecker;
	
	//FIXME - why don't we have interface for DataCenterLinkLocalIpAddressDao?
	protected static final DataCenterLinkLocalIpAddressDaoImpl _LinkLocalIpAllocDao = ComponentLocator.inject(DataCenterLinkLocalIpAddressDaoImpl.class);

    private int _maxVolumeSizeInGb;

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
    	_name = name;

        String maxVolumeSizeInGbString = _configDao.getValue("max.volume.size.gb");
        int maxVolumeSizeGb = NumbersUtil.parseInt(maxVolumeSizeInGbString, 2000);

        _maxVolumeSizeInGb = maxVolumeSizeGb;

    	return true;
    }
    
    @Override
    public String getName() {
        return _name;
    }
    
    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
    
    @Override
    public void updateConfiguration(long userId, String name, String value)  {
    	if (value != null && (value.trim().isEmpty() || value.equals("null"))) {
    		value = null;
    	}
    	
    	String validationMsg = validateConfigurationValue(name, value);
    	
    	if (validationMsg != null) {
    		s_logger.error("Invalid configuration option, name: " + name + ", value:" + value);
    		throw new InvalidParameterValueException(validationMsg);
    	}
    	
    	if (!_configDao.update(name, value)) {
    		s_logger.error("Failed to update configuration option, name: " + name + ", value:" + value);
    		throw new CloudRuntimeException("Failed to update configuration value. Please contact Cloud Support.");
    	}
    	
    	saveConfigurationEvent(userId, null, EventTypes.EVENT_CONFIGURATION_VALUE_EDIT, "Successfully edited configuration value.", "name=" + name, "value=" + value);
    }
    
    @Override
    public Configuration updateConfiguration(UpdateCfgCmd cmd) throws InvalidParameterValueException{
    	Long userId = UserContext.current().getUserId();
    	String name = cmd.getCfgName();
    	String value = cmd.getValue();
    	
    	//check if config value exists
    	if (_configDao.findByName(name) == null)
    	    throw new InvalidParameterValueException("Config parameter with name " + name + " doesn't exist");
    	
    	if (value == null)
    	    return _configDao.findByName(name);
    	
    	updateConfiguration (userId, name, value);
    	if (_configDao.getValue(name).equalsIgnoreCase(value))
    		return _configDao.findByName(name);
    	else 
    		throw new CloudRuntimeException("Unable to update configuration parameter " + name);
    }
    
    
    private String validateConfigurationValue(String name, String value) throws InvalidParameterValueException {
    	if (value == null) {
    		return null;
    	}
    	
    	Config c = Config.getConfig(name);
    	value = value.trim();
    	
    	if (c == null) {
    		s_logger.error("Missing configuration variable " + name + " in configuration table");
    		return "Invalid configuration variable.";
    	}
    	
    	Class<?> type = c.getType();
    	if (type.equals(Boolean.class)) {
    		if (!(value.equals("true") || value.equals("false"))) {
 	    		s_logger.error("Configuration variable " + name + " is expecting true or false in stead of " + value);
    			return "Please enter either \"true\" or \"false\".";
    		}
    		return null;
    	}
    	
		String range = c.getRange();
		if (range == null) {
			return null;
		}
		
    	if(type.equals(String.class)) {
			if (range.equals("privateip")) 
			{
				try {
					if (!NetUtils.isSiteLocalAddress(value)) {
						s_logger.error("privateip range " + value
										+ " is not a site local address for configuration variable " + name);
						return "Please enter a site local IP address.";
					}
				} catch (NullPointerException e) 
				{
					s_logger.error("Error parsing ip address for " + name);
					throw new InvalidParameterValueException("Error parsing ip address");
				}
			} else if (range.equals("netmask")) {
				if (!NetUtils.isValidNetmask(value)) {
					s_logger.error("netmask " + value + " is not a valid net mask for configuration variable " + name);
					return "Please enter a valid netmask.";
				}
			} else if (range.equals("hypervisorList")) {
				String [] hypervisors = value.split(",");
				if (hypervisors == null) {
					return "Please enter hypervisor list, seperated by comma";
				}
				for (String hypervisor : hypervisors) {
					if (HypervisorType.getType(hypervisor) == HypervisorType.Any || 
						HypervisorType.getType(hypervisor) == HypervisorType.None) {
						return "Please enter valid hypervisor type";
					}
				}
    		} else {
				String [] options = range.split(",");
				for( String option : options) {
					if( option.trim().equals(value) ) {
						return null;
					}
				}
				s_logger.error("configuration value for " + name + " is invalid");
				return "Please enter : " + range;
				
			}
    	}else if(type.equals(Integer.class)) {
			String [] options = range.split("-");
			if( options.length != 2 ) {
				String msg = "configuration range " + range + " for " + name + " is invalid";
				s_logger.error(msg);
				return msg;
			}
			int min = Integer.parseInt(options[0]);
			int max = Integer.parseInt(options[1]);
			int val = Integer.parseInt(value);
			if( val < min || val > max ) {
				s_logger.error("configuration value for " + name + " is invalid");
				return "Please enter : " + range;
			}
    	}
    	return null;
    }
    
    private boolean podHasAllocatedPrivateIPs(long podId) {
    	HostPodVO pod = _podDao.findById(podId);
    	int count = _privateIpAddressDao.countIPs(podId, pod.getDataCenterId(), true);
    	return (count > 0);
    }
    
    @DB
    protected void checkIfPodIsDeletable(long podId)  {
    	List<List<String>> tablesToCheck = new ArrayList<List<String>>();
    	
    	HostPodVO pod = _podDao.findById(podId);
    	
    	// Check if there are allocated private IP addresses in the pod
    	if (_privateIpAddressDao.countIPs(podId, pod.getDataCenterId(), true) != 0) {
    		throw new CloudRuntimeException("There are private IP addresses allocated for this pod");
    	}

    	List<String> volumes = new ArrayList<String>();
    	volumes.add(0, "volumes");
    	volumes.add(1, "pod_id");
    	volumes.add(2, "there are storage volumes for this pod");
    	tablesToCheck.add(volumes);
    	
    	List<String> host = new ArrayList<String>();
    	host.add(0, "host");
    	host.add(1, "pod_id");
    	host.add(2, "there are servers running in this pod");
    	tablesToCheck.add(host);
    	
    	List<String> vmInstance = new ArrayList<String>();
    	vmInstance.add(0, "vm_instance");
    	vmInstance.add(1, "pod_id");
    	vmInstance.add(2, "there are virtual machines running in this pod");
    	tablesToCheck.add(vmInstance);
    	
    	List<String> alert = new ArrayList<String>();
		alert.add(0, "alert");
		alert.add(1, "pod_id");
		alert.add(2, "there are alerts for this pod");
		tablesToCheck.add(alert);
    	
    	for (List<String> table : tablesToCheck) {
    		String tableName = table.get(0);
    		String column = table.get(1);
    		String errorMsg = table.get(2);
    		
    		String dbName;
    		if (tableName.equals("event") || tableName.equals("cloud_usage") || tableName.equals("usage_vm_instance") ||
    			tableName.equals("usage_ip_address") || tableName.equals("usage_network") || tableName.equals("usage_job") ||
    			tableName.equals("account") || tableName.equals("user_statistics")) {
    			dbName = "cloud_usage";
    		} else {
    			dbName = "cloud";
    		}
    		
    		String selectSql = "SELECT * FROM `" + dbName + "`.`" + tableName + "` WHERE " + column + " = ?";
    		
            Transaction txn = Transaction.currentTxn();
    		try {
                PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
                stmt.setLong(1, podId);
                ResultSet rs = stmt.executeQuery();
                if (rs != null && rs.next()) {
                	throw new CloudRuntimeException("The pod cannot be edited because " + errorMsg);
                }
            } catch (SQLException ex) {
                throw new CloudRuntimeException("The Management Server failed to detect if pod is editable. Please contact Cloud Support.");
            }
    	}
    }
    
    private void checkPodAttributes(long podId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp, boolean checkForDuplicates) throws InvalidParameterValueException {
    	// Check if the zone is valid
		if (!validZone(zoneId)) {
			throw new InvalidParameterValueException("Please specify a valid zone.");
		}

		if (checkForDuplicates) {
			// Check if the pod already exists
			if (validPod(podName, zoneId)) {
				throw new InvalidParameterValueException("A pod with name: " + podName + " already exists in zone " + zoneId + ". Please specify a different pod name. ");
			}
		}
		
		String cidrAddress;
		long cidrSize;
		// Get the individual cidrAddress and cidrSize values, if the CIDR is valid. If it's not valid, return an error.
		if (NetUtils.isValidCIDR(cidr)) {
			cidrAddress = getCidrAddress(cidr);
			cidrSize = getCidrSize(cidr);
		} else {
			throw new InvalidParameterValueException("Please enter a valid CIDR for pod: " + podName);
		}
		
		// Check if the IP range is valid
		if (startIp != null || endIp != null) {
			checkIpRange(startIp, endIp, cidrAddress, cidrSize);
		}
		
		// Check if the gateway is a valid IP address
		if (!NetUtils.isValidIp(gateway)) {
    		throw new InvalidParameterValueException("The gateway is not a valid IP address.");
    	}
		
		// Check if the gateway is in the CIDR subnet
		if (!NetUtils.getCidrSubNet(gateway, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
    		throw new InvalidParameterValueException("The gateway is not in the CIDR subnet.");
    	}
		
		String checkPodCIDRs = _configDao.getValue("check.pod.cidrs");
		if (checkPodCIDRs == null || checkPodCIDRs.trim().isEmpty() || Boolean.parseBoolean(checkPodCIDRs)) {
			// Check if the CIDR conflicts with the Guest Network or other pods
			HashMap<Long, List<Object>> currentPodCidrSubnets = _podDao.getCurrentPodCidrSubnets(zoneId, podId);
			List<Object> newCidrPair = new ArrayList<Object>();
			newCidrPair.add(0, cidrAddress);
			newCidrPair.add(1, new Long(cidrSize));
			currentPodCidrSubnets.put(new Long(-1), newCidrPair);
			checkPodCidrSubnets(zoneId, currentPodCidrSubnets);
		}
    }
    
    @Override
    @DB
    public boolean deletePod(DeletePodCmd cmd)  {
    	Long podId = cmd.getId();
    	Long userId = 1L;
    	
    	if (UserContext.current() != null)
    		userId = UserContext.current().getUserId();
    	
    	// Make sure the pod exists
    	if (!validPod(podId)) {
    		throw new InvalidParameterValueException("A pod with ID: " + podId + " does not exist.");
    	}

    	checkIfPodIsDeletable(podId);

    	HostPodVO pod = _podDao.findById(podId);
    	DataCenterVO zone = _zoneDao.findById(pod.getDataCenterId());
    	
    	//Delete private ip addresses for the pod if there are any
    	List<DataCenterIpAddressVO> privateIps = _privateIpAddressDao.listByPodIdDcId(Long.valueOf(podId), pod.getDataCenterId());
	    if (privateIps != null && privateIps.size() != 0) {
	        if (!(_privateIpAddressDao.deleteIpAddressByPod(podId))) {
	            throw new CloudRuntimeException("Failed to cleanup private ip addresses for pod " + podId);
	        }
	    }
    	
    	//Delete link local ip addresses for the pod
    	if (!(_LinkLocalIpAllocDao.deleteIpAddressByPod(podId))) {
            throw new CloudRuntimeException("Failed to cleanup private ip addresses for pod " + podId);
        }

    	//Delete the pod
    	if (!(_podDao.expunge(podId))) {
    	    throw new CloudRuntimeException("Failed to delete pod " + podId);
    	}
    	
		saveConfigurationEvent(userId, null, EventTypes.EVENT_POD_DELETE, "Successfully deleted pod with name: " + pod.getName() + " in zone: " + zone.getName() + ".", "podId=" + podId, "dcId=" + zone.getId());
		return true;
    }

    @Override
    @DB
    public Pod editPod(UpdatePodCmd cmd)  
    {
    	
    	//Input validation
    	String cidr = cmd.getCidr();
    	String startIp = cmd.getStartIp();
    	String endIp = cmd.getEndIp();
    	String gateway = cmd.getGateway();
    	Long id = cmd.getId();
    	String name = cmd.getPodName();
    	Long userId = UserContext.current().getUserId();

    	if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
    	
    	//verify parameters
    	HostPodVO pod = _podDao.findById(id);;
    	if (pod == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find pod by id " + id);
    	}
    	
    	long zoneId = pod.getDataCenterId();
    	DataCenterVO zone = _zoneDao.findById(zoneId);
    	if (zone == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find zone by id " + zoneId);
    	}
    	
    	if (endIp != null && startIp == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "If an end IP is specified, a start IP must be specified.");
    	}
    	
    	// Make sure the pod exists
    	if (!validPod(id)) {
    		throw new InvalidParameterValueException("A pod with ID: " + id + " does not exist.");
    	}
    	
    	// If the gateway, CIDR, private IP range is being updated, check if the pod has allocated private IP addresses
    	if (gateway!= null || cidr != null || startIp != null || endIp != null) {
    		if (podHasAllocatedPrivateIPs(id)) {
    			throw new CloudRuntimeException("The specified pod has allocated private IP addresses, so its CIDR and IP address range cannot be changed.");
    		}
    	}
    	
    	String oldPodName = pod.getName();
    	
    	if (name == null) {
    		name = oldPodName;
    	}
    	
    	if (gateway == null) {
    		gateway = pod.getGateway();
    	}
    	
    	if (cidr == null) {
    		cidr = pod.getCidrAddress() + "/" + pod.getCidrSize();
    	}
    	
    	boolean checkForDuplicates = !oldPodName.equals(name);
    	checkPodAttributes(id, name, pod.getDataCenterId(), gateway, cidr, startIp, endIp, checkForDuplicates);
    	
    	String cidrAddress = getCidrAddress(cidr);
    	int cidrSize = getCidrSize(cidr);
    	
    	if (startIp != null && endIp == null) {
    		endIp = NetUtils.getIpRangeEndIpFromCidr(cidrAddress, cidrSize);
		}
		
		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			
			String ipRange;
			if (startIp != null) {
				// remove old private ip address
				_zoneDao.deletePrivateIpAddressByPod(pod.getId());
				
				// re-allocate private ip addresses for pod
				_zoneDao.addPrivateIpAddress(zoneId, pod.getId(), startIp, endIp);
				
				ipRange = startIp + "-";
				if (endIp != null) {
					ipRange += endIp;
				}
			} else {
				ipRange = pod.getDescription();
			}
			
	    	pod.setName(name);
	    	pod.setDataCenterId(zoneId);
	    	pod.setGateway(gateway);
	    	pod.setCidrAddress(cidrAddress);
	    	pod.setCidrSize(cidrSize);
	    	pod.setDescription(ipRange);
	    	
	    	if (!_podDao.update(id, pod)) {
	    		throw new CloudRuntimeException("Failed to edit pod. Please contact Cloud Support.");
	    	}
    	
	    	txn.commit();
		} catch(Exception e) {
			s_logger.error("Unable to edit pod due to " + e.getMessage(), e);
			txn.rollback();
			throw new CloudRuntimeException("Failed to edit pod. Please contact Cloud Support.");
		}
		
		saveConfigurationEvent(userId, null, EventTypes.EVENT_POD_EDIT, "Successfully edited pod. New pod name is: " + name + " and new zone name is: " + zone.getName() + ".", "podId=" + pod.getId(), "dcId=" + zone.getId(), "gateway=" + gateway, "cidr=" + cidr, "startIp=" + startIp, "endIp=" + endIp);
		
		return pod;
    }

    @Override
    public Pod createPod(CreatePodCmd cmd)  {
        String cidr = cmd.getCidr();
        String endIp = cmd.getEndIp();
        String gateway = cmd.getGateway();
        String name = cmd.getPodName();
        String startIp = cmd.getStartIp();
        Long zoneId = cmd.getZoneId();

        //verify input parameters
        DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Failed to create pod " + name + " -- unable to find zone " + zoneId);
        }

        if (endIp != null && startIp == null) {
            throw new InvalidParameterValueException("Failed to create pod " + name + " -- if an end IP is specified, a start IP must be specified.");
        }

        Long userId = UserContext.current().getUserId();
        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        return createPod(userId.longValue(), name, zoneId, gateway, cidr, startIp, endIp);
    }

    @Override @DB
    public HostPodVO createPod(long userId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp)  {
    	checkPodAttributes(-1, podName, zoneId, gateway, cidr, startIp, endIp, true);
		
		String cidrAddress = getCidrAddress(cidr);
		int cidrSize = getCidrSize(cidr);
		
		if (startIp != null) {
			if (endIp == null) {
				endIp = NetUtils.getIpRangeEndIpFromCidr(cidrAddress, cidrSize);
			}
		}
		
		// Create the new pod in the database
		String ipRange;
		if (startIp != null) {
			ipRange = startIp + "-";
			if (endIp != null) {
				ipRange += endIp;
			}
		} else {
			ipRange = "";
		}
		
		HostPodVO pod = new HostPodVO(podName, zoneId, gateway, cidrAddress, cidrSize, ipRange);
		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			
			if (_podDao.persist(pod) == null) {
				txn.rollback();
				throw new CloudRuntimeException("Failed to create new pod. Please contact Cloud Support.");
			}
			
			if (startIp != null) {
				_zoneDao.addPrivateIpAddress(zoneId, pod.getId(), startIp, endIp);
			}
			
			String[] linkLocalIpRanges = getLinkLocalIPRange();
			if (linkLocalIpRanges != null) {
				_zoneDao.addLinkLocalIpAddress(zoneId, pod.getId(), linkLocalIpRanges[0], linkLocalIpRanges[1]);
			}

			txn.commit();

		} catch(Exception e) {
			txn.rollback();
			s_logger.error("Unable to create new pod due to " + e.getMessage(), e);
			throw new CloudRuntimeException("Failed to create new pod. Please contact Cloud Support.");
		}
		
		DataCenterVO zone = _zoneDao.findById(zoneId);
		saveConfigurationEvent(userId, null, EventTypes.EVENT_POD_CREATE, "Successfully created new pod with name: " + podName + " in zone: " + zone.getName() + ".", "podId=" + pod.getId(), "zoneId=" + zone.getId(), "gateway=" + gateway, "cidr=" + cidr, "startIp=" + startIp, "endIp=" + endIp);
		
		return pod;
    }
    
    private boolean zoneHasVMs(long zoneId) {
    	List<VMInstanceVO> vmInstances = _vmInstanceDao.listByZoneId(zoneId);
    	return !vmInstances.isEmpty();
    }
    
    private boolean zoneHasAllocatedVnets(long zoneId)  {
    	return !_zoneDao.listAllocatedVnets(zoneId).isEmpty();
    }
    
    @DB
    protected void checkIfZoneIsDeletable(long zoneId)  {
    	List<List<String>> tablesToCheck = new ArrayList<List<String>>();
    	
    	List<String> alert = new ArrayList<String>();
		alert.add(0, "alert");
		alert.add(1, "data_center_id");
		alert.add(2, "there are alerts for this zone");
		tablesToCheck.add(alert);
    	
    	List<String> host = new ArrayList<String>();
    	host.add(0, "host");
    	host.add(1, "data_center_id");
    	host.add(2, "there are servers running in this zone");
    	tablesToCheck.add(host);
    	
    	List<String> hostPodRef = new ArrayList<String>();
		hostPodRef.add(0, "host_pod_ref");
		hostPodRef.add(1, "data_center_id");
		hostPodRef.add(2, "there are pods in this zone");
		tablesToCheck.add(hostPodRef);
    	
    	List<String> privateIP = new ArrayList<String>();
    	privateIP.add(0, "op_dc_ip_address_alloc");
    	privateIP.add(1, "data_center_id");
    	privateIP.add(2, "there are private IP addresses allocated for this zone");
    	tablesToCheck.add(privateIP);
    	
    	List<String> publicIP = new ArrayList<String>();
    	publicIP.add(0, "user_ip_address");
    	publicIP.add(1, "data_center_id");
    	publicIP.add(2, "there are public IP addresses allocated for this zone");
    	tablesToCheck.add(publicIP);
    	
    	List<String> vmInstance = new ArrayList<String>();
    	vmInstance.add(0, "vm_instance");
    	vmInstance.add(1, "data_center_id");
    	vmInstance.add(2, "there are virtual machines running in this zone");
    	tablesToCheck.add(vmInstance);
    	
    	List<String> volumes = new ArrayList<String>();
    	volumes.add(0, "volumes");
    	volumes.add(1, "data_center_id");
    	volumes.add(2, "there are storage volumes for this zone");
    	tablesToCheck.add(volumes);
    	
    	List<String> vnet = new ArrayList<String>();
    	vnet.add(0, "op_dc_vnet_alloc");
    	vnet.add(1, "data_center_id");
    	vnet.add(2, "there are allocated vnets for this zone");
    	tablesToCheck.add(vnet);

    	for (List<String> table : tablesToCheck) {
    		String tableName = table.get(0);
    		String column = table.get(1);
    		String errorMsg = table.get(2);
    		
    		String dbName = "cloud";
    		
    		String selectSql = "SELECT * FROM `" + dbName + "`.`" + tableName + "` WHERE " + column + " = ?";
    		
    		if (tableName.equals("op_dc_vnet_alloc")) {
    			selectSql += " AND taken IS NOT NULL";
    		}
    		
            Transaction txn = Transaction.currentTxn();
    		try {
                PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
                stmt.setLong(1, zoneId);
                ResultSet rs = stmt.executeQuery();
                if (rs != null && rs.next()) {
                	throw new CloudRuntimeException("The zone is not deletable because " + errorMsg);
                }
            } catch (SQLException ex) {
            	throw new CloudRuntimeException("The Management Server failed to detect if zone is deletable. Please contact Cloud Support.");
            }
    	}
    
    }
    
    private void checkZoneParameters(String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, boolean checkForDuplicates, Long domainId) throws InvalidParameterValueException {
    	if (checkForDuplicates) {
    		// Check if a zone with the specified name already exists
    		if (validZone(zoneName)) {
    			throw new InvalidParameterValueException("A zone with that name already exists. Please specify a unique zone name.");
    		}
    	}
    	
    	//check if valid domain
    	if(domainId != null){
    		DomainVO domain = _domainDao.findById(domainId);
    	
    		if(domain == null)
    			throw new InvalidParameterValueException("Please specify a valid domain id");
    	}
    	
    	// Check IP validity for DNS addresses
    	
		if (dns1 != null  && !NetUtils.isValidIp(dns1)) {
			throw new InvalidParameterValueException("Please enter a valid IP address for DNS1");
		}
		
		if (dns2 != null  && !NetUtils.isValidIp(dns2)) {
			throw new InvalidParameterValueException("Please enter a valid IP address for DNS2");
		}
		
		if (internalDns1 != null  && !NetUtils.isValidIp(internalDns1)) {
			throw new InvalidParameterValueException("Please enter a valid IP address for internal DNS1");
		}
		
		if (internalDns2 != null  && !NetUtils.isValidIp(internalDns2)) {
			throw new InvalidParameterValueException("Please enter a valid IP address for internal DNS2");
		}
    }
    
    private void checkIpRange(String startIp, String endIp, String cidrAddress, long cidrSize) throws InvalidParameterValueException {
    	if (!NetUtils.isValidIp(startIp)) {
    		throw new InvalidParameterValueException("The start address of the IP range is not a valid IP address.");
    	}
    	
    	if (endIp != null && !NetUtils.isValidIp(endIp)) {
    		throw new InvalidParameterValueException("The end address of the IP range is not a valid IP address.");
    	}
    	
    	if (!NetUtils.getCidrSubNet(startIp, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
    		throw new InvalidParameterValueException("The start address of the IP range is not in the CIDR subnet.");
    	}
    	
    	if (endIp != null && !NetUtils.getCidrSubNet(endIp, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
    		throw new InvalidParameterValueException("The end address of the IP range is not in the CIDR subnet.");
    	}
    	
    	if (endIp != null && NetUtils.ip2Long(startIp) > NetUtils.ip2Long(endIp)) {
			throw new InvalidParameterValueException("The start IP address must have a lower value than the end IP address.");
		}
    	
    }
    
    @Override
    @DB
    public boolean deleteZone(DeleteZoneCmd cmd) {
    	
    	Long userId = UserContext.current().getUserId();
    	Long zoneId = cmd.getId();
    		
    	if (userId == null) {
    		userId = Long.valueOf(User.UID_SYSTEM);
		}
    	
    	// Make sure the zone exists
    	if (!validZone(zoneId)) {
    		throw new InvalidParameterValueException("A zone with ID: " + zoneId + " does not exist.");
    	}
    	
    	checkIfZoneIsDeletable(zoneId);
    	
    	DataCenterVO zone = _zoneDao.findById(zoneId);
    	
    	boolean success = _zoneDao.expunge(zoneId);
    	
    	try {
    	    // Delete vNet
            _zoneDao.deleteVnet(zoneId);
    	} catch (Exception ex) {
    	    s_logger.error("Failed to delete zone " + zoneId);
    	    throw new CloudRuntimeException("Failed to delete zone " + zoneId);
    	}
    	
        if (success){
            saveConfigurationEvent(userId, null, EventTypes.EVENT_ZONE_DELETE, "Successfully deleted zone with name: " + zone.getName() + ".", "dcId=" + zoneId);
            return true;
        } else{
            return false;
        }
            
    }
    
    @Override
    public DataCenter editZone(UpdateZoneCmd cmd) {
    	//Parameter validation as from execute() method in V1
    	Long zoneId = cmd.getId();
    	String zoneName = cmd.getZoneName();
    	String dns1 = cmd.getDns1();
    	String dns2 = cmd.getDns2();
    	String internalDns1 = cmd.getInternalDns1();
    	String internalDns2 = cmd.getInternalDns2();
    	String vnetRange = cmd.getVlan();
    	String guestCidr = cmd.getGuestCidrAddress();
//    	String domain = cmd.getDomain();
    	Long userId = UserContext.current().getUserId();
//    	Long domainId = cmd.getDomainId();
    	
    	if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

    	DataCenterVO zone = _zoneDao.findById(zoneId);
    	if (zone == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find zone by id " + zoneId);
    	}
    	
    	if (zoneName == null) {
    		zoneName = zone.getName();
    	}
    	
    	//if zone is of Basic type, don't allow to add vnet range
    	if (vnetRange != null && zone.getNetworkType() == DataCenterNetworkType.Basic) {
    	    throw new InvalidParameterValueException("Can't add vnet range for the zone that supports Basic network");
    	}

    	// Make sure the zone exists
    	if (!validZone(zoneId)) {
    		throw new InvalidParameterValueException("A zone with ID: " + zoneId + " does not exist.");
    	}

    	// If the Vnet range is being changed, make sure there are no allocated VNets
    	if (vnetRange != null) {
    		if (zoneHasAllocatedVnets(zoneId)) {
    			throw new CloudRuntimeException("The vlan range is not editable because there are allocated vlans.");
    		}
    	}

    	String oldZoneName = zone.getName();
    	
    	if (zoneName == null) {
    		zoneName = oldZoneName;
    	}
    	
    	boolean dnsUpdate = false;
    	
    	if(dns1 != null || dns2 != null){
    	    dnsUpdate = true;
    	}
    	
    	if (dns1 == null) {
    		dns1 = zone.getDns1();
    	}
    	
    	if (dns2 == null) {
    	    dns2 = zone.getDns2();
    	}
    	
    	if(internalDns1 == null)
    	{
    		internalDns1 = zone.getInternalDns1();
    	}

    	if(guestCidr == null)
    		guestCidr = zone.getGuestNetworkCidr();    	
    	
//    	if(domain == null)
//    	    domain = zone.getDomain();
    	
    	boolean checkForDuplicates = !zoneName.equals(oldZoneName);
    	checkZoneParameters(zoneName, dns1, dns2, internalDns1, internalDns2, checkForDuplicates, null);//not allowing updating domain associated with a zone, once created

    	zone.setName(zoneName);
    	zone.setDns1(dns1);
    	zone.setDns2(dns2);
    	zone.setInternalDns1(internalDns1);
    	zone.setInternalDns2(internalDns2);
    	zone.setGuestNetworkCidr(guestCidr);
//    	zone.setDomain(domain);
    	
    	if (vnetRange != null) {
    		zone.setVnet(vnetRange);
    	}
    	
    	if (!_zoneDao.update(zoneId, zone)) {
    		throw new CloudRuntimeException("Failed to edit zone. Please contact Cloud Support.");
    	}
    	
    	if (vnetRange != null) {
    		String[] tokens = vnetRange.split("-");
	    	int begin = Integer.parseInt(tokens[0]);
	    	int end = tokens.length == 1 ? (begin) : Integer.parseInt(tokens[1]);
	    	
	    	_zoneDao.deleteVnet(zoneId);
	    	_zoneDao.addVnet(zone.getId(), begin, end);
    	}
    	
    	if(dnsUpdate){
    	    
    	    //Update dns for domRs in zone
    	    
    	    List<DomainRouterVO> DomainRouters = _domrDao.listByDataCenter(zoneId);

    	    for(DomainRouterVO domR : DomainRouters)
    	    {
    	        domR.setDns1(dns1);
    	        domR.setDns2(dns2);
    	        _domrDao.update(domR.getId(), domR);
    	    }
    	    
    	  //Update dns for console proxies in zone
            List<VMInstanceVO> ConsoleProxies = _vmInstanceDao.listByZoneIdAndType(zoneId, VirtualMachine.Type.ConsoleProxy);
            
            for(VMInstanceVO consoleVm : ConsoleProxies)
            {
                ConsoleProxyVO proxy = _consoleDao.findById(consoleVm.getId());
                if( proxy!= null ){
                    proxy.setDns1(dns1);
                    proxy.setDns2(dns2);
                    _consoleDao.update(proxy.getId(), proxy);
                }
            }    	    
            
          //Update dns for secondary storage Vms in zone
            List<VMInstanceVO> storageVms = _vmInstanceDao.listByZoneIdAndType(zoneId, VirtualMachine.Type.SecondaryStorageVm);
            
            for(VMInstanceVO storageVm : storageVms)
            {
                SecondaryStorageVmVO secStorageVm = _secStorageDao.findById(storageVm.getId());
                if( secStorageVm!= null ){
                    secStorageVm.setDns1(dns1);
                    secStorageVm.setDns2(dns2);
                    _secStorageDao.update(secStorageVm.getId(), secStorageVm);
                }
            }           
            
    	}

    	
    	saveConfigurationEvent(userId, null, EventTypes.EVENT_ZONE_EDIT, "Successfully edited zone with name: " + zone.getName() + ".", "dcId=" + zone.getId(), "dns1=" + dns1, "dns2=" + dns2, "internalDns1=" + internalDns1, "internalDns2=" + internalDns2, "vnetRange=" + vnetRange, "guestCidr=" + guestCidr);
    	
    	return zone;
    }

    @Override @DB
    public DataCenterVO createZone(long userId, String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, String vnetRange, String guestCidr, String domain, Long domainId, DataCenterNetworkType zoneType)  {
        int vnetStart = 0;
        int vnetEnd = 0;
        if (vnetRange != null) {
            String[] tokens = vnetRange.split("-");
            try {
            	vnetStart = Integer.parseInt(tokens[0]);
            	if (tokens.length == 1) {
            		vnetEnd = vnetStart;
            	} else {
            		vnetEnd = Integer.parseInt(tokens[1]);
            	}
            } catch (NumberFormatException e) {
                throw new InvalidParameterValueException("Please specify valid integers for the vlan range.");
            }
        } 
        
        //checking the following params outside checkzoneparams method as we do not use these params for updatezone
        //hence the method below is generic to check for common params
        if ((guestCidr != null) && !NetUtils.isValidCIDR(guestCidr)) {
            throw new InvalidParameterValueException("Please enter a valid guest cidr");
        }

        checkZoneParameters(zoneName, dns1, dns2, internalDns1, internalDns2, true, domainId);

        // Create the new zone in the database
        DataCenterVO zone = new DataCenterVO(zoneName, null, dns1, dns2, internalDns1, internalDns2, vnetRange, guestCidr, domain, domainId, zoneType);
        zone = _zoneDao.persist(zone);

        // Add vnet entries for the new zone if zone type is Advanced
        if (vnetRange != null) {
            _zoneDao.addVnet(zone.getId(), vnetStart, vnetEnd);
        }
        
        if (vnetRange != null) {
            saveConfigurationEvent(userId, null, EventTypes.EVENT_ZONE_CREATE, "Successfully created new zone with name: " + zoneName + ".", "dcId=" + zone.getId(), "dns1=" + dns1, "dns2=" + dns2, "internalDns1=" + internalDns1, "internalDns2=" + internalDns2, "vnetRange=" + vnetRange, "guestCidr=" + guestCidr);
        } else {
            saveConfigurationEvent(userId, null, EventTypes.EVENT_ZONE_CREATE, "Successfully created new zone with name: " + zoneName + ".", "dcId=" + zone.getId(), "dns1=" + dns1, "dns2=" + dns2, "internalDns1=" + internalDns1, "internalDns2=" + internalDns2, "guestCidr=" + guestCidr);
        }
        return zone;
    }

    @Override
    public DataCenter createZone(CreateZoneCmd cmd)  {
        // grab parameters from the command
        Long userId = UserContext.current().getUserId();
        String zoneName = cmd.getZoneName();
        String dns1 = cmd.getDns1();
        String dns2 = cmd.getDns2();
        String internalDns1 = cmd.getInternalDns1();
        String internalDns2 = cmd.getInternalDns2();
        String vnetRange = cmd.getVlan();
        String guestCidr = cmd.getGuestCidrAddress();
        String domain = cmd.getDomain();//we are not passing domain right now, always null
        Long domainId = cmd.getDomainId();
        String type = cmd.getNetworkType();
        Boolean isBasic = false;

        
        if (!(type.equalsIgnoreCase(DataCenterNetworkType.Basic.toString())) && !(type.equalsIgnoreCase(DataCenterNetworkType.Advanced.toString()))) {
            throw new InvalidParameterValueException("Invalid zone type; only Advanced and Basic values are supported");
        } else if (type.endsWith(DataCenterNetworkType.Basic.toString())) {
            isBasic = true;
        }
        
        DataCenterNetworkType zoneType = isBasic ? DataCenterNetworkType.Basic : DataCenterNetworkType.Advanced;
        DomainVO domainVO = null;
        
        if (userId == null) {
            userId = User.UID_SYSTEM;
        }

        if(domainId != null){
        	domainVO = _domainDao.findById(domainId); 
        }
        
        //Verify zone type 
        if (zoneType == DataCenterNetworkType.Basic && vnetRange != null) {
            vnetRange = null;
        }
        return createZone(userId, zoneName, dns1, dns2, internalDns1, internalDns2, vnetRange, guestCidr, domainVO != null ? domainVO.getName() : null, domainId, zoneType);
    }

    @Override
    public ServiceOffering createServiceOffering(CreateServiceOfferingCmd cmd) throws InvalidParameterValueException {
        Long userId = UserContext.current().getUserId();
        if (userId == null) {
            userId = User.UID_SYSTEM;
        }

        String name = cmd.getServiceOfferingName();
        if ((name == null) || (name.length() == 0)) {
            throw new InvalidParameterValueException("Failed to create service offering: specify the name that has non-zero length");
        }

        String displayText = cmd.getDisplayText();
        if ((displayText == null) || (displayText.length() == 0)) {
            throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the display text that has non-zero length");
        }

        Long cpuNumber = cmd.getCpuNumber();
        if ((cpuNumber == null) || (cpuNumber.intValue() <= 0) || (cpuNumber.intValue() > 2147483647)) {
            throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the cpu number value between 1 and 2147483647");
        }

        Long cpuSpeed = cmd.getCpuSpeed();
        if ((cpuSpeed == null) || (cpuSpeed.intValue() <= 0) || (cpuSpeed.intValue() > 2147483647)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to create service offering " + name + ": specify the cpu speed value between 1 and 2147483647");
        }

        Long memory = cmd.getMemory();
        if ((memory == null) || (memory.intValue() <= 0) || (memory.intValue() > 2147483647)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to create service offering " + name + ": specify the memory value between 1 and 2147483647");
        }

        boolean localStorageRequired = false;
        String storageType = cmd.getStorageType();
        if (storageType == null) {
            localStorageRequired = false;
        } else if (storageType.equals("local")) {
            localStorageRequired = true;
        } else if (storageType.equals("shared")) {
            localStorageRequired = false;
        } else {
            throw new InvalidParameterValueException("Invalid storage type " + storageType + " specified, valid types are: 'local' and 'shared'");
        }

        Boolean offerHA = cmd.getOfferHa();
        if (offerHA == null) {
            offerHA = false;
        }

        Boolean useVirtualNetwork = cmd.getUseVirtualNetwork();
        if (useVirtualNetwork == null) {
            useVirtualNetwork = Boolean.TRUE;
        }

        return createServiceOffering(userId, cmd.getServiceOfferingName(), cpuNumber.intValue(), memory.intValue(), cpuSpeed.intValue(), cmd.getDisplayText(),
                localStorageRequired, offerHA, useVirtualNetwork, cmd.getTags());
    }

    @Override
    public ServiceOfferingVO createServiceOffering(long userId, String name, int cpu, int ramSize, int speed, String displayText, boolean localStorageRequired, boolean offerHA, boolean useVirtualNetwork, String tags) {
    	String networkRateStr = _configDao.getValue("network.throttling.rate");
    	String multicastRateStr = _configDao.getValue("multicast.throttling.rate");
    	int networkRate = ((networkRateStr == null) ? 200 : Integer.parseInt(networkRateStr));
    	int multicastRate = ((multicastRateStr == null) ? 10 : Integer.parseInt(multicastRateStr));
    	NetworkOffering.GuestIpType guestIpType = useVirtualNetwork ? NetworkOffering.GuestIpType.Virtualized : NetworkOffering.GuestIpType.DirectSingle;        
    	tags = cleanupTags(tags);
    	ServiceOfferingVO offering = new ServiceOfferingVO(name, cpu, ramSize, speed, networkRate, multicastRate, offerHA, displayText, guestIpType, localStorageRequired, false, tags, false);
    	
    	if ((offering = _serviceOfferingDao.persist(offering)) != null) {
    		saveConfigurationEvent(userId, null, EventTypes.EVENT_SERVICE_OFFERING_CREATE, "Successfully created new service offering with name: " + name + ".", "soId=" + offering.getId(), "name=" + name, "numCPUs=" + cpu, "ram=" + ramSize, "cpuSpeed=" + speed,
    				"displayText=" + displayText, "guestIPType=" + guestIpType, "localStorageRequired=" + localStorageRequired, "offerHA=" + offerHA, "useVirtualNetwork=" + useVirtualNetwork, "tags=" + tags);
    		return offering;
    	} else {
    		return null;
    	}
    }

    @Override
    public ServiceOffering updateServiceOffering(UpdateServiceOfferingCmd cmd) {
    	String displayText = cmd.getDisplayText();
    	Long id = cmd.getId();
    	String name = cmd.getServiceOfferingName();
    	Boolean ha = cmd.getOfferHa();
//    	String tags = cmd.getTags();
    	Boolean useVirtualNetwork = cmd.getUseVirtualNetwork();
    	Long userId = UserContext.current().getUserId();

        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
        
        // Verify input parameters
        ServiceOfferingVO offeringHandle = _serviceOfferingDao.findById(id);;
    	if (offeringHandle == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find service offering " + id);
    	}
    	
    	boolean updateNeeded = (name != null || displayText != null || ha != null || useVirtualNetwork != null);
    	if (!updateNeeded) {
    		return _serviceOfferingDao.findById(id);
    	}
    	
        ServiceOfferingVO offering = _serviceOfferingDao.createForUpdate(id);
        
        if (name != null) {
        	offering.setName(name);
        }
        
        if (displayText != null) {
        	offering.setDisplayText(displayText);
        }
        
	    if (ha != null) {
	    	offering.setOfferHA(ha);
        }
	    
        if (useVirtualNetwork != null) {
        	NetworkOffering.GuestIpType guestIpType = useVirtualNetwork ? NetworkOffering.GuestIpType.Virtualized : NetworkOffering.GuestIpType.DirectSingle;
            offering.setGuestIpType(guestIpType);
        }

//        if (tags != null) 
//        {
//        	if (tags.trim().isEmpty() && offeringHandle.getTags() == null) 
//        	{
//        		//no new tags; no existing tags
//        		offering.setTagsArray(csvTagsToList(null));
//        	} 
//        	else if (!tags.trim().isEmpty() && offeringHandle.getTags() != null)
//        	{
//        		//new tags + existing tags
//        		List<String> oldTags = csvTagsToList(offeringHandle.getTags());
//        		List<String> newTags = csvTagsToList(tags);
//        		oldTags.addAll(newTags);
//        		offering.setTagsArray(oldTags);
//        	}
//        	else if(!tags.trim().isEmpty())
//        	{
//        		//new tags; NO existing tags
//        		offering.setTagsArray(csvTagsToList(tags));
//        	}     	
//        }
        
        if (_serviceOfferingDao.update(id, offering)) {
        	offering = _serviceOfferingDao.findById(id);
    		saveConfigurationEvent(userId, null, EventTypes.EVENT_SERVICE_OFFERING_EDIT, "Successfully updated service offering with name: " + offering.getName() + ".", "soId=" + offering.getId(), "name=" + offering.getName(),
    				"displayText=" + offering.getDisplayText(), "offerHA=" + offering.getOfferHA(), "useVirtualNetwork=" + (offering.getGuestIpType() == NetworkOffering.GuestIpType.Virtualized), "tags=" + offering.getTags());
        	return offering;
        } else {
        	return null;
        }
    }

    @Override
    public DiskOfferingVO createDiskOffering(long domainId, String name, String description, Long numGibibytes, String tags, Boolean isCustomized) throws InvalidParameterValueException {
        long diskSize = 0;//special case for custom disk offerings
    	if (numGibibytes != null && (numGibibytes <= 0)) {
            throw new InvalidParameterValueException("Please specify a disk size of at least 1 Gb.");
        } else if (numGibibytes != null && (numGibibytes > _maxVolumeSizeInGb)) {
            throw new InvalidParameterValueException("The maximum size for a disk is " + _maxVolumeSizeInGb + " Gb.");
        }

    	if(numGibibytes != null){
    		diskSize = numGibibytes * 1024;
    	}
    	
    	if(diskSize == 0){
    		isCustomized = true;
    	}
    	
        tags = cleanupTags(tags);
        DiskOfferingVO newDiskOffering = new DiskOfferingVO(domainId, name, description, diskSize,tags, isCustomized);
        return _diskOfferingDao.persist(newDiskOffering);
    }

    @Override
    public DiskOffering createDiskOffering(CreateDiskOfferingCmd cmd) throws InvalidParameterValueException {
        String name = cmd.getOfferingName();
        String description = cmd.getDisplayText();
        Long numGibibytes = cmd.getDiskSize();
        Boolean isCustomized = cmd.isCustomized() != null ? cmd.isCustomized() : false; //false by default
        String tags = cmd.getTags();        
        Long domainId = Long.valueOf(DomainVO.ROOT_DOMAIN); // disk offering always gets created under the root domain.Bug # 6055        

        if(!isCustomized && numGibibytes == null){
        	throw new InvalidParameterValueException("Disksize is required for non-customized disk offering");
        }
        
        return createDiskOffering(domainId, name, description, numGibibytes, tags, isCustomized);
    }

    @Override
    public DiskOffering updateDiskOffering(UpdateDiskOfferingCmd cmd) throws InvalidParameterValueException{
    	Long diskOfferingId = cmd.getId();
    	String name = cmd.getDiskOfferingName();
    	String displayText = cmd.getDisplayText();
//    	String tags = cmd.getTags();
    	
    	//Check if diskOffering exists
    	DiskOfferingVO diskOfferingHandle = _diskOfferingDao.findById(diskOfferingId);
    	
    	if (diskOfferingHandle == null) {
    		throw new InvalidParameterValueException("Unable to find disk offering by id " + diskOfferingId);
    	}
    	
    	boolean updateNeeded = (name != null || displayText != null);
    	if (!updateNeeded) {
    		return _diskOfferingDao.findById(diskOfferingId);
    	}
    	
    	DiskOfferingVO diskOffering = _diskOfferingDao.createForUpdate(diskOfferingId);
    	
    	if (name != null) {
    		diskOffering.setName(name);
    	}
    	
    	if (displayText != null) {
    		diskOffering.setDisplayText(displayText);
    	}
    	
//        if (tags != null) 
//        {
//        	if (tags.trim().isEmpty() && diskOfferingHandle.getTags() == null) 
//        	{
//        		//no new tags; no existing tags
//        		diskOffering.setTagsArray(csvTagsToList(null));
//        	} 
//        	else if (!tags.trim().isEmpty() && diskOfferingHandle.getTags() != null)
//        	{
//        		//new tags + existing tags
//        		List<String> oldTags = csvTagsToList(diskOfferingHandle.getTags());
//        		List<String> newTags = csvTagsToList(tags);
//        		oldTags.addAll(newTags);
//        		diskOffering.setTagsArray(oldTags);
//        	}
//        	else if(!tags.trim().isEmpty())
//        	{
//        		//new tags; NO existing tags
//        		diskOffering.setTagsArray(csvTagsToList(tags));
//        	}
//        }

    	if (_diskOfferingDao.update(diskOfferingId, diskOffering)) {
            saveConfigurationEvent(UserContext.current().getUserId(), null, EventTypes.EVENT_DISK_OFFERING_EDIT, "Successfully updated disk offering with name: " + diskOffering.getName() + ".", "doId=" + diskOffering.getId(), "name=" + diskOffering.getName(),
                    "displayText=" + diskOffering.getDisplayText(), "diskSize=" + diskOffering.getDiskSize(),"tags=" + diskOffering.getTags());
    		return _diskOfferingDao.findById(diskOfferingId);
    	} else { 
    		return null;
    	}
    }

    @Override
    public boolean deleteDiskOffering(DeleteDiskOfferingCmd cmd) throws InvalidParameterValueException{
    	Long diskOfferingId = cmd.getId();
    	
    	DiskOfferingVO offering = _diskOfferingDao.findById(diskOfferingId);
    	
    	if (offering == null) {
    		throw new InvalidParameterValueException("Unable to find disk offering by id " + diskOfferingId);
    	}
    	
    	if (_diskOfferingDao.remove(diskOfferingId)) {
    		return true;
    	} else {
    		return false;
    	}
    }

    @Override
    public boolean deleteServiceOffering(DeleteServiceOfferingCmd cmd) throws InvalidParameterValueException{
    	
        Long offeringId = cmd.getId();
        Long userId = UserContext.current().getUserId();
        
        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
 
        //Verify service offering id
        ServiceOfferingVO offering = _serviceOfferingDao.findById(offeringId);
    	if (offering == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find service offering " + offeringId);
    	} else if (offering.getRemoved() != null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find service offering " + offeringId);
    	}
    	
    	if (_serviceOfferingDao.remove(offeringId)) {
    		saveConfigurationEvent(userId, null, EventTypes.EVENT_SERVICE_OFFERING_EDIT, "Successfully deleted service offering with name: " + offering.getName(), "soId=" + offeringId, "name=" + offering.getName(),
    				"displayText=" + offering.getDisplayText(), "offerHA=" + offering.getOfferHA(), "useVirtualNetwork=" + (offering.getGuestIpType() == GuestIpType.Virtualized));
    		return true;
    	} else {
    		return false;
    	}
    }

    @Override
    public String changePrivateIPRange(boolean add, long podId, String startIP, String endIP) throws InvalidParameterValueException {
    	checkPrivateIpRangeErrors(podId, startIP, endIP);
    	
		long zoneId = _podDao.findById(podId).getDataCenterId();
		List<String> problemIPs = null;
		if (add) {
			problemIPs = savePrivateIPRange(startIP, endIP, podId, zoneId);
		} else {
			problemIPs = deletePrivateIPRange(startIP, endIP, podId, zoneId);
		}
		
		if (problemIPs == null) {
			throw new InvalidParameterValueException ("Failed to change private IP range. Please contact Cloud Support.");
		} else {
			return genChangeRangeSuccessString(problemIPs, add);
		}
    }

    @Override
    public Vlan createVlanAndPublicIpRange(CreateVlanIpRangeCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException {
        Long zoneId = cmd.getZoneId();
        Long podId = cmd.getPodId();
        String startIP = cmd.getStartIp();
        String endIP = cmd.getEndIp();
        String vlanGateway = cmd.getGateway();
        String vlanNetmask = cmd.getNetmask();
        Long userId = UserContext.current().getUserId();
        
        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
        
     // Check that the pod ID is valid
        if (podId != null && ((_podDao.findById(podId)) == null)) {
            throw new InvalidParameterValueException("Please specify a valid pod.");
        }

        
        if (podId != null && _podDao.findById(podId).getDataCenterId() != zoneId) {
            throw new InvalidParameterValueException("Pod id=" + podId + " doesn't belong to zone id=" + zoneId);
        }

        // If forVirtualNetworks isn't specified, default it to true
        Boolean forVirtualNetwork = cmd.isForVirtualNetwork();
        if (forVirtualNetwork == null) {
            forVirtualNetwork = Boolean.TRUE;
        }

        // If the VLAN id is null, default it to untagged
        String vlanId = cmd.getVlan();
        if (vlanId == null) {
            vlanId = Vlan.UNTAGGED;
        }
        
        DataCenterVO zone;
        if (zoneId == null || ((zone = _zoneDao.findById(zoneId)) == null)) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }
        
        //Allow adding untagged direct vlan only for Basic zone
        if (zone.getNetworkType() == DataCenterNetworkType.Advanced && vlanId.equals(Vlan.UNTAGGED) && !forVirtualNetwork) {
            throw new InvalidParameterValueException("Direct untagged network is not supported for the zone " + zone.getId() + " of type " + zone.getNetworkType());
        } else if (zone.getNetworkType() == DataCenterNetworkType.Basic && !(vlanId.equals(Vlan.UNTAGGED) && !forVirtualNetwork)) {
            throw new InvalidParameterValueException("Only direct untagged network is supported in the zone " + zone.getId() + " of type " + zone.getNetworkType());
        }
        
        //don't allow to create a virtual vlan when zone's vnet is NULL
        if (zone.getVnet() == null && forVirtualNetwork) {
            throw new InvalidParameterValueException("Can't add virtual network to the zone id=" + zone.getId() + " as zone doesn't have guest vlan configured");
        }

        // If an account name and domain ID are specified, look up the account
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Account account = null;
        if ((accountName != null) && (domainId != null)) {
            account = _accountDao.findActiveAccount(accountName, domainId);
            if (account == null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid account.");
            }
        }       
        
        VlanType vlanType = forVirtualNetwork ? VlanType.VirtualNetwork : VlanType.DirectAttached;
        

        //check for hypervisor type to be xenserver
		String hypervisorType = _configDao.getValue("hypervisor.type");
				
		if(hypervisorType.equalsIgnoreCase("xenserver")) {
	    	//check for the vlan being added before going to db, to see if it is untagged
	    	if(vlanType.toString().equalsIgnoreCase("VirtualNetwork") && vlanId.equalsIgnoreCase("untagged"))
	    	{
	    		if(_configDao.getValue("xen.public.network.device") == null || _configDao.getValue("xen.public.network.device").equals(""))
	    		{
	    			throw new CloudRuntimeException("For adding an untagged IP range, please set up xen.public.network.device");
	    		}
	    	}
	    	
		}

        //if we have an ip range for vlan id=x, vlantype=y; we should
	    //only allow adding another range with id=x for same type y
        if (!vlanId.equals(Vlan.UNTAGGED)) {
            VlanVO vlanHandle = _vlanDao.findByZoneAndVlanId(zoneId, vlanId);

            if (vlanHandle!=null && !vlanHandle.getVlanType().equals(vlanType))
                throw new InvalidParameterValueException("This vlan id is already associated with the vlan type "+vlanHandle.getVlanType().toString()
                        +",whilst you are trying to associate it with vlan type "+vlanType.toString());
        }


    	
//    	//check if the account's domain is a child of the zone's domain, for adding vlan ip ranges
//		if(domainId != null && !_domainDao.isChildDomain(zone.getDomainId(), domainId)){
//			//this is for account specific case, as domainId != null
//			throw new PermissionDeniedException("The account associated with specific domain id:"+domainId+" doesn't have permissions to add vlan ip ranges for the zone:"+zone.getId());
//		}
        //ACL check
        checkAccess(account, zone);

    	boolean associateIpRangeToAccount = false;
    	if (vlanType.equals(VlanType.VirtualNetwork)) {
    	    if (account != null) {
    	        // verify resource limits
    	        long ipResourceLimit = _accountMgr.findCorrectResourceLimit((AccountVO)account, ResourceType.public_ip);
    	        long accountIpRange  = NetUtils.ip2Long(endIP) - NetUtils.ip2Long(startIP) + 1;
    	        if (s_logger.isDebugEnabled()) {
                    s_logger.debug(" IPResourceLimit " +ipResourceLimit + " accountIpRange " + accountIpRange);
    	        }
    	        if (ipResourceLimit != -1 && accountIpRange > ipResourceLimit){ // -1 means infinite
    	            throw new InvalidParameterValueException(" Public IP Resource Limit is set to " + ipResourceLimit + " which is less than the IP range of " + accountIpRange + " provided");
    	        }
    	        associateIpRangeToAccount = true;
    	    }
    	} else if (vlanType.equals(VlanType.DirectAttached)) {
//    		if (!((account != null && podId == null) ||
//    		      (account == null && podId != null))) {
//    			throw new InvalidParameterValueException("Direct Attached IP ranges must either be pod-wide, or for one account.");
//    		}

    		if (account != null) {
    			// VLANs for an account must be tagged
        		if (vlanId.equals(Vlan.UNTAGGED)) {
        			throw new InvalidParameterValueException("Direct Attached IP ranges for an account must be tagged.");
        		}

        		// Make sure there aren't any pod VLANs in this zone
        		List<HostPodVO> podsInZone = _podDao.listByDataCenterId(zone.getId());
        		for (HostPodVO pod : podsInZone) {
        			if (_podVlanMapDao.listPodVlanMapsByPod(pod.getId()).size() > 0) {
        				throw new InvalidParameterValueException("Zone " + zone.getName() + " already has pod-wide IP ranges. A zone may contain either pod-wide IP ranges or account-wide IP ranges, but not both.");
        			}
        		}
    		} else if (podId != null) {
    			// Pod-wide VLANs must be untagged
        		if (!vlanId.equals(Vlan.UNTAGGED)) {
        			throw new InvalidParameterValueException("Direct Attached IP ranges for a pod must be untagged.");
        		}

        		
        		// Make sure there aren't any account VLANs in this zone
        		List<AccountVlanMapVO> accountVlanMaps = _accountVlanMapDao.listAllIncludingRemoved();
        		for (AccountVlanMapVO accountVlanMap : accountVlanMaps) {
        			VlanVO vlan = _vlanDao.findById(accountVlanMap.getVlanDbId());
        			if (vlan.getDataCenterId() == zone.getId()) {
        				throw new InvalidParameterValueException("Zone " + zone.getName() + " already has account-wide IP ranges. A zone may contain either pod-wide IP ranges or account-wide IP ranges, but not both.");
        			}
        		}
    		}
    	} else {
    		throw new InvalidParameterValueException("Please specify a valid IP range type. Valid types are: " + VlanType.values().toString());
    	}

    	// Make sure the gateway is valid
		if (!NetUtils.isValidIp(vlanGateway)) {
			throw new InvalidParameterValueException("Please specify a valid gateway");
		}

		// Make sure the netmask is valid
		if (!NetUtils.isValidIp(vlanNetmask)) {
			throw new InvalidParameterValueException("Please specify a valid netmask");
		}

		String newVlanSubnet = NetUtils.getSubNet(vlanGateway, vlanNetmask);
		    	    		
		// Check if the new VLAN's subnet conflicts with the guest network in the specified zone
		String guestNetworkCidr = zone.getGuestNetworkCidr();
    	String[] cidrPair = guestNetworkCidr.split("\\/");
		String guestIpNetwork = NetUtils.getIpRangeStartIpFromCidr(cidrPair[0],Long.parseLong(cidrPair[1]));
		long guestCidrSize = Long.parseLong(cidrPair[1]);
		long vlanCidrSize = NetUtils.getCidrSize(vlanNetmask);

		long cidrSizeToUse = -1;
		if (vlanCidrSize < guestCidrSize) {
			cidrSizeToUse = vlanCidrSize;
		} else {
			cidrSizeToUse = guestCidrSize;
		}

		String guestSubnet = NetUtils.getCidrSubNet(guestIpNetwork, cidrSizeToUse);

		if (newVlanSubnet.equals(guestSubnet)) {
			throw new InvalidParameterValueException("The new IP range you have specified has the same subnet as the guest network in zone: " + zone.getName() + ". Please specify a different gateway/netmask.");
		}

		// Check if there are any errors with the IP range
		checkPublicIpRangeErrors(zoneId, vlanId, vlanGateway, vlanNetmask, startIP, endIP);

		// Throw an exception if any of the following is true:
		// 1. Another VLAN in the same zone has a different tag but the same subnet as the new VLAN
		// 2. Another VLAN in the same zone that has the same tag and subnet as the new VLAN has IPs that overlap with the IPs being added
		// 3. Another VLAN in the same zone that has the same tag and subnet as the new VLAN has a different gateway than the new VLAN
		List<VlanVO> vlans = _vlanDao.listByZone(zone.getId());
		for (VlanVO vlan : vlans) {
			String otherVlanGateway = vlan.getVlanGateway();
			String otherVlanSubnet = NetUtils.getSubNet(vlan.getVlanGateway(), vlan.getVlanNetmask());
			String[] otherVlanIpRange = vlan.getIpRange().split("\\-");
			String otherVlanStartIP = otherVlanIpRange[0];
			String otherVlanEndIP = null;
			if (otherVlanIpRange.length > 1) {
				otherVlanEndIP = otherVlanIpRange[1];
			}
			
			if (!vlanId.equals(vlan.getVlanId()) && newVlanSubnet.equals(otherVlanSubnet)) {
				throw new InvalidParameterValueException("The IP range with tag: " + vlan.getVlanId() + " in zone " + zone.getName() + " has the same subnet. Please specify a different gateway/netmask.");
			}
			
			if (vlanId.equals(vlan.getVlanId()) && newVlanSubnet.equals(otherVlanSubnet)) {
				if (NetUtils.ipRangesOverlap(startIP, endIP, otherVlanStartIP, otherVlanEndIP)) {
					throw new InvalidParameterValueException("The IP range with tag: " + vlan.getVlanId() + " already has IPs that overlap with the new range. Please specify a different start IP/end IP.");
				}
				
				if (!vlanGateway.equals(otherVlanGateway)) {
					throw new InvalidParameterValueException("The IP range with tag: " + vlan.getVlanId() + " has already been added with gateway " + otherVlanGateway + ". Please specify a different tag.");
				}
			}
		}
		
		// Check if a guest VLAN is using the same tag
		if (_zoneDao.findVnet(zoneId, vlanId).size() > 0) {
			throw new InvalidParameterValueException("The VLAN tag " + vlanId + " is already being used for the guest network in zone " + zone.getName());
		}
		
		// Everything was fine, so persist the VLAN
		String ipRange = startIP;
		if (endIP != null) {
			ipRange += "-" + endIP;
		}
		VlanVO vlan = new VlanVO(vlanType, vlanId, vlanGateway, vlanNetmask, zone.getId(), ipRange);
		vlan = _vlanDao.persist(vlan);
		
		// Persist the IP range
		if (account != null && vlanType.equals(VlanType.VirtualNetwork)){
			if(!savePublicIPRangeForAccount(startIP, endIP, zoneId, vlan.getId(), account.getId(), account.getDomainId())) {
				deletePublicIPRange(vlan.getId());
				_vlanDao.expunge(vlan.getId());
				throw new CloudRuntimeException("Failed to save IP range. Please contact Cloud Support."); //It can be Direct IP or Public IP.
			}				
		}else if (!savePublicIPRange(startIP, endIP, zoneId, vlan.getId())) {
			deletePublicIPRange(vlan.getId());
			_vlanDao.expunge(vlan.getId());
			throw new CloudRuntimeException("Failed to save IP range. Please contact Cloud Support."); //It can be Direct IP or Public IP.
		}
		
		if (account != null) {
			// This VLAN is account-specific, so create an AccountVlanMapVO entry
			AccountVlanMapVO accountVlanMapVO = new AccountVlanMapVO(account.getId(), vlan.getId());
			_accountVlanMapDao.persist(accountVlanMapVO);
		} else if (podId != null) {
			// This VLAN is pod-wide, so create a PodVlanMapVO entry
			PodVlanMapVO podVlanMapVO = new PodVlanMapVO(podId, vlan.getId());
			_podVlanMapDao.persist(podVlanMapVO);
		}
		
		String eventMsg = "Successfully created new IP range (tag = " + vlanId + ", gateway = " + vlanGateway + ", netmask = " + vlanNetmask + ", start IP = " + startIP;
		if (endIP != null) {
			eventMsg += ", end IP = " + endIP;
		}
		eventMsg += ".";
		Long accountId = ((account == null) ? Account.ACCOUNT_ID_SYSTEM : account.getId());
		saveConfigurationEvent(userId, accountId, EventTypes.EVENT_VLAN_IP_RANGE_CREATE, eventMsg, "vlanType=" + vlanType, "dcId=" + zoneId,
																												"accountId=" + accountId, "podId=" + podId,
																												"vlanId=" + vlanId, "vlanGateway=" + vlanGateway,
																												"vlanNetmask=" + vlanNetmask, "startIP=" + startIP,
																												"endIP=" + endIP);

		if (associateIpRangeToAccount) {
	        // if this is an account VLAN, now associate the IP Addresses to the account
	        associateIpAddressListToAccount(userId, account.getId(), zoneId, vlan.getId());
		}

		return vlan;
    }

    @Override @DB
    public void associateIpAddressListToAccount(long userId, long accountId, long zoneId, Long vlanId) throws InsufficientAddressCapacityException,
            ConcurrentOperationException {
        
        Transaction txn = Transaction.currentTxn();
        AccountVO account = null;
        
        try {
            //Acquire Lock                    
            account = _accountDao.acquireInLockTable(accountId);
            if (account == null) {
                s_logger.warn("Unable to lock account: " + accountId);
                throw new ConcurrentOperationException("Unable to acquire account lock");
            }            
            s_logger.debug("Associate IP address lock acquired");
            
            //Get Router
            DomainRouterVO router = _domrDao.findBy(accountId, zoneId);
            if (router == null) {
                s_logger.debug("No router found for account: " + account.getAccountName() + ".");
                return;
            }
            
            if (router.getState() == State.Running) {
                //Get Vlans associated with the account
                List<VlanVO> vlansForAccount = new ArrayList<VlanVO>();
                if (vlanId == null){
                    vlansForAccount.addAll(_vlanDao.listVlansForAccountByType(zoneId, account.getId(), VlanType.VirtualNetwork));
                    s_logger.debug("vlansForAccount "+ vlansForAccount);
                }else{
                    vlansForAccount.add(_vlanDao.findById(vlanId));
                }
                 
                // Creating a list of all the ips that can be assigned to this account
                txn.start();
                List<String> ipAddrsList = new ArrayList<String>();
                for (VlanVO vlan : vlansForAccount){
                    ipAddrsList.addAll(_publicIpAddressDao.assignAcccountSpecificIps(accountId, account.getDomainId(), vlan.getId(), false));

                    long size = ipAddrsList.size();
                    _accountMgr.incrementResourceCount(accountId, ResourceType.public_ip, size);
                    s_logger.debug("Assigning new ip addresses " +ipAddrsList);                 
                }
                if(ipAddrsList.isEmpty())
                    return;

                String params = "\nsourceNat=" + false + "\ndcId=" + zoneId;

                // Associate the IP's to DomR
                boolean success = _networkMgr.associateIP(router,ipAddrsList, true, 0);
                String errorMsg = "Unable to assign public IP address pool";
                if (!success) {
                    s_logger.debug(errorMsg);
                     for(String ip : ipAddrsList){
                         EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, EventTypes.EVENT_NET_IP_ASSIGN, "Unable to assign public IP " +ip, params);
                     }
                    throw new CloudRuntimeException(errorMsg);
                }
                txn.commit();
                for(String ip : ipAddrsList){
                    EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_INFO, EventTypes.EVENT_NET_IP_ASSIGN, "Successfully assigned account IP " +ip, params);
                }
            }
            } catch (CloudRuntimeException iee) {
                s_logger.error("Associate IP threw an CloudRuntimeException.", iee);
                throw iee;
            } catch (Throwable t) {
                s_logger.error("Associate IP address threw an exception.", t);
                throw new CloudRuntimeException("Associate IP address exception");
            } finally {
                if (account != null) {
                    _accountDao.releaseFromLockTable(accountId);
                    s_logger.debug("Associate IP address lock released");
                }
            }
    
    }
    
    @Override
    public boolean deleteVlanAndPublicIpRange(long userId, long vlanDbId) throws InvalidParameterValueException {
    	VlanVO vlan = _vlanDao.findById(vlanDbId);
    	if (vlan == null) {
    		throw new InvalidParameterValueException("Please specify a valid IP range id.");
    	}
    	
    	// Check if the VLAN has any allocated public IPs
    	if (_publicIpAddressDao.countIPs(vlan.getDataCenterId(), vlanDbId, true) > 0) {
    		throw new InvalidParameterValueException("The IP range can't be deleted because it has allocated public IP addresses.");
    	}
    	
    	// Check if the VLAN is being used by any domain router
    	if (_domrDao.listByVlanDbId(vlanDbId).size() > 0) {
    		throw new InvalidParameterValueException("The IP range can't be deleted because it is being used by a domain router.");
    	}
    	
    	Long accountId = null;
		Long podId = null;
		List<AccountVlanMapVO> accountVlanMaps = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanDbId);
		List<PodVlanMapVO> podVlanMaps = _podVlanMapDao.listPodVlanMapsByVlan(vlanDbId);
		
		if (accountVlanMaps.size() > 0) {
			accountId = accountVlanMaps.get(0).getAccountId();
		}
		
		if (podVlanMaps.size() > 0) {
			podId = podVlanMaps.get(0).getPodId();
		}

    	// Delete all public IPs in the VLAN
    	if (!deletePublicIPRange(vlanDbId)) {
    		return false;
    	}
    	
		// Delete the VLAN
		boolean success = _vlanDao.expunge(vlanDbId);
		
		if (success) {
			String[] ipRange = vlan.getIpRange().split("\\-");
			String startIP = ipRange[0];
			String endIP = (ipRange.length > 1) ? ipRange[1] : null;
			String eventMsg = "Successfully deleted IP range (tag = " + vlan.getVlanId() + ", gateway = " + vlan.getVlanGateway() + ", netmask = " + vlan.getVlanNetmask() + ", start IP = " + startIP;
			if (endIP != null) {
				eventMsg += ", end IP = " + endIP;
			}
			eventMsg += ".";
			saveConfigurationEvent(userId, null, EventTypes.EVENT_VLAN_IP_RANGE_DELETE, eventMsg, "vlanType=" + vlan.getVlanType(), "dcId=" + vlan.getDataCenterId(),
																												"accountId=" + accountId, "podId=" + podId,
																												"vlanId=" + vlan.getVlanId(), "vlanGateway=" + vlan.getVlanGateway(),
																												"vlanNetmask=" + vlan.getVlanNetmask(), "startIP=" + startIP,
																												"endIP=" + endIP);
		}
		
		return success;
    }
    
    @Override
    public List<String> csvTagsToList(String tags) {
    	List<String> tagsList = new ArrayList<String>();
    	
    	if (tags != null) {
            String[] tokens = tags.split(",");
            for (int i = 0; i < tokens.length; i++) {
                tagsList.add(tokens[i].trim());
            }
        }
    	
    	return tagsList;
    }
    
    @Override
    public String listToCsvTags(List<String> tagsList) {
    	String tags = "";
    	if (tagsList.size() > 0) {
    		for (int i = 0; i < tagsList.size(); i++) {
    			tags += tagsList.get(i);
    			if (i != tagsList.size() - 1) {
    				tags += ",";
    			}
    		}
    	} 
    	
    	return tags;
    }
    
    private String cleanupTags(String tags) {
    	if (tags != null) {
            String[] tokens = tags.split(",");
            StringBuilder t = new StringBuilder();
            for (int i = 0; i < tokens.length; i++) {
                t.append(tokens[i].trim()).append(",");
            }
            t.delete(t.length() - 1, t.length());
            tags = t.toString();
        }
    	
    	return tags;
    }
    	
	private boolean isPrivateIPAllocated(String ip, long podId, long zoneId, PreparedStatement stmt) {
		try {
			stmt.clearParameters();
        	stmt.setString(1, ip);
        	stmt.setLong(2, zoneId);
        	stmt.setLong(3, podId);
        	ResultSet rs = stmt.executeQuery();
        	if (rs.next()) return (rs.getString("taken") != null);
        	else return false;
        } catch (SQLException ex) {
        	System.out.println(ex.getMessage());
            return true;
        }
	}
	
	@DB
	protected boolean deletePublicIPRange(long vlanDbId) {
    	Transaction txn = Transaction.currentTxn();
		String deleteSql = "DELETE FROM `cloud`.`user_ip_address` WHERE vlan_db_id = ?";

		txn.start();
		try {
			PreparedStatement stmt = txn.prepareAutoCloseStatement(deleteSql);
			stmt.setLong(1, vlanDbId);
			stmt.executeUpdate();
		} catch (Exception ex) {
			return false;
		}
		txn.commit();
			
        return true;
	}
	
	@DB
	protected List<String> deletePrivateIPRange(String startIP, String endIP, long podId, long zoneId) {
		long startIPLong = NetUtils.ip2Long(startIP);
		long endIPLong = NetUtils.ip2Long(endIP);
		Transaction txn = Transaction.currentTxn();
		String deleteSql = "DELETE FROM `cloud`.`op_dc_ip_address_alloc` WHERE ip_address = ? AND pod_id = ? AND data_center_id = ?";
		String isPrivateIPAllocatedSelectSql = "SELECT * FROM `cloud`.`op_dc_ip_address_alloc` WHERE ip_address = ? AND data_center_id = ? AND pod_id = ?";
		
		List<String> problemIPs = new ArrayList<String>();
		PreparedStatement deleteIPStmt = null;
		PreparedStatement isAllocatedStmt = null;
				
		txn.start();
		try {
			deleteIPStmt = txn.prepareAutoCloseStatement(deleteSql);
			isAllocatedStmt = txn.prepareAutoCloseStatement(isPrivateIPAllocatedSelectSql);
		} catch (SQLException e) {
			return null;
		}
		
		while (startIPLong <= endIPLong) {
			if (!isPrivateIPAllocated(NetUtils.long2Ip(startIPLong), podId, zoneId, isAllocatedStmt)) {
				try {
					deleteIPStmt.clearParameters();
					deleteIPStmt.setString(1, NetUtils.long2Ip(startIPLong));
					deleteIPStmt.setLong(2, podId);
					deleteIPStmt.setLong(3, zoneId);
					deleteIPStmt.executeUpdate();
				} catch (Exception ex) {
				}
			} else {
				problemIPs.add(NetUtils.long2Ip(startIPLong));
			}
        	startIPLong += 1;
		}
		txn.commit();

        return problemIPs;
	}
    
	@DB
    protected boolean savePublicIPRange(String startIP, String endIP, long zoneId, long vlanDbId) {
    	long startIPLong = NetUtils.ip2Long(startIP);
    	long endIPLong = NetUtils.ip2Long(endIP);
    	Transaction txn = Transaction.currentTxn();
		String insertSql = "INSERT INTO `cloud`.`user_ip_address` (public_ip_address, data_center_id, vlan_db_id) VALUES (?, ?, ?)";
		
		txn.start();
		PreparedStatement stmt = null;
        while (startIPLong <= endIPLong) {
        	try {
        		stmt = txn.prepareAutoCloseStatement(insertSql);
        		stmt.setString(1, NetUtils.long2Ip(startIPLong));
        		stmt.setLong(2, zoneId);
        		stmt.setLong(3, vlanDbId);
        		stmt.executeUpdate();
        		stmt.close();
        	} catch (Exception ex) {
        		s_logger.debug("Exception saving public IP range: " + ex);
        		return false;
        	}
        	startIPLong += 1;
        }
        txn.commit();
        
        return true;
	}
	
	@DB
    protected boolean savePublicIPRangeForAccount(String startIP, String endIP, long zoneId, long vlanDbId, long accountId, long domainId) {
    	long startIPLong = NetUtils.ip2Long(startIP);
    	long endIPLong = NetUtils.ip2Long(endIP);
    	Transaction txn = Transaction.currentTxn();
		String insertSql = "INSERT INTO `cloud`.`user_ip_address` (public_ip_address, data_center_id, vlan_db_id, account_id, domain_id, allocated) VALUES (?, ?, ?, ?, ?, ?)";
		
		txn.start();
		PreparedStatement stmt = null;
        while (startIPLong <= endIPLong) {
        	try {
        		stmt = txn.prepareAutoCloseStatement(insertSql);
        		stmt.setString(1, NetUtils.long2Ip(startIPLong));
        		stmt.setLong(2, zoneId);
        		stmt.setLong(3, vlanDbId);
        		stmt.setLong(4, accountId);
        		stmt.setLong(5, domainId);
        		stmt.setDate(6,  new java.sql.Date(new java.util.Date().getTime()));
        		stmt.executeUpdate();
        		stmt.close();
        	} catch (Exception ex) {
        		s_logger.debug("Exception saving public IP range: " + ex);
        		return false;
        	}
        	startIPLong += 1;
        }
        txn.commit();
        
        return true;
	}
	
	@DB
	protected List<String> savePrivateIPRange(String startIP, String endIP, long podId, long zoneId) {
		long startIPLong = NetUtils.ip2Long(startIP);
		long endIPLong = NetUtils.ip2Long(endIP);
		Transaction txn = Transaction.currentTxn();
		String insertSql = "INSERT INTO `cloud`.`op_dc_ip_address_alloc` (ip_address, data_center_id, pod_id) VALUES (?, ?, ?)";
		List<String> problemIPs = new ArrayList<String>();
		
		txn.start();
		PreparedStatement stmt = null;
        while (startIPLong <= endIPLong) {
        	try {
        		stmt = txn.prepareAutoCloseStatement(insertSql);
        		stmt.setString(1, NetUtils.long2Ip(startIPLong));
        		stmt.setLong(2, zoneId);
        		stmt.setLong(3, podId);
        		stmt.executeUpdate();
        		stmt.close();
        	} catch (Exception ex) {
        		 problemIPs.add(NetUtils.long2Ip(startIPLong));
        	}
        	startIPLong += 1;
        }
        txn.commit();
        
        return problemIPs;
	}
    
	private String genChangeRangeSuccessString(List<String> problemIPs, boolean add) {
		if (problemIPs == null) return "";
		
		if (problemIPs.size() == 0) {
			if (add) return "Successfully added all IPs in the specified range.";
			else return "Successfully deleted all IPs in the specified range.";
		} else {
			String successString = "";
			if (add) successString += "Failed to add the following IPs, because they are already in the database: ";
			else  successString += "Failed to delete the following IPs, because they are in use: ";
			
			for (int i = 0; i < problemIPs.size(); i++) {
				successString += problemIPs.get(i);
				if (i != (problemIPs.size() - 1)) successString += ", ";
			}
			
			successString += ". ";
			
			if (add) successString += "Successfully added all other IPs in the specified range.";
			else successString += "Successfully deleted all other IPs in the specified range.";
			
			return successString;
		}
	}
	
	private void checkPublicIpRangeErrors(long zoneId, String vlanId, String vlanGateway, String vlanNetmask, String startIP, String endIP) throws InvalidParameterValueException {
		// Check that the start and end IPs are valid
		if (!NetUtils.isValidIp(startIP)) {
			throw new InvalidParameterValueException("Please specify a valid start IP");
		}
		
		if (endIP != null && !NetUtils.isValidIp(endIP)) {
			throw new InvalidParameterValueException("Please specify a valid end IP");
		}
		
		if (endIP != null && !NetUtils.validIpRange(startIP, endIP)) {
			throw new InvalidParameterValueException("Please specify a valid IP range.");
		}
		
		// Check that the IPs that are being added are compatible with the VLAN's gateway and netmask
		if (vlanNetmask == null) {
			throw new InvalidParameterValueException("Please ensure that your IP range's netmask is specified");
		}
		
		if (endIP != null && !NetUtils.sameSubnet(startIP, endIP, vlanNetmask)) {
			throw new InvalidParameterValueException("Please ensure that your start IP and end IP are in the same subnet, as per the IP range's netmask.");
		}
		
		if (!NetUtils.sameSubnet(startIP, vlanGateway, vlanNetmask)) {
			throw new InvalidParameterValueException("Please ensure that your start IP is in the same subnet as your IP range's gateway, as per the IP range's netmask.");
		}
		
		if (endIP != null && !NetUtils.sameSubnet(endIP, vlanGateway, vlanNetmask)) {
			throw new InvalidParameterValueException("Please ensure that your end IP is in the same subnet as your IP range's gateway, as per the IP range's netmask.");
		}
	}
	
	private void checkPrivateIpRangeErrors(Long podId, String startIP, String endIP) throws InvalidParameterValueException {
		HostPodVO pod = _podDao.findById(podId);
		if (pod == null) {
			throw new InvalidParameterValueException("Please specify a valid pod.");
		}
		
		// Check that the start and end IPs are valid
		if (!NetUtils.isValidIp(startIP)) {
			throw new InvalidParameterValueException("Please specify a valid start IP");
		}
		
		if (endIP != null && !NetUtils.isValidIp(endIP)) {
			throw new InvalidParameterValueException("Please specify a valid end IP");
		}
		
		if (endIP != null && !NetUtils.validIpRange(startIP, endIP)) {
			throw new InvalidParameterValueException("Please specify a valid IP range.");
		}
		
		// Check that the IPs that are being added are compatible with the pod's CIDR
		String cidrAddress = getCidrAddress(podId);
		long cidrSize = getCidrSize(podId);

		if (endIP != null && !NetUtils.sameSubnetCIDR(startIP, endIP, cidrSize)) {
			throw new InvalidParameterValueException("Please ensure that your start IP and end IP are in the same subnet, as per the pod's CIDR size.");
		}
		
		if (!NetUtils.sameSubnetCIDR(startIP, cidrAddress, cidrSize)) {
			throw new InvalidParameterValueException("Please ensure that your start IP is in the same subnet as the pod's CIDR address.");
		}
		
		if (endIP != null && !NetUtils.sameSubnetCIDR(endIP, cidrAddress, cidrSize)) {
			throw new InvalidParameterValueException("Please ensure that your end IP is in the same subnet as the pod's CIDR address.");
		}
	}
    
	private String getCidrAddress(String cidr) {
		String[] cidrPair = cidr.split("\\/");
		return cidrPair[0];
	}
	
	private int getCidrSize(String cidr) {
		String[] cidrPair = cidr.split("\\/");
		return Integer.parseInt(cidrPair[1]);
	}
	
	private String getCidrAddress(long podId) {
		HostPodVO pod = _podDao.findById(podId);
		return pod.getCidrAddress();
	}
	
	private long getCidrSize(long podId) {
		HostPodVO pod = _podDao.findById(podId);
		return pod.getCidrSize();
	}
	
	private void checkPodCidrSubnets(long dcId, HashMap<Long, List<Object>> currentPodCidrSubnets) throws InvalidParameterValueException {
		// For each pod, return an error if any of the following is true:
		// 1. The pod's CIDR subnet conflicts with the guest network subnet
		// 2. The pod's CIDR subnet conflicts with the CIDR subnet of any other pod
		DataCenterVO dcVo = _zoneDao.findById(dcId);
		String guestNetworkCidr = dcVo.getGuestNetworkCidr();
		String[] cidrTuple = guestNetworkCidr.split("\\/");
		
		String zoneName = getZoneName(dcId);
		String guestIpNetwork = NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0],Long.parseLong(cidrTuple[1]));
		long guestCidrSize = Long.parseLong(cidrTuple[1]);
		
		// Iterate through all pods in this zone
		for (Long podId : currentPodCidrSubnets.keySet()) {
			String podName;
			if (podId.longValue() == -1) podName = "newPod";
			else podName = getPodName(podId.longValue());
			
			List<Object> cidrPair = currentPodCidrSubnets.get(podId);
			String cidrAddress = (String) cidrPair.get(0);
			long cidrSize = ((Long) cidrPair.get(1)).longValue();
			
			long cidrSizeToUse = -1;
			if (cidrSize < guestCidrSize) cidrSizeToUse = cidrSize;
			else cidrSizeToUse = guestCidrSize;
			
			String cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);
			String guestSubnet = NetUtils.getCidrSubNet(guestIpNetwork, cidrSizeToUse);
			
			// Check that cidrSubnet does not equal guestSubnet
			if (cidrSubnet.equals(guestSubnet)) {
				if (podName.equals("newPod")) {
					throw new InvalidParameterValueException("The subnet of the pod you are adding conflicts with the subnet of the Guest IP Network. Please specify a different CIDR.");
				} else {
					throw new InvalidParameterValueException("Warning: The subnet of pod " + podName + " in zone " + zoneName + " conflicts with the subnet of the Guest IP Network. Please change either the pod's CIDR or the Guest IP Network's subnet, and re-run install-vmops-management.");
				}
			}
			
			// Iterate through the rest of the pods
			for (Long otherPodId : currentPodCidrSubnets.keySet()) {
				if (podId.equals(otherPodId)) continue;
				
				// Check that cidrSubnet does not equal otherCidrSubnet
				List<Object> otherCidrPair = currentPodCidrSubnets.get(otherPodId);
				String otherCidrAddress = (String) otherCidrPair.get(0);
				long otherCidrSize = ((Long) otherCidrPair.get(1)).longValue();
				
				if (cidrSize < otherCidrSize) cidrSizeToUse = cidrSize;
				else cidrSizeToUse = otherCidrSize;
				
				cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);
				String otherCidrSubnet = NetUtils.getCidrSubNet(otherCidrAddress, cidrSizeToUse);
				
				if (cidrSubnet.equals(otherCidrSubnet)) {
					String otherPodName = getPodName(otherPodId.longValue());
					if (podName.equals("newPod")) {
						throw new InvalidParameterValueException("The subnet of the pod you are adding conflicts with the subnet of pod " + otherPodName + " in zone " + zoneName + ". Please specify a different CIDR.");
					} else {
						throw new InvalidParameterValueException("Warning: The pods " + podName + " and " + otherPodName + " in zone " + zoneName + " have conflicting CIDR subnets. Please change the CIDR of one of these pods.");
					}
				}
			}
		}
		
	}
	
	private boolean validPod(long podId) {
		return (_podDao.findById(podId) != null);
	}
    
    private boolean validPod(String podName, long zoneId) {
    	if (!validZone(zoneId)) {
    		return false;
    	}
    	
		return (_podDao.findByName(podName, zoneId) != null);
	}
    
    private String getPodName(long podId) {
    	return _podDao.findById(new Long(podId)).getName();
    }
    
    private boolean validZone(String zoneName) {
    	return(_zoneDao.findByName(zoneName) != null);
    }
    
    private boolean validZone(long zoneId) {
    	return (_zoneDao.findById(zoneId) != null);
    }
    
    private String getZoneName(long zoneId) {
    	DataCenterVO zone = _zoneDao.findById(new Long(zoneId));
    	if (zone != null)
    		return zone.getName();
    	else
    		return null;
    }
    
    private Long saveConfigurationEvent(long userId, Long accountId, String type, String description, String... paramsList) {
    	UserVO user = _userDao.findById(userId);
    	long accountIdToUse = (accountId != null) ? accountId : user.getAccountId();
    	
    	String eventParams = "";
    	String logParams = "";
    	for (int i = 0; i < paramsList.length; i++) {
    		String param = paramsList[i];
    		boolean lastParam = (i == (paramsList.length - 1));
    		
    		logParams += param;
    		if (!lastParam) {
    			logParams += ", ";
    		}
    		
    		String[] valList = param.split("\\=");
    		String val = (valList.length < 2) ? "null" : valList[1];
    		if (val.equals("null")) {
    			continue;
    		}
    		
    		eventParams += param;
    		if (!lastParam) {
    			eventParams += "\n";
    		}
    	}
    	
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountIdToUse);
        event.setType(type);
        event.setDescription(description);
        event.setLevel(EventVO.LEVEL_INFO);
        event.setParameters(eventParams);
        event = _eventDao.persist(event);
        
        s_logger.debug("User " + user.getUsername() + " performed configuration action: " + type + ", " + description + " | params: " + logParams);
        
        return event.getId();
    }
    
    private String[] getLinkLocalIPRange() throws InvalidParameterValueException {
    	String ipNums = _configDao.getValue("linkLocalIp.nums");
    	int nums = Integer.parseInt(ipNums);
    	if (nums > 16 || nums <= 0) {
    		throw new InvalidParameterValueException("The linkLocalIp.nums: " + nums + "is wrong, should be 1~16");
    	}
    	/*local link ip address starts from 169.254.0.2 - 169.254.(nums)*/
    	String[] ipRanges = NetUtils.getLinkLocalIPRange(nums);
    	if (ipRanges == null)
    		throw new InvalidParameterValueException("The linkLocalIp.nums: " + nums + "may be wrong, should be 1~16");
    	return ipRanges;
    }

	@Override
	public Configuration addConfig(CreateCfgCmd cmd){
		String category = cmd.getCategory();
		String instance = cmd.getInstance();
		String component = cmd.getComponent();
		String name = cmd.getConfigPropName();
		String value = cmd.getValue();
		String description = cmd.getDescription();
		try
		{
			ConfigurationVO entity = new ConfigurationVO(category, instance, component, name, value, description);
			_configDao.persist(entity);
			s_logger.info("Successfully added configuration value into db: category:"+category+" instance:"+instance+" component:"+component+" name:"+name+" value:"+value);
			return _configDao.findByName(name);
		}
		catch(Exception ex)
		{
			s_logger.error("Unable to add the new config entry:",ex);
			throw new CloudRuntimeException("Unable to add configuration parameter " + name);
		}
	}

	@Override
	public boolean deleteVlanIpRange(DeleteVlanIpRangeCmd cmd) throws InvalidParameterValueException {
		
    	Long vlanDbId = cmd.getId();
    	Long userId = UserContext.current().getUserId();
    	
    	if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

    	return deleteVlanAndPublicIpRange(userId, vlanDbId);
		
	}   
	
	@Override
    public void checkAccess(Account caller, DataCenter zone) throws PermissionDeniedException {
        for (SecurityChecker checker : _secChecker) {
            if (checker.checkAccess(caller, zone)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + caller + " to zone:" + zone.getId() + " by " + checker.getName());
                }
                return;
            }else{
            	throw new PermissionDeniedException("Access denied to "+caller+" by "+checker.getName());
            }
        }
        
        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to zone:" + zone.getId());
    }
}
