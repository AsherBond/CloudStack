from configGenerator import *
if __name__ == "__main__":
    zs = cloudstackConfiguration()
    #Define tags
    tag="TAG1","TAG1","TAG1","TAG1","TAG1","TAG2","TAG2","TAG3","TAG3","TAG3","TAG3","TAG3" 
    #Define Zone
    z = zone()
    z.dns1 = "8.8.8.8"
    z.dns2 = "4.4.4.4"
    z.internaldns1 = "192.168.110.254"
    z.internaldns2 = "192.168.110.253"
    z.name = "testZone"
    z.networktype = 'Basic'

    #Define SecondaryStorage
    ss = secondaryStorage()
    ss.url ="nfs://172.16.15.32/export/share/secondary"
    z.secondaryStorages.append(ss)
    
    #Define pods
    a = 1
    b = 1
    tagcount = 0
    for i in range(10):
        p = pod()
        p.name = "POD-" + str(i)
        p.gateway = "172."+ str(a)+"."+ str(b)+"."+"129"
        p.netmask = "255.255.255.128"
        p.startip = "172."+ str(a)+"."+ str(b)+"."+"130"
        p.endip = "172."+ str(a)+"."+ str(b)+"."+ "189"
         
        #Define VlanRange    
        ip = iprange()
        ip.vlan="untagged"
        ip.gateway = p.gateway
        ip.netmask = p.netmask
        ip.startip = "172."+ str(a)+"."+ str(b)+"."+"190"
        ip.endip = "172."+ str(a)+"."+ str(b)+"."+"249"
        p.guestIpRanges.append(ip)
        b = b+1
        if b == 256:
            a = a+1    
        #Define Cluster  (10 clusters/pod)      
        for j in range(10):
            c = cluster()
            c.clustername = "CLUSTER-"+str(i)+"-"+str(j)
            c.clustertype = "CloudManaged"
            c.hypervisor = "Simulator"
            
            h = host()
            h.username = "root"
            h.password = "password"
            h.url = "http://sim/"+"test-"+str(i)+"-"+str(j)
            h.hosttags = tag[tagcount]
            if tagcount == 11:
                tagcount = 0
            else:
                tagcount = tagcount + 1
            c.hosts.append(h)
            p.clusters.append(c)
            
        z.pods.append(p)
    zs.zones.append(z)
    
    '''Add one mgt server'''
    mgt = managementServer()
    mgt.mgtSvrIp = "192.168.132.253"
    zs.mgtSvr.append(mgt)
    
    '''Add a database'''
    db = dbServer()
    db.dbSvr = "192.168.132.253"
    db.user = "root"
    db.passwd = ""
    zs.dbSvr = db

    generate_setup_config(zs,"setup.conf")
    