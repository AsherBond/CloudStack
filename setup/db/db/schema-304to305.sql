# Copyright 2012 Citrix Systems, Inc. Licensed under the
# Apache License, Version 2.0 (the "License"); you may not use this
# file except in compliance with the License.  Citrix Systems, Inc.
# reserves all rights not expressly granted by the License.
# You may obtain a copy of the License at http:#www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 

#Schema upgrade from 3.0.4 to 3.0.5;

CREATE TABLE `cloud`.`resource_tags` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `key` varchar(255),
  `value` varchar(255),
  `resource_id` bigint unsigned NOT NULL,
  `resource_uuid` varchar(40),
  `resource_type` varchar(255),
  `customer` varchar(255),
  `domain_id` bigint unsigned NOT NULL COMMENT 'foreign key to domain id',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner of this network',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_tags__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`),
  CONSTRAINT `fk_tags__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`),
  UNIQUE `i_tags__resource_id__resource_type__key`(`resource_id`, `resource_type`, `key`),
  CONSTRAINT `uc_resource_tags__uuid` UNIQUE (`uuid`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vpc_offerings` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) NOT NULL,
  `unique_name` varchar(64) UNIQUE COMMENT 'unique name of the vpc offering',
  `name` varchar(255) COMMENT 'vpc name',
  `display_text` varchar(255) COMMENT 'display text',
  `state` char(32) COMMENT 'state of the vpc offering that has Disabled value by default',
  `default` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if vpc offering is default',
  `removed` datetime COMMENT 'date removed if not null',
  `created` datetime NOT NULL COMMENT 'date created',
  `service_offering_id` bigint unsigned COMMENT 'service offering id that virtual router is tied to',
  PRIMARY KEY  (`id`),
  INDEX `i_vpc__removed`(`removed`),
  CONSTRAINT `fk_vpc_offerings__service_offering_id` FOREIGN KEY `fk_vpc_offerings__service_offering_id` (`service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`vpc_offering_service_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vpc_offering_id` bigint unsigned NOT NULL COMMENT 'vpc_offering_id',
  `service` varchar(255) NOT NULL COMMENT 'service',
  `provider` varchar(255) COMMENT 'service provider',
  `created` datetime COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vpc_offering_service_map__vpc_offering_id` FOREIGN KEY(`vpc_offering_id`) REFERENCES `vpc_offerings`(`id`) ON DELETE CASCADE,
  UNIQUE (`vpc_offering_id`, `service`, `provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vpc` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) NOT NULL,
  `name` varchar(255) COMMENT 'vpc name',
  `display_text` varchar(255) COMMENT 'vpc display text',
  `cidr` varchar(18) COMMENT 'vpc cidr',
  `vpc_offering_id` bigint unsigned NOT NULL COMMENT 'vpc offering id that this vpc is created from',
  `zone_id` bigint unsigned NOT NULL COMMENT 'the id of the zone this Vpc belongs to',
  `state` varchar(32) NOT NULL COMMENT 'state of the VP (can be Enabled and Disabled)',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain the vpc belongs to',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner of this vpc',
  `network_domain` varchar(255) COMMENT 'network domain',
  `removed` datetime COMMENT 'date removed if not null',
  `created` datetime NOT NULL COMMENT 'date created',
  `restart_required` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if restart is required for the VPC',
  PRIMARY KEY  (`id`),
  INDEX `i_vpc__removed`(`removed`),
  CONSTRAINT `fk_vpc__zone_id` FOREIGN KEY `fk_vpc__zone_id` (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vpc__vpc_offering_id` FOREIGN KEY (`vpc_offering_id`) REFERENCES `vpc_offerings`(`id`), 
  CONSTRAINT `fk_vpc__account_id` FOREIGN KEY `fk_vpc__account_id` (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vpc__domain_id` FOREIGN KEY `fk_vpc__domain_id` (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`router_network_ref` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `router_id` bigint unsigned NOT NULL COMMENT 'router id',
  `network_id` bigint unsigned NOT NULL COMMENT 'network id',
  `guest_type` char(32) COMMENT 'type of guest network that can be shared or isolated',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_router_network_ref__networks_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE,
  UNIQUE `i_router_network_ref__router_id__network_id`(`router_id`, `network_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`vpc_gateways` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `ip4_address` char(40) COMMENT 'ip4 address of the gateway',
  `netmask` varchar(15) COMMENT 'netmask of the gateway',
  `gateway` varchar(15) COMMENT 'gateway',
  `vlan_tag` varchar(255),
  `type` varchar(32) COMMENT 'type of gateway; can be Public/Private/Vpn',
  `network_id` bigint unsigned NOT NULL COMMENT 'network id vpc gateway belongs to',
  `vpc_id` bigint unsigned NOT NULL COMMENT 'id of the vpc the gateway belongs to',
  `zone_id` bigint unsigned NOT NULL COMMENT 'id of the zone the gateway belongs to',
  `created` datetime COMMENT 'date created',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner id',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain id',
  `state` varchar(32) NOT NULL COMMENT 'what state the vpc gateway in',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vpc_gateways__network_id` FOREIGN KEY `fk_vpc_gateways__network_id`(`network_id`) REFERENCES `networks`(`id`),
  CONSTRAINT `fk_vpc_gateways__vpc_id` FOREIGN KEY `fk_vpc_gateways__vpc_id`(`vpc_id`) REFERENCES `vpc`(`id`),
  CONSTRAINT `fk_vpc_gateways__zone_id` FOREIGN KEY `fk_vpc_gateways__zone_id`(`zone_id`) REFERENCES `data_center`(`id`),
  CONSTRAINT `fk_vpc_gateways__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vpc_gateways__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_vpc_gateways__uuid` UNIQUE (`uuid`),
  INDEX `i_vpc_gateways__removed`(`removed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`private_ip_address` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `ip_address` char(40) NOT NULL COMMENT 'ip address',
  `network_id` bigint unsigned NOT NULL COMMENT 'id of the network ip belongs to',
  `reservation_id` char(40) COMMENT 'reservation id',
  `mac_address` varchar(17) COMMENT 'mac address',
  `vpc_id` bigint unsigned COMMENT 'vpc this ip belongs to',
  `taken` datetime COMMENT 'Date taken',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_private_ip_address__vpc_id` FOREIGN KEY `fk_private_ip_address__vpc_id`(`vpc_id`) REFERENCES `vpc`(`id`),
  CONSTRAINT `fk_private_ip_address__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`static_routes` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `vpc_gateway_id` bigint unsigned COMMENT 'id of the corresponding ip address',
  `cidr` varchar(18) COMMENT 'cidr for the static route', 
  `state` char(32) NOT NULL COMMENT 'current state of this rule',
  `vpc_id` bigint unsigned COMMENT 'vpc the firewall rule is associated with',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner id',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain id',
  `created` datetime COMMENT 'Date created',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_static_routes__vpc_gateway_id` FOREIGN KEY(`vpc_gateway_id`) REFERENCES `vpc_gateways`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_static_routes__vpc_id` FOREIGN KEY (`vpc_id`) REFERENCES `vpc`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_static_routes__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_static_routes__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_static_routes__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`networks` ADD COLUMN `vpc_id` bigint unsigned COMMENT 'vpc this network belongs to';
ALTER TABLE `cloud`.`networks`ADD CONSTRAINT `fk_networks__vpc_id` FOREIGN KEY(`vpc_id`) REFERENCES `vpc`(`id`);

ALTER TABLE `cloud`.`firewall_rules` ADD COLUMN `vpc_id` bigint unsigned COMMENT 'vpc the firewall rule is associated with';
ALTER TABLE `cloud`.`firewall_rules` ADD COLUMN `traffic_type` char(32) COMMENT 'the type of the rule, can be Ingress or Egress';
ALTER TABLE `cloud`.`firewall_rules` MODIFY `ip_address_id` bigint unsigned COMMENT 'id of the corresponding ip address';
ALTER TABLE `cloud`.`firewall_rules` ADD CONSTRAINT `fk_firewall_rules__vpc_id` FOREIGN KEY (`vpc_id`) REFERENCES `vpc`(`id`) ON DELETE CASCADE;


ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `vpc_id` bigint unsigned COMMENT 'vpc the ip address is associated with';
ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__vpc_id` FOREIGN KEY (`vpc_id`) REFERENCES `vpc`(`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`domain_router` ADD COLUMN `vpc_id` bigint unsigned COMMENT 'correlated virtual router vpc ID';
ALTER TABLE `cloud`.`domain_router` ADD CONSTRAINT `fk_domain_router__vpc_id` FOREIGN KEY `fk_domain_router__vpc_id`(`vpc_id`) REFERENCES `vpc`(`id`);


ALTER TABLE `cloud`.`physical_network_service_providers` ADD COLUMN `networkacl_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is Network ACL service provided';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vpc.cleanup.interval', '3600', 'The interval (in seconds) between cleanup for Inactive VPCs');

CREATE TABLE `cloud`.`counter` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `source` varchar(255) NOT NULL COMMENT 'source e.g. netscaler, snmp',
  `name` varchar(255) NOT NULL COMMENT 'Counter name',
  `value` varchar(255) NOT NULL COMMENT 'Value in case of source=snmp',
  `removed` datetime COMMENT 'date removed if not null',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_counter__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`conditions` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `counter_id` bigint unsigned NOT NULL COMMENT 'Counter Id',
  `threshold` bigint unsigned NOT NULL COMMENT 'threshold value for the given counter',
  `relational_operator` char(2) COMMENT 'relational operator to be used upon the counter and condition',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain the Condition belongs to',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner of this Condition',
  `removed` datetime COMMENT 'date removed if not null',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_conditions__counter_id` FOREIGN KEY `fk_condition__counter_id`(`counter_id`) REFERENCES `counter`(`id`),
  CONSTRAINT `fk_conditions__account_id` FOREIGN KEY `fk_condition__account_id` (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conditions__domain_id` FOREIGN KEY `fk_condition__domain_id` (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_conditions__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`autoscale_vmprofiles` (
  `id` bigint unsigned NOT NULL auto_increment,
  `uuid` varchar(40),
  `zone_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `autoscale_user_id` bigint unsigned NOT NULL,
  `service_offering_id` bigint unsigned NOT NULL,
  `template_id` bigint unsigned NOT NULL,
  `other_deploy_params` varchar(1024) COMMENT 'other deployment parameters that is in addition to zoneid,serviceofferingid,domainid',
  `destroy_vm_grace_period` int unsigned COMMENT 'the time allowed for existing connections to get closed before a vm is destroyed',
  `snmp_community` varchar(255) COMMENT 'the community string to be used to reach out to the VM deployed by this profile',
  `snmp_port` int unsigned COMMENT 'the snmp port to be used to reach out to the VM deployed by this profile',
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_vmprofiles__domain_id` FOREIGN KEY `fk_autoscale_vmprofiles__domain_id` (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmprofiles__account_id` FOREIGN KEY `fk_autoscale_vmprofiles__account_id` (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmprofiles__autoscale_user_id` FOREIGN KEY `fk_autoscale_vmprofiles__autoscale_user_id` (`autoscale_user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_autoscale_vmprofiles__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`autoscale_policies` (
  `id` bigint unsigned NOT NULL auto_increment,
  `uuid` varchar(40),
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `duration` int unsigned NOT NULL,
  `quiet_time` int unsigned NOT NULL,
  `action` varchar(15),
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_policies__domain_id` FOREIGN KEY `fk_autoscale_policies__domain_id` (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_policies__account_id` FOREIGN KEY `fk_autoscale_policies__account_id` (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_autoscale_policies__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`autoscale_vmgroups` (
  `id` bigint unsigned NOT NULL auto_increment,
  `uuid` varchar(40),
  `zone_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `load_balancer_id` bigint unsigned NOT NULL,
  `min_members` int unsigned DEFAULT 1,
  `max_members` int unsigned NOT NULL,
  `member_port` int unsigned NOT NULL,
  `interval` int unsigned NOT NULL,
  `profile_id` bigint unsigned NOT NULL,
  `state` varchar(255) NOT NULL COMMENT 'enabled or disabled, a vmgroup is disabled to stop autoscaling activity',
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_vmgroup__autoscale_vmprofile_id` FOREIGN KEY(`profile_id`) REFERENCES `autoscale_vmprofiles`(`id`),
  CONSTRAINT `fk_autoscale_vmgroup__load_balancer_id` FOREIGN KEY(`load_balancer_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmgroups__domain_id` FOREIGN KEY `fk_autoscale_vmgroups__domain_id` (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmgroups__account_id` FOREIGN KEY `fk_autoscale_vmgroups__account_id` (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmgroups__zone_id` FOREIGN KEY `fk_autoscale_vmgroups__zone_id`(`zone_id`) REFERENCES `data_center`(`id`),
  CONSTRAINT `uc_autoscale_vmgroups__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`autoscale_policy_condition_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `policy_id` bigint unsigned NOT NULL,
  `condition_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_policy_condition_map__policy_id` FOREIGN KEY `fk_autoscale_policy_condition_map__policy_id` (`policy_id`) REFERENCES `autoscale_policies` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_policy_condition_map__condition_id` FOREIGN KEY `fk_autoscale_policy_condition_map__condition_id` (`condition_id`) REFERENCES `conditions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`autoscale_vmgroup_policy_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vmgroup_id` bigint unsigned NOT NULL,
  `policy_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_vmgroup_policy_map__vmgroup_id` FOREIGN KEY `fk_autoscale_vmgroup_policy_map__vmgroup_id` (`vmgroup_id`) REFERENCES `autoscale_vmgroups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmgroup_policy_map__policy_id` FOREIGN KEY `fk_autoscale_vmgroup_policy_map__policy_id` (`policy_id`) REFERENCES `autoscale_policies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

