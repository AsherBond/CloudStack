<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>



<!-- VM detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_instanceicons.gif" alt="Instance" /></div>
    <h1 id="vm_name">
        Instance
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on" id="tab_details">
            <%=t.t("Details")%></div>
        <div class="content_tabs off" id="tab_volume">
            <%=t.t("Volume")%></div>
        <div class="content_tabs off" id="tab_statistics">
            <%=t.t("Statistics")%></div>
        <div class="content_tabs off" id="tab_router" style="display:none">
            <%=t.t("Router")%></div>
    </div>
    <!--Details tab (start)-->
    <div  id="tab_content_details">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div>  
        <div id="tab_container"> 
	        <div class="grid_container" style="display: block;">            
	            <div class="grid_header">
	            	<div id="title" class="grid_header_title">Title</div>
	                    <div class="grid_actionbox" id="action_link">
	                        <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
	                            <ul class="actionsdropdown_boxlist" id="action_list">
	                                <li><%=t.t("no.available.actions")%></li>
	                            </ul>
	                        </div>
	                    </div>
	                <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
	                display: none;">
	                    <div class="gridheader_loader" id="icon">
	                    </div>
	                    <p id="description">
	                        Waiting &hellip;</p>
	                </div>
	            </div>            
	            <div class="grid_rows odd">
	                <div class="vm_statusbox">
	                    <div id="view_console_container" style="float:left;">  
	                	    <div id="view_console_template" style="display:block">
	    					    <div class="vm_consolebox" id="box0">
	    					    </div>
	   						    <div class="vm_consolebox" id="box1" style="display: none">
	    					    </div>
						    </div>           
	                    </div>
	                    <div class="vm_status_textbox">
	                        <div class="vm_status_textline green" id="state">
	                        </div>
	                        <br />
	                        <p id="ipAddress">
	                        </p>
	                    </div>
	                </div>
	            </div>
	            
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("ID")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="id">
	                    </div>
	                </div>
	            </div>
	            
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Zone")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="zoneName">
	                    </div>
	                </div>
	            </div>        
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Name")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="vmname">
	                    </div>
	                    <input class="text" id="vmname_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="vmname_edit_errormsg" style="display:none"></div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("IP")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="ipaddress">
	                    </div>
	                </div>
	            </div>        
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Template")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="templateName">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Service")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="serviceOfferingName">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("HA.Enabled")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="haenable">                   
	                    </div>
	                    <select class="select" id="haenable_edit" style="width: 202px; display: none;">
	                        <option value="false">No</option>
							<option value="true">Yes</option>
	                    </select>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Created")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="created">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Account")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="account">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Domain")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="domain">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Host")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="hostName">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("ISO.attached")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="iso">                    
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Group")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="group">
	                    </div>
	                    <input class="text" id="group_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="group_edit_errormsg" style="display:none"></div>
	                </div>
	            </div>	
	        </div>
	        
            <div class="grid_botactionpanel">
	        	<div class="gridbot_buttons" id="save_button" style="display:none;">Save</div>
	            <div class="gridbot_buttons" id="cancel_button" style="display:none;">Cancel</div>
	        </div>
	        
	    </div>
    </div>  
    <!--Details tab (end)-->
    <!--Volume tab (start)-->
    <div style="display: none;" id="tab_content_volume">    
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div> 
        <div id="tab_container">        
        </div>
    </div>
    <!--Volume tab (end)-->
    <!--Statistics tab (start)-->
    <div style="display: none;" id="tab_content_statistics">
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container"> 
            <div class="grid_container">
        	    <div class="grid_header">
            	    <div id="grid_header_title" class="grid_header_title"></div>
                </div>
                
                
                <div class="dbrow odd" id="cpu_barchart">
                    <div class="dbrow_cell" style="width: 40%;">
                        <div class="dbgraph_titlebox">
                            <h2>
                                CPU</h2>
                            <div class="dbgraph_title_usedbox">
                                <p>
                                    Total: <span id="capacityused">
	                                    <span id="cpunumber">M</span> 
	                                    x 
	                                    <span id="cpuspeed">N</span> 
                                    </span>
                                </p>
                            </div>
                        </div>
                    </div>
                    <div class="dbrow_cell" style="width: 43%; border: none;">
                        <div class="db_barbox low" id="bar_chart">
                        </div>
                    </div>
                    <div class="dbrow_cell" style="width: 16%; border: none;">
                        <div class="db_totaltitle" id="percentused">
                        0%
                        </div>
                    </div>
                </div>
                
                
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            Network Read:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="networkkbsread">
                        </div>
                    </div>
                </div>
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            Network Write:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="networkkbswrite">
                        </div>
                    </div>
                </div>  
            </div>
        </div>   
    </div>
    <!--Statistics tab (end)-->
    <!--Routers tab (start)-->
    <div style="display: none;" id="tab_content_router">
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div>  
        <div id="tab_container">
        </div>
    </div>
    <!--Routers tab (end)-->
