# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
""" P1 tests for Resource limits
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from integration.lib.utils import *
from integration.lib.base import *
from integration.lib.common import *
import datetime

class Services:
    """Test Resource Limits Services
    """

    def __init__(self):
        self.services = {
                        "domain": {
                                   "name": "Domain",
                        },
                        "project": {
                                    "name": "Project",
                                    "displaytext": "Test project",
                        },
                        "account": {
                                    "email": "administrator@clogeny.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "fr3sca",
                         },
                         "user": {
                                    "email": "administrator@clogeny.com",
                                    "firstname": "User",
                                    "lastname": "User",
                                    "username": "User",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "fr3sca",
                         },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100, # in MHz
                                    "memory": 64, # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Tiny Disk Offering",
                                    "name": "Tiny Disk Offering",
                                    "disksize": 1
                        },
                        "volume": {
                                   "diskname": "Test Volume",
                        },
                        "server": {
                                    "displayname": "TestVM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                        },
                        "template": {
                                    "displaytext": "Cent OS Template",
                                    "name": "Cent OS Template",
                                    "ostypeid": '471a4b5b-5523-448f-9608-7d6218995733',
                                    "templatefilter": 'self',
                        },
                        "ostypeid": '471a4b5b-5523-448f-9608-7d6218995733',
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "timeout": 10,
                        "mode": 'advanced',
                    }


class TestProjectLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestProjectLimits, 
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone
        cls.zone = get_zone(cls.api_client, cls.services)
        
        # Create domains, account etc.
        cls.domain = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"]
                                   )

        cls.admin = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )
        cls.user = Account.create(
                            cls.api_client,
                            cls.services["user"],
                            domainid=cls.domain.id
                            )
        cls._cleanup = [
			cls.admin,
			cls.user,
			cls.domain
			]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def test_01_project_limits(self):
        """ Test project limits
        """

        # Validate the following
        # 1. Create a Project. Verify once projects are created, they inherit
        #    a default set of resource limits as configured by the Cloud Stack
        #    ROOT admin.
        # 2. Reduce Project resources limits. Verify limits can be reduced by
        #    the Project Owner of each project and project limit applies to
        #    number of virtual instances, disk volumes, snapshots, IP address.
        #    Also, verify resource limits for the project are independent of
        #    account resource limits
        # 3. Increase Projects Resources limits above domains limit. Verify
        #    project can’t have more resources than domain level limit allows.
        # 4. Create Resource more than its set limit for a project. Verify
        #    resource allocation should fail giving proper message

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.admin.account.name,
                                 domainid=self.admin.account.domainid
                                 )
        # Cleanup created project at end of test
        self.cleanup.append(project)
        self.debug("Created project with domain admin with ID: %s" %
                                                                project.id)
        
        list_projects_reponse = Project.list(
                                             self.apiclient, 
                                             id=project.id,
                                             listall=True
                                             )

        self.assertEqual(
                            isinstance(list_projects_reponse, list),
                            True,
                            "Check for a valid list projects response"
                            )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
                    len(list_projects_reponse),
                    0,
                    "Check list project response returns a valid project"
                    )

        self.assertEqual(
                            project.name,
                            list_project.name,
                            "Check project name from list response"
                            )
        # Get the resource limits for ROOT domain
        resource_limits = list_resource_limits(self.apiclient)

        self.assertEqual(
                         isinstance(resource_limits, list),
                         True,
                         "List resource API should return a valid list"
                         )
        self.assertNotEqual(
                         len(resource_limits),
                         0,
                         "List resource API response should not be empty"
                         )

        # Reduce resource limits for project
        # Resource: 0 - Instance. Number of instances a user can create. 
        # Resource: 1 - IP. Number of public IP addresses a user can own.
        # Resource: 2 - Volume. Number of disk volumes a user can create.
        # Resource: 3 - Snapshot. Number of snapshots a user can create.
        # Resource: 4 - Template. Number of templates that a user can
        #               register/create
        for resource in resource_limits:
            update_resource_limit(
                                    self.apiclient,
                                    resource.resourcetype,
                                    max=1,
                                    projectid=project.id
                                    )
            self.debug(
            "Updating resource (ID: %s) limit for project: %s" % (
                                                                  resource,
                                                                  project.id
                                                                  ))
        resource_limits = list_resource_limits(
                                                self.apiclient,
                                                projectid=project.id
                                                )
        self.assertEqual(
                         isinstance(resource_limits, list),
                         True,
                         "List resource API should return a valid list"
                         )
        self.assertNotEqual(
                         len(resource_limits),
                         0,
                         "List resource API response should not be empty"
                         )
        for resource in resource_limits:
            self.assertEqual(
                         resource.max,
                         1,
                         "Resource limit should be updated to 1"
                         )
        
        # Get the resource limits for domain
        resource_limits = list_resource_limits(
                                                self.apiclient,
                                                domainid=self.domain.id
                                                )
        self.assertEqual(
                         isinstance(resource_limits, list),
                         True,
                         "List resource API should return a valid list"
                         )
        self.assertNotEqual(
                         len(resource_limits),
                         0,
                         "List resource API response should not be empty"
                         )
        
        for resource in resource_limits:
            # Update domain resource limits to 2
            update_resource_limit(
                                        self.apiclient,
                                        resource.resourcetype,
                                        domainid=self.domain.id,
                                        max=2
                                      )
            with self.assertRaises(Exception):
                self.debug(
                    "Attempting to update project: %s resource limit to: %s" % (
                                                                project.id,
                                                                max_value
                                                                ))
                # Update project resource limits to 3
                update_resource_limit(
                                        self.apiclient,
                                        resource.resourcetype,
                                        max=3,
                                        projectid=project.id
                                      )
        return
    @unittest.skip("No provision for updating resource limits from account through API")
    def test_02_project_limits_normal_user(self):
        """ Test project limits
        """

        # Validate the following
        # 1. Create a Project
        # 2. Reduce the projects limits as a domain admin. Verify resource
        #    count is updated
        # 3. Reduce the projects limits as a project user owner who is not a
        #    domain admin. Resource count should fail

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.admin.account.name,
                                 domainid=self.admin.account.domainid
                                 )
        # Cleanup created project at end of test
        self.cleanup.append(project)
        self.debug("Created project with domain admin with ID: %s" %
                                                                project.id)
        
        list_projects_reponse = Project.list(
                                             self.apiclient, 
                                             id=project.id,
                                             listall=True
                                             )

        self.assertEqual(
                            isinstance(list_projects_reponse, list),
                            True,
                            "Check for a valid list projects response"
                            )
        list_project = list_projects_reponse[0]

        self.assertNotEqual(
                    len(list_projects_reponse),
                    0,
                    "Check list project response returns a valid project"
                    )

        self.assertEqual(
                            project.name,
                            list_project.name,
                            "Check project name from list response"
                            )
        # Get the resource limits for ROOT domain
        resource_limits = list_resource_limits(self.apiclient)

        self.assertEqual(
                         isinstance(resource_limits, list),
                         True,
                         "List resource API should return a valid list"
                         )
        self.assertNotEqual(
                         len(resource_limits),
                         0,
                         "List resource API response should not be empty"
                         )

        # Reduce resource limits for project
        # Resource: 0 - Instance. Number of instances a user can create. 
        # Resource: 1 - IP. Number of public IP addresses a user can own.
        # Resource: 2 - Volume. Number of disk volumes a user can create.
        # Resource: 3 - Snapshot. Number of snapshots a user can create.
        # Resource: 4 - Template. Number of templates that a user can
        #               register/create
        for resource in resource_limits:
            update_resource_limit(
                                    self.apiclient,
                                    resource.resourcetype,
                                    max=1,
                                    projectid=project.id
                                    )
            self.debug(
            "Updating resource (ID: %s) limit for project: %s" % (
                                                                  resource,
                                                                  project.id
                                                                  ))
        resource_limits = list_resource_limits(
                                                self.apiclient,
                                                projectid=project.id
                                                )
        self.assertEqual(
                         isinstance(resource_limits, list),
                         True,
                         "List resource API should return a valid list"
                         )
        self.assertNotEqual(
                         len(resource_limits),
                         0,
                         "List resource API response should not be empty"
                         )
        for resource in resource_limits:
            self.assertEqual(
                         resource.max,
                         1,
                         "Resource limit should be updated to 1"
                         )
        
        self.debug("Adding %s user to project: %s" % (
                                                self.user.account.name,
                                                project.name
                                                ))

        # Add user to the project
        project.addAccount(
                           self.apiclient, 
                           self.user.account.name, 
                           )
        
        # Get the resource limits for domain
        resource_limits = list_resource_limits(
                                                self.apiclient,
                                                domainid=self.domain.id
                                                )
        self.assertEqual(
                         isinstance(resource_limits, list),
                         True,
                         "List resource API should return a valid list"
                         )
        self.assertNotEqual(
                         len(resource_limits),
                         0,
                         "List resource API response should not be empty"
                         )
        
        for resource in resource_limits:
            #with self.assertRaises(Exception):
            self.debug(
                    "Attempting to update resource limit by user: %s" % (
                                                        self.user.account.name
                                                        ))
            # Update project resource limits to 3
            update_resource_limit(
                                    self.apiclient,
                                    resource.resourcetype,
                                    account=self.user.account.name,
                                    domainid=self.user.account.domainid,
                                    max=3,
                                    projectid=project.id
                                )
        return


class TestResourceLimitsProject(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestResourceLimitsProject, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client, cls.services)

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["server"]["zoneid"] = cls.zone.id

        # Create Domains, Account etc
        cls.domain = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"]
                                   )

        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        # Create project as a domain admin
        cls.project = Project.create(
                                 cls.api_client,
                                 cls.services["project"],
                                 account=cls.account.account.name,
                                 domainid=cls.account.account.domainid
                                 )
        cls.services["account"] = cls.account.account.name

        # Create Service offering and disk offerings etc
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        cls._cleanup = [
                        cls.project,
                        cls.service_offering,
                        cls.disk_offering,
                        cls.account,
                        cls.domain
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def test_03_vm_per_project(self):
        """Test VM limit per project
        """

        # Validate the following
        # 1. Set max VM per project to 2
        # 2. Create account and start 2 VMs. Verify VM state is Up and Running
        # 3. Try to create 3rd VM instance. The appropriate error or alert
        #    should be raised

        self.debug(
            "Updating instance resource limits for project: %s" % 
                                                        self.project.id)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              0, # Instance
                              max=2,
                              projectid=self.project.id
                              )

        self.debug("Deploying VM for project: %s" % self.project.id)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
        self.debug("Deploying VM for project: %s" % self.project.id)
        virtual_machine_2 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
        self.cleanup.append(virtual_machine_2)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_2.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
        # Exception should be raised for second instance
        with self.assertRaises(Exception):
            VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
        return

    def test_04_publicip_per_project(self):
        """Test Public IP limit per project
        """

        # Validate the following
        # 1. set max no of IPs per project to 2.
        # 2. Create an account in this domain
        # 3. Create 1 VM in this domain
        # 4. Acquire 1 IP in the domain. IP should be successfully acquired
        # 5. Try to acquire 3rd IP in this domain. It should give the user an
        #    appropriate error and an alert should be generated.

        self.debug(
            "Updating public IP resource limits for project: %s" % 
                                                            self.project.id)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              1, # Public Ip
                              max=2,
                              projectid=self.project.id
                              )

        self.debug("Deploying VM for Project: %s" % self.project.id)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
        networks = Network.list(
                                self.apiclient, 
                                projectid=self.project.id,
                                listall=True
                                )
        self.assertEqual(
                    isinstance(networks, list),
                    True,
                    "Check list networks response returns a valid response"
                    )
        self.assertNotEqual(
                    len(networks),
                    0,
                    "Check list networks response returns a valid network"
                    )
        network = networks[0]
        self.debug("Associating public IP for project: %s" % 
                                                            self.project.id)
        public_ip_1 = PublicIPAddress.create(
                                           self.apiclient,
                                           zoneid=virtual_machine_1.zoneid,
                                           services=self.services["server"],
                                           networkid=network.id,
                                           projectid=self.project.id
                                           )
        self.cleanup.append(public_ip_1)
        # Verify Public IP state
        self.assertEqual(
                            public_ip_1.ipaddress.state in [
                                                 'Allocated',
                                                 'Allocating'
                                                 ],
                            True,
                            "Check Public IP state is allocated or not"
                        )

        # Exception should be raised for second Public IP
        with self.assertRaises(Exception):
            public_ip_2 = PublicIPAddress.create(
                                           self.apiclient,
                                           zoneid=virtual_machine_1.zoneid,
                                           services=self.services["server"],
                                           networkid=network.id,
                                           projectid=self.project.id
                                           )
        return

    def test_05_snapshots_per_project(self):
        """Test Snapshot limit per project
        """

        # Validate the following
        # 1. set max no of snapshots per project to 1.
        # 2. Create one snapshot in the project. Snapshot should be
        #    successfully created
        # 5. Try to create another snapshot in this project. It should give      
        #    user an appropriate error and an alert should be generated.

        self.debug(
            "Updating snapshot resource limits for project: %s" % 
                                        self.project.id)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              3, # Snapshot
                              max=1,
                              projectid=self.project.id
                              )

        self.debug("Deploying VM for account: %s" % self.account.account.name)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        # Get the Root disk of VM
        volumes = list_volumes(
                            self.apiclient,
                            projectid=self.project.id,
                            type='ROOT',
                            listall=True
                            )
        self.assertEqual(
                        isinstance(volumes, list),
                        True,
                        "Check for list volume response return valid data"
                        )
        volume = volumes[0]

        self.debug("Creating snapshot from volume: %s" % volumes[0].id)
        # Create a snapshot from the ROOTDISK
        snapshot_1 = Snapshot.create(self.apiclient,
                            volumes[0].id,
                            projectid=self.project.id
                            )
        self.cleanup.append(snapshot_1)
        # Verify Snapshot state
        self.assertEqual(
                            snapshot_1.state in [
                                                 'BackedUp',
                                                 'CreatedOnPrimary'
                                                 ],
                            True,
                            "Check Snapshot state is Running or not"
                        )

        # Exception should be raised for second snapshot
        with self.assertRaises(Exception):
            Snapshot.create(self.apiclient,
                            volumes[0].id,
                            projectid=self.project.id
                            )
        return

    def test_06_volumes_per_project(self):
        """Test Volumes limit per project
        """

        # Validate the following
        # 1. set max no of volume per project to 1.
        # 2. Create 1 VM in this project
        # 4. Try to Create another VM in the project. It should give the user
        #    an appropriate error that Volume limit is exhausted and an alert
        #    should be generated.

        self.debug(
            "Updating volume resource limits for project: %s" % 
                                                    self.project.id)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              2, # Volume
                              max=2,
                              projectid=self.project.id
                              )

        self.debug("Deploying VM for project: %s" % self.project.id)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        # Exception should be raised for second volume
        with self.assertRaises(Exception):
            Volume.create(
                          self.apiclient,
                          self.services["volume"],
                          zoneid=self.zone.id,
                          diskofferingid=self.disk_offering.id,
                          projectid=self.project.id
                        )
        return
    
    def test_07_templates_per_project(self):
        """Test Templates limit per project
        """

        # Validate the following 
        # 1. set max no of templates per project to 1.
        # 2. Create a template in this project. Both template should be in
        #    ready state
        # 3. Try create 2nd template in the project. It should give the user
        #    appropriate error and an alert should be generated.

        # Reset the volume limits
        update_resource_limit(
                              self.apiclient,
                              2, # Volume
                              max=5,
                              projectid=self.project.id
                              )
        self.debug(
            "Updating template resource limits for domain: %s" % 
                                        self.account.account.domainid)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              4, # Template
                              max=1,
                              projectid=self.project.id
                              )

        self.debug("Deploying VM for account: %s" % self.account.account.name)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                serviceofferingid=self.service_offering.id,
                                projectid=self.project.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
        virtual_machine_1.stop(self.apiclient)
        # Get the Root disk of VM
        volumes = list_volumes(
                            self.apiclient,
                            projectid=self.project.id,
                            type='ROOT',
                            listall=True
                            )
        self.assertEqual(
                        isinstance(volumes, list),
                        True,
                        "Check for list volume response return valid data"
                        )
        volume = volumes[0]

        self.debug("Creating template from volume: %s" % volume.id)
        # Create a template from the ROOTDISK
        template_1 = Template.create(
                            self.apiclient,
                            self.services["template"],
                            volumeid=volume.id,
                            projectid=self.project.id
                            )

        self.cleanup.append(template_1)
        # Verify Template state
        self.assertEqual(
                            template_1.isready,
                            True,
                            "Check Template is in ready state or not"
                        )

        # Exception should be raised for second template
        with self.assertRaises(Exception):
            Template.create(
                            self.apiclient,
                            self.services["template"],
                            volumeid=volume.id,
                            projectid=self.project.id
                            )
        return