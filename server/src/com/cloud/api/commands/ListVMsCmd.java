/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

package com.cloud.api.commands;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.async.AsyncJobVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.domain.DomainVO;
import com.cloud.host.HostVO;
import com.cloud.server.Criteria;
import com.cloud.service.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVm;
import com.cloud.vm.VmStats;

public class ListVMsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListVMsCmd.class.getName());

    private static final String s_name = "listvirtualmachinesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.STATE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.POD_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.HOST_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.FOR_VIRTUAL_NETWORK, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
    }

    @Override
	public String getName() {
        return s_name;
    }
    @Override
	public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String name = (String) params.get(BaseCmd.Properties.NAME.getName());
        String state = (String) params.get(BaseCmd.Properties.STATE.getName());
        Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());
        Long podId = (Long)params.get(BaseCmd.Properties.POD_ID.getName());
        Long hostId = (Long)params.get(BaseCmd.Properties.HOST_ID.getName());
        Boolean forVirtualNetwork = (Boolean)params.get(BaseCmd.Properties.FOR_VIRTUAL_NETWORK.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        Long accountId = null;
        Boolean isAdmin = false;

        if ((account == null) || isAdmin(account.getType())) {
            isAdmin = true;
            if (domainId != null) {
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list virtual machines.");
                }

                if (accountName != null) {
                    account = getManagementServer().findActiveAccount(accountName, domainId);
                    if (account == null) {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = account.getId();
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }
        } else {
            accountId = account.getId();
            domainId = account.getDomainId();
        }

        Long[] accountIds = null;
        if (accountId != null) {
            accountIds = new Long[1];
            accountIds[0] = accountId;
        }

        Long startIndex = Long.valueOf(0);
        int pageSizeNum = getManagementServer().getDefaultPageSize();
    	if (pageSize != null) {
    		pageSizeNum = pageSize.intValue();
    	}
        if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeNum * (pageNum-1));
            }
        }
        Criteria c = new Criteria("id", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        
        c.addCriteria(Criteria.ACCOUNTID, accountIds);
        c.addCriteria(Criteria.ISADMIN, isAdmin); 
        c.addCriteria(Criteria.KEYWORD, keyword);
        c.addCriteria(Criteria.ID, id);
        c.addCriteria(Criteria.NAME, name);
        c.addCriteria(Criteria.STATE, state);
        c.addCriteria(Criteria.DATACENTERID, zoneId);

        // ignore these search requests if it's not an admin
        if (isAdmin == true) {
            c.addCriteria(Criteria.DOMAINID, domainId);
            c.addCriteria(Criteria.PODID, podId);
            c.addCriteria(Criteria.HOSTID, hostId);
        } 

        List<? extends UserVm> virtualMachines = getManagementServer().searchForUserVMs(c);

        List vmTag = new ArrayList();

        HashMap<Long, HostVO> hostMap = new HashMap<Long, HostVO>();
        List<HostVO> hostList = getManagementServer().listAllActiveHosts();
        for (HostVO hostVO : hostList) {
        	hostMap.put(hostVO.getId(), hostVO);
        }

        
        Map<Long, DataCenterVO> zones = new HashMap<Long, DataCenterVO>();
        Map<Long, Account> accounts = new HashMap<Long, Account>();
        Map<Long, VMTemplateVO> templates = new HashMap<Long, VMTemplateVO>();
        Map<Long, ServiceOfferingVO> offerings = new HashMap<Long, ServiceOfferingVO>();
        
        for (UserVm vmInstance : virtualMachines) {
    
        	//if the account is deleted, do not return the user vm 
        	Account currentVmAccount = getManagementServer().getAccount(vmInstance.getAccountId());
        	if(currentVmAccount.getRemoved()!=null)
        	{
        		continue; //not returning this vm
        	}
        	
            List<Pair<String, Object>> vmData = new ArrayList<Pair<String, Object>>();
            AsyncJobVO asyncJob = getManagementServer().findInstancePendingAsyncJob("vm_instance", vmInstance.getId());
            if(asyncJob != null) {
                vmData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), asyncJob.getId().toString()));
                vmData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_STATUS.getName(), String.valueOf(asyncJob.getStatus())));
            } 

            vmData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), vmInstance.getId().toString()));
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), vmInstance.getName()));
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(vmInstance.getCreated())));
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.IP_ADDRESS.getName(), vmInstance.getPrivateIpAddress()));
            if (vmInstance.getState() != null) {
                vmData.add(new Pair<String, Object>(BaseCmd.Properties.STATE.getName(), vmInstance.getState().toString()));
            }
            
            Account acct = accounts.get(vmInstance.getAccountId());
            if (acct == null) {
                acct = getManagementServer().findAccountById(Long.valueOf(vmInstance.getAccountId()));
                accounts.put(vmInstance.getAccountId(), acct);
            }
            
            if (acct != null) {
                vmData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), acct.getAccountName()));
                vmData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), acct.getDomainId().toString()));
                vmData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(acct.getDomainId()).getName()));
            }
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.HA_ENABLE.getName(), Boolean.valueOf(vmInstance.isHaEnabled()).toString()));
            
            if (vmInstance.getDisplayName() != null) {
    			vmData.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_NAME.getName(), vmInstance.getDisplayName()));
    		}
            else {
            	vmData.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_NAME.getName(), vmInstance.getName()));
            }

    		if (vmInstance.getGroup() != null) {
    			vmData.add(new Pair<String, Object>(BaseCmd.Properties.GROUP.getName(), vmInstance.getGroup()));
    		}

            // Data Center Info
    		DataCenterVO zone = zones.get(vmInstance.getDataCenterId());
    		if (zone == null) {
    		    zone = getManagementServer().findDataCenterById(vmInstance.getDataCenterId());
    		    zones.put(zone.getId(), zone);
    		}
    		
    		
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), zone.getId().toString()));
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), zone.getName()));
            //if user is an admin, display host id
            if ( (isAdmin == true) && (vmInstance.getHostId() != null)) {
            	vmData.add(new Pair<String, Object>(BaseCmd.Properties.HOST_ID.getName(), vmInstance.getHostId().toString()));
            	vmData.add(new Pair<String, Object>(BaseCmd.Properties.HOST_NAME.getName(), getManagementServer().getHostBy(vmInstance.getHostId()).getName()));
            }

            // Template Info
            VMTemplateVO template = templates.get(vmInstance.getTemplateId());
            if (template == null) {
                template = getManagementServer().findTemplateById(vmInstance.getTemplateId());
                if (template != null) {
                    templates.put(template.getId(), template);
                }
            }
            
            if (template != null) {
            	vmData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_ID.getName(), template.getId().toString()));
            	vmData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_NAME.getName(), template.getName()));
            	vmData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_DISPLAY_TEXT.getName(), template.getDisplayText()));
            	vmData.add(new Pair<String, Object>(BaseCmd.Properties.PASSWORD_ENABLED.getName(), template.getEnablePassword()));
            } else {
            	vmData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_ID.getName(), "-1"));
            	vmData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_NAME.getName(), "ISO Boot"));
            	vmData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_DISPLAY_TEXT.getName(), "ISO Boot"));
            	vmData.add(new Pair<String, Object>(BaseCmd.Properties.PASSWORD_ENABLED.getName(), false));
            }

            // ISO Info 
            if (vmInstance.getIsoId() != null) {
                VMTemplateVO iso = templates.get(vmInstance.getIsoId());
                if (iso == null) {
                    iso = getManagementServer().findTemplateById(vmInstance.getIsoId().longValue());
                    templates.put(vmInstance.getIsoId(), iso);
                }
                
                if (iso != null) {
                    vmData.add(new Pair<String, Object>(BaseCmd.Properties.ISO_ID.getName(), iso.getId().toString()));
                    vmData.add(new Pair<String, Object>(BaseCmd.Properties.ISO_NAME.getName(), iso.getName()));
                }
            }

            // Service Offering Info
            ServiceOfferingVO offering = offerings.get(vmInstance.getServiceOfferingId());
            if (offering == null) {
                offering = getManagementServer().findServiceOfferingById(vmInstance.getServiceOfferingId());
                offerings.put(vmInstance.getServiceOfferingId(), offering);
            }
            
            // Probably not the best place to check but it will have to do for now to make this
            // the least intrusive fix.
            if (forVirtualNetwork != null && forVirtualNetwork && offering.getGuestIpType() != ServiceOffering.GuestIpType.Virtualized) {
            	continue;
            }
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.SERVICE_OFFERING_ID.getName(), offering.getId().toString()));
	        vmData.add(new Pair<String, Object>(BaseCmd.Properties.SERVICE_OFFERING_NAME.getName(), offering.getName()));
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_NUMBER.getName(), Integer.valueOf(offering.getCpu()).toString()));
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_SPEED.getName(), Integer.valueOf(offering.getSpeed()).toString()));
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.MEMORY.getName(), Integer.valueOf(offering.getRamSize()).toString()));
            
            //stats calculation
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            String cpuUsed = null;
            VmStats vmStats = getManagementServer().getVmStatistics(vmInstance.getId());
            if (vmStats != null) 
            {
                float cpuUtil = (float) vmStats.getCPUUtilization();
                cpuUsed = decimalFormat.format(cpuUtil) + "%";
                vmData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_USED.getName(), cpuUsed));
                
                long networkKbRead = (long)vmStats.getNetworkReadKBs();
                vmData.add(new Pair<String, Object>(BaseCmd.Properties.NETWORK_KB_READ.getName(), networkKbRead));
                
                long networkKbWrite = (long)vmStats.getNetworkWriteKBs();
                vmData.add(new Pair<String, Object>(BaseCmd.Properties.NETWORK_KB_WRITE.getName(), networkKbWrite));
            }
            
            //network groups
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.NETWORK_GROUP_LIST.getName(), getManagementServer().getNetworkGroupsNamesForVm(vmInstance.getId())));
            
            vmTag.add(vmData);
        }
        List<Pair<String, Object>> returnTags = new ArrayList<Pair<String, Object>>();
        Pair<String, Object> vmTags = new Pair<String, Object>("virtualmachine", vmTag.toArray());
        returnTags.add(vmTags);
        return returnTags;
    }
}
