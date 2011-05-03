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
  
function afterLoadClusterJSP($midmenuItem1) {
    hideMiddleMenu();  
    initDialog("dialog_add_host", 400);
    
    //add pool dialog
    initDialog("dialog_add_pool", 400);   
    bindEventHandlerToDialogAddPool($("#dialog_add_pool"));	 
}


function clusterToRightPanel($midmenuItem1) {  
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);        
    clusterJsonToDetailsTab(); 
}

function clusterClearRightPanel() {
    clusterClearDetailsTab();
}

function clusterJsonToDetailsTab() {	   
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        clusterClearDetailsTab();
        return;
    }
    
    var jsonObj = $midmenuItem1.data("jsonObj");    
    if(jsonObj == null) {
        clusterClearDetailsTab();
	    return;	
	}
    
    bindAddHostButton($midmenuItem1); 
    bindAddPrimaryStorageButton($midmenuItem1);  
        
    $.ajax({
        data: createURL("command=listClusters&id="+jsonObj.id),
        dataType: "json",
        async: false,
        success: function(json) {            
            var items = json.listclustersresponse.cluster;	           
			if(items != null && items.length > 0) {
                jsonObj = items[0];
                $midmenuItem1.data("jsonObj", jsonObj);                  
            }
        }
    });     
     
    var $thisTab = $("#right_panel_content").find("#tab_content_details");   
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename));        
    $thisTab.find("#podname").text(fromdb(jsonObj.podname));    
    $thisTab.find("#hypervisortype").text(fromdb(jsonObj.hypervisortype));
    $thisTab.find("#clustertype").text(fromdb(jsonObj.clustertype));
    $thisTab.find("#allocationstate").text(fromdb(jsonObj.allocationstate));
    
    //actions ***   
    var $actionLink = $thisTab.find("#action_link"); 
    bindActionLink($actionLink);
    /*
    $actionLink.bind("mouseover", function(event) {	    
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#action_menu").hide();    
        return false;
    });	  
    */
    
    var $actionMenu = $actionLink.find("#action_menu");
    $actionMenu.find("#action_list").empty();       
    buildActionLinkForTab("label.action.delete.cluster", clusterActionMap, $actionMenu, $midmenuItem1, $thisTab);        
}

function clusterClearDetailsTab() {	   
    var $thisTab = $("#right_panel_content").find("#tab_content_details");   
    $thisTab.find("#grid_header_title").text("");
    $thisTab.find("#id").text("");
    $thisTab.find("#name").text("");
    $thisTab.find("#zonename").text("");        
    $thisTab.find("#podname").text("");     
    $thisTab.find("#hypervisortype").text("");
    $thisTab.find("#clustertype").text("");
    
    //actions ***   
    var $actionMenu = $thisTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();   
	$actionMenu.find("#action_list").append($("#no_available_actions").clone().show());	       
}

var clusterActionMap = {   
    "label.action.delete.cluster": {  
        api: "deleteCluster",            
        isAsyncJob: false,      
        dialogBeforeActionFn : doDeleteCluster,   
        inProcessText: "label.action.delete.cluster.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {     
            $midmenuItem1.slideUp("slow", function() {
                $(this).remove();                
                if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#id").text()) {
                    clearRightPanel();
                    clusterClearRightPanel();
                }                
            });            
        }
    }
}

function doDeleteCluster($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
		
	$("#dialog_confirmation")
	.text(dictionary["message.action.delete.cluster"])
	.dialog('option', 'buttons', { 					
		"Confirm": function() { 			
			$(this).dialog("close");			
			var apiCommand = "command=deleteCluster&id="+id;
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		}
	}).dialog("open");
}