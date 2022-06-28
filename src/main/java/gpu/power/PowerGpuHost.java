package gpu.power;

import gpu.GpuPe;
import gpu.VideoCard;
import gpu.allocation.VideoCardAllocationPolicy;
import gpu.performance.PerformanceGpuHost;
import gpu.power.models.GpuHostPowerModelNull;
import gpu.provisioners.BwProvisioner;
import gpu.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.power.models.PowerModelHost;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.vms.Vm;

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

	 *      VmScheduler, VideoCardAllocationPolicy)
	 * @param powerModel
	 *            the power model associated with the host (video cards have their
	 *            own power models)
	 */
	public PowerGpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
						List<GpuPe> peList, VmScheduler vmScheduler, VideoCardAllocationPolicy videoCardAllocationPolicy,
						PowerModelHost powerModel) {
		super(id, type, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, videoCardAllocationPolicy);
		setPowerModel(powerModel);
	}
	

	public PowerGpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
			List<GpuPe> peList, VmScheduler vmScheduler, VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super(id, type, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, videoCardAllocationPolicy);
		setPowerModel(new GpuHostPowerModelNull());
	}

	public PowerGpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
			List<GpuPe> peList, VmScheduler vmScheduler, PowerModelHost powerModel) {
		super(id, type, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
		setPowerModel(powerModel);
	}
	

	public PowerGpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
			List< GpuPe> peList, VmScheduler vmScheduler) {
		super(id, type, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
		setPowerModel(new GpuHostPowerModelNull());
	}

	/**
	 * Returns the current total utilization of host's CPUs.
	 * 
	 * @return total utilization of host CPUs
	 **/
	@SuppressWarnings("unchecked")
/*	protected double getCurrentCpuUtilization() {
		double totalRequestedMips = 0.0;
		for (GpuVm vm : (List<GpuVm>) (List<?>) getVmList()) {
			totalRequestedMips += vm.getCurrentRequestedMips();
		}
		return totalRequestedMips / getMips();
	}*/

	protected double getCurrentCpuUtilization() {
		return getVmList().stream().mapToDouble(Vm::getTotalCpuMipsUtilization).sum();
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
		return getPowerModel().getPower(getCurrentCpuUtilization());
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
	public  void setPowerModel(PowerModelHost powerModel) {
		this.powerModel = powerModel;
	}

}