</div>
<!-- VM detail panel (end) -->
<!-- VM wizard (begin)-->
<div id="vm_popup" class="vmpopup_container" style="display: none">
    <div id="step1" style="display: block;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step1_bg.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 4</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 5</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        Step 1: <strong>Select a Template</strong></h2>
                    <p>
                        Please select a template for your new virtual instance. You can also choose to select
                        a blank template from which an ISO image can be installed onto.
                    </p>
                </div>
                <div class="vmpopup_contentpanel">
                    <div class="rev_tempsearchpanel">
                        <label for="wizard_zone">
                            Availability Zone:</label>
                        <select class="select" id="wizard_zone" name="zone">
                        </select>
                        
                                        
                        
                        <div class="rev_tempsearchbox">
                            <form method="post" action="#">
                            <ol>
                                <li>
                                    <input id="search_input" class="text" type="text" name="search_input" />
                            </ol>
                            </form>
                            <div id="search_button" class="rev_searchbutton">
                                Search</div>
                        </div>
                    </div>
                    <div class="rev_wizformarea">
                        <div class="revwiz_message_container" style="display: none;" id="wiz_message">
                            <div class="revwiz_message_top">
                                <p id="wiz_message_text">
                                    Please select a template or ISO to continue</p>
                            </div>
                            <div class="revwiz_message_bottom">
                                <div class="revwizcontinue_button" id="wiz_message_continue">
                                </div>
                            </div>
                        </div>
                        <div class="rev_wizmid_tempbox">
                            <div class="revwiz_loadingbox" id="wiz_template_loading" style="display: none">
                                <div class="loading_gridanimation">
                                </div>
                                <p>
                                    Loading...</p>
                            </div>
                            <div class="rev_wizmid_tempbox_left" id="wiz_template_filter">
                                <div class="rev_wizmid_selectedtempbut" id="wiz_featured">
                                    Featured Template</div>
                                <div class="rev_wizmid_nonselectedtempbut" id="wiz_my">
                                    My Template</div>
                                <div class="rev_wizmid_nonselectedtempbut" id="wiz_community">
                                    Community Template</div>
                                <div class="rev_wizmid_nonselectedtempbut" id="wiz_blank">
                                    Blank Template</div>
                            </div>
                            <div class="rev_wizmid_tempbox_right">
                                <div class="rev_wiztemplistpanel" id="template_container">  
                                    
                                </div>
                                <div class="rev_wiztemplistactions">
                                    <div class="rev_wiztemplist_actionsbox">
                                        <a href="#" id="prev_page">Prev</a> <a href="#" id="next_page">Next</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step" style="display: none;">
                    </div>
                    <div class="vmpop_nextbutton" id="next_step">
                        Go to Step 2</div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
    <div id="step2" style="display: none;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 4</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 5</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        Step 2: <strong>Service Offering</strong></h2>
                    <p>
                        <!--  
                        Please select the CPU, Memory and Storage requirement you need for your new Virtual
                        Instance-->
                    </p>
                </div>
                <div class="vmpopup_contentpanel">
                    <h3>
                        <!--Service Offering-->
                    </h3>
                    
                    <div class="revwiz_message_container" style="display: none;" id="wiz_message">
                        <div class="revwiz_message_top">
                            <p id="wiz_message_text">
                                Please select a service offering to continue</p>
                        </div>
                        <div class="revwiz_message_bottom">
                            <div class="revwizcontinue_button" id="wiz_message_continue">
                            </div>
                        </div>
                    </div>
                    
                    <div class="vmpopup_offeringpanel" id="service_offering_container">
                        <!--
                        <div class="vmpopup_offeringbox">
                            <input type="radio" name="radiogroup" class="radio" />
                            <label class="label">
                                X-Large Instance</label>
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                        4 x 2.00 Ghz CPU, 16.00 GB of Memory, High Availability Enabled</p>
                                </div>
                            </div>
                        </div>
                        <div class="vmpopup_offeringbox">
                            <input type="radio" name="radiogroup" class="radio" />
                            <label class="label">
                                Large Instance</label>
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                        4 x 2.00 Ghz CPU, 16.00 GB of Memory, High Availability Enabled</p>
                                </div>
                            </div>
                        </div>
                        <div class="vmpopup_offeringbox">
                            <input type="radio" name="radiogroup" class="radio" />
                            <label class="label">
                                Medium Instance</label>
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                        4 x 2.00 Ghz CPU, 16.00 GB of Memory, High Availability Enabled</p>
                                </div>
                            </div>
                        </div>
                        <div class="vmpopup_offeringbox">
                            <input type="radio" name="radiogroup" class="radio" />
                            <label class="label">
                                Small Instance</label>
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                        4 x 2.00 Ghz CPU, 16.00 GB of Memory, High Availability Enabled</p>
                                </div>
                            </div>
                        </div>
                        -->
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step">
                        Back</div>
                    <div class="vmpop_nextbutton" id="next_step">
                        Go to Step 3</div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
    <div id="step3" style="display: none;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                Step 4</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 5</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        Step 3: <strong id="step3_label">Select a Disk Offering</strong></h2>
                    <p>
                    </p>
                </div>
                <div class="vmpopup_contentpanel">
                    <h3>
                    </h3>
                    
                    <div class="revwiz_message_container" style="display: none;" id="wiz_message">
                        <div class="revwiz_message_top">
                            <p id="wiz_message_text">
                                Please select a disk offering to continue</p>
                        </div>
                        <div class="revwiz_message_bottom">
                            <div class="revwizcontinue_button" id="wiz_message_continue">
                            </div>
                        </div>
                    </div>
                    
                    <div class="vmpopup_offeringpanel" id="data_disk_offering_container" style="display: none">
                    </div>
                    <div class="vmpopup_offeringpanel" id="root_disk_offering_container" style="display: none">
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step">
                        Back</div>
                    <div class="vmpop_nextbutton" id="next_step">
                        Go to Step 4</div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
    <div id="step4" style="display: none;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                Step 4</div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                Step 5</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        Step 4: <strong>Network</strong></h2>
                    <p>
                    </p>
                </div>
                <div class="vmpopup_contentpanel">
                    <h3>
                    </h3>
                    <div class="vmpopup_offeringpanel" style="position:relative;">
                    	<div id="underconstruction">
                            <div class="vmpopup_offeringpanel_overlay"></div>
                            <div class="vmpopup_offeringpanel_underconstructionbox"><p> Under Construction </p></div>
                        </div>
                        <div class="vmpopup_offeringbox">
                            <input type="radio" name="radiogroup" class="radio" selected />
                            <label class="label">
                                My Virtual Network</label>
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                        A virtual private network that is fronted by a virtual router. 
                                    </p>
                                </div>
                            </div>
                        </div>
						<div class="vmpopup_offeringbox">
                            <input type="radio" name="radiogroup" class="radio" selected />
                            <label class="label">
                                My Virtual Network with Guest CIDR support.  Specify guest CIDR: 
							</label>
							<input type="text" name="disksize" class="text" />
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                        A virtual private network that is fronted by a virtual router.  An optional guest CIDR can be specified.
                                    </p>
                                </div>
                            </div>
                        </div>
                        <div class="vmpopup_offeringbox" style="margin-top: 15px;">
                            <input type="radio" name="radiogroup" class="radio" />
                            <label class="label">
                                My Direct Network:</label>
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                       A network where the virtual instance is directly assigned an IP from.
									</p>
                                </div>
                            </div>
                        </div>
						<div class="vmpopup_offeringbox" style="margin-top: 15px;">
                            <input type="radio" name="radiogroup" class="radio" />
                            <label class="label">
                                My Direct Shared Network: </label>
                            <select class="select">
							  <option>VLAN 100</option>
							  <option>VLAN 101</option>
							  <option>VLAN 102</option>
							</select>
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                       A network where the virtual instance is directly assigned an IP from.  A VLAN must be selected.
									</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step">
                        Back</div>
                    <div class="vmpop_nextbutton" id="next_step">
                        Go to Step 5</div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
    <div id="step5" style="display: none;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 4</div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                Step 5</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_slectedbg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        Step 5: <strong>Last Step</strong></h2>
                    <p>
                    </p>
                </div>
                <div class="vmpopup_contentpanel">
                    <h3>
                    </h3>
                    <div class="vmpopup_offeringpanel" style="margin-top: 10px;">
                        <div class="vmpopup_reviewbox odd">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    Zone:</div>
                                <span id="wizard_review_zone"></span>
                            </div>
                        </div>                        
                        <div class="vmpopup_reviewbox even">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    Hypervisor:</div>
                                <span id="wizard_review_hypervisor"></span>
                            </div>
                        </div>                        
                        <div class="vmpopup_reviewbox odd">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    Template:
                                </div>
                                <span id="wizard_review_template"></span>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox even">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    Service Offering:</div>
                                <span id="wizard_review_service_offering"></span>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox odd">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label" id="wizard_review_disk_offering_label">
                                    Disk Offering:
                                </div>
                                <span id="wizard_review_disk_offering"></span>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox even">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    Network:</div>
                                <span id="wizard_review_network"></span>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox odd">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    Name (optional):
                                </div>
                                <input class="text" type="text" id="wizard_vm_name" />
                                <div id="wizard_vm_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                                </div>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox even">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    Group (optional):</div>
                                <input class="text" type="text" id="wizard_vm_group" />
                                <div id="wizard_vm_group_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step">
                        Back</div>
                    <div class="vmpop_nextbutton" id="next_step">
                        Submit</div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
