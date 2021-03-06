package cloudsimMixedPeEnv;

import cloudsimMixedPeEnv.allocation.VideoCardAllocationPolicy;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.hosts.HostSuitability;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;

import java.util.List;
import java.util.Set;

/**
 * 
 * {@link GpuHost} extends {@link Host} and supports {@link VideoCard}s through
 * a {@link VideoCardAllocationPolicy}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuHost extends HostSimple {

	/**
	 * type of the host
	 */
	public String type;

	/** video card allocation policy */
	private VideoCardAllocationPolicy videoCardAllocationPolicy;

	/**
	 * 
	 * See {@link Host#*Host}
	 * 
	 * @param *type type of the host which is specified in
	 *                                  {@link GpuHostTags}.
	 * @param videoCardAllocationPolicy the policy in which the host allocates video
	 *                                  cards to vms
	 */
	public GpuHost(ResourceProvisioner  ramProvisioner, ResourceProvisioner bwProvisioner, long storage,
			List<Pe> peList, VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super(ramProvisioner, bwProvisioner, storage, peList);
		setVideoCardAllocationPolicy(videoCardAllocationPolicy);
	}

	public GpuHost(long  ram, long bw, long storage,
				   List<Pe> peList, VmScheduler vmscheduler, VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super(ram, bw, storage, peList);
		setVideoCardAllocationPolicy(videoCardAllocationPolicy);
		setVmScheduler(vmscheduler);
	}


	/**
	 * 
	 * See {@link Host#*Host}
	 * 
	 * @param *type type of the host which is specified in {@link GpuHostTags}.
	 */
	public GpuHost(long ramProvisioner, long bwProvisioner, long storage,
			List<Pe> peList, boolean activate) {
		super(ramProvisioner, bwProvisioner, storage, peList, activate);
		setVideoCardAllocationPolicy(null);
	}

	public double updateVgpusProcessing(double currentTime) {
		double smallerTime = Double.MAX_VALUE;
		if (getVideoCardAllocationPolicy() != null) {
			// Update resident vGPUs
			for (Vgpu vgpu : getVideoCardAllocationPolicy().getVgpuVideoCardMap().keySet()) {
				double time = vgpu.updateGpuTaskProcessing(currentTime, getVideoCardAllocationPolicy()
						.getVgpuVideoCardMap().get(vgpu).getVgpuScheduler().getAllocatedMipsForVgpu(vgpu));
				if (time > 0.0 && time < smallerTime) {
					smallerTime = time;
				}
			}
		}
		return smallerTime;
	}

	/**
	 * @return the videoCardAllocationPolicy
	 */
	public VideoCardAllocationPolicy getVideoCardAllocationPolicy() {
		return videoCardAllocationPolicy;
	}

	/**
	 * @param videoCardAllocationPolicy the videoCardAllocationPolicy to set
	 */
	public void setVideoCardAllocationPolicy(VideoCardAllocationPolicy videoCardAllocationPolicy) {
		this.videoCardAllocationPolicy = videoCardAllocationPolicy;
	}

	/**
	 * Checks the existence of a given video card id in the host
	 * 
	 * @param videoCardId id of the video card
	 * @return
	 */
	public boolean hasVideoCard(int videoCardId) {
		if (getVideoCardAllocationPolicy() == null || getVideoCardAllocationPolicy().getVideoCards().isEmpty()) {
			return false;
		}
		for (VideoCard videoCard : getVideoCardAllocationPolicy().getVideoCards()) {
			if (videoCard.getId() == videoCardId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks the existence of a given pgpu id in the host
	 * 
	 * @param pgpuId id of the video card
	 * @return
	 */
	public boolean hasPgpu(int pgpuId) {
		if (getVideoCardAllocationPolicy() == null || getVideoCardAllocationPolicy().getVideoCards().isEmpty()) {
			return false;
		}
		for (VideoCard videoCard : getVideoCardAllocationPolicy().getVideoCards()) {
			for (Pgpu pgpu : videoCard.getVgpuScheduler().getPgpuList()) {
				if (pgpu.getId() == pgpuId) {
					return true;
				}
			}
		}
		return false;
	}

	public void vgpuDestroy(Vgpu vgpu) {
		if (vgpu != null) {
			getVideoCardAllocationPolicy().deallocate(vgpu);
		}
	}

	public boolean vgpuCreate(Vgpu vgpu) {
		return getVideoCardAllocationPolicy().allocate(vgpu, vgpu.getPCIeBw());
	}

	public HostSuitability vgpuCreate(Vgpu vgpu, Pgpu pgpu) {
		return getVideoCardAllocationPolicy().allocate(pgpu, vgpu, vgpu.getPCIeBw());
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	protected void setType(String type) {
		this.type = type;
	}

	public Set<Vgpu> getVgpuSet() {
		if (getVideoCardAllocationPolicy() == null) {
			return null;
		} else if (getVideoCardAllocationPolicy().getVideoCards().isEmpty()) {
			return null;
		}
		return getVideoCardAllocationPolicy().getVgpuVideoCardMap().keySet();
	}

	public boolean isIdle() {

		List<Vm> vmList = getVmList();
		Set<Vgpu> vgpuList = getVgpuSet();

		if (!getVmList().isEmpty()) {
			return false;
		} else if (vgpuList != null && !vgpuList.isEmpty()) {
			return false;
		}

		return true;
	}
	@Override
	public boolean isSuitableForVm(final Vm vm) {
		return getSuitabilityFor(vm).fully();
	}


	}
