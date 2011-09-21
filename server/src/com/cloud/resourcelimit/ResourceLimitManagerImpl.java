/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
package com.cloud.resourcelimit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.alert.AlertManager;
import com.cloud.api.commands.UpdateResourceCountCmd;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dao.EntityManager;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

import edu.emory.mathcs.backport.java.util.Arrays;

@Local(value = { ResourceLimitService.class})
public class ResourceLimitManagerImpl implements ResourceLimitService, Manager{
   public static final Logger s_logger = Logger.getLogger(ResourceLimitManagerImpl.class);
    
    private String _name;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private AlertManager _alertMgr;
    @Inject
    private ResourceCountDao _resourceCountDao;
    @Inject
    private ResourceLimitDao _resourceLimitDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    protected SnapshotDao _snapshotDao;
    @Inject
    protected VMTemplateDao _vmTemplateDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private IPAddressDao _ipAddressDao;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    EntityManager _entityMgr;
    
    protected SearchBuilder<ResourceCountVO> ResourceCountSearch;
    
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
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;
        
        ResourceCountSearch = _resourceCountDao.createSearchBuilder();
        ResourceCountSearch.and("id", ResourceCountSearch.entity().getId(), SearchCriteria.Op.IN);
        ResourceCountSearch.and("accountId", ResourceCountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        ResourceCountSearch.and("domainId", ResourceCountSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        ResourceCountSearch.done();

        return true;
    }
    
    @Override
    public void incrementResourceCount(long accountId, ResourceType type, Long... delta) {
        //don't upgrade resource count for system account
        if (accountId == Account.ACCOUNT_ID_SYSTEM) {
            s_logger.trace("Not incrementing resource count for system accounts, returning");
            return;
        }
        long numToIncrement = (delta.length == 0) ? 1 : delta[0].longValue();

        if (!updateResourceCountForAccount(accountId, type, true, numToIncrement)) {
            //we should fail the operation (resource creation) when failed to update the resource count
            throw new CloudRuntimeException("Failed to increment resource count of type " + type + " for account id=" + accountId);
        }
    }

    @Override
    public void decrementResourceCount(long accountId, ResourceType type, Long... delta) {
        //don't upgrade resource count for system account
        if (accountId == Account.ACCOUNT_ID_SYSTEM) {
            s_logger.trace("Not decrementing resource count for system accounts, returning");
            return;
        }
        long numToDecrement = (delta.length == 0) ? 1 : delta[0].longValue();
        
        if (!updateResourceCountForAccount(accountId, type, false, numToDecrement)) {
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_UPDATE_RESOURCE_COUNT, 0L, 0L, "Failed to decrement resource count of type " + type + " for account id=" + accountId, 
                        "Failed to decrement resource count of type " + type + " for account id=" + accountId + "; use updateResourceCount API to recalculate/fix the problem");
        }
    }
    