</div>
<!-- VM wizard (end)-->

<!-- VM Wizard - VM template (begin) -->
<div id="vmtemplate_in_vmwizard" class="rev_wiztemplistbox" style="display:none">
    <div id="icon">
    </div>
    <div class="rev_wiztemp_listtext">
        <span id="name"></span>
    </div>
    <div class="rev_wiztemp_hypervisortext">
            Hypervisor: <strong id="hypervisor_text"></strong>
    </div>
    <div class="rev_wiztemp_ownertext">
        [Submitted by: <span id="submitted_by"></span>]</div>
</div>
<!-- VM Wizard - VM template (end) -->

<!-- VM Wizard - ISO template (begin) -->
<div id="vmiso_in_vmwizard" class="rev_wiztemplistbox" style="display:none">
    <div id="icon" class="rev_wiztemo_centosicons">
    </div>
    <div class="rev_wiztemp_listtext">
        <span id="name">Centos</span>
    </div>
    <div class="rev_wiztemp_hypervisortext">
        Hypervisor:
        <select id="hypervisor_select" class="select" style="width: 70px; float: none; height: 15px; font-size: 10px; margin: 0 0 0 5px; display: inline;">
            <option value='XenServer'>XenServer</option>
            <option value='VmWare'>VmWare</option>
            <option value='KVM'>KVM</option>
        </select>
    </div>
    <div class="rev_wiztemp_ownertext">
        [Submitted by: <span id="submitted_by"></span>]</div>
