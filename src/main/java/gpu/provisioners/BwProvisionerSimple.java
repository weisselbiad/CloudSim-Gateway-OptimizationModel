/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package gpu.provisioners;

import gpu.GpuVm;
import gpu.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.HashMap;
import java.util.Map;

/**
 * BwProvisionerSimple is an extension of {@link BwProvisioner} which uses a best-effort policy to
 * allocate bandwidth (bw) to VMs:
 * if there is available bw on the host, it allocates; otherwise, it fails.
 * Each host has to have its own instance of a RamProvisioner.
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class BwProvisionerSimple extends BwProvisioner {

    /** The BW map, where each key is a VM id and each value
     * is the amount of BW allocated to that VM. */
    private Map<String, Long> bwTable;

    /**
     * Instantiates a new bw provisioner simple.
     *
     * @param bw The total bw capacity from the host that the provisioner can allocate to VMs.
     */
    public BwProvisionerSimple(long bw) {
        super(bw);
        setBwTable(new HashMap<String, Long>());
    }


    public boolean allocateBwForVm(GpuVm vm, long bw) {
        deallocateBwForVm(vm);

        if (getAvailableBw() >= bw) {
            setAvailableBw(getAvailableBw() - bw);
            getBwTable().put(vm.getUid(), bw);
            vm.setCurrentAllocatedBw(getAllocatedBwForVm(vm));
            return true;
        }

        vm.setCurrentAllocatedBw(getAllocatedBwForVm(vm));
        return false;
    }


    public long getAllocatedBwForVm(GpuVm vm) {
        if (getBwTable().containsKey(vm.getUid())) {
            return getBwTable().get(vm.getUid());
        }
        return 0;
    }


    public void deallocateBwForVm(GpuVm vm) {
        if (getBwTable().containsKey(vm.getUid())) {
            long amountFreed = getBwTable().remove(vm.getUid());
            setAvailableBw(getAvailableBw() + amountFreed);
            vm.setCurrentAllocatedBw(0);
        }
    }

    @Override
    public void deallocateBwForAllVms() {
        super.deallocateBwForAllVms();
        getBwTable().clear();
    }

    @Override
    public boolean isSuitableForVm(GpuVm vm, long bw) {
        long allocatedBw = getAllocatedBwForVm(vm);
        boolean result = allocateBwForVm(vm, bw);
        deallocateBwForVm(vm);
        if (allocatedBw > 0) {
            allocateBwForVm(vm, allocatedBw);
        }
        return result;
    }

    /**
     * Gets the map between VMs and allocated bw.
     *
     * @return the bw map
     */
    protected Map<String, Long> getBwTable() {
        return bwTable;
    }

    /**
     * Sets the map between VMs and allocated bw.
     *
     * @param bwTable the bw map
     */
    protected void setBwTable(Map<String, Long> bwTable) {
        this.bwTable = bwTable;
    }

}
