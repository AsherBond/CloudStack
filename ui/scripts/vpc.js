// Copyright 2012 Citrix Systems, Inc. Licensed under the
// Apache License, Version 2.0 (the "License"); you may not use this
// file except in compliance with the License.  Citrix Systems, Inc.
// reserves all rights not expressly granted by the License.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License. 
(function($, cloudStack) {
  var aclMultiEdit = {
    noSelect: true,
    fields: {
      'cidrlist': { edit: true, label: 'Source CIDR' },
      'protocol': {
        label: 'Protocol',
        select: function(args) {
          args.$select.change(function() {
            var $inputs = args.$form.find('input');
            var $icmpFields = $inputs.filter(function() {
              var name = $(this).attr('name');

              return $.inArray(name, [
                'icmptype',
                'icmpcode'
              ]) > -1;
            });
            var $otherFields = $inputs.filter(function() {
              var name = $(this).attr('name');

              return name != 'icmptype' && name != 'icmpcode' && name != 'cidrlist';
            });

            if ($(this).val() == 'icmp') {
              $icmpFields.attr('disabled', false);
              $otherFields.attr('disabled', 'disabled');
            } else {
              $otherFields.attr('disabled', false);
              $icmpFields.attr('disabled', 'disabled');
            }
          });

          args.response.success({
            data: [
              { name: 'tcp', description: 'TCP' },
              { name: 'udp', description: 'UDP' },
              { name: 'icmp', description: 'ICMP' }
            ]
          });
        }
      },
      'startport': { edit: true, label: 'Start Port' },
      'endport': { edit: true, label: 'End Port' },
      'icmptype': { edit: true, label: 'ICMP Type', isDisabled: true },
      'icmpcode': { edit: true, label: 'ICMP Code', isDisabled: true },
      'add-rule': {
        label: 'Add',
        addButton: true
      }
    },
    add: {
      label: 'Add',
      action: function(args) {
        setTimeout(function() {
          args.response.success({
            notification: {
              label: 'Add ACL rule',
              poll: function(args) { args.complete(); }
            }
          });
        }, 500);
      }
    },
    actions: {
      destroy: {
        label: 'Remove Rule',
        action: function(args) {
          setTimeout(function() {
            args.response.success({
              notification: {
                label: 'Remove ACL rule',
                poll: function(args) { args.complete(); }
              }
            });
          }, 500);
        }
      }
    },
    dataProvider: function(args) {
      setTimeout(function() {
        args.response.success({
          data: [
            {
              "id": 11,
              "protocol": "icmp",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/0",
              "icmptype": 2,
              "icmpcode": 22
            },
            {
              "id": 10,
              "protocol": "udp",
              "startport": "500",
              "endport": "10000",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/24"
            },
            {
              "id": 9,
              "protocol": "tcp",
              "startport": "20",
              "endport": "200",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/24"
            },
            {
              "id": 11,
              "protocol": "icmp",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/0",
              "icmptype": 2,
              "icmpcode": 22
            },
            {
              "id": 10,
              "protocol": "udp",
              "startport": "500",
              "endport": "10000",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/24"
            },
            {
              "id": 9,
              "protocol": "tcp",
              "startport": "20",
              "endport": "200",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/24"
            },
            {
              "id": 11,
              "protocol": "icmp",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/0",
              "icmptype": 2,
              "icmpcode": 22
            },
            {
              "id": 10,
              "protocol": "udp",
              "startport": "500",
              "endport": "10000",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/24"
            },
            {
              "id": 9,
              "protocol": "tcp",
              "startport": "20",
              "endport": "200",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/24"
            },
            {
              "id": 11,
              "protocol": "icmp",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/0",
              "icmptype": 2,
              "icmpcode": 22
            },
            {
              "id": 10,
              "protocol": "udp",
              "startport": "500",
              "endport": "10000",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/24"
            },
            {
              "id": 9,
              "protocol": "tcp",
              "startport": "20",
              "endport": "200",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/24"
            },
            {
              "id": 11,
              "protocol": "icmp",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/0",
              "icmptype": 2,
              "icmpcode": 22
            },
            {
              "id": 10,
              "protocol": "udp",
              "startport": "500",
              "endport": "10000",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/24"
            },
            {
              "id": 9,
              "protocol": "tcp",
              "startport": "20",
              "endport": "200",
              "ipaddressid": 4,
              "ipaddress": "10.223.71.23",
              "state": "Active",
              "cidrlist": "0.0.0.0/24"
            }
          ]
        });
      }, 100);
    }
  };
  
  cloudStack.vpc = {
    vmListView: {
      id: 'vpcTierInstances',
      listView: {
        filters: {
          mine: { label: 'My instances' },
          all: { label: 'All instances' },
          running: { label: 'Running instances' },
          destroyed: { label: 'Destroyed instances' }
        },
        fields: {
          name: { label: 'Name', editable: true },
          account: { label: 'Account' },
          zonename: { label: 'Zone' },
          state: {
            label: 'Status',
            indicator: {
              'Running': 'on',
              'Stopped': 'off',
              'Destroyed': 'off'
            }
          }
        },

        // List view actions
        actions: {
          restart: {
            label: 'Restart instance',
            action: function(args) {
              setTimeout(function() {
                args.response.success({
                  data: {
                    state: 'Restarting'
                  }
                });
              }, 1000);
            },
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to restart ' + args.name + '?';
              },
              notification: function(args) {
                return 'Rebooting VM: ' + args.name;
              }
            },
            notification: {
              poll: function(args) { args.complete(); }
            }
          },
          stop: {
            label: 'Stop instance',
            action: function(args) {
              setTimeout(function() {
                args.response.success({
                  data: { state: 'Stopping' }
                });
              }, 500);
            },
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to shutdown ' + args.name + '?';
              },
              notification: function(args) {
                return 'Rebooting VM: ' + args.name;
              }
            },
            notification: {
              poll: function(args) { args.complete(); }
            }
          },
          start: {
            label: 'Start instance',
            action: function(args) {
              setTimeout(function() {
                args.response.success({
                  data: { state: 'Starting' }
                });
              }, 500);
            },
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to start ' + args.name + '?';
              },
              notification: function(args) {
                return 'Starting VM: ' + args.name;
              }
            },
            notification: {
              poll: function(args) { args.complete(); }
            }
          },
          destroy: {
            label: 'Destroy instance',
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to destroy ' + args.name + '?';
              },
              notification: function(args) {
                return 'Destroyed VM: ' + args.name;
              }
            },
            action: function(args) {
              setTimeout(function() {
                args.response.success({ data: { state: 'Destroying' }});
              }, 200);
            },
            notification: {
              poll: function(args) { args.complete(); }
            }
          }
        },
        dataProvider: function(args) {
          $.ajax({
            url: createURL('listVirtualMachines'),
            success: function(json) {
              args.response.success({ data: json.listvirtualmachinesresponse.virtualmachine });
            }
          });
        }
      }
    },
    siteToSiteVPN: {
      listView: function() { 
        return cloudStack.sections.network.sections.siteToSiteVpn;
      }
    },
    tiers: {
      actionPreFilter: function(args) {
        var tier = args.context.tiers[0];
        var state = tier.state;

        return state == 'Running' ? ['start'] : ['stop'];
      },
      actions: {        
        add: {
					label: 'Add new tier',     
					createForm: {
						title: 'Add new tier',               
						fields: {
							name: { label: 'label.name', 
								validation: { required: true } 
							},                                
							networkOfferingId: {
								label: 'label.network.offering',
								validation: { required: true },
								dependsOn: 'zoneId',
								select: function(args) {										 
									$.ajax({
										url: createURL('listNetworkOfferings'),
										data: {
											forvpc: true,
											zoneid: args.zoneId,
											guestiptype: 'Isolated',
											supportedServices: 'SourceNat',
											specifyvlan: false,
											state: 'Enabled'
										},
										success: function(json) {
											var networkOfferings = json.listnetworkofferingsresponse.networkoffering;
											args.response.success({
												data: $.map(networkOfferings, function(zone) {
													return {
														id: zone.id,
														description: zone.name
													};
												})
											});
										}
									});
								}
							},
							gateway: { 
								label: 'label.gateway', 
								validation: { required: true } 
							},        
							netmask: { 
								label: 'label.netmask', 
								validation: { required: true } 
							},        
						}
					},
					action: function(args) {	
						var dataObj = {
						  vpcid: args.context.vpc[0].id,
							zoneId: args.context.vpc[0].zoneid,
							networkOfferingId: args.data.networkOfferingId,
							name: args.data.name,
							displayText: args.data.name,
							gateway: args.data.gateway, 
							netmask: args.data.netmask
						};
					
						$.ajax({
							url: createURL('createNetwork'),
							dataType: 'json',
							data: dataObj,
							success: function(json) {		                				
								args.response.success({
									data: json.createnetworkresponse.network
								});
							},
							error: function(XMLHttpResponse) {                				
								args.response.error(parseXMLHttpResponse(XMLHttpResponse));
							}
						});
					},
					messages: {
						notification: function() { return 'Add new tier'; }
					}
				},
				
        start: {
          label: 'Start tier',
          shortLabel: 'Start',
          action: function(args) {
            args.response.success();
          },
          notification: {
            poll: function(args) { args.complete({ data: { state: 'Running' } }); }
          }
        },
				
        stop: {
          label: 'Stop tier',
          shortLabel: 'Stop',
          action: function(args) {
            args.response.success();
          },
          notification: {
            poll: function(args) { args.complete({ data: { state: 'Stopped' } }); }
          }
        },
				
        addVM: {
          label: 'Add VM to tier',
          shortLabel: 'Add VM',
          action: cloudStack.uiCustom.instanceWizard(
            cloudStack.instanceWizard
          ),
          notification: {
            poll: pollAsyncJobResult
          }
        },
				
        acl: {
          label: 'Configure ACL for tier',
          shortLabel: 'ACL',
          multiEdit: aclMultiEdit
        },
				
        remove: {
          label: 'Remove tier',
          action: function(args) {
            args.response.success();
          },
          notification: {
            poll: function(args) { args.complete(); }
          }
        }
      },

      // Get tiers
      dataProvider: function(args) {		
				$.ajax({
					url: createURL("listNetworks"),
					dataType: "json",
					data: {
					  vpcid: args.context.vpc[0].id,
						listAll: true
					},
					async: true,
					success: function(json) {					  
						var networks = json.listnetworksresponse.network;												
						if(networks != null && networks.length > 0) {
						  for(var i = 0; i < networks.length; i++) {							 
							  $.ajax({
								  url: createURL("listVirtualMachines"),
									dataType: "json",
									data: {
									  networkid: networks[i].id,
										listAll: true
									},
									async: false,
									success: function(json) {									  
									  networks[i].virtualMachines = json.listvirtualmachinesresponse.virtualmachine;										
									}
								});								
							}
						}								
						args.response.success({ tiers: networks });
					}
				});	 
      }
    }
  };
}(jQuery, cloudStack));
