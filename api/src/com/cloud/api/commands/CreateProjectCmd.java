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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ProjectResponse;
import com.cloud.projects.Project;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description="Creates a project", responseObject=ProjectResponse.class)
public class CreateProjectCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateProjectCmd.class.getName());

    private static final String s_name = "createprojectresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="account who will own the project")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="domain ID of the account owning a project")
    private Long domainId;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="name of the project")
    private String name;

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="display text of the project")
    private String displayText;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="the zone id of the project")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getName() {
        return name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        Account account = UserContext.current().getCaller();
        if ((account == null) || isAdmin(account.getType())) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = _responseGenerator.findAccountByNameDomain(accountName, domainId);
                if (userAccount != null) {
                    return userAccount.getId();
                }
            }
        }

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }
 

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute(){
        UserContext.current().setEventDetails("Project Name: "+ getName() + ", zoneId " + zoneId);
        Project project = _projectService.createProject(getName(), getDisplayText(), getZoneId(), getAccountName(), getDomainId());
        if (project != null) {
            ProjectResponse response = _responseGenerator.createProjectResponse(project);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create a project");
        }
    }
}