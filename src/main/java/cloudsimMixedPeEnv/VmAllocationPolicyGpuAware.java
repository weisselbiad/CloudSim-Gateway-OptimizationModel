package cloudsimMixedPeEnv;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyAbstract;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Comparator.comparing;

public class VmAllocationPolicyGpuAware extends VmAllocationPolicyAbstractGpuAware implements VmAllocationPolicy {
    private int lastHostIndex;
     private int lastGpuHostIndex;

    /**
     * Instantiates a VmAllocationPolicySimple.
     */
    public VmAllocationPolicyGpuAware() {
        super();
    }

    /**
     * Instantiates a VmAllocationPolicySimple, changing the {@link Function} to select a Host for a Vm
     * in order to define a different policy.
     *
     * @param findHostForVmFunction a {@link Function} to select a Host for a given Vm.
     * @see VmAllocationPolicy#setFindHostForVmFunction(java.util.function.BiFunction)
     */
    public VmAllocationPolicyGpuAware(final BiFunction<VmAllocationPolicy, Vm, Optional<Host>> findHostForVmFunction) {
        super(findHostForVmFunction);
    }

    /**
     * Gets the first suitable host from the {@link #getHostList()} that has the fewest number of used PEs (i.e, higher free PEs).
     *
     * @return an {@link Optional} containing a suitable Host to place the VM or an empty {@link Optional} if not found
     */
    @Override
        protected Optional<Host> defaultFindHostForVm(final Vm vm) {
        final Comparator<Host> comparator = comparing(Host::isActive).thenComparingLong(Host::getFreePesNumber);
        final var simplehostStream = isParallelHostSearchEnabled() ? getSimpleHostList().stream().parallel() : getSimpleHostList().stream();
        return simplehostStream.filter(host -> host.isSuitableForVm(vm)).max(comparator);

        //   for (int i = 0; i < maxTries; i++) {
        //Different from the FirstFit policy, it always increments the host index.

            // final Host gpuhost = getGpuHostList().get(lastGpuHostIndex);
            // if (gpuhost.isSuitableForVm(vm)) {
            //   lastGpuHostIndex = ++lastGpuHostIndex % getGpuHostList().size();
            //   Optional.of(gpuhost);

            // final Host simplehost = getSimpleHostList().get(lastHostIndex);
            // if (simplehost.isSuitableForVm(vm)) {
            //  lastHostIndex = ++lastHostIndex % getSimpleHostList().size();
            //  Optional.of(simplehost);

    }
    protected Optional<Host> GpuFindHostForVm(final Vm vm) {
        final Comparator<Host> comparator = comparing(Host::isActive).thenComparingLong(Host::getFreePesNumber);
        final var gpuhostStream = isParallelHostSearchEnabled() ? getGpuHostList().stream().parallel() : getGpuHostList().stream();
        return gpuhostStream.filter(host -> host.isSuitableForVm(vm)).max(comparator);

    }


    }


