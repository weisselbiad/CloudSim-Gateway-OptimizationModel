package gpu;

import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import gpu.selection.PgpuSelectionPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * VgpuSchedulerTimeShared is a vgpu allocation policy that allocates one or
 * more gpu PEs from a video card to a vgpu, and allows sharing of gpu PEs by
 * multiple vgpus. This scheduler does not support over-subscription.
 * 
 * @author Ahmad Siavashi
 */
public class VgpuSchedulerTimeShared extends VgpuScheduler {

	/**
	 * Instantiates a new vgpu time-shared scheduler.
	 * 
	 *                 is associated to.
	 */
	public VgpuSchedulerTimeShared(String videoCardType, List<Pgpu> pgpuList, PgpuSelectionPolicy pgpuSelectionPolicy) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy);
	}

	@Override
	public void deallocatePgpuForVgpu(Vgpu vgpu) {
		Pgpu pgpu = getPgpuForVgpu(vgpu);
		pgpu.getGddramProvisioner().deallocateGddramForVgpu(vgpu);
		pgpu.getBwProvisioner().deallocateBwForVgpu(vgpu);
		getPgpuVgpuMap().get(pgpu).remove(vgpu);
		for (GpuPe pe : getVgpuPeMap().get(vgpu)) {
			pe.getPeProvisioner().deallocateMipsForVm(vgpu.getVm());
		}
		getVgpuPeMap().remove(vgpu);
		getMipsMap().remove(vgpu);
		vgpu.setCurrentAllocatedMips(null);
	}

	@Override
	public boolean isSuitable(Pgpu pgpu, Vgpu vgpu) {
		final List<Double> mipsShare = vgpu.getCurrentRequestedMips();
		final int gddramShare = vgpu.getCurrentRequestedGddram();
		final long bwShare = vgpu.getCurrentRequestedBw();

		if (!pgpu.getGddramProvisioner().isSuitableForVgpu(vgpu, gddramShare)
				|| !pgpu.getBwProvisioner().isSuitableForVgpu(vgpu, bwShare)) {
			return false;
		}
		if (pgpu.getPeList().size() < mipsShare.size()) {
			return false;
		}
		List<GpuPe> pgpuPes = pgpu.getPeList();
		for (int i = 0; i < mipsShare.size(); i++) {
			if (mipsShare.get(i) > pgpuPes.get(i).getPeProvisioner().getAvailableMips()) {
				return false;
			}
		}
		return true;

	}

	@Override
	public boolean allocatePgpuForVgpu(Pgpu pgpu, Vgpu vgpu, List<Double> mipsShare, int gddramShare, long bwShare) {
		if (!isSuitable(pgpu, vgpu)) {
			return false;
		}
		pgpu.getGddramProvisioner().allocateGddramForVgpu(vgpu, gddramShare);
		pgpu.getBwProvisioner().allocateBwForVgpu(vgpu, bwShare);
		List<GpuPe> selectedPgpuPes = pgpu.getPeList();
		List<GpuPe> selectedPes = new ArrayList<GpuPe>();
		for (int i = 0; i < mipsShare.size(); i++) {
			GpuPe pe = selectedPgpuPes.get(i);
			pe.getPeProvisioner().allocateMipsForVm(vgpu.getVm(), mipsShare.get(i));
			selectedPes.add(pe);
		}
		getPgpuVgpuMap().get(pgpu).add(vgpu);
		getVgpuPeMap().put(vgpu, selectedPes);
		getMipsMap().put(vgpu, mipsShare);
		vgpu.setCurrentAllocatedMips(mipsShare);
		return true;
	}

}
