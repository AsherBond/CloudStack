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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.VpnUsersResponse;
import com.cloud.network.VpnUser;

@Implementation(description="Lists vpn users", responseObject=VpnUsersResponse.class)
public class ListVpnUsersCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger (ListVpnUsersCmd.class.getName());

    private static final String s_name = "listvpnusersresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING, description="the account of the remote access vpn. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG, description="the domain ID of the remote access vpn. If used with the account parameter, lists remote access vpns for the account in the specified domain.")
    private Long domainId;

    @Parameter(name="id", type=CommandType.LONG, description="the ID of the vpn user")
    private Long id;

    @Parameter(name="username", type=CommandType.STRING, description="the username of the vpn user.")
    private String userName;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return userName;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public void execute(){
        List<? extends VpnUser> vpnUsers = _ravService.searchForVpnUsers(this);

        ListResponse<VpnUsersResponse> response = new ListResponse<VpnUsersResponse>();
        List<VpnUsersResponse> vpnResponses = new ArrayList<VpnUsersResponse>();
        for (VpnUser vpnUser : vpnUsers) {
            vpnResponses.add(_responseGenerator.createVpnUserResponse(vpnUser));
        }

        response.setResponses(vpnResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
