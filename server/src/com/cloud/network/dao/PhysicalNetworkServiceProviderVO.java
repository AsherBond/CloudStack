/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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
package com.cloud.network.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.Network.Service;
import com.cloud.network.PhysicalNetworkServiceProvider;

@Entity
@Table(name = "physical_network_service_providers")
public class PhysicalNetworkServiceProviderVO implements PhysicalNetworkServiceProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
    
    @Column(name="uuid")
    private String uuid; 

    @Column(name = "physical_network_id")
    private long physicalNetworkId;
    
    @Column(name = "destination_physical_network_id")
    private long destPhysicalNetworkId;    

    @Column(name = "provider_name")
    private String providerName;

    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    State state;
    
    @Column(name = "vpn_service_provided")
    boolean vpnServiceProvided;

    @Column(name = "dhcp_service_provided")
    boolean dhcpServiceProvided;
    
    @Column(name = "dns_service_provided")
    boolean dnsServiceProvided;
    
    @Column(name = "gateway_service_provided")
    boolean gatewayServiceProvided;
    
    @Column(name = "firewall_service_provided")
    boolean firewallServiceProvided;
    
    @Column(name = "source_nat_service_provided")
    boolean sourcenatServiceProvided;
    
    @Column(name = "load_balance_service_provided")
    boolean lbServiceProvided;
    
    @Column(name = "static_nat_service_provided")
    boolean staticnatServiceProvided;
    
    @Column(name = "port_forwarding_service_provided")
    boolean portForwardingServiceProvided;
    
    @Column(name = "user_data_service_provided")
    boolean userdataServiceProvided;
    
    @Column(name = "security_group_service_provided")
    boolean securitygroupServiceProvided;
    
    
    public PhysicalNetworkServiceProviderVO() {
    }

    public PhysicalNetworkServiceProviderVO(long physicalNetworkId, String name) {
        this.physicalNetworkId = physicalNetworkId;
        this.providerName = name;
        this.state = State.Disabled;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getPhysicalNetworkId() {
        return physicalNetworkId;
    }


    @Override
    public State getState() {
        return state;
    }
    
    @Override
    public void setState(State state) {
        this.state = state;
    }    

    @Override
    public String getProviderName() {
        return providerName;
    }

    public void setDestinationPhysicalNetworkId(long destPhysicalNetworkId) {
        this.destPhysicalNetworkId = destPhysicalNetworkId;
    }
    
    @Override
    public long getDestinationPhysicalNetworkId() {
        return destPhysicalNetworkId;
    }
    
    @Override
    public boolean isVpnServiceProvided() {
        return vpnServiceProvided;
    }

    public void setVpnServiceProvided(boolean vpnServiceProvided) {
        this.vpnServiceProvided = vpnServiceProvided;
    }

    @Override
    public boolean isDhcpServiceProvided() {
        return dhcpServiceProvided;
    }

    public void setDhcpServiceProvided(boolean dhcpServiceProvided) {
        this.dhcpServiceProvided = dhcpServiceProvided;
    }

    @Override
    public boolean isDnsServiceProvided() {
        return dnsServiceProvided;
    }

    public void setDnsServiceProvided(boolean dnsServiceProvided) {
        this.dnsServiceProvided = dnsServiceProvided;
    }

    @Override
    public boolean isGatewayServiceProvided() {
        return gatewayServiceProvided;
    }

    public void setGatewayServiceProvided(boolean gatewayServiceProvided) {
        this.gatewayServiceProvided = gatewayServiceProvided;
    }

    @Override
    public boolean isFirewallServiceProvided() {
        return firewallServiceProvided;
    }

    public void setFirewallServiceProvided(boolean firewallServiceProvided) {
        this.firewallServiceProvided = firewallServiceProvided;
    }

    @Override
    public boolean isSourcenatServiceProvided() {
        return sourcenatServiceProvided;
    }

    public void setSourcenatServiceProvided(boolean sourcenatServiceProvided) {
        this.sourcenatServiceProvided = sourcenatServiceProvided;
    }

    @Override
    public boolean isLbServiceProvided() {
        return lbServiceProvided;
    }

    public void setLbServiceProvided(boolean lbServiceProvided) {
        this.lbServiceProvided = lbServiceProvided;
    }

    public boolean isStaticnatServiceProvided() {
        return staticnatServiceProvided;
    }

    public void setStaticnatServiceProvided(boolean staticnatServiceProvided) {
        this.staticnatServiceProvided = staticnatServiceProvided;
    }

    public boolean isPortForwardingServiceProvided() {
        return portForwardingServiceProvided;
    }

    public void setPortForwardingServiceProvided(boolean portForwardingServiceProvided) {
        this.portForwardingServiceProvided = portForwardingServiceProvided;
    }

    @Override
    public boolean isUserdataServiceProvided() {
        return userdataServiceProvided;
    }

    public void setUserdataServiceProvided(boolean userdataServiceProvided) {
        this.userdataServiceProvided = userdataServiceProvided;
    }

    @Override
    public boolean isSecuritygroupServiceProvided() {
        return securitygroupServiceProvided;
    }

    public void setSecuritygroupServiceProvided(boolean securitygroupServiceProvided) {
        this.securitygroupServiceProvided = securitygroupServiceProvided;
    }
    
    @Override
    public String getUuid() {
        return this.uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public void setEnabledServices(List<Service> services){
        this.setVpnServiceProvided(services.contains(Service.Vpn));
        this.setDhcpServiceProvided(services.contains(Service.Dhcp));
        this.setDnsServiceProvided(services.contains(Service.Dns));
        this.setGatewayServiceProvided(services.contains(Service.Gateway));
        this.setFirewallServiceProvided(services.contains(Service.Firewall));
        this.setLbServiceProvided(services.contains(Service.Lb));
        this.setSourcenatServiceProvided(services.contains(Service.SourceNat));
        this.setStaticnatServiceProvided(services.contains(Service.StaticNat));
        this.setPortForwardingServiceProvided(services.contains(Service.PortForwarding));
        this.setUserdataServiceProvided(services.contains(Service.UserData));
        this.setSecuritygroupServiceProvided(services.contains(Service.SecurityGroup));
    }
    
    @Override
    public List<Service> getEnabledServices(){
        List<Service> services = new ArrayList<Service>();
        if(this.isVpnServiceProvided()){
            services.add(Service.Vpn);
        }
        if(this.isDhcpServiceProvided()){
            services.add(Service.Dhcp);
        }
        if(this.isDnsServiceProvided()){
            services.add(Service.Dns);
        }
        if(this.isGatewayServiceProvided()){
            services.add(Service.Gateway);
        }
        if(this.isFirewallServiceProvided()){
            services.add(Service.Firewall);
        }
        if(this.isLbServiceProvided()){
            services.add(Service.Lb);
        }
        if(this.sourcenatServiceProvided){
            services.add(Service.SourceNat);
        }
        if(this.staticnatServiceProvided){
            services.add(Service.StaticNat);
        }
        if(this.portForwardingServiceProvided){
            services.add(Service.PortForwarding);
        }
        if(this.isUserdataServiceProvided()){
            services.add(Service.UserData);
        }
        if(this.isSecuritygroupServiceProvided()){
            services.add(Service.SecurityGroup);
        }
        return services;
    }
}