</div>
<!-- VM Wizard - ISO template (end) -->

<!-- VM Wizard - Service Offering template (begin) -->
<div class="vmpopup_offeringbox" id="vm_popup_service_offering_template" style="display: none">
    <input type="radio" name="service_offering_radio" class="radio" checked />
    <label class="label" id="name">
    </label>
    <div class="vmpopup_offdescriptionbox">
        <div class="vmpopup_offdescriptionbox_top">
        </div>
        <div class="vmpopup_offdescriptionbox_bot">
            <p id="description">
            </p>
        </div>
    </div>
</div>
<!-- VM Wizard - Service Offering template (end) -->
<!-- VM Wizard - disk Offering template (begin)-->
<div class="vmpopup_offeringbox" id="vm_popup_disk_offering_template_no" style="display: none">
    <input type="radio" class="radio" value="no" checked />
    <label class="label">
        No Thanks</label>
</div>
<div class="vmpopup_offeringbox" id="vm_popup_disk_offering_template_custom" style="display: none">
    <input type="radio" class="radio" value="custom" checked />
    <label class="label" id="name">
    </label>
    <div class="vmpopup_offdescriptionbox_bot" style="background:none; border:none;">
        <label class="label1" style="margin-left:33px; display:inline;">
            Disk Size:</label>
        <input type="text" id="custom_disk_size" class="text" />
        <span>GB</span>
       
        <div id="custom_disk_size_errormsg" class="errormsg" style="display: none; margin-left:89px; display:inline;">
        </div>
     </div>
</div>
<div class="vmpopup_offeringbox" id="vm_popup_disk_offering_template_existing" style="display: none">
    <input type="radio" class="radio" checked />
    <label class="label" id="name">
    </label>
    <div class="vmpopup_offdescriptionbox">
        <div class="vmpopup_offdescriptionbox_top">
        </div>
        <div class="vmpopup_offdescriptionbox_bot">
            <p id="description">
            </p>
        </div>
    </div>
