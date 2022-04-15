package cloudsimMixedPeEnv.power;

import cloudsimMixedPeEnv.GpuVm;
import cloudsimMixedPeEnv.VideoCard;
import cloudsimMixedPeEnv.allocation.VideoCardAllocationPolicy;
import cloudsimMixedPeEnv.performance.PerformanceGpuHost;
import cloudsimMixedPeEnv.power.models.GpuHostPowerModelNull;
import org.cloudbus.cloudsim.power.models.PowerModel;

import org.cloudbus.cloudsim.power.models.PowerModelHost;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link PowerGpuHost} extends {@link PerformanceGpuHost} to represent a
 * power-aware host.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PowerGpuHost extends PerformanceGpuHost {

	/** The power model associated with this host (video cards excluded) */
	private PowerModelHost powerModel;

	/**
	 * 
	 * @see PerformanceGpuHost#+PerformanceGpuHost(int, int, ResourceProvisioner,
	 *      ResourceProvisioner, long, List, VmScheduler, VideoCardAllocationPolicy)
	 *      erformanceGpuHost(int, int, ResourceProvisioner, ResourceProvisioner, long, List,
	 *      VmScheduler, VideoCardAllocationPolicy)
	 * @param powerModel
	 *            the power model associated with the host (video cards have their
	 *            own power models)
	 */
	public PowerGpuHost(int id, String type, ResourceProvisioner ramProvisioner, ResourceProvisioner bwProvisioner, long storage,
						List<Pe> peList, VideoCardAllocationPolicy videoCardAllocationPolicy,
						PowerModelHost powerModel) {
		super( ramProvisioner, bwProvisioner, storage, peList,  videoCardAllocationPolicy);
		setPowerModel(powerModel);
		setVideoCardAllocationPolicy(videoCardAllocationPolicy);

	}
	
	/**
	 * 
	 * @see PerformanceGpuHost#*PerformanceGpuHost(int, int, ResourceProvisioner,
	 *      ResourceProvisioner, long, List, VmScheduler, VideoCardAllocationPolicy)
	 *      erformanceGpuHost(int, int, ResourceProvisioner, ResourceProvisioner, long, List,
	 *      VmScheduler, VideoCardAllocationPolicy)
	 */
	public PowerGpuHost(long ramProvisioner, long bwProvisioner, long storage,
			List< Pe> peList, VmScheduler vmScheduler, VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super(ramProvisioner, bwProvisioner, storage, peList, vmScheduler, videoCardAllocationPolicy);
		setPowerModel(new GpuHostPowerModelNull());
	}

	/**
	 * 
	 * @see PerformanceGpuHost#*PerformanceGpuHost(int, int, ResourceProvisioner,
	 *      ResourceProvisioner, long, List, VmScheduler) erformanceGpuHost(int, int,
	 *      ResourceProvisioner, ResourceProvisioner, long, List, VmScheduler)
	 */
	public PowerGpuHost(long ramProvisioner, long bwProvisioner, long storage,
			List< Pe> peList, VmScheduler vmScheduler,VideoCardAllocationPolicy videoCardAllocationPolicy, PowerModelHost powerModel) {
		super(ramProvisioner, bwProvisioner, storage, peList, vmScheduler, videoCardAllocationPolicy);
		setPowerModel(powerModel);
	}
	
	/**
	 * 
	 * @see PerformanceGpuHost#*PerformanceGpuHost(int, int, ResourceProvisioner,
	 *      ResourceProvisioner, long, List, VmScheduler) erformanceGpuHost(int, int,
	 *      ResourceProvisioner, ResourceProvisioner, long, List, VmScheduler)
	 */
	public PowerGpuHost( ResourceProvisioner ramProvisioner, ResourceProvisioner bwProvisioner, long storage,
			List< Pe> peList, VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super(ramProvisioner, bwProvisioner, storage, peList, videoCardAllocationPolicy);
		setPowerModel(new GpuHostPowerModelNull());
	}

	/**
	 * Returns the current total utilization of host's CPUs.
	 * 
	 * @return total utilization of host CPUs
	 **/
	@SuppressWarnings("unchecked")
	protected double getCurrentCpuUtilization() {
		double totalRequestedMips = 0.0;
		for (GpuVm vm : (List<GpuVm>) (List<?>) getVmList()) {
			totalRequestedMips += vm.getTotalCpuMipsRequested();
		}
		return totalRequestedMips / getTotalMipsCapacity();
	}

	/**
	 * Returns the current total power consumption of the host (CPUs + GPUs).
	 * 
	 * @return current total power consumption of the host
	 */
	public double getCurrentTotalPower() {
		double totalPower = 0;
		totalPower += getCurrentHostCpuPower();
		for (Double videoCardPower : getCurrentVideoCardsPower().values()) {
			totalPower += videoCardPower;
		}
		return totalPower;
	}

	/**
	 * Returns the current power consumption of host's CPUs
	 * 
	 * @return current power consumption of host's CPUs
	 */
	public double getCurrentHostCpuPower() {
		return getPowerModel().getPower();
	}

	/**
	 * Returns the current power consumption of host's video cards
	 * 
	 * @return the current power consumption of host's video cards
	 */
	public Map<VideoCard, Double> getCurrentVideoCardsPower() {
		Map<VideoCard, Double> videoCardsPower = new HashMap<VideoCard, Double>();
		if (getVideoCardAllocationPolicy() != null) {
			for (VideoCard videoCard : getVideoCardAllocationPolicy().getVideoCards()) {
				PowerVideoCard powerVideoCard = (PowerVideoCard) videoCard;
				videoCardsPower.put(powerVideoCard, powerVideoCard.getPower());
			}
		}
		return videoCardsPower;
	}

	/**
	 * @return the powerModel
	 */
	public PowerModelHost getPowerModel() {
		return powerModel;
	}

	/**
	 * @param powerModel
	 *            the powerModel to set
	 */
	
/*	public void setPowerModel(PowerModelHost powerModel) {
		this.powerModel = powerModel;
		powerModel.setHost(this);
	}*/

}
