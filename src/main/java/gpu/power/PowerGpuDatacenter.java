package gpu.power;

import gpu.GpuDatacenter;
import gpu.VideoCard;
import gpu.allocation.VideoCardAllocationPolicy;
import gpu.core.GpuCloudSimTags;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTag;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.PredicateType;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.resources.SanStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * {@link PowerGpuDatacenter} extends {@link GpuDatacenter} to enable simulation
 * of power-aware data centers.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PowerGpuDatacenter extends GpuDatacenter {

	/** host-energy mapping. */
	private Map<PowerGpuHost, Double> hostEnergyMap;
	/** host-cpu energy mapping. */
	private Map<PowerGpuHost, Double> hostCpuEnergyMap;
	/** host-videoCard energy mapping. */
	private Map<PowerGpuHost, Map<PowerVideoCard, Double>> hostVideoCardEnergyMap;
	
	/** to set aside idle hosts from power calculations. **/
	private boolean powerSavingMode;

	/**
	 * @see org.cloudbus.cloudsim.gpu.GpuDatacenter#GpuDatacenter(String,
	 *      DatacenterCharacteristics, VmAllocationPolicy, List, double)
	 *      GpuDatacenter(String, DatacenterCharacteristics, VmAllocationPolicy,
	 *      List, double)
	 */
	@SuppressWarnings("unchecked")
	public PowerGpuDatacenter(String name, Simulation simulation, DatacenterCharacteristics characteristics,
							  VmAllocationPolicy vmAllocationPolicy, List<SanStorage> storageList, double schedulingInterval)
			throws Exception {
		super(name, simulation, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		setHostEnergyMap(new HashMap<PowerGpuHost, Double>());
		setHostCpuEnergyMap(new HashMap<PowerGpuHost, Double>());
		setHostVideoCardEnergyMap(new HashMap<PowerGpuHost, Map<PowerVideoCard, Double>>());
		for (Host host : getCharacteristics().getDatacenter().getHostList()) {
			PowerGpuHost powerGpuHost = (PowerGpuHost) host;
			getHostEnergyMap().put(powerGpuHost, 0.0);
			getHostCpuEnergyMap().put(powerGpuHost, 0.0);
			getHostVideoCardEnergyMap().put(powerGpuHost, new HashMap<PowerVideoCard, Double>());
			VideoCardAllocationPolicy videoCardAllocationPolicy = powerGpuHost.getVideoCardAllocationPolicy();
			if (videoCardAllocationPolicy != null) {
				for (PowerVideoCard videoCard : (List<PowerVideoCard>) videoCardAllocationPolicy.getVideoCards()) {
					getHostVideoCardEnergyMap().get(powerGpuHost).put(videoCard, 0.0);
				}
			}
		}
		setPowerSavingMode(false);
	}

	@SuppressWarnings("unchecked")
	protected void updatePower(double deltaTime) {
		for (Host host : getHostList()) {
			PowerGpuHost powerGpuHost = (PowerGpuHost) host;
			if (isPowerSavingMode() && powerGpuHost.isIdle()) {
				// Assume unused machines are powered off
				continue;
			}
			double hostCpuPower = powerGpuHost.getCurrentHostCpuPower();
			double hostCpuEnergy = getHostCpuEnergyMap().get(powerGpuHost);
			double hostCpuDeltaEnergy = hostCpuPower * deltaTime;
			hostCpuEnergy += hostCpuDeltaEnergy;
			getHostCpuEnergyMap().put(powerGpuHost, hostCpuEnergy);
			double videoCardsEnergy = 0.0;
			for (Entry<VideoCard, Double> videoCardPowerEntry : powerGpuHost.getCurrentVideoCardsPower().entrySet()) {
				PowerVideoCard videoCard = (PowerVideoCard) videoCardPowerEntry.getKey();
				double videoCardPower = videoCardPowerEntry.getValue();
				double videoCardDeltaEnergy = videoCardPower * deltaTime;
				double videoCardEnergy = getHostVideoCardEnergyMap().get(powerGpuHost).get(videoCard);
				videoCardEnergy += videoCardDeltaEnergy;
				getHostVideoCardEnergyMap().get(powerGpuHost).put(videoCard, videoCardEnergy);
				videoCardsEnergy += videoCardEnergy;
			}
			double hostTotalEnergy = hostCpuEnergy + videoCardsEnergy;
			getHostEnergyMap().put(powerGpuHost, hostTotalEnergy);
		}
	}


	public void processgpuEvent(SimEvent ev) {
		// if this is the first time processing happens
		if (simulation.clock() == 0.0
				&& simulation.select(ev.getSource(), new PredicateType(CloudSimTag.GPU_VM_DATACENTER_POWER_EVENT)) == null) {
			schedule(ev.getSource(), getSchedulingInterval(), CloudSimTag.GPU_VM_DATACENTER_POWER_EVENT);
		}
		super.processgpuEvent(ev);
	}

	@Override
	protected void processOtherEvent(SimEvent ev) {
		super.processOtherEvent(ev);
		switch (ev.getTag()) {
		case GPU_VM_DATACENTER_POWER_EVENT:
			updatePower(getSchedulingInterval());
			schedule(ev.getSource(), getSchedulingInterval(), CloudSimTag.GPU_VM_DATACENTER_POWER_EVENT);
			break;
		}
	}

	/**
	 * @return consumed energy so far
	 */
	public double getConsumedEnergy() {
		Double totalEnergy = 0.0;
		for (Double hostEnergy : getHostEnergyMap().values()) {
			totalEnergy += hostEnergy;
		}
		return totalEnergy.doubleValue();
	}

	/**
	 * @return the hostEnergyMap
	 */
	public Map<PowerGpuHost, Double> getHostEnergyMap() {
		return hostEnergyMap;
	}

	/**
	 * @param hostEnergyMap the hostEnergyMap to set
	 */
	protected void setHostEnergyMap(Map<PowerGpuHost, Double> hostEnergyMap) {
		this.hostEnergyMap = hostEnergyMap;
	}

	/**
	 * @return the hostVideoCardEnergyMap
	 */
	public Map<PowerGpuHost, Map<PowerVideoCard, Double>> getHostVideoCardEnergyMap() {
		return hostVideoCardEnergyMap;
	}

	/**
	 * @param hostVideoCardEnergyMap the hostVideoCardEnergyMap to set
	 */
	protected void setHostVideoCardEnergyMap(Map<PowerGpuHost, Map<PowerVideoCard, Double>> hostVideoCardEnergyMap) {
		this.hostVideoCardEnergyMap = hostVideoCardEnergyMap;
	}

	/**
	 * @return the hostCpuEnergyMap
	 */
	public Map<PowerGpuHost, Double> getHostCpuEnergyMap() {
		return hostCpuEnergyMap;
	}

	/**
	 * @param hostCpuEnergyMap the hostCpuEnergyMap to set
	 */
	protected void setHostCpuEnergyMap(Map<PowerGpuHost, Double> hostCpuEnergyMap) {
		this.hostCpuEnergyMap = hostCpuEnergyMap;
	}

	public boolean isPowerSavingMode() {
		return powerSavingMode;
	}

	public void setPowerSavingMode(boolean consolidate) {
		this.powerSavingMode = consolidate;
	}

}