</div>
<!-- VM Wizard - disk Offering template (end)-->

<!--  volume tab template (begin) -->
<div class="grid_container" id="volume_tab_template" style="display: none">	
    <div class="grid_header">
        <div class="grid_header_title" id="title">
        </div>
        <div class="grid_actionbox" id="volume_action_link">
            <div class="grid_actionsdropdown_box" id="volume_action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; border: 1px solid #999; ">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                Waiting &hellip;
            </p>
        </div>       
    </div>
    
    <div class="grid_rows" id="after_action_info_container" style="display:none">
        <div class="grid_row_cell" style="width: 90%; border: none;">
            <div class="row_celltitles">
                <strong id="after_action_info">Message will appear here</strong></div>
        </div>
    </div>
        
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                ID:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="id">
            </div>
        </div>
    </div>
     <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Name:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="name">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Type:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="type">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Size:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="size">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Created:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="created">
            </div>
        </div>
    </div>
</div>
<!--  volume tab template (end) -->

<!-- view console template (begin)  -->
<div id="view_console_template" style="display:none">
    <div class="vm_consolebox" id="box0">
    </div>
    <div class="vm_consolebox" id="box1" style="display: none">
    </div>
</div>
<!-- view console template (end)  -->

<!--  router tab template (begin) -->
<div class="grid_container" id="router_tab_template" style="display: none">	
    <div class="grid_header">
        <div class="grid_header_title" id="title">
        </div>
        <div class="grid_actionbox" id="router_action_link">
            <div class="grid_actionsdropdown_box" id="router_action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; border: 1px solid #999;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                Waiting &hellip;
            </p>
        </div>
    </div>
    <div class="grid_rows" id="after_action_info_container" style="display: none">
        <div class="grid_row_cell" style="width: 90%; border: none;">
            <div class="row_celltitles">
                <strong id="after_action_info">Message will appear here</strong></div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="vm_statusbox">
            <div id="view_console_container" style="float:left;">
                <div id="view_console_template" style="display: block">
                    <div class="vm_consolebox" id="box0">
                    </div>
                    <div class="vm_consolebox" id="box1" style="display: none">
                    </div>
                </div>
            </div>
            <div class="vm_status_textbox">
                <div class="vm_status_textline green" id="state">
                </div>
                <br />
                <p id="ipAddress">
                </p>
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Zone")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="zonename">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Name")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="name">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Public IP")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="publicip">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Private IP")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="privateip">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Guest IP")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="guestipaddress">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Host")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="hostname">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Network Domain")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="networkdomain">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Account")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="account">
            </div>
        </div>
    </div>
	<div class="grid_rows even">
		<div class="grid_row_cell" style="width: 20%;">
			<div class="row_celltitles">
				<%=t.t("Domain")%>:</div>
		</div>
		<div class="grid_row_cell" style="width: 79%;">
			<div class="row_celltitles" id="domain">
			</div>
		</div>
	</div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Created")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="created">
            </div>
        </div>
    </div>
</div>
<!--  router tab template (end) -->

<!--  ***** Dialogs (begin) ***** -->
<!-- Detach ISO Dialog -->
<div id="dialog_detach_iso_from_vm" title="Confirmation" style="display:none">
    <p><%=t.t("please.confirm.you.want.to.detach.an.iso.from.the.virtual.machine")%></p>   
</div>

<!-- Attach ISO Dialog -->
<div id="dialog_attach_iso" title="Attach ISO" style="display: none">
    <p> 
        <%=t.t("please.specify.the.iso.you.wish.to.attach.to.virtual.machine")%>        
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <%=t.t("iso")%>:</label>
                <select class="select" id="attach_iso_select">
                    <option value="none"><%=t.t("no.available.iso")%></option>
                </select>
                <div id="attach_iso_select_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
            </li>
        </ol>
        </form>
    </div>
</div>

<!-- Change Name Dialog -->
<div id="dialog_change_name" title="Change Name" style="display: none">
    <p> 
        <%=t.t("please.specify.the.new.name.you.want.to.change.for.the.virtual.machine")%>        
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <%=t.t("instance.name")%>:</label>
                <input class="text" type="text" id="change_instance_name" />
                <div id="change_instance_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>

