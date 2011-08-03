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
package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class RemoteAccessVpnResponse extends BaseResponse {
    
    @SerializedName(ApiConstants.PUBLIC_IP_ID) @Param(description="the public ip address of the vpn server")
    private Long publicIpId;
    
    @SerializedName(ApiConstants.PUBLIC_IP) @Param(description="the public ip address of the vpn server")
    private String publicIp;
    
    @SerializedName("iprange") @Param(description="the range of ips to allocate to the clients")
    private String ipRange;

    @SerializedName("presharedkey") @Param(description="the ipsec preshared key")
    private String presharedKey;

    @SerializedName("account") @Param(description="the account of the remote access vpn")
    private String accountName;

    @SerializedName("domainid") @Param(description="the domain id of the account of the remote access vpn")
	private long domainId;

    @SerializedName("domainname") @Param(description="the domain name of the account of the remote access vpn")
	private String domainName;
    
    @SerializedName("state") @Param(description="the state of the rule")
    private String state;
    
	public String getAccountName() {
		return accountName;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(String publicIp) {
		this.publicIp = publicIp;
	}

	public String getIpRange() {
		return ipRange;
	}

	public void setIpRange(String ipRange) {
		this.ipRange = ipRange;
	}

	public String getPresharedKey() {
		return presharedKey;
	}

	public void setPresharedKey(String presharedKey) {
		this.presharedKey = presharedKey;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
		
	}

	public void setDomainId(long domainId) {
		this.domainId = domainId;
		
	}

	public void setDomainName(String name) {
		this.domainName = name;		
	}

	public long getDomainId() {
		return domainId;
	}

	public String getDomainName() {
		return domainName;
	}

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getPublicIpId() {
        return publicIpId;
    }

    public void setPublicIpId(Long publicIpId) {
        this.publicIpId = publicIpId;
    }
    
}
