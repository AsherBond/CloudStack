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
package com.cloud.network;

import java.util.List;
import java.util.Map;

import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.vpn.RemoteAccessVpnElement;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;
import com.cloud.utils.net.Ip;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

/**
 * NetworkManager manages the network for the different end users.
 *
 */
public interface NetworkManager extends NetworkService {
    public static final boolean USE_POD_VLAN = false;

    /**
     * Assigns a new public ip address.
     * 
     * @param dcId
     * @param podId TODO
     * @param owner
     * @param type
     * @param networkId
     * @return
     * @throws InsufficientAddressCapacityException
     */
    PublicIp assignPublicIpAddress(long dcId, Long podId, Account owner, VlanType type, Long networkId) throws InsufficientAddressCapacityException;
    
    /**
     * assigns a source nat ip address to an account within a network.
     * 
     * @param owner
     * @param network
     * @param callerId
     * @return
     * @throws ConcurrentOperationException
     * @throws InsufficientAddressCapacityException
     */
    PublicIp assignSourceNatIpAddress(Account owner, Network network, long callerId) throws ConcurrentOperationException, InsufficientAddressCapacityException;
    
    /**
     * Do all of the work of releasing public ip addresses.  Note that
     * if this method fails, there can be side effects.
     * @param userId
     * @param ipAddress
     * @return true if it did; false if it didn't
     */
    public boolean releasePublicIpAddress(Ip ipAddress, long ownerId, long userId);
    
    /**
     * Associates or disassociates a list of public IP address for a router.
     * @param router router object to send the association to
     * @param ipAddrList list of public IP addresses
     * @param add true if associate, false if disassociate
     * @param vmId
     * @return
     */
    boolean associateIP(DomainRouterVO router, List<String> ipAddrList, boolean add, long vmId) throws ConcurrentOperationException;
    
    /**
     * Lists IP addresses that belong to VirtualNetwork VLANs
     * @param accountId - account that the IP address should belong to
     * @param dcId - zone that the IP address should belong to
     * @param sourceNat - (optional) true if the IP address should be a source NAT address
     * @return - list of IP addresses
     */
    List<IPAddressVO> listPublicIpAddressesInVirtualNetwork(long accountId, long dcId, Boolean sourceNat);	
    
    List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, DeploymentPlan plan, String name, String displayText, boolean isShared, boolean isDefault) throws ConcurrentOperationException;
    List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, Network predefined, DeploymentPlan plan, String name, String displayText, boolean isShared, boolean isDefault) throws ConcurrentOperationException;
    
    List<NetworkOfferingVO> getSystemAccountNetworkOfferings(String... offeringNames);
    
    void allocate(VirtualMachineProfile<? extends VMInstanceVO> vm, List<Pair<NetworkVO, NicProfile>> networks) throws InsufficientCapacityException, ConcurrentOperationException;

    void prepare(VirtualMachineProfile<? extends VMInstanceVO> profile, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException;
    void release(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, boolean forced);
    
    void cleanupNics(VirtualMachineProfile<? extends VMInstanceVO> vm);
    
    List<? extends Nic> getNics(VirtualMachine vm);
	
    List<AccountVO> getAccountsUsingNetwork(long configurationId);    
    AccountVO getNetworkOwner(long configurationId);
    
    List<NetworkVO> getNetworksforOffering(long offeringId, long dataCenterId, long accountId);
    
	String getNextAvailableMacAddressInNetwork(long networkConfigurationId) throws InsufficientAddressCapacityException;

	boolean applyRules(List<? extends FirewallRule> rules, boolean continueOnError) throws ResourceUnavailableException;
	
	Map<Service, Map<Capability, String>> getZoneCapabilities(long zoneId);
	
	Network getSystemNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType);
	
	List<? extends RemoteAccessVpnElement> getRemoteAccessVpnElements();
	
	PublicIpAddress getPublicIpAddress(Ip ipAddress);
	
	List<? extends Vlan> listPodVlans(long podId);

	Pair<NetworkGuru, NetworkVO> implementNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;
	List<NetworkVO> listNetworksUsedByVm(long vmId, boolean isSystem);

    <T extends VMInstanceVO> void prepareNicForMigration(VirtualMachineProfile<T> vm, DeployDestination dest);

}
