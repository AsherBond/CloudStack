package com.cloud.migration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;

@Entity
@Table(name="service_offering_21")
@DiscriminatorValue(value="Service")
@PrimaryKeyJoinColumn(name="id")
public class ServiceOffering21VO extends DiskOffering21VO implements ServiceOffering {
    @Column(name="cpu")
	private int cpu;
    
    @Column(name="speed")
    private int speed;
    
    @Column(name="ram_size")
	private int ramSize;
    
    @Column(name="nw_rate")
    private int rateMbps;
    
    @Column(name="mc_rate")
    private int multicastRateMbps;
    
    @Column(name="ha_enabled")
    private boolean offerHA;
    
    @Column(name="guest_ip_type")
    @Enumerated(EnumType.STRING)
    private NetworkOffering.GuestIpType guestIpType;
    
    protected ServiceOffering21VO() {
        super();
    }

    public ServiceOffering21VO(String name, int cpu, int ramSize, int speed, int rateMbps, int multicastRateMbps, boolean offerHA, String displayText, NetworkOffering.GuestIpType guestIpType, boolean useLocalStorage, boolean recreatable, String tags) {
        super(name, displayText, false, tags, recreatable, useLocalStorage);
        this.cpu = cpu;
        this.ramSize = ramSize;
        this.speed = speed;
        this.rateMbps = rateMbps;
        this.multicastRateMbps = multicastRateMbps;
        this.offerHA = offerHA;
        this.guestIpType = guestIpType;
    }

	@Override
	public boolean getOfferHA() {
	    return offerHA;
	}
	
	public void setOfferHA(boolean offerHA) {
		this.offerHA = offerHA;
	}
	
	@Override
    @Transient
	public String[] getTagsArray() {
	    String tags = getTags();
	    if (tags == null || tags.length() == 0) {
	        return new String[0];
	    }
	    
	    return tags.split(",");
	}
	
	@Override
	public int getCpu() {
	    return cpu;
	}
	
	public void setCpu(int cpu) {
		this.cpu = cpu;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public void setRamSize(int ramSize) {
		this.ramSize = ramSize;
	}
	
	@Override
	public int getSpeed() {
	    return speed;
	}
	
	@Override
	public int getRamSize() {
	    return ramSize;
	}
	
	public void setRateMbps(int rateMbps) {
		this.rateMbps = rateMbps;
	}

	public int getRateMbps() {
		return rateMbps;
	}

	public void setMulticastRateMbps(int multicastRateMbps) {
		this.multicastRateMbps = multicastRateMbps;
	}
	
	public int getMulticastRateMbps() {
		return multicastRateMbps;
	}

	public void setGuestIpType(NetworkOffering.GuestIpType guestIpType) {
		this.guestIpType = guestIpType;
	}

	public NetworkOffering.GuestIpType getGuestIpType() {
		return guestIpType;
	}
	public String gethypervisorType() {
		return null;
	}
}
