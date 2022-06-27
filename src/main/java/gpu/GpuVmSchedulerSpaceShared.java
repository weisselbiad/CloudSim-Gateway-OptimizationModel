package gpu;

import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.schedulers.MipsShare;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * VmSchedulerSpaceShared is a VMM allocation policy that allocates one or more PEs from a host to a
 * Virtual Machine Monitor (VMM), and doesn't allow sharing of PEs.
 * The allocated PEs will be used until the VM finishes running.
 * If there is no enough free PEs as required by a VM,
 * or whether the available PEs doesn't have enough capacity, the allocation fails.
 * In the case of fail, no PE is allocated to the requesting VM.
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class GpuVmSchedulerSpaceShared extends GpuVmSchedulerAbstract {

        /** A map between each VM and its allocated PEs, where the key is a VM ID and
     * the value a list of PEs allocated to VM. */
    private Map<String, List<GpuPe>> peAllocationMap;

    /** The list of free PEs yet available in the host. */
    private List<GpuPe> freePes;

    /**
     * Instantiates a new vm space-shared scheduler.
     *
     * @param pelist the pelist
     */
    public GpuVmSchedulerSpaceShared(List<? extends GpuPe> pelist) {
        super(pelist);
        setPeAllocationMap(new HashMap<String, List<GpuPe>>());
        setFreePes(new ArrayList<GpuPe>());
        getFreePes().addAll(pelist);
    }

    @Override
    public boolean allocatePesForVm(GpuVm vm, List<Double> mipsShare) {
        // if there is no enough free PEs, fails
        if (getFreePes().size() < mipsShare.size()) {
            return false;
        }

        List<GpuPe> selectedPes = new ArrayList<GpuPe>();
        Iterator<GpuPe> peIterator = getFreePes().iterator();
        GpuPe pe = peIterator.next();
        double totalMips = 0;
        for (Double mips : mipsShare) {
            if (mips <= pe.getMips()) {
                selectedPes.add(pe);
                if (!peIterator.hasNext()) {
                    break;
                }
                pe = peIterator.next();
                totalMips += mips;
            }
        }
        if (mipsShare.size() > selectedPes.size()) {
            return false;
        }

        getFreePes().removeAll(selectedPes);

        getPeAllocationMap().put(vm.getUid(), selectedPes);
        getMipsMap().put(vm.getUid(), mipsShare);
        setAvailableMips(getAvailableMips() - totalMips);
        return true;
    }

    @Override
    public void deallocatePesForVm(GpuVm vm) {
        getFreePes().addAll(getPeAllocationMap().get(vm.getUid()));
        getPeAllocationMap().remove(vm.getUid());

        double totalMips = 0;
        for (double mips : getMipsMap().get(vm.getUid())) {
            totalMips += mips;
        }
        setAvailableMips(getAvailableMips() + totalMips);

        getMipsMap().remove(vm.getUid());
    }

    /**
     * Sets the pe allocation map.
     *
     * @param peAllocationMap the pe allocation map
     */
    protected void setPeAllocationMap(Map<String, List<GpuPe>> peAllocationMap) {
        this.peAllocationMap = peAllocationMap;
    }

    /**
     * Gets the pe allocation map.
     *
     * @return the pe allocation map
     */
    protected Map<String, List<GpuPe>> getPeAllocationMap() {
        return peAllocationMap;
    }

    /**
     * Sets the free pes list.
     *
     * @param freePes the new free pes list
     */
    protected void setFreePes(List<GpuPe> freePes) {
        this.freePes = freePes;
    }

    /**
     * Gets the free pes list.
     *
     * @return the free pes list
     */
    protected List<GpuPe> getFreePes() {
        return freePes;
    }

    @Override
    public boolean allocatePesForVm(Vm vm, MipsShare requestedMips) {
        return false;
    }

    @Override
    public boolean allocatePesForVm(Vm vm) {
        return false;
    }

    @Override
    public void deallocatePesFromVm(Vm vm) {

    }

    @Override
    public void deallocatePesFromVm(Vm vm, int pesToRemove) {

    }

    @Override
    public MipsShare getAllocatedMips(Vm vm) {
        return null;
    }

    @Override
    public double getTotalAvailableMips() {
        return 0;
    }

    @Override
    public MipsShare getRequestedMips(Vm vm) {
        return null;
    }

    @Override
    public boolean isSuitableForVm(Vm vm) {
        return false;
    }

    @Override
    public boolean isSuitableForVm(Vm vm, MipsShare requestedMips) {
        return false;
    }

    @Override
    public double getTotalAllocatedMipsForVm(Vm vm) {
        return 0;
    }

    @Override
    public double getMaxCpuUsagePercentDuringOutMigration() {
        return 0;
    }

    @Override
    public double getVmMigrationCpuOverhead() {
        return 0;
    }

    @Override
    public Host getHost() {
        return null;
    }

    @Override
    public VmScheduler setHost(Host host) {
        return null;
    }
}
