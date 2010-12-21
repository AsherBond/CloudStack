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
  
function afterLoadClusterJSP($leftmenuItem1) {
    showMiddleMenu();
     
    clearAddButtonsOnTop(); 
    initAddHostButton($("#midmenu_add_host_button"), "cluster_page", $leftmenuItem1); 
    initAddPrimaryStorageButton($("#midmenu_add_primarystorage_button"), "cluster_page", $leftmenuItem1);  
    
    initDialog("dialog_add_host");
    initDialog("dialog_add_pool");    
    bindEventHandlerToDialogAddPool($("#dialog_add_pool"));	       
    
	clusterJsonToRightPanel($leftmenuItem1);	
	var clusterId = $leftmenuItem1.data("jsonObj").id;            
    var $midmenuContainer = $("#midmenu_container").empty();	
            
    var $container_host = $("<div id='midmenu_host_container'></div>"); 
    $midmenuContainer.append($container_host);        
    var $header1 = $("#midmenu_itemheader_without_margin").clone().show();  //without margin on top
    $header1.find("#name").text("Host");
    $container_host.append($header1);    
    var count = 0;    
    $.ajax({
        cache: false,
        data: createURL("command=listHosts&type=Routing&clusterid="+clusterId),
        dataType: "json",
        async: false,
        success: function(json) { 
            selectedItemsInMidMenu = {};    	                
            var items = json.listhostsresponse.host;      
            if(items != null && items.length > 0) {
                for(var i=0; i<items.length;i++) { 
                    var $midmenuItem1 = $("#midmenu_item").clone();                      
                    $midmenuItem1.data("toRightPanelFn", hostToRightPanel);                             
                    hostToMidmenu(items[i], $midmenuItem1);    
                    bindClickToMidMenu($midmenuItem1, hostToRightPanel, hostGetMidmenuId);             
                   
                    $container_host.append($midmenuItem1.show());   
                    if(i == 0)  { //click the 1st item in middle menu as default                        
                        $midmenuItem1.click();   
                    }                 
                }  
                count = items.length;
            }  
            else {
                $container_host.append($("#midmenu_container_no_items_available").clone().attr("id","midmenu_container_no_items_available_clone").show());  
            }                  
        }
    });	 	
	
    listMidMenuItems("hosts", ("listHosts&type=Routing&clusterid="+objCluster.id), hostGetSearchParams, "listhostsresponse", "host", "jsp/host.jsp", 
    	afterLoadHostJSP, hostToMidmenu, hostToRightPanel, getMidmenuId, false, ("cluster_"+objCluster.id));    
}

function clusterJsonToRightPanel($leftmenuItem1) {
    $("#right_panel_content").data("$leftmenuItem1", $leftmenuItem1);
    clusterJsonToDetailsTab();    
}

function clusterJsonToDetailsTab() {	
    var $leftmenuItem1 = $("#right_panel_content").data("$leftmenuItem1");
    if($leftmenuItem1 == null)
        return;
    
    var jsonObj = $leftmenuItem1.data("jsonObj");    
    if(jsonObj == null) 
	    return;	
        
    $.ajax({
        data: createURL("command=listClusters&id="+jsonObj.id),
        dataType: "json",
        async: false,
        success: function(json) {            
            var items = json.listclustersresponse.cluster;	           
			if(items != null && items.length > 0) {
                jsonObj = items[0];
                $leftmenuItem1.data("jsonObj", jsonObj);                  
            }
        }
    });     
     
    var $detailsTab = $("#right_panel_content").find("#tab_content_details");   
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));        
    $detailsTab.find("#podname").text(fromdb(jsonObj.podname));            
}