    @Override
    public long findCorrectResourceLimitForAccount(long accountId, ResourceType type) {
        long max = -1;

        ResourceLimitVO limit = _resourceLimitDao.findByOwnerIdAndType(accountId, ResourceOwnerType.Account, type);

        // Check if limit is configured for account
        if (limit != null) {
            max = limit.getMax().longValue();
        } else {
            // If the account has an no limit set, then return global default account limits
            try {
                if (type == Resource.ResourceType.public_ip) {
                    max = Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountPublicIPs.key()));
                } else if (type == ResourceType.snapshot) {
                    max = Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountSnapshots.key()));
                } else if (type == ResourceType.template) {
                    max = Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountTemplates.key()));
                } else if (type == ResourceType.user_vm) {
                    max = Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountUserVms.key()));
                } else if (type == ResourceType.volume) {
                    max = Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountVolumes.key()));
                }  else {
                    throw new InvalidParameterValueException("Unsupported resource type " + type);
                }
            } catch (NumberFormatException nfe) {
                s_logger.error("Invalid value is set for the default account limit.");
            }
        }

        return max;
    }

    @Override
    public long findCorrectResourceLimitForDomain(Domain domain, ResourceType type) {
        long max = -1;

        // Check account
        ResourceLimitVO limit = _resourceLimitDao.findByOwnerIdAndType(domain.getId(), ResourceOwnerType.Domain, type);

        if (limit != null) {
            max = limit.getMax().longValue();
        } else {
            // check domain hierarchy
            Long domainId = domain.getParent();
            while ((domainId != null) && (limit == null)) {
                limit = _resourceLimitDao.findByOwnerIdAndType(domainId, ResourceOwnerType.Domain, type);
                DomainVO tmpDomain = _domainDao.findById(domainId);
                domainId = tmpDomain.getParent();
            }

            if (limit != null) {
                max = limit.getMax().longValue();
            }
        }

        return max;
    }

    @Override @DB
    public boolean resourceLimitExceeded(Account account, ResourceType type, long... count) {
        long numResources = ((count.length == 0) ? 1 : count[0]);

        // Don't place any limits on system or admin accounts
        if (_accountMgr.isAdmin(account.getType())) {
            return false;
        }
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        try {
            //Lock all rows first so nobody else can read it 
            Set<Long> rowIdsToLock = _resourceCountDao.listAllRowsToUpdate(account.getId(), ResourceOwnerType.Account, type);
            SearchCriteria<ResourceCountVO> sc = ResourceCountSearch.create();
            sc.setParameters("id", rowIdsToLock.toArray());
            _resourceCountDao.lockRows(sc, null, true);

            // Check account limits
            long accountLimit = findCorrectResourceLimitForAccount(account.getId(), type);
            long potentialCount = _resourceCountDao.getResourceCount(account.getId(), ResourceOwnerType.Account, type) + numResources;
            if (accountLimit != -1 && potentialCount > accountLimit) {
                return true;
            }

            // check all domains in the account's domain hierarchy
            Long domainId = account.getDomainId();
            while (domainId != null) {
                ResourceLimitVO domainLimit = _resourceLimitDao.findByOwnerIdAndType(domainId, ResourceOwnerType.Domain, type);
                if (domainLimit != null) {
                    long domainCount = _resourceCountDao.getResourceCount(domainId, ResourceOwnerType.Domain, type);
                    if ((domainCount + numResources) > domainLimit.getMax().longValue()) {
                        return true;
                    }
                }
                DomainVO domain = _domainDao.findById(domainId);
                domainId = domain.getParent();
            }
            
            return false;
        } finally {
            txn.commit();
        }
    }
    
    @DB @Override
    public boolean resourceLimitExceededForDomain(Domain domain, ResourceType type, long... count) {
        long numResources = ((count.length == 0) ? 1 : count[0]);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        try {
            //Lock all rows first so nobody else can read it 
            Set<Long> rowIdsToLock = _resourceCountDao.listAllRowsToUpdate(domain.getId(), ResourceOwnerType.Domain, type);
            SearchCriteria<ResourceCountVO> sc = ResourceCountSearch.create();
            sc.setParameters("id", rowIdsToLock.toArray());
            _resourceCountDao.lockRows(sc, null, true);

            Long domainId = domain.getId();
            // check all domains in the domain hierarchy
            while (domainId != null) {
                ResourceLimitVO domainLimit = _resourceLimitDao.findByOwnerIdAndType(domainId, ResourceOwnerType.Domain, type);
                if (domainLimit != null) {
                    long domainCount = _resourceCountDao.getResourceCount(domainId, ResourceOwnerType.Domain, type);
                    if ((domainCount + numResources) > domainLimit.getMax().longValue()) {
                        return true;
                    }
                }
                domain = _domainDao.findById(domainId);
                domainId = domain.getParent();
            }
            
            return false;
        } finally {
            txn.commit();
        }
    }
    
    
    @Override
    public List<ResourceLimitVO> searchForLimits(Long id, String accountName, Long domainId, Integer type, Long startIndex, Long pageSizeVal) {
       Account caller = UserContext.current().getCaller();
       List<ResourceLimitVO> limits = new ArrayList<ResourceLimitVO>();
       boolean isAccount = true;
       
       Long accountId = null;
       
       if (!_accountMgr.isAdmin(caller.getType())) {
           accountId = caller.getId();
           domainId = null;
       } else {
           if (domainId != null) {
               //verify domain information and permissions
               Domain domain = _domainDao.findById(domainId);
               if (domain == null) {
                   //return empty set
                   return limits;
               }
               
               _accountMgr.checkAccess(caller, domain);
               
               if (accountName != null) {
                   //Verify account information and permissions
                   Account account = _accountDao.findAccount(accountName, domainId);
                   if (account == null) {
                       //return empty set
                       return limits;
                   }
                   
                   _accountMgr.checkAccess(caller, null, account);
                   
                   accountId = account.getId();
                   domainId = null;
               } 
           }
       }
       
       // Map resource type
       ResourceType resourceType = null;
       if (type != null) {
           try {
               resourceType = ResourceType.values()[type];
           } catch (ArrayIndexOutOfBoundsException e) {
               throw new InvalidParameterValueException("Please specify a valid resource type.");
           }
       }

       //If id is passed in, get the record and return it if permission check has passed
       if (id != null) {
           ResourceLimitVO vo = _resourceLimitDao.findById(id);
           if (vo.getAccountId() != null) {
               _accountMgr.checkAccess(caller, null, _accountDao.findById(vo.getAccountId()));
               limits.add(vo);
           } else if (vo.getDomainId() != null) {
               _accountMgr.checkAccess(caller, _domainDao.findById(vo.getDomainId()));
               limits.add(vo);
           }
           
           return limits;
       } 
       
       
       //If account is not specified, default it to caller account
       if (accountId == null) {
           if (domainId == null) {
               accountId = caller.getId();
               isAccount = true;
           } else {
               isAccount = false;
           }
       } else {
           isAccount = true;
       }   
       
       SearchBuilder<ResourceLimitVO> sb = _resourceLimitDao.createSearchBuilder();
       sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
       sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
       sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);

       SearchCriteria<ResourceLimitVO> sc = sb.create();
       Filter filter = new Filter(ResourceLimitVO.class, "id", true, startIndex, pageSizeVal);
       
       if (accountId != null) {
           sc.setParameters("accountId", accountId);
       } 
       
       if (domainId != null) {
           sc.setParameters("domainId", domainId);
           sc.setParameters("accountId", null);
       }
       
       if (resourceType != null) {
           sc.setParameters("type", resourceType);
       }
       
       List<ResourceLimitVO> foundLimits = _resourceLimitDao.search(sc, filter);
       
       if (resourceType != null) {
           if (foundLimits.isEmpty()) {
               if (isAccount) {
                   limits.add(new ResourceLimitVO(resourceType, findCorrectResourceLimitForAccount(accountId, resourceType), accountId, ResourceOwnerType.Account));
               } else {
                   limits.add(new ResourceLimitVO(resourceType, findCorrectResourceLimitForDomain(_domainDao.findById(domainId), resourceType), domainId, ResourceOwnerType.Domain));
               }
           } else {
               limits.addAll(foundLimits);
           }
       } else {
           limits.addAll(foundLimits);
           
           //see if any limits are missing from the table, and if yes - get it from the config table and add
           ResourceType[] resourceTypes = ResourceCount.ResourceType.values();
           if (foundLimits.size() != resourceTypes.length) {
               List<String> accountLimitStr = new ArrayList<String>();
               List<String> domainLimitStr = new ArrayList<String>();
               for (ResourceLimitVO foundLimit : foundLimits) {
                   if (foundLimit.getAccountId() != null) {
                       accountLimitStr.add(foundLimit.getType().toString());
                   } else {
                       domainLimitStr.add(foundLimit.getType().toString());
                   }
               }
               
               //get default from config values
               if (isAccount) {
                   if (accountLimitStr.size() < resourceTypes.length) {
                       for (ResourceType rt : resourceTypes) {
                           if (!accountLimitStr.contains(rt.toString()) && rt.supportsOwner(ResourceOwnerType.Account)) {
                               limits.add(new ResourceLimitVO(rt, findCorrectResourceLimitForAccount(accountId, rt), accountId, ResourceOwnerType.Account));
                           }
                       }
                   }
                   
               } else {
                   if (domainLimitStr.size() < resourceTypes.length) {
                       for (ResourceType rt : resourceTypes) {
                           if (!domainLimitStr.contains(rt.toString()) && rt.supportsOwner(ResourceOwnerType.Domain)) {
                               limits.add(new ResourceLimitVO(rt, findCorrectResourceLimitForDomain(_domainDao.findById(domainId), rt), domainId, ResourceOwnerType.Domain));
                           }
                       }
                   }
               }
           }
       }
       
       return limits;
    }
    
    @Override
    public ResourceLimitVO updateResourceLimit(Long ownerId, ResourceOwnerType ownerType, Integer typeId, Long max) {
        Account caller = UserContext.current().getCaller();

        if (max == null) {
            max = new Long(-1);
        } else if (max < -1) {
            throw new InvalidParameterValueException("Please specify either '-1' for an infinite limit, or a limit that is at least '0'.");
        }

        // Map resource type
        ResourceType resourceType = null;
        if (typeId != null) {
            for (ResourceType type : Resource.ResourceType.values()) {
                if (type.getOrdinal() == typeId.intValue()) {
                    resourceType = type;
                }
            }
            if (resourceType == null) {
                throw new InvalidParameterValueException("Please specify valid resource type");
            }
        }
        
        if (ownerType == ResourceOwnerType.Domain) {
            Domain domain = _entityMgr.findById(Domain.class, ownerId);
            _accountMgr.checkAccess(caller, domain);
            if ((caller.getDomainId() == ownerId.longValue()) && caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
                // if the admin is trying to update their own domain, disallow...
                throw new PermissionDeniedException("Unable to update resource limit for domain " + ownerId + ", permission denied");
            }
            Long parentDomainId = domain.getParent();
            if (parentDomainId != null) {
                DomainVO parentDomain = _domainDao.findById(parentDomainId);
                long parentMaximum = findCorrectResourceLimitForDomain(parentDomain, resourceType);
                if ((parentMaximum >= 0) && (max.longValue() > parentMaximum)) {
                    throw new InvalidParameterValueException("Domain " + domain.getName() + "(id: " + ownerId + ") has maximum allowed resource limit " + parentMaximum + " for " + resourceType
                            + ", please specify a value less that or equal to " + parentMaximum);
                }
            }
        } else if (ownerType == ResourceOwnerType.Account) {
            Account account = _entityMgr.findById(Account.class, ownerId);
            if (account.getType() == Account.ACCOUNT_ID_SYSTEM) {
                throw new InvalidParameterValueException("Can't update system account");
            }
            
            _accountMgr.checkAccess(caller, null, account);
        }

        ResourceLimitVO limit = _resourceLimitDao.findByOwnerIdAndType(ownerId, ownerType, resourceType);
        if (limit != null) {
            // Update the existing limit
            _resourceLimitDao.update(limit.getId(), max);
            return _resourceLimitDao.findById(limit.getId());
        } else {
            return _resourceLimitDao.persist(new ResourceLimitVO(resourceType, max, ownerId, ownerType));
        }
    }

    @Override
    public List<ResourceCountVO> recalculateResourceCount(UpdateResourceCountCmd cmd) throws InvalidParameterValueException, CloudRuntimeException, PermissionDeniedException{
        Account callerAccount = UserContext.current().getCaller();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long accountId = null;
        long count=0;
        List<ResourceCountVO> counts = new ArrayList<ResourceCountVO>();
        List<ResourceType> resourceTypes = new ArrayList<ResourceType>();

        ResourceType resourceType = null;
        Integer typeId = cmd.getResourceType();

        if (typeId != null) {
            for (ResourceType type : resourceTypes) {
                if (type.getOrdinal() == typeId.intValue()) {
                    resourceType = type;
                }
            }
            if (resourceType == null) {
                throw new InvalidParameterValueException("Please specify valid resource type");
            }
        }

        DomainVO domain = _domainDao.findById(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("Please specify a valid domain ID.");
        }
        _accountMgr.checkAccess(callerAccount, domain);

        if (accountName != null) {
            Account userAccount = _accountMgr.getActiveAccountByName(accountName, domainId);
            if (userAccount == null) {
                throw new InvalidParameterValueException("unable to find account by name " + accountName + " in domain with id " + domainId);
            }
            accountId = userAccount.getId();
        }

        
        if (resourceType != null) {
            resourceTypes.add(resourceType);
        } else {
            resourceTypes = Arrays.asList(Resource.ResourceType.values());
        }

        for (ResourceType type : resourceTypes) {
            if (accountId != null) {
                if (type.supportsOwner(ResourceOwnerType.Account)) {
                    count = recalculateAccountResourceCount(accountId, type);
                    counts.add(new ResourceCountVO(type, count, accountId, ResourceOwnerType.Account));
                }
                
            } else {
                if (type.supportsOwner(ResourceOwnerType.Domain)) {
                    count = recalculateDomainResourceCount(domainId, type);
                    counts.add(new ResourceCountVO(type, count, domainId, ResourceOwnerType.Domain));
                }            
            }
        }
     
        return counts;
    }
    
    @DB
    protected boolean updateResourceCountForAccount(long accountId, ResourceType type, boolean increment, long delta) {
        boolean result = true;
        try {
            Transaction txn = Transaction.currentTxn();
            txn.start();
            
            Set<Long> rowsToLock = _resourceCountDao.listAllRowsToUpdate(accountId, ResourceOwnerType.Account, type);
            
            //Lock rows first
            SearchCriteria<ResourceCountVO> sc = ResourceCountSearch.create();
            sc.setParameters("id", rowsToLock.toArray());
            List<ResourceCountVO> rowsToUpdate = _resourceCountDao.lockRows(sc, null, true);    
            
            for (ResourceCountVO rowToUpdate : rowsToUpdate) {
                if (!_resourceCountDao.updateById(rowToUpdate.getId(), increment, delta)) {
                    s_logger.trace("Unable to update resource count for the row " + rowToUpdate);
                    result = false;
                }
            }
            
            txn.commit();
        } catch (Exception ex) {
            s_logger.error("Failed to update resource count for account id=" + accountId);
            result = false;
        }
        return result;
    }
    
    @DB
    protected long recalculateDomainResourceCount(long domainId, ResourceType type) {
        long count=0;

        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        try {
            //Lock all rows first so nobody else can read it 
            Set<Long> rowIdsToLock = _resourceCountDao.listAllRowsToUpdate(domainId, ResourceOwnerType.Domain, type);
            SearchCriteria<ResourceCountVO> sc = ResourceCountSearch.create();
            sc.setParameters("id", rowIdsToLock.toArray());
            _resourceCountDao.lockRows(sc, null, true);
            
            List<DomainVO> domainChildren = _domainDao.findImmediateChildrenForParent(domainId);
            // for each child domain update the resource count
            if (type.supportsOwner(ResourceOwnerType.Domain)) {
                for (DomainVO domainChild : domainChildren) {
                    long domainCount = recalculateDomainResourceCount(domainChild.getId(), type);
                    count = count + domainCount; // add the child domain count to parent domain count
                }
            }
            
            if (type.supportsOwner(ResourceOwnerType.Account)) {
                List<AccountVO> accounts = _accountDao.findActiveAccountsForDomain(domainId);
                for (AccountVO account : accounts) {
                    long accountCount = recalculateAccountResourceCount(account.getId(), type);
                    count = count + accountCount; // add account's resource count to parent domain count
                }
            }
            _resourceCountDao.setResourceCount(domainId, ResourceOwnerType.Domain, type, count);
       } catch (Exception e) {
           throw new CloudRuntimeException("Failed to update resource count for domain with Id " + domainId);
       } finally {
          txn.commit();
       }

       return count;
    }
    
    @DB
    protected long recalculateAccountResourceCount(long accountId, ResourceType type) {
        Long count=null;

        Transaction txn = Transaction.currentTxn();
        txn.start();

        // this lock guards against the updates to user_vm, volume, snapshot, public _ip and template table 
        // as any resource creation precedes with the resourceLimitExceeded check which needs this lock too
        SearchCriteria<ResourceCountVO> sc = ResourceCountSearch.create();
        sc.setParameters("accountId", accountId);
        _resourceCountDao.lockRows(sc, null, true);
        
        if (type == Resource.ResourceType.user_vm) {
            count = _userVmDao.countAllocatedVMsForAccount(accountId);
        } else if (type == Resource.ResourceType.volume) {
            count = _volumeDao.countAllocatedVolumesForAccount(accountId);
            long virtualRouterCount = _vmDao.countAllocatedVirtualRoutersForAccount(accountId);
            count = count - virtualRouterCount;  // don't count the volumes of virtual router
        } else if (type == Resource.ResourceType.snapshot) {
            count = _snapshotDao.countSnapshotsForAccount(accountId);
        } else if (type == Resource.ResourceType.public_ip) {
            count = _ipAddressDao.countAllocatedIPsForAccount(accountId);
        } else if (type == Resource.ResourceType.template) {
            count = _vmTemplateDao.countTemplatesForAccount(accountId);
        } else {
            throw new InvalidParameterValueException("Unsupported resource type " + type);
        }
        
        _resourceCountDao.setResourceCount(accountId, ResourceOwnerType.Account, type, (count == null) ? 0 : count.longValue());
   
        txn.commit();
  
        return (count == null) ? 0 : count.longValue();
    }
    
    @Override
    public long getResourceCount(Account account, ResourceType type) {
        return _resourceCountDao.getResourceCount(account.getId(), ResourceOwnerType.Account, type);
    }
    
}
