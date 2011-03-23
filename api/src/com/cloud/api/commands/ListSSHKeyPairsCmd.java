package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.SSHKeyPairResponse;
import com.cloud.user.SSHKeyPair;

@Implementation(description="List registered keypairs", responseObject=SSHKeyPairResponse.class, includeInApiDoc=false) 
public class ListSSHKeyPairsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListSSHKeyPairsCmd.class.getName());
    private static final String s_name = "listsshkeypairsresponse";
    
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
	
	@Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="A key pair name to look for") 
	private String name;
	
    @Parameter(name="fingerprint", type=CommandType.STRING, description="A public key fingerprint to look for") 
    private String fingerprint;

    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    ///////////////////////////////////////////////////// 
    
	public String getName() {
		return name;
	}
	
	public String getFingerprint() {
		return fingerprint;
	}
    
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    
	@Override
	public void execute() {
		List<? extends SSHKeyPair> resultList = _mgr.listSSHKeyPairs(this);
		List<SSHKeyPairResponse> responses = new ArrayList<SSHKeyPairResponse>();
		for (SSHKeyPair result : resultList) {
			SSHKeyPairResponse r = new SSHKeyPairResponse(result.getName(), result.getFingerprint());
			r.setObjectName("keypair");
			responses.add(r);
		}
		
        ListResponse<SSHKeyPairResponse> response = new ListResponse<SSHKeyPairResponse>();
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
	}

	@Override
	public String getCommandName() {
		return s_name;
	}

}