<!-- Change Group Dialog -->
<div id="dialog_change_group" title="Change Group" style="display: none">
    <p>
        <%=t.t("please.specify.the.new.group.you.want.to.assign.the.virtual.machine.to")%>        
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <%=t.t("group.name")%>:</label>
                <input class="text" type="text" id="change_group_name" />
                <div id="change_group_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>


<!-- Change Service Offering Dialog -->
<div id="dialog_change_service_offering" title="Change Service Offering" style="display: none">
    <p> 
        <%=t.t("after.changing.service.offering.you.must.restart.the.virtual.machine.for.new.service.offering.to.take.effect")%>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <%=t.t("service.offering")%>:</label>
                <select class="select" id="change_service_offerings">
                </select>
                <div id="change_service_offerings_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>

<!-- Create template of disk volume dialog (begin) -->
<div id="dialog_create_template" title="Create template of disk volume" style="display: none">
    <p> 
        <%=t.t("creating.a.template.of.disk.volume.could.take.up.to.several.hours.depending.on.the.size.of.the.disk.volume")%>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <%=t.t("name")%>:</label>
                <input class="text" type="text" name="create_template_name" id="create_template_name" />
                <div id="create_template_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <%=t.t("display.text")%>:</label>
                <input class="text" type="text" name="create_template_desc" id="create_template_desc" />
                <div id="create_template_desc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="create_template_os_type">
                    <%=t.t("os.type")%>:</label>
                <select class="select" name="create_template_os_type" id="create_template_os_type">
                </select>
            </li>
            <li>
                <label for="create_template_public">
                    <%=t.t("public")%>:</label>
                <select class="select" name="create_template_public" id="create_template_public">
                    <option value="false">No</option>
                    <option value="true">Yes</option>
                </select>
            </li>
            <li>
                <label>
                    <%=t.t("password.enabled")%>?:</label>
                <select class="select" name="create_template_password" id="create_template_password">
                    <option value="false">No</option>
                    <option value="true">Yes</option>
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Create template of disk volume dialog (end) -->

<div id="dialog_confirmation_change_root_password" title="Confirmation" style="display:none">
    <p>
        <%=t.t("please.confirm.you.want.to.change.the.root.password.for.the.virtual.machine")%>        
    </p>
</div>

<div id="dialog_confirmation_enable_ha" title="Confirmation" style="display:none">
    <p>
        <%=t.t("please.confirm.you.want.to.enable.HA.for.your.virtual.machine.once.HA.is.enabled.your.virtual.machine.will.be.automatically.restarted.in.the.event.it.is.detected.to.have.failed")%>
    </p>
</div>

<div id="dialog_confirmation_disable_ha" title="Confirmation" style="display:none">
    <p>
        <%=t.t("please.confirm.you.want.to.disable.HA.for.the.virtual.machine.once.HA.is.disabled.the.virtual.machine.will.no.longer.be.automatically.restarted.in.the.event.of.a.failure")%>
    </p>
</div>

<div id="dialog_confirmation_start_vm" title="Confirmation" style="display:none">
    <p>        
        <%=t.t("please.confirm.you.want.to.start.instance")%>
    </p>
</div>

<div id="dialog_confirmation_stop_vm" title="Confirmation" style="display:none">
    <p>        
        <%=t.t("please.confirm.you.want.to.stop.instance")%>
    </p>
</div>

<div id="dialog_confirmation_reboot_vm" title="Confirmation" style="display:none">
    <p>        
        <%=t.t("please.confirm.you.want.to.reboot.instance")%>
    </p>
</div>

<div id="dialog_confirmation_destroy_vm" title="Confirmation" style="display:none">
    <p>        
        <%=t.t("please.confirm.you.want.to.destroy.instance")%>
    </p>
</div>

<div id="dialog_confirmation_restore_vm" title="Confirmation" style="display:none">
    <p>        
        <%=t.t("please.confirm.you.want.to.restore.instance")%>
    </p>
</div>

<div id="dialog_confirmation_start_router" title="Confirmation" style="display:none">
    <p>        
        <%=t.t("please.confirm.you.want.to.start.router")%>
    </p>
</div>

<div id="dialog_confirmation_stop_router" title="Confirmation" style="display:none">
    <p>        
        <%=t.t("please.confirm.you.want.to.stop.router")%>
    </p>
</div>

<div id="dialog_confirmation_reboot_router" title="Confirmation" style="display:none">
    <p>        
        <%=t.t("please.confirm.you.want.to.reboot.router")%>
    </p>
</div>

<!--  ***** Dialogs (end) ***** -->
