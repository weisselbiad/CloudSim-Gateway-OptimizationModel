package gpu.provisioners;

import gpu.GpuVm;
import org.cloudbus.cloudsim.provisioners.PeProvisioner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GpuPeProvisionerSimple extends GpuPeProvisioner {

    /** The PE map, where each key is a VM id and each value
     * is the list of PEs (in terms of their amount of MIPS)
     * allocated to that VM. */
    private Map<String, List<Double>> peTable;

    /**
     * Instantiates a new pe provisioner simple.
     *
     * @param availableMips The total mips capacity of the PE that the provisioner can allocate to VMs.
     *
     * @pre $none
     * @post $none
     */
    public GpuPeProvisionerSimple(double availableMips) {
        super(availableMips);
        setPeTable(new HashMap<String, ArrayList<Double>>());
    }

    @Override
    public boolean allocateMipsForVm(GpuVm vm, double mips) {
        return allocateMipsForVm(vm.getUid(), mips);
    }

    @Override
    public boolean allocateMipsForVm(String vmUid, double mips) {
        if (getAvailableMips() < mips) {
            return false;
        }

        List<Double> allocatedMips;

        if (getPeTable().containsKey(vmUid)) {
            allocatedMips = getPeTable().get(vmUid);
        } else {
            allocatedMips = new ArrayList<Double>();
        }

        allocatedMips.add(mips);

        setAvailableMips(getAvailableMips() - mips);
        getPeTable().put(vmUid, allocatedMips);

        return true;
    }

    @Override
    public boolean allocateMipsForVm(GpuVm vm, List<Double> mips) {
        int totalMipsToAllocate = 0;
        for (double _mips : mips) {
            totalMipsToAllocate += _mips;
        }

        if (getAvailableMips() + getTotalAllocatedMipsForVm(vm) < totalMipsToAllocate) {
            return false;
        }

        setAvailableMips(getAvailableMips() + getTotalAllocatedMipsForVm(vm) - totalMipsToAllocate);

        getPeTable().put(vm.getUid(), mips);

        return true;
    }

    @Override
    public void deallocateMipsForAllVms() {
        super.deallocateMipsForAllVms();
        getPeTable().clear();
    }

    @Override
    public double getAllocatedMipsForVmByVirtualPeId(GpuVm vm, int peId) {
        if (getPeTable().containsKey(vm.getUid())) {
            try {
                return getPeTable().get(vm.getUid()).get(peId);
            } catch (Exception e) {
            }
        }
        return 0;
    }

    @Override
    public List<Double> getAllocatedMipsForVm(GpuVm vm) {
        if (getPeTable().containsKey(vm.getUid())) {
            return getPeTable().get(vm.getUid());
        }
        return null;
    }

    @Override
    public double getTotalAllocatedMipsForVm(GpuVm vm) {
        if (getPeTable().containsKey(vm.getUid())) {
            double totalAllocatedMips = 0.0;
            for (double mips : getPeTable().get(vm.getUid())) {
                totalAllocatedMips += mips;
            }
            return totalAllocatedMips;
        }
        return 0;
    }

    @Override
    public void deallocateMipsForVm(GpuVm vm) {
        if (getPeTable().containsKey(vm.getUid())) {
            for (double mips : getPeTable().get(vm.getUid())) {
                setAvailableMips(getAvailableMips() + mips);
            }
            getPeTable().remove(vm.getUid());
        }
    }

    /**
     * Gets the pe map.
     *
     * @return the pe map
     */
    protected Map<String, List<Double>> getPeTable() {
        return peTable;
    }

    /**
     * Sets the pe map.
     *
     * @param peTable the peTable to set
     */
    @SuppressWarnings("unchecked")
    protected void setPeTable(Map<String, ? extends List<Double>> peTable) {
        this.peTable = (Map<String, List<Double>>) peTable;
    }
}