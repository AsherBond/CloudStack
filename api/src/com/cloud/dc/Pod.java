/**
 * 
 */
package com.cloud.dc;

import com.cloud.org.Grouping;
import com.cloud.org.Grouping.AllocationState;

/**
 * Represents one pod in the cloud stack.
 *
 */
public interface Pod extends Grouping {
    /**
     * @return unique id mapped to the pod.
     */
    long getId();
    
    String getCidrAddress();
    
    int getCidrSize();

    String getGateway();
    
    long getDataCenterId();
    
    //String getUniqueName();
    
    String getDescription();
    
    String getName();
    
    AllocationState getAllocationState();
}
