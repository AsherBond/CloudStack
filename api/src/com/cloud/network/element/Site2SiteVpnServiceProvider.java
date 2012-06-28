package com.cloud.network.element;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Site2SiteVpnConnection;

public interface Site2SiteVpnServiceProvider extends NetworkElement {
    boolean startSite2SiteVpn(Site2SiteVpnConnection conn) throws ResourceUnavailableException;
    
    boolean stopSite2SiteVpn(Site2SiteVpnConnection conn) throws ResourceUnavailableException;
}
