package gpu;

import gpu.allocation.VideoCardAllocationPolicy;
import gpu.core.Log;
import gpu.core.PeList;
import gpu.provisioners.BwProvisioner;
import gpu.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
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
public class GpuHost extends HostSimple implements Host{

	private List<? extends GpuPe> peList;
	/**
	 * type of the host
	 */
	public String type;

	/** video card allocation policy */
	private VideoCardAllocationPolicy videoCardAllocationPolicy;
	private boolean failed;
	/**
	 * 
	 * See {@link}
	 * 
	 * @param type                      type of the host which is specified in
	 *                                  {@link GpuHostTags}.
	 * @param videoCardAllocationPolicy the policy in which the host allocates video
	 *                                  cards to vms
	 */
	public GpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
				   List<GpuPe> peList, VmScheduler vmScheduler, VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super( ramProvisioner, bwProvisioner, storage,  vmScheduler);
		setGpuPeList(peList);
		setId(id);
		setType(type);
		setVideoCardAllocationPolicy(videoCardAllocationPolicy);
		setGpuHostFailed(false);
	}

	public GpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
				   List<? extends GpuPe> peList, VmScheduler vmScheduler) {
		super(ramProvisioner, bwProvisioner, storage, vmScheduler);
		setGpuPeList(peList);
		setId(id);
		setType(type);
		setVideoCardAllocationPolicy(null);
	}

	public Vm getVm(int vmId, int userId) {
		for (GpuVm vm : getGpuVmList()) {
			if (vm.getId() == vmId && vm.getUserId() == userId) {
				return vm;
			}
		}
		return null;
	}

	public List<GpuVm> getGpuVmList(){
		List<GpuVm> list = new ArrayList<>();
		for (Vm vm : getVmList()) {
			if (vm.getClass().equals(GpuVm.class)) {
				list.add((GpuVm) vm);
			}
			}return list;
	}

	/**
	 * 
	 * See {@link }
	 * 
	 * @param type type of the host which is specified in {@link GpuHostTags}.
	 */
	public GpuHost(int id, String type, RamProvisioner ramProvisioner, RamProvisioner bwProvisioner, long storage,
			 VmScheduler vmScheduler) {
		super(ramProvisioner, bwProvisioner, storage,  vmScheduler);
		setId(id);
		setType(type);
		setVideoCardAllocationPolicy(null);
		setGpuHostFailed(false);
	}
	public int getNumberOfGpuPes() {
		return getPeList().size();
	}
	public int getNumberOfFreePes() {
		return PeList.getNumberOfFreePes(getGpuPeList());
	}
	public int getTotalMips() {
		return PeList.getTotalMips(getGpuPeList());
	}
	public <T extends GpuPe> List<T> getGpuPeList() {
		return (List<T>) peList;
	}
	protected <T extends GpuPe> void setGpuPeList(List<T> peList) {
		this.peList = peList;
	}

	public boolean setGpuHostFailed(boolean failed) {
		// all the PEs are failed (or recovered, depending on fail)
		this.failed = failed;
		PeList.setStatusFailed(getGpuPeList(), failed);
		return true;
	}

	public boolean setPeStatus(int peId, int status) {
		return PeList.setPeStatus(getGpuPeList(), peId, status);
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

	public boolean vgpuCreate(Vgpu vgpu, Pgpu pgpu) {
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

		if (!getVmList().isEmpty()) {
			return false;
		} else if (getVgpuSet() != null && !getVgpuSet().isEmpty()) {
			return false;
		}

		return true;
	}

	public boolean isGpuEquipped() {
		return getVideoCardAllocationPolicy() != null && !getVideoCardAllocationPolicy().getVideoCards().isEmpty();
	}


	public boolean vmCreate(Vm vm) {
		if (getStorage().getAvailableResource() < vm.getStorage().getCapacity()) {
			Log.printConcatLine("[VmScheduler.vmCreate] Allocation of VM #", vm.getId(), " to Host #", getId(),
					" failed by storage");
			return false;
		}

		if (!getRamProvisioner().allocateResourceForVm(vm, vm.getCurrentRequestedRam())) {
			Log.printConcatLine("[VmScheduler.vmCreate] Allocation of VM #", vm.getId(), " to Host #", getId(),
					" failed by RAM");
			return false;
		}

		if (!getBwProvisioner().allocateResourceForVm(vm, vm.getCurrentRequestedBw())) {
			Log.printConcatLine("[VmScheduler.vmCreate] Allocation of VM #", vm.getId(), " to Host #", getId(),
					" failed by BW");
			getRamProvisioner().deallocateResourceForVm(vm);
			return false;
		}

		if (!getVmScheduler().allocatePesForVm(vm, vm.getCurrentRequestedMips())) {
			Log.printConcatLine("[VmScheduler.vmCreate] Allocation of VM #", vm.getId(), " to Host #", getId(),
					" failed by MIPS");
			getRamProvisioner().deallocateResourceForVm(vm);
			getBwProvisioner().deallocateResourceForVm(vm);
			return false;
		}

		setDefaultStorageCapacity(getStorage().getAvailableResource() - vm.getStorage().getCapacity());
		getVmList().add(vm);
		vm.setHost(this);
		return true;
	}


}
