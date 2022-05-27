package cloudsimMixedPeEnv;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.hosts.HostSuitability;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.Processor;
import org.cloudbus.cloudsim.schedulers.MipsShare;
import org.cloudbus.cloudsim.util.Conversion;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmGroup;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.autoscaling.VerticalVmScaling;

import java.util.*;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public abstract class VmAllocationPolicyAbstractGpuAware implements VmAllocationPolicy {
    private BiFunction<VmAllocationPolicy, Vm, Optional<Host>> findHostForVmFunction;

    /** @see #getDatacenter() */
    private Datacenter datacenter;

    /**@see #getHostCountForParallelSearch() */
    private int hostCountForParallelSearch;

    /**
     * Creates a VmAllocationPolicy.
     */
    public VmAllocationPolicyAbstractGpuAware() {
        this(null);
    }

    /**
     * Creates a VmAllocationPolicy, changing the {@link BiFunction} to select a Host for a Vm.
     *
     * @param findHostForVmFunction a {@link BiFunction} to select a Host for a given Vm.
     * @see VmAllocationPolicy#setFindHostForVmFunction(BiFunction)
     */
    public VmAllocationPolicyAbstractGpuAware(final BiFunction<VmAllocationPolicy, Vm, Optional<Host>> findHostForVmFunction) {
        setDatacenter(Datacenter.NULL);
        setFindHostForVmFunction(findHostForVmFunction);
        this.hostCountForParallelSearch = DEF_HOST_COUNT_PARALLEL_SEARCH;
    }

    @Override
    public final <T extends Host> List<T> getHostList() {
        return datacenter.getHostList();
    }
    public final <T extends Host> List<T> getGpuHostList() {
        final var hostList = datacenter.getHostList();

        final ArrayList GpuList = new ArrayList<>();
        for (Host host : hostList) {
            if (host instanceof  GpuHost) {
                GpuList.add(host);
            }
        }
        return GpuList;
    }

    public final <T extends Host> List<T> getSimpleHostList() {
        final var hostList = datacenter.getHostList();

        final ArrayList SimpleList = new ArrayList<>();
        for (Host host : hostList) {
            if (host instanceof  HostSimple) {
                SimpleList.add(host);
            }
        }
        return SimpleList;
    }


    @Override
    public Datacenter getDatacenter() {
        return datacenter;
    }

    /**
     * Sets the Datacenter associated to the Allocation Policy
     *
     * @param datacenter the Datacenter to set
     */
    @Override
    public void setDatacenter(final Datacenter datacenter) {
        this.datacenter = requireNonNull(datacenter);
    }

    @Override
    public boolean scaleVmVertically(final VerticalVmScaling scaling) {
        if (scaling.isVmUnderloaded()) {
            return downScaleVmVertically(scaling);
        }

        if (scaling.isVmOverloaded()) {
            return upScaleVmVertically(scaling);
        }

        return false;
    }

    /**
     * Performs the up scaling of Vm resource associated to a given scaling object.
     *
     * @param scaling the Vm's scaling object
     * @return true if the Vm was overloaded and the up scaling was performed, false otherwise
     */
    private boolean upScaleVmVertically(final VerticalVmScaling scaling) {
        return isRequestingCpuScaling(scaling) ? scaleVmPesUpOrDown(scaling) : upScaleVmNonCpuResource(scaling);
    }

    /**
     * Performs the down scaling of Vm resource associated to a given scaling object.
     *
     * @param scaling the Vm's scaling object
     * @return true if the down scaling was performed, false otherwise
     */
    private boolean downScaleVmVertically(final VerticalVmScaling scaling) {
        return isRequestingCpuScaling(scaling) ? scaleVmPesUpOrDown(scaling) : downScaleVmNonCpuResource(scaling);
    }

    /**
     * Performs the up or down scaling of Vm {@link Pe}s,
     * depending if the VM is under or overloaded.
     *
     * @param scaling the Vm's scaling object
     * @return true if the scaling was performed, false otherwise
     * @see #upScaleVmVertically(VerticalVmScaling)
     */
    private boolean scaleVmPesUpOrDown(final VerticalVmScaling scaling) {
        final double pesNumberForScaling = scaling.getResourceAmountToScale();
        if (pesNumberForScaling == 0) {
            return false;
        }

        if (scaling.isVmOverloaded() && isNotHostPesSuitableToUpScaleVm(scaling)) {
            scaling.logResourceUnavailable();
            return false;
        }

        final Vm vm = scaling.getVm();
        vm.getHost().getVmScheduler().deallocatePesFromVm(vm);
        final int signal = scaling.isVmUnderloaded() ? -1 : 1;
        //Removes or adds some capacity from/to the resource, respectively if the VM is under or overloaded
        vm.getProcessor().sumCapacity((long) pesNumberForScaling * signal);

        vm.getHost().getVmScheduler().allocatePesForVm(vm);
        return true;
    }

    private boolean isNotHostPesSuitableToUpScaleVm(final VerticalVmScaling scaling) {
        final Vm vm = scaling.getVm();
        final long pesCountForScaling = (long)scaling.getResourceAmountToScale();
        final MipsShare additionalVmMips = new MipsShare(pesCountForScaling, vm.getMips());
        return !vm.getHost().getVmScheduler().isSuitableForVm(vm, additionalVmMips);
    }

    /**
     * Checks if the scaling object is in charge of scaling CPU resource.
     *
     * @param scaling the Vm scaling object
     * @return true if the scaling is for CPU, false if it is
     * for any other kind of resource
     */
    private boolean isRequestingCpuScaling(final VerticalVmScaling scaling) {
        return Processor.class.equals(scaling.getResourceClass());
    }

    /**
     * Performs the up scaling of a Vm resource that is anything else than CPU.
     *
     * @param scaling the Vm's scaling object
     * @return true if the up scaling was performed, false otherwise
     * @see #scaleVmPesUpOrDown(VerticalVmScaling)
     * @see #upScaleVmVertically(VerticalVmScaling)
     */
    private boolean upScaleVmNonCpuResource(final VerticalVmScaling scaling) {
        return scaling.allocateResourceForVm();
    }

    /**
     * Performs the down scaling of a Vm resource that is anything else than CPU.
     *
     * @param scaling the Vm's scaling object
     * @return true if the down scaling was performed, false otherwise
     * @see #downScaleVmVertically(VerticalVmScaling)
     */
    private boolean downScaleVmNonCpuResource(final VerticalVmScaling scaling) {
        final var resourceManageableClass = scaling.getResourceClass();
        final var vmResource = scaling.getVm().getResource(resourceManageableClass);
        final double amountToDeallocate = scaling.getResourceAmountToScale();
        final var resourceProvisioner = scaling.getVm().getHost().getProvisioner(resourceManageableClass);
        final double newTotalVmResource = vmResource.getCapacity() - amountToDeallocate;
        if (resourceProvisioner.allocateResourceForVm(scaling.getVm(), newTotalVmResource)) {
            LOGGER.info(
                    "{}: {}: {} {} deallocated from {}: new capacity is {}. Current resource usage is {}%",
                    scaling.getVm().getSimulation().clockStr(),
                    scaling.getClass().getSimpleName(),
                    (long) amountToDeallocate, resourceManageableClass.getSimpleName(),
                    scaling.getVm(), vmResource.getCapacity(),
                    vmResource.getPercentUtilization() * 100);
            return true;
        }

        LOGGER.error(
                "{}: {}: {} requested to reduce {} capacity by {} but an unexpected error occurred and the resource was not resized",
                scaling.getVm().getSimulation().clockStr(),
                scaling.getClass().getSimpleName(),
                scaling.getVm(),
                resourceManageableClass.getSimpleName(), (long) amountToDeallocate);
        return false;

    }

    /**
     * Allocates the host with less PEs in use for a given VM.
     *
     * @param vm {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public HostSuitability allocateHostForVm(final Vm vm) {
        if (getHostList().isEmpty()) {
            LOGGER.error(
                    "{}: {}: {} could not be allocated because there isn't any Host for Datacenter {}",
                    vm.getSimulation().clockStr(), getClass().getSimpleName(), vm, getDatacenter().getId());
            return new HostSuitability("Datacenter has no host.");
        }

        if (vm.isCreated()) {
            return new HostSuitability("VM is already created");
        }

        final var optionalHost = findHostForVm(vm);
        if (optionalHost.filter(Host::isActive).isPresent()) {
            return allocateHostForVm(vm, optionalHost.get());
        }

        LOGGER.warn("{}: {}: No suitable host found for {} in {}", vm.getSimulation().clockStr(), getClass().getSimpleName(), vm, datacenter);
        return new HostSuitability("No suitable host found");
    }

    @Override
    public <T extends Vm> List<T> allocateHostForVm(final Collection<T> vmCollection) {
        requireNonNull(vmCollection, "The list of VMs to allocate a host to cannot be null");
        return vmCollection.stream().filter(vm -> !allocateHostForVm(vm).fully()).collect(toList());
    }

    @Override
    public HostSuitability allocateHostForVm(final Vm vm, final Host host) {
        if(vm instanceof VmGroup vmGroup){
            return createVmsFromGroup(vmGroup, host);
        }

        return createVm(vm, host);
    }

    private HostSuitability createVmsFromGroup(final VmGroup vmGroup, final Host host) {
        int createdVms = 0;
        final var hostSuitabilityForVmGroup = new HostSuitability();
        for (final Vm vm : vmGroup.getVmList()) {
            final var hostSuitability = createVm(vm, host);
            hostSuitabilityForVmGroup.setSuitability(hostSuitability);
            createdVms += Conversion.boolToInt(hostSuitability.fully());
        }

        vmGroup.setCreated(createdVms > 0);
        if(vmGroup.isCreated()) {
            vmGroup.setHost(host);
        }

        return hostSuitabilityForVmGroup;
    }

    private HostSuitability createVm(final Vm vm, final Host host) {
        final var suitability = host.createVm(vm);
        if (suitability.fully()) {
            LOGGER.info(
                    "{}: {}: {} has been allocated to {}",
                    vm.getSimulation().clockStr(), getClass().getSimpleName(), vm, host);
        } else {
            LOGGER.error(
                    "{}: {} Creation of {} on {} failed due to {}.",
                    vm.getSimulation().clockStr(), getClass().getSimpleName(), vm, host, suitability);
        }

        return suitability;
    }

    @Override
    public void deallocateHostForVm(final Vm vm) {
        vm.getHost().destroyVm(vm);
    }

    /**
     * {@inheritDoc}
     * The default implementation of such a Function is provided by the method {@link #findHostForVm(Vm)}.
     *
     * @param findHostForVmFunction {@inheritDoc}.
     *                              Passing null makes the default method to find a Host for a VM to be used.
     */
    @Override
    public final void setFindHostForVmFunction(final BiFunction<VmAllocationPolicy, Vm, Optional<Host>> findHostForVmFunction) {
        this.findHostForVmFunction = findHostForVmFunction;
    }

    @Override
    public final Optional<Host> findHostForVm(final Vm vm) {
        if (vm instanceof GpuVm) {
            final var optionalHost = findHostForVmFunction == null ? GpuFindHostForVm(vm) : findHostForVmFunction.apply(this, vm);
            //If the selected Host is not active, activate it (if it's already active, setActive has no effect)
            return optionalHost.map(host -> host.setActive(true));

    }else if (vm instanceof VmSimple){
        final var optionalHost = findHostForVmFunction == null ? defaultFindHostForVm(vm) : findHostForVmFunction.apply(this, vm);
        //If the selected Host is not active, activate it (if it's already active, setActive has no effect)
        return optionalHost.map(host -> host.setActive(true));
    }
        return null;
    }

    /**
     * Provides the default implementation of the policy
     * to find a suitable Host for a given VM.
     *
     * @param vm the VM to find a suitable Host to
     * @return an {@link Optional} containing a suitable Host to place the VM or an empty {@link Optional} if no suitable Host was found
     * @see #setFindHostForVmFunction(BiFunction)
     */
    protected abstract Optional<Host> defaultFindHostForVm(Vm vm);
    protected abstract Optional<Host> GpuFindHostForVm(Vm vm);



    @Override
    public Map<Vm, Host> getOptimizedAllocationMap(final List<? extends Vm> vmList) {
        /*
         * This method implementation doesn't perform any
         * VM placement optimization and, in fact, has no effect.
         * Classes implementing the {@link VmAllocationPolicyMigration}
         * provide actual implementations for this method that can be overridden
         * by subclasses.
         */
        return Collections.emptyMap();
    }

    @Override
    public int getHostCountForParallelSearch() {
        return hostCountForParallelSearch;
    }

    @Override
    public void setHostCountForParallelSearch(final int hostCountForParallelSearch) {
        this.hostCountForParallelSearch = hostCountForParallelSearch;
    }

    @Override
    public boolean isVmMigrationSupported() {
        return false;
    }
}
