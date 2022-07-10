/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.brokers;

import gpu.GpuCloudlet;
import gpu.GpuDatacenter;
import gpu.GpuDatacenterBroker;
import gpu.core.CloudSimTags;
import gpu.core.CloudletList;
import gpu.core.Log;
import gpu.core.VmList;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.*;
import org.cloudbus.cloudsim.core.events.CloudSimEvent;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.datacenters.TimeZoned;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.util.InvalidEventDataTypeException;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmGroup;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.autoscaling.VerticalVmScaling;
import org.cloudsimplus.listeners.DatacenterBrokerEventInfo;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.cloudsimplus.traces.google.GoogleTaskEventsTraceReader;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * An abstract class for implementing {@link DatacenterBroker}s.
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Manoel Campos da Silva Filho
 */
public abstract class DatacenterBrokerAbstract extends CloudSimEntity implements DatacenterBroker {
    /**
     * A default {@link Function} which always returns {@link #DEF_VM_DESTRUCTION_DELAY}
     * to indicate that any VM should not be immediately destroyed after it becomes idle.
     * This way, using this Function the broker will destroy VMs only after:
     * <ul>
     *   <li>all submitted Cloudlets from all its VMs are finished and there are no waiting Cloudlets;</li>
     *   <li>or all running Cloudlets are finished and there are some of them waiting their VMs to be created.</li>
     * </ul>
     *
     * @see #setVmDestructionDelayFunction(Function)
     */
    private static final Function<Vm, Double> DEF_VM_DESTRUCTION_DELAY_FUNC = vm -> DEF_VM_DESTRUCTION_DELAY;

    private boolean selectClosestDatacenter;

    /**
     * A List of registered event listeners for the onVmsCreatedListeners event.
     *
     * @see #addOnVmsCreatedListener(EventListener)
     */
    private  List<EventListener<DatacenterBrokerEventInfo>> onVmsCreatedListeners;

    /**
     * Last Vm selected to run some Cloudlets.
     */
    private Vm lastSelectedVm;

    /**
     * The last datacenter where a VM was created or tried to be created.
     */
    private Datacenter lastSelectedDc;

    /** @see #setFailedVmsRetryDelay(double)  */
    private double failedVmsRetryDelay;

    /** @see #getVmFailedList() */
    private  List<Vm> vmFailedList;

    /** @see #getVmWaitingList() */
    private  List<Vm> vmWaitingList;

    /** @see #getVmExecList() */
    private  List<Vm> vmExecList;

    /** @see #getVmCreatedList() */
    private  List<Vm> vmCreatedList;

    /** @see #getCloudletWaitingList() */
    private  List<Cloudlet> cloudletWaitingList;

    /** @see #getCloudletSubmittedList() */
    public  List<Cloudlet> cloudletSubmittedList;

    /** @see #getCloudletFinishedList() */
    private  List<Cloudlet> cloudletsFinishedList;

    /** @see #getCloudletCreatedList() () */
    private  List<Cloudlet> cloudletsCreatedList;

    /**
     * Checks if the last time checked, there were waiting cloudlets or not.
     */
    private boolean wereThereWaitingCloudlets;

    /** @see #setDatacenterMapper(BiFunction) */
    private BiFunction<Datacenter, Vm, Datacenter> datacenterMapper;

    /** @see #setVmMapper(Function) */
    private Function<Cloudlet, Vm> vmMapper;

    /** @see #setVmComparator(Comparator) */
    private Comparator<Vm> vmComparator;

    /** @see #setCloudletComparator(Comparator) */
    private Comparator<Cloudlet> cloudletComparator;

    /** @see #getVmCreationRequests() */
    private int vmCreationRequests;

    /** @see #getDatacenterList() */
    private List<Datacenter> datacenterList;

    private Cloudlet lastSubmittedCloudlet;
    private Vm lastSubmittedVm;

    /** @see #getVmDestructionDelayFunction() */
    private Function<Vm, Double> vmDestructionDelayFunction;

    /**
     * Indicates if a shutdown request was already sent or not.
     */
    private boolean shutdownRequested;

    /** @see #isShutdownWhenIdle()  */
    private boolean shutdownWhenIdle;
    private boolean vmCreationRetrySent;

    /**
     * Creates a DatacenterBroker giving a specific name.
     * Subclasses usually should provide this constructor
     * and overloaded version that just requires the {@link CloudSim} parameter.
     *
     * @param simulation the CloudSim instance that represents the simulation the Entity is related to
     * @param name the DatacenterBroker name
     */
    public DatacenterBrokerAbstract(final CloudSim simulation, final String name) {
        super(simulation);
        if(!name.isEmpty()) {
            setName(name);
        }

        this.onVmsCreatedListeners = new ArrayList<>();
        this.lastSubmittedCloudlet = Cloudlet.NULL;
        this.lastSubmittedVm = Vm.NULL;
        this.lastSelectedVm = Vm.NULL;
        this.lastSelectedDc = Datacenter.NULL;
        this.shutdownWhenIdle = true;

        this.vmCreationRequests = 0;
        this.failedVmsRetryDelay = 5;
        this.vmFailedList = new ArrayList<>();
        this.vmWaitingList = new ArrayList<>();
        this.vmExecList = new ArrayList<>();
        this.vmCreatedList = new ArrayList<>();
        this.cloudletWaitingList = new ArrayList<>();
        this.cloudletsFinishedList = new ArrayList<>();
        this.cloudletsCreatedList = new ArrayList<>();
        this.cloudletSubmittedList = new ArrayList<>();
        setDatacenterList(new ArrayList<>());

        setDatacenterMapper(this::defaultDatacenterMapper);
        setVmMapper(this::defaultVmMapper);
        vmDestructionDelayFunction = DEF_VM_DESTRUCTION_DELAY_FUNC;
    }

    /***
     * BRocker for GPU Datacenter constructor
     * @param name
     * @throws Exception
     */
    /** The list of VMs submitted to be managed by the broker. */
    protected List<? extends Vm> vmList;

    /** The list of VMs created by the broker. */
    protected List<? extends Vm> vmsCreatedList;

    /** The list of cloudlet submitted to the broker.
     * @see #submitCloudletList(java.util.List)
     */
    protected List<? extends Cloudlet> cloudletList;

    /** The list of submitted cloudlets. */
    protected List<GpuCloudlet> gpucloudletSubmittedList;

    /** The list of received cloudlet. */
    protected List<? extends Cloudlet> cloudletReceivedList;

    /** The number of submitted cloudlets. */
    protected int cloudletsSubmitted;


    /** The number of requests to create VM. */
    protected int vmsRequested;

    /** The number of acknowledges (ACKs) sent in response to
     * VM creation requests. */
    protected int vmsAcks;

    /** The number of destroyed VMs. */
    protected int vmsDestroyed;

    /** The id's list of available datacenters. */
    protected List<Integer> datacenterIdsList;

    /** The list of datacenters where was requested to place VMs. */
    protected List<Integer> datacenterRequestedIdsList;

    /** The vms to datacenters map, where each key is a VM id
     * and each value is the datacenter id whwere the VM is placed. */
    protected Map<Integer, Integer> vmsToDatacentersMap;

    /** The datacenter characteristics map where each key
     * is a datacenter id and each value is its characteristics.. */
    protected Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList;
    public CloudSim simulation ;

    public DatacenterBrokerAbstract(final CloudSim simulation) throws Exception {
        super(simulation);

        setgpuVmList(new ArrayList<Vm>());
        setgpuVmsCreatedList(new ArrayList<Vm>());
        setgpuCloudletList(new ArrayList<Cloudlet>());
        setgpuCloudletSubmittedList(new ArrayList<GpuCloudlet>());
        setgpuCloudletReceivedList(new ArrayList<Cloudlet>());

        cloudletsSubmitted = 0;
        setgpuVmsRequested(0);
        setVmsAcks(0);
        setVmsDestroyed(0);

        setDatacenterIdsList(new LinkedList<Integer>());
        setDatacenterRequestedIdsList(new ArrayList<Integer>());
        setVmsToDatacentersMap(new HashMap<Integer, Integer>());
        setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());

        this.simulation = simulation;
    }



    @Override
    public final DatacenterBroker setSelectClosestDatacenter(final boolean select) {
        this.selectClosestDatacenter = select;
        if(select){
            setDatacenterMapper(this::closestDatacenterMapper);
        }

        return this;
    }

    @Override
    public boolean isSelectClosestDatacenter() {
        return selectClosestDatacenter;
    }

    @Override
    public DatacenterBroker submitVmList(final List<? extends Vm> list, final double submissionDelay) {
        setDelayForEntitiesWithNoDelay(list, submissionDelay);
        return submitVmList(list);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The individual submission delay of VMs inside the group will be ignored.
     * Only the submission delay set for the {@link VmGroup} will be considered.</p>
     *
     * <p>If the entity already started (the simulation is running),
     * the creation of previously submitted VMs already was requested
     * by the {@link #start()} method that is called just once.
     * By this way, this method will immediately request the creation of these
     * just submitted VMs in order to allow VM creation after
     * the simulation has started. This avoid the developer to
     * dynamically create brokers just to create VMs or Cloudlets during
     * simulation execution.</p>
     *
     * @param list {@inheritDoc}
     * @see VmGroup
     * @return {@inheritDoc}
     */
    @Override
    public DatacenterBroker submitVmList(final List<? extends Vm> list) {
        sortVmsIfComparatorIsSet(list);
        configureEntities(list);
        lastSubmittedVm = setIdForEntitiesWithoutOne(list, lastSubmittedVm);
        vmWaitingList.addAll(list);

        if (isStarted() && !list.isEmpty()) {
            LOGGER.info(
                "{}: {}: List of {} VMs submitted to the broker during simulation execution. VMs creation request sent to Datacenter.",
                getSimulation().clockStr(), getName(), list.size());
            requestDatacentersToCreateWaitingCloudlets();
            if(!vmCreationRetrySent) {
                lastSelectedDc = null;
                requestDatacenterToCreateWaitingVms(false, false);
            }
        }

        return this;
    }

    /**
     * Configures attributes for each {@link CustomerEntity} into a given list.
     *
     * @param customerEntities the List of {@link CustomerEntity} to configure.
     */
    private void configureEntities(final List<? extends CustomerEntity> customerEntities) {
        for (final var entity : customerEntities) {
            entity.setBroker(this);
            entity.setArrivedTime(getSimulation().clock());
            if(entity instanceof VmGroup vmGroup) {
                configureEntities(vmGroup.getVmList());
            }
        }
    }

    /**
     * Defines IDs for a list of {@link CustomerEntity} entities that don't
     * have one already assigned. Such entities can be a {@link Cloudlet},
     * {@link Vm}, {@link VmGroup} or any object that implements {@link CustomerEntity}.
     *
     * @param list                list of objects to define an ID
     * @param lastSubmittedEntity the last Entity that was submitted to the broker
     * @return the last Entity in the given List of the lastSubmittedEntity if the List is empty
     */
    private <T extends CustomerEntity> T setIdForEntitiesWithoutOne(final List<? extends T> list, T lastSubmittedEntity) {
        return Simulation.setIdForEntitiesWithoutOne(list, lastSubmittedEntity);
    }

    private void sortVmsIfComparatorIsSet(final List<? extends Vm> list) {
        if (vmComparator != null) {
            list.sort(vmComparator);
        }
    }

    @Override
    public DatacenterBroker submitVm(final Vm vm) {
        requireNonNull(vm);
        if (Vm.NULL.equals(vm)) {
            return this;
        }

        final var newVmList = new ArrayList<Vm>(1);
        newVmList.add(vm);
        return submitVmList(newVmList);
    }

    @Override
    public DatacenterBroker submitCloudlet(final Cloudlet cloudlet) {
        requireNonNull(cloudlet);
        if (cloudlet == Cloudlet.NULL) {
            return this;
        }

        final var newCloudletList = new ArrayList<Cloudlet>(1);
        newCloudletList.add(cloudlet);
        return submitCloudletList(newCloudletList);
    }

    @Override
    public DatacenterBroker submitCloudletList(final List<? extends Cloudlet> list, double submissionDelay) {
        return submitCloudletList(list, Vm.NULL, submissionDelay);
    }

    @Override
    public DatacenterBroker submitCloudletList(final List<? extends Cloudlet> list, Vm vm) {
        return submitCloudletList(list, vm, -1);
    }

    @Override
    public DatacenterBroker submitCloudletList(final List<? extends Cloudlet> list, Vm vm, double submissionDelay) {
        setDelayForEntitiesWithNoDelay(list, submissionDelay);
        bindCloudletsToVm(list, vm);
        return submitCloudletList(list);
    }

    /**
     * {@inheritDoc}
     * <p>If the entity already started (the simulation is running),
     * the creation of previously submitted Cloudlets already was requested
     * by the {@link #start()} method that is called just once.
     * By this way, this method will immediately request the creation of these
     * just submitted Cloudlets if all submitted VMs were already created,
     * in order to allow Cloudlet creation after
     * the simulation has started. This avoid the developer to
     * dynamically create brokers just to create VMs or Cloudlets during
     * simulation execution.</p>
     *
     * @param list {@inheritDoc}
     * @see #submitCloudletList(List, double)
     * @return {@inheritDoc}
     */
    @Override
    public DatacenterBroker submitCloudletList(final List<? extends Cloudlet> list) {
        if (list.isEmpty()) {
            return this;
        }

        sortCloudletsIfComparatorIsSet(list);
        configureEntities(list);
        lastSubmittedCloudlet = setIdForEntitiesWithoutOne(list, lastSubmittedCloudlet);
        cloudletSubmittedList.addAll(list);
        setSimulationForCloudletUtilizationModels(list);
        cloudletWaitingList.addAll(list);
        wereThereWaitingCloudlets = true;

        if (!isStarted()) {
            return this;
        }

        LOGGER.info(
            "{}: {}: List of {} Cloudlets submitted to the broker during simulation execution.",
            getSimulation().clockStr(), getName(), list.size());

        LOGGER.info("Cloudlets creation request sent to Datacenter.");
        requestDatacentersToCreateWaitingCloudlets();

        return this;
    }

    /**
     * Binds a list of Cloudlets to a given {@link Vm}.
     * If the {@link Vm} is {@link Vm#NULL}, the Cloudlets will not be bound.
     *
     * @param cloudlets the List of Cloudlets to be bound to a VM
     * @param vm        the VM to bind the Cloudlets to
     */
    private void bindCloudletsToVm(final List<? extends Cloudlet> cloudlets, Vm vm) {
        if (Vm.NULL.equals(vm)) {
            return;
        }

        cloudlets.forEach(c -> c.setVm(vm));
    }

    private void sortCloudletsIfComparatorIsSet(final List<? extends Cloudlet> cloudlets) {
        if (cloudletComparator != null) {
            cloudlets.sort(cloudletComparator);
        }
    }

    private void setSimulationForCloudletUtilizationModels(final List<? extends Cloudlet> cloudletList) {
        for (final var cloudlet : cloudletList) {
            setSimulationForUtilizationModel(cloudlet.getUtilizationModelCpu());
            setSimulationForUtilizationModel(cloudlet.getUtilizationModelBw());
            setSimulationForUtilizationModel(cloudlet.getUtilizationModelRam());
        }
    }

    private void setSimulationForUtilizationModel(final UtilizationModel cloudletUtilizationModel) {
        if (cloudletUtilizationModel.getSimulation() == null || cloudletUtilizationModel.getSimulation() == Simulation.NULL) {
            cloudletUtilizationModel.setSimulation(getSimulation());
        }
    }

    /**
     * Sets the delay for a list of {@link CustomerEntity} entities that don't
     * have a delay already assigned. Such entities can be a {@link Cloudlet},
     * {@link Vm} or any object that implements {@link CustomerEntity}.
     *
     * <p>If the delay is defined as a negative number, objects' delay
     * won't be changed.</p>
     *
     * @param entities list of objects to set their delays
     * @param submissionDelay the submission delay to set
     */
    private void setDelayForEntitiesWithNoDelay(final List<? extends CustomerEntity> entities, final double submissionDelay) {
        if (submissionDelay < 0) {
            return;
        }

        entities.stream()
            .filter(entity -> entity.getSubmissionDelay() <= 0)
            .forEach(entity -> entity.setSubmissionDelay(submissionDelay));
    }

    @Override
    public boolean bindCloudletToVm(final Cloudlet cloudlet, final Vm vm) {
        if (!this.equals(cloudlet.getBroker()) && !DatacenterBroker.NULL.equals(cloudlet.getBroker())) {
            return false;
        }

        cloudlet.setVm(vm);
        return true;
    }

    @Override
    public void processEvent(final SimEvent evt) {
        Object sr = evt.getSource();
        Object dst = evt.getDestination();
        if ( sr instanceof DatacenterSimple || sr instanceof DatacenterBrokerSimple || dst instanceof DatacenterSimple || dst instanceof DatacenterBrokerSimple ) {
            if (processCloudletEvents(evt) || processVmEvents(evt) || processGeneralEvents(evt)) {
                return;
            }
        } else if (sr instanceof GpuDatacenterBroker || sr instanceof GpuDatacenter || dst instanceof GpuDatacenterBroker || dst instanceof GpuDatacenter) {


        switch (evt.getTag()) {
                // Resource characteristics request
                case RESOURCE_CHARACTERISTICS_REQUEST:
                    processResourceCharacteristicsRequest(evt);
                    break;
                // Resource characteristics answer
                case RESOURCE_CHARACTERISTICS:
                    processResourceCharacteristics(evt);
                    break;
                // VM Creation answer
                case VM_CREATE_ACK:
                    processVmCreate(evt);
                    break;
                // A finished cloudlet returned
                case CLOUDLET_RETURN:
                    processgpuCloudletReturn(evt);
                    break;
                // if the simulation finishes
                case END_OF_SIMULATION:
                    shutdownEntity();
                    break;
                // other unknown tags are processed by this method
                default:
                    processOtherEvent(evt);
                    break;


            }
        }else
        LOGGER.trace("{}: {}: Unknown event {} received.", getSimulation().clockStr(), this, evt.getTag());
    }

    private boolean processCloudletEvents(final SimEvent evt) {
        return switch (evt.getTag()) {
            case CLOUDLET_RETURN -> processCloudletReturn(evt);
            case CLOUDLET_READY -> processCloudletReady(evt);
            /* The data of such a kind of event is a Runnable that has all
             * the logic to update Cloudlet's attributes.
             * This way, it will be run to perform such an update.
             * Check the documentation of the tag below for details.*/
            case CLOUDLET_UPDATE_ATTRIBUTES -> executeRunnableEvent(evt);
            case CLOUDLET_PAUSE -> processCloudletPause(evt);
            case CLOUDLET_CANCEL -> processCloudletCancel(evt);
            case CLOUDLET_FINISH -> processCloudletFinish(evt);
            case CLOUDLET_FAIL -> processCloudletFail(evt);
            default -> false;
        };
    }

    private boolean executeRunnableEvent(final SimEvent evt){
        if(evt.getData() instanceof Runnable runnable) {
            runnable.run();
            return true;
        }

        throw new InvalidEventDataTypeException(evt, "CLOUDLET_UPDATE_ATTRIBUTES", "Runnable");
    }

    private boolean processVmEvents(final SimEvent evt) {
        return switch (evt.getTag()) {
            case VM_CREATE_RETRY -> {
                vmCreationRetrySent = false;
                yield requestDatacenterToCreateWaitingVms(false, true);
            }
            case VM_CREATE_ACK -> processVmCreateResponseFromDatacenter(evt);
            case VM_VERTICAL_SCALING -> requestVmVerticalScaling(evt);
            default -> false;
        };
    }

    private boolean processGeneralEvents(final SimEvent evt) {
        if (evt.getTag() == CloudSimTag.DC_LIST_REQUEST) {
            processDatacenterListRequest(evt);
            return true;
        }

        if (evt.getTag() == CloudSimTag.ENTITY_SHUTDOWN || evt.getTag() == CloudSimTag.SIMULATION_END) {
            shutdown();
            return true;
        }

        return false;
    }

    /**
     * Sets the status of a received Cloudlet to {@link Cloudlet.Status#READY}
     * so that the Cloudlet can be selected to start running as soon as possible
     * by a {@link CloudletScheduler}.
     *
     * <p>This tag is commonly used when Cloudlets are created
     * from a trace file such as a {@link GoogleTaskEventsTraceReader Google Cluster Trace}.</p>
     *
     * @param evt the event to process
     */
    private boolean processCloudletReady(final SimEvent evt){
        final var cloudlet = (Cloudlet)evt.getData();
        if(cloudlet.getStatus() == Cloudlet.Status.PAUSED)
             logCloudletStatusChange(cloudlet, "resume execution of");
        else logCloudletStatusChange(cloudlet, "start executing");

        cloudlet.getVm().getCloudletScheduler().cloudletReady(cloudlet);
        return true;
    }

    private boolean processCloudletPause(final SimEvent evt){
        final var cloudlet = (Cloudlet)evt.getData();
        logCloudletStatusChange(cloudlet, "de-schedule (pause)");
        cloudlet.getVm().getCloudletScheduler().cloudletPause(cloudlet);
        return true;
    }

    private boolean processCloudletCancel(final SimEvent evt){
        final var cloudlet = (Cloudlet)evt.getData();
        logCloudletStatusChange(cloudlet, "cancel execution of");
        cloudlet.getVm().getCloudletScheduler().cloudletCancel(cloudlet);
        return true;
    }

    /**
     * Process the request to finish a Cloudlet with a indefinite length,
     * setting its length as the current number of processed MI.
     * @param evt the event data
     */
    private boolean processCloudletFinish(final SimEvent evt){
        final var cloudlet = (Cloudlet)evt.getData();
        logCloudletStatusChange(cloudlet, "finish running");
        /* If the executed length is zero, it means the cloudlet processing was not updated yet.
         * This way, calls the method to update the Cloudlet's processing.*/
        if(cloudlet.getFinishedLengthSoFar() == 0){
            updateHostProcessing(cloudlet);
        }

        /* If after updating the host processing, the cloudlet executed length is still zero,
         * it means the Cloudlet has never started. This happens, for instance, due
         * to lack of PEs to run the Cloudlet (usually when you're using a CloudletSchedulerSpaceShared).
         * This way, sets the Cloudlet as failed. */
        if(cloudlet.getFinishedLengthSoFar() == 0) {
            cloudlet.getVm().getCloudletScheduler().cloudletFail(cloudlet);
            return true;
        }

        final long prevLength = cloudlet.getLength();
        cloudlet.setLength(cloudlet.getFinishedLengthSoFar());

        /* After defining the Cloudlet length, updates the Cloudlet processing again so that the Cloudlet status
         * is updated at this clock tick instead of the next one.*/
        updateHostProcessing(cloudlet);

        /* If the Cloudlet length was negative, after finishing it,
         * a VM update event is sent to ensure the broker is notified the Cloudlet has finished.
         * A negative length makes the Cloudlet to keep running until a finish message is
         * sent to the broker. */
        if(prevLength < 0){
            final double delay = cloudlet.getSimulation().getMinTimeBetweenEvents();
            final Datacenter dc = cloudlet.getVm().getHost().getDatacenter();
            dc.schedule(delay, CloudSimTag.VM_UPDATE_CLOUDLET_PROCESSING);
        }

        return true;
    }

    /**
     * Updates the processing of the Host where a Cloudlet's VM is running.
     * @param cloudlet
     */
    private void updateHostProcessing(final Cloudlet cloudlet) {
        cloudlet.getVm().getHost().updateProcessing(getSimulation().clock());
    }

    private void logCloudletStatusChange(final Cloudlet cloudlet, final String status) {
        final String msg = cloudlet.getJobId() > 0 ? String.format("(job %d) ", cloudlet.getJobId()) : "";
        LOGGER.info("{}: {}: Request to {} {} {}received.", getSimulation().clockStr(), getName(), status, cloudlet, msg);
    }

    private boolean processCloudletFail(final SimEvent evt){
        final var cloudlet = (Cloudlet)evt.getData();
        cloudlet.getVm().getCloudletScheduler().cloudletFail(cloudlet);
        return true;
    }

    private boolean requestVmVerticalScaling(final SimEvent evt) {
        if (evt.getData() instanceof VerticalVmScaling scaling) {
            getSimulation().sendNow(
                evt.getSource(), scaling.getVm().getHost().getDatacenter(),
                CloudSimTag.VM_VERTICAL_SCALING, scaling);
            return true;
        }

        throw new InvalidEventDataTypeException(evt, "VM_VERTICAL_SCALING", "VerticalVmScaling");
    }

    /**
     * Process a request to get the list of all Datacenters registered in the
     * Cloud Information Service (CIS) of the {@link #getSimulation() simulation}.
     *
     * @param evt a CloudSimEvent object
     */
    private void processDatacenterListRequest(final SimEvent evt) {
        if(evt.getData() instanceof List datacenterSet) {
            setDatacenterList(datacenterSet);
            LOGGER.info("{}: {}: List of {} datacenters(s) received.", getSimulation().clockStr(), getName(), datacenterList.size());
            requestDatacenterToCreateWaitingVms(false, false);
            return;
        }

        throw new InvalidEventDataTypeException(evt, "DC_LIST_REQUEST", "List<Datacenter>");
    }

    /**
     * Process the ack received from a Datacenter to a broker's request for
     * creation of a Vm in that Datacenter.
     *
     * @param evt a SimEvent object
     * @return true if the VM was created successfully, false otherwise
     */
    private boolean processVmCreateResponseFromDatacenter(final SimEvent evt) {
        final var vm = (Vm) evt.getData();

        //if the VM was successfully created in the requested Datacenter
        if (vm.isCreated()) {
            processSuccessVmCreationInDatacenter(vm);
            vm.notifyOnHostAllocationListeners();
        } else {
            vm.setFailed(true);
            if(!isRetryFailedVms()){
                vmWaitingList.remove(vm);
                vmFailedList.add(vm);
                LOGGER.warn("{}: {}: {} has been moved to the failed list because creation retry is not enabled.", getSimulation().clockStr(), getName(), vm);
            }

            vm.notifyOnCreationFailureListeners(lastSelectedDc);
        }

        //Decreases to indicate an ack for the request was received (either if the VM was created or not)
        vmCreationRequests--;

        if(vmCreationRequests == 0 && !vmWaitingList.isEmpty()) {
            requestCreationOfWaitingVmsToFallbackDatacenter();
        }

        if(allNonDelayedVmsCreated()) {
            requestDatacentersToCreateWaitingCloudlets();
        }

        return vm.isCreated();
    }

    /**
     * Checks if all VMs submitted with no delay were created.
     * Only after that, cloudlets creation is requested.
     * Otherwise, all waiting cloudlets would be sent to the
     * first created VM.
     * @return
     */
    private boolean allNonDelayedVmsCreated() {
        return vmWaitingList.stream().noneMatch(vm -> vm.getSubmissionDelay() == 0);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private void notifyOnVmsCreatedListeners() {
        if(!vmWaitingList.isEmpty()) {
            return;
        }

        //Uses indexed for to avoid ConcurrentModificationException
        for (int i = 0; i < onVmsCreatedListeners.size(); i++) {
            final var listener = onVmsCreatedListeners.get(i);
            listener.update(DatacenterBrokerEventInfo.of(listener, this));
        }
    }

    /**
     * After the response (ack) of all VM creation requests were received
     * but not all VMs could be created (what means some
     * acks informed about Vm creation failures), try to find
     * another Datacenter to request the creation of the VMs
     * in the waiting list.
     */
    private void requestCreationOfWaitingVmsToFallbackDatacenter() {
        this.lastSelectedDc = Datacenter.NULL;
        if (vmWaitingList.isEmpty() || requestDatacenterToCreateWaitingVms(false, true)) {
            return;
        }

        final var msg =
            "{}: {}: {} of the requested {} VMs couldn't be created because suitable Hosts weren't found in any available Datacenter."
            + (vmExecList.isEmpty() && !isRetryFailedVms() ? " Shutting broker down..." : "");
        LOGGER.error(msg, getSimulation().clockStr(), getName(), vmWaitingList.size(), getVmsNumber());

        /* If it gets here, it means that all datacenters were already queried and not all VMs could be created. */
        if (!vmWaitingList.isEmpty()) {
            processVmCreationFailure();
            return;
        }

        requestDatacentersToCreateWaitingCloudlets();
    }

    private void processVmCreationFailure() {
        if (isRetryFailedVms()) {
            lastSelectedDc = datacenterList.get(0);
            this.vmCreationRetrySent = true;
            schedule(failedVmsRetryDelay, CloudSimTag.VM_CREATE_RETRY);
        } else shutdown();
    }

    /**
     * Request the creation of {@link #getVmWaitingList() waiting VMs} in some Datacenter.
     *
     * <p>If it's trying a fallback datacenter and the {@link #selectClosestDatacenter} is enabled,
     * that means the function assigned to the {@link #datacenterMapper} is the
     * {@link #closestDatacenterMapper(Datacenter, Vm)}
     * which has failed to find a suitable Datacenter for the VM.
     * This way, it uses the {@link #defaultDatacenterMapper(Datacenter, Vm)} instead.
     * </p>
     *
     * @param isFallbackDatacenter true to indicate that a fallback Datacenter will be tried,
     *                             after the previous one was not able to create all waiting VMs,
     *                             false to indicate it will try the default datacenter.
     * @return true if some Datacenter was selected, false if all Datacenter were tried
     *         and not all VMs could be created
     * @see #submitVmList(List)
     */
    private boolean requestDatacenterToCreateWaitingVms(final boolean isFallbackDatacenter, final boolean creationRetry) {
        for (final Vm vm : vmWaitingList) {
            this.lastSelectedDc = isFallbackDatacenter && selectClosestDatacenter ?
                                        defaultDatacenterMapper(lastSelectedDc, vm) :
                                        datacenterMapper.apply(lastSelectedDc, vm);
            if(creationRetry) {
                vm.setLastTriedDatacenter(Datacenter.NULL);
            }
            this.vmCreationRequests += requestVmCreation(lastSelectedDc, isFallbackDatacenter, vm);
        }

        return lastSelectedDc != Datacenter.NULL;
    }

    @Override
    public int getVmsNumber() {
        return vmCreatedList.size() + vmWaitingList.size() + vmFailedList.size();
    }

    /**
     * Process a response from a Datacenter informing that it was able to
     * create the VM requested by the broker.
     *
     * @param vm id of the Vm that succeeded to be created inside the Datacenter
     */
    private void processSuccessVmCreationInDatacenter(final Vm vm) {
        if(vm instanceof VmGroup vmGroup){
            int createdVms = 0;
            for (final Vm nextVm : vmGroup.getVmList()) {
                if (nextVm.isCreated()) {
                    processSuccessVmCreationInDatacenter(nextVm);
                    createdVms++;
                }
            }

            if(createdVms == vmGroup.size()){
                vmWaitingList.remove(vmGroup);
            }

            return;
        }

        vmWaitingList.remove(vm);
        vmExecList.add(vm);
        vmCreatedList.add(vm);
        notifyOnVmsCreatedListeners();
    }

    /**
     * Processes the end of execution of a given cloudlet inside a Vm.
     *
     * @param evt a SimEvent object containing the cloudlet that has just finished executing and returned to the broker
     */
    private boolean processCloudletReturn(final SimEvent evt) {
        final var cloudlet = (Cloudlet) evt.getData();
        cloudletsFinishedList.add(cloudlet);
        ((VmSimple) cloudlet.getVm()).addExpectedFreePesNumber(cloudlet.getNumberOfPes());
        LOGGER.info("{}: {}: {} finished in {} and returned to broker.", getSimulation().clockStr(), getName(), cloudlet, cloudlet.getVm());

        if (cloudlet.getVm().getCloudletScheduler().isEmpty()) {
            requestIdleVmDestruction(cloudlet.getVm());
            return true;
        }

        requestVmDestructionAfterAllCloudletsFinished();
        return true;
    }

    /**
     * Request the destruction of VMs after all running cloudlets have finished and returned to the broker.
     * If there is no waiting Cloudlet, request all VMs to be destroyed.
     */
    private void requestVmDestructionAfterAllCloudletsFinished() {
        for (int i = vmExecList.size() - 1; i >= 0; i--) {
            requestIdleVmDestruction(vmExecList.get(i));
        }

        if (cloudletWaitingList.isEmpty()) {
            return;
        }

        /*
        There are some cloudlets waiting their VMs to be created.
        Idle VMs were destroyed above and here it requests the creation of waiting ones.
        When there are waiting Cloudlets, the destruction
        of idle VMs possibly free resources to start waiting VMs.
        This way, if a VM destruction delay function is not set,
        it defines one that always return 0 to indicate
        idle VMs must be destroyed immediately.
        */
        requestDatacenterToCreateWaitingVms(false, false);
    }

    @Override
    public DatacenterBroker requestIdleVmDestruction(final Vm vm) {
        if (vm.isCreated()) {
            if(isVmIdleEnough(vm) || isFinished()) {
                LOGGER.info("{}: {}: Requesting {} destruction.", getSimulation().clockStr(), getName(), vm);
                sendNow(getDatacenter(vm), CloudSimTag.VM_DESTROY, vm);
            }

            if(isVmIdlenessVerificationRequired((VmSimple)vm)) {
                getSimulation().send(
                    new CloudSimEvent(vmDestructionDelayFunction.apply(vm),
                        vm.getHost().getDatacenter(),
                        CloudSimTag.VM_UPDATE_CLOUDLET_PROCESSING));
                return this;
            }
        }

        requestShutdownWhenIdle();
        return this;
    }

    private boolean isVmIdleEnough(final Vm vm) {
        final double delay = vmDestructionDelayFunction.apply(vm);
        return delay > DEF_VM_DESTRUCTION_DELAY && vm.isIdleEnough(delay);
    }

    @Override
    public void requestShutdownWhenIdle() {
        if (!shutdownRequested && isTimeToShutdownBroker()) {
            schedule(CloudSimTag.ENTITY_SHUTDOWN);
            shutdownRequested = true;
        }
    }

    @Override
    public List<Cloudlet> destroyVm(final Vm vm) {
        if(vm.isCreated()) {
            final var cloudletsAffectedList = new ArrayList<Cloudlet>();

            for (final var iterator = gpucloudletSubmittedList.iterator(); iterator.hasNext(); ) {
                final Cloudlet cloudlet = iterator.next();
                if(cloudlet.getVm().equals(vm) && !cloudlet.isFinished()) {
                    cloudlet.setVm(Vm.NULL);
                    cloudletsAffectedList.add(cloudlet.reset());
                    iterator.remove();
                }
            }

            vm.getHost().destroyVm(vm);
            vm.getCloudletScheduler().clear();
            return cloudletsAffectedList;
        }

        LOGGER.warn("Vm: " + vm.getId() + " does not belong to this broker! Broker: " + this);
        return new ArrayList<>();
    }

    /**
     * Checks if an event must be sent to verify if a VM became idle.
     * That will happen when the {@link #getVmDestructionDelayFunction() VM destruction delay}
     * is set and is not multiple of the {@link Datacenter#getSchedulingInterval()}
     *
     * <p>
     * In such situation, that means it is required to send additional events to check if a VM became idle.
     * No additional events are required when:
     * <ul>
     *   <li>the VM destruction delay was not set (VMs will be destroyed only when the broker is shutdown);</li>
     *   <li>the delay was set and it's multiple of the scheduling interval
     *   (VM idleness will be checked in the interval defined by the Datacenter scheduling).</li>
     * </ul>
     *
     * Avoiding additional messages improves performance of large scale simulations.
     * </p>
     * @param vm the Vm to check
     * @return true if a message to check VM idleness has to be sent, false otherwise
     */
    private boolean isVmIdlenessVerificationRequired(final VmSimple vm) {
        if(vm.hasStartedSomeCloudlet() && vm.getCloudletScheduler().isEmpty()){
            final int schedulingInterval = (int)vm.getHost().getDatacenter().getSchedulingInterval();
            final int delay = vmDestructionDelayFunction.apply(vm).intValue();
            return delay > DEF_VM_DESTRUCTION_DELAY && (schedulingInterval <= 0 || delay % schedulingInterval != 0);
        }

        return false;
    }

    /**
     * Checks if the broker is still alive and it's idle, so that it may be shutdown
     * @return
     */
    private boolean isTimeToShutdownBroker() {
        return isAlive() && isTimeToTerminateSimulation() && shutdownWhenIdle && isBrokerIdle();
    }

    private boolean isTimeToTerminateSimulation() {
        return !getSimulation().isTerminationTimeSet() || getSimulation().isTimeToTerminateSimulationUnderRequest();
    }

    private boolean isBrokerIdle() {
        return cloudletWaitingList.isEmpty() && vmWaitingList.isEmpty() && vmExecList.isEmpty();
    }

    /**
     * Try to request the creation of a VM into a given datacenter
     * @param datacenter the Datacenter to try creating the VM
     *                   (or {@link Datacenter#NULL} if not Datacenter is available)
     * @param isFallbackDatacenter indicate if the given Datacenter was selected when
     *                             a previous one don't have enough capacity to place the requested VM
     * @param vm the VM to be placed
     * @return 1 to indicate a VM creation request was sent to the datacenter,
     *         0 to indicate the request was not sent due to lack of available datacenter
     */
    private int requestVmCreation(final Datacenter datacenter, final boolean isFallbackDatacenter, final Vm vm) {
        if (datacenter == Datacenter.NULL || datacenter.equals(vm.getLastTriedDatacenter())) {
            return 0;
        }

        logVmCreationRequest(datacenter, isFallbackDatacenter, vm);
        send(datacenter, vm.getSubmissionDelay(), CloudSimTag.VM_CREATE_ACK, vm);
        vm.setLastTriedDatacenter(datacenter);
        return 1;
    }

    private void logVmCreationRequest(final Datacenter datacenter, final boolean isFallbackDatacenter, final Vm vm) {
        final var fallbackMsg = isFallbackDatacenter ? " (due to lack of a suitable Host in previous one)" : "";
        if(vm.getSubmissionDelay() == 0)
            LOGGER.info(
                "{}: {}: Trying to create {} in {}{}",
                getSimulation().clockStr(), getName(), vm, datacenter.getName(), fallbackMsg);
        else
            LOGGER.info(
                "{}: {}: Creation of {} in {}{} will be requested in {} seconds",
                getSimulation().clockStr(), getName(), vm, datacenter.getName(),
                fallbackMsg, vm.getSubmissionDelay());
    }

    /**
     * Request Datacenters to create the Cloudlets in the
     * {@link #getCloudletWaitingList() Cloudlets waiting list}.
     * If there aren't available VMs to host all cloudlets,
     * the creation of some ones will be postponed.
     *
     * <p>This method is called after all submitted VMs are created
     * in some Datacenter.</p>
     *
     * @see #submitCloudletList(List)
     */
    protected void requestDatacentersToCreateWaitingCloudlets() {
        /* Uses Iterator to remove Cloudlets from the waiting list
         * while iterating over that List. This avoids the collection of successfully
         * created Cloudlets into a separate list.
         * Cloudlets in such new list were removed just after the loop,
         * degrading performance in large scale simulations. */
        int createdCloudlets = 0;
        for (final var iterator = cloudletWaitingList.iterator(); iterator.hasNext(); ) {
            final CloudletSimple cloudlet = (CloudletSimple)iterator.next();
            if (!cloudlet.getLastTriedDatacenter().equals(Datacenter.NULL)) {
                continue;
            }

            //selects a VM for the given Cloudlet
            lastSelectedVm = vmMapper.apply(cloudlet);
            if (!lastSelectedVm.isCreated()) {
                logPostponingCloudletExecution(cloudlet);
                continue;
            }

            ((VmSimple) lastSelectedVm).removeExpectedFreePesNumber(cloudlet.getNumberOfPes());

            logCloudletCreationRequest(cloudlet);
            cloudlet.setVm(lastSelectedVm);
            final Datacenter dc = getDatacenter(lastSelectedVm);
            send(dc, cloudlet.getSubmissionDelay(), CloudSimTag.CLOUDLET_SUBMIT, cloudlet);
            cloudlet.setLastTriedDatacenter(dc);
            cloudletsCreatedList.add(cloudlet);
            iterator.remove();
            createdCloudlets++;
        }

        allWaitingCloudletsSubmittedToVm(createdCloudlets);
    }

    private void logPostponingCloudletExecution(final Cloudlet cloudlet) {
        if(getSimulation().isAborted() || getSimulation().isAbortRequested())
            return;

        final Vm vm = cloudlet.getVm();
        final String vmMsg = Vm.NULL.equals(vm) ?
                                "it couldn't be mapped to any VM" :
                                String.format("bind Vm %d is not available", vm.getId());

        final String msg = String.format(
            "%s: %s: Postponing execution of Cloudlet %d because {}.",
            getSimulation().clockStr(), getName(), cloudlet.getId());

        if(vm.getSubmissionDelay() > 0) {
            final String secs = vm.getSubmissionDelay() > 1 ? "seconds" : "second";
            final var reason = String.format("bind Vm %d was requested to be created with %.2f %s delay", vm.getId(), vm.getSubmissionDelay(), secs);
            LOGGER.info(msg, reason);
        } else LOGGER.warn(msg, vmMsg);
    }

    private void logCloudletCreationRequest(final Cloudlet cloudlet) {
        final String delayMsg =
            cloudlet.getSubmissionDelay() > 0 ?
                String.format(" with a requested delay of %.0f seconds", cloudlet.getSubmissionDelay()) :
                "";

        LOGGER.info(
            "{}: {}: Sending Cloudlet {} to {} in {}{}.",
            getSimulation().clockStr(), getName(), cloudlet.getId(),
            lastSelectedVm, lastSelectedVm.getHost(), delayMsg);
    }

    /**
     * @param createdCloudlets number of Cloudlets previously waiting that have been just created
     */
    private boolean allWaitingCloudletsSubmittedToVm(final int createdCloudlets) {
        if (!cloudletWaitingList.isEmpty()) {
            return false;
        }

        //avoid duplicated notifications
        if (wereThereWaitingCloudlets) {
            LOGGER.info(
                "{}: {}: All {} waiting Cloudlets submitted to some VM.",
                getSimulation().clockStr(), getName(), createdCloudlets);
            wereThereWaitingCloudlets = false;
        }

        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        LOGGER.info("{}: {} is shutting down...", getSimulation().clockStr(), getName());
        requestVmDestructionAfterAllCloudletsFinished();
    }

    @Override
    public void startInternal() {
        LOGGER.info("{} is starting...", getName());
        schedule(getSimulation().getCloudInfoService(), 0, CloudSimTag.DC_LIST_REQUEST);
    }

    @Override
    public <T extends Vm> List<T> getVmCreatedList() {
        return (List<T>) vmCreatedList;
    }

    @Override
    public <T extends Vm> List<T> getVmExecList() {
        return (List<T>) vmExecList;
    }

    @Override
    public <T extends Vm> List<T> getVmWaitingList() {
        return (List<T>) vmWaitingList;
    }

    @Override
    public Vm getWaitingVm(final int index) {
        if (index >= 0 && index < vmWaitingList.size()) {
            return vmWaitingList.get(index);
        }

        return Vm.NULL;
    }

    @Override
    public List<Cloudlet> getCloudletCreatedList() {
        return cloudletsCreatedList;
    }

    @Override
    public <T extends Cloudlet> List<T> getCloudletWaitingList() {
        return (List<T>) cloudletWaitingList;
    }

    @Override
    public <T extends Cloudlet> List<T> getCloudletFinishedList() {
        return (List<T>) new ArrayList<>(cloudletsFinishedList);
    }

    /**
     * Gets a Vm at a given index from the {@link #getVmExecList() list of created VMs}.
     *
     * @param vmIndex the index where a VM has to be got from the created VM list
     * @return the VM at the given index or {@link Vm#NULL} if the index is invalid
     */
    protected Vm getVmFromCreatedList(final int vmIndex) {
        return vmIndex >= 0 && vmIndex < vmExecList.size() ? vmExecList.get(vmIndex) : Vm.NULL;
    }

    /**
     * Gets the number of VM creation requests.
     *
     * @return the number of VM creation requests
     */
    protected int getVmCreationRequests() {
        return vmCreationRequests;
    }

    /**
     * Gets the list of available datacenters.
     *
     * @return the dc list
     */
    protected List<Datacenter> getDatacenterList() {
        return datacenterList;
    }

    /**
     * Sets the list of available datacenters.
     *
     * @param datacenterList the new dc list
     */
    private void setDatacenterList(final List<Datacenter> datacenterList) {
        this.datacenterList = datacenterList;
        if(selectClosestDatacenter){
            this.datacenterList.sort(Comparator.comparingDouble(Datacenter::getTimeZone));
        }
    }

    /**
     * Gets the Datacenter where a VM is placed.
     *
     * @param vm the VM to get its Datacenter
     * @return
     */
    protected Datacenter getDatacenter(final Vm vm) {
        return vm.getHost().getDatacenter();
    }

    @Override
    public final DatacenterBroker setDatacenterMapper(final BiFunction<Datacenter, Vm, Datacenter> datacenterMapper) {
        this.datacenterMapper = requireNonNull(datacenterMapper);
        return this;
    }

    @Override
    public final DatacenterBroker setVmMapper(final Function<Cloudlet, Vm> vmMapper) {
        this.vmMapper = requireNonNull(vmMapper);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If null is given, VMs won't be sorted and follow submission order.</p>
     * @param comparator {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public DatacenterBroker setVmComparator(final Comparator<Vm> comparator) {
        this.vmComparator = comparator;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If null is given, Cloudlets won't be sorted and follow submission order.</p>
     * @param comparator {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public void setCloudletComparator(final Comparator<Cloudlet> comparator) {
        this.cloudletComparator = comparator;
    }

    @Override
    public DatacenterBroker addOnVmsCreatedListener(final EventListener<DatacenterBrokerEventInfo> listener) {
        this.onVmsCreatedListeners.add(requireNonNull(listener));
        return this;
    }

    @Override
    public DatacenterBroker removeOnVmsCreatedListener(final EventListener<? extends EventInfo> listener) {
        this.onVmsCreatedListeners.remove(requireNonNull(listener));
        return this;
    }

    @Override
    public String toString() {
        return "Broker " + getId();
    }

    @Override
    public Function<Vm, Double> getVmDestructionDelayFunction() {
        return vmDestructionDelayFunction;
    }

    @Override
    public DatacenterBroker setVmDestructionDelay(final double delay) {
        if(delay <= getSimulation().getMinTimeBetweenEvents() && delay != DEF_VM_DESTRUCTION_DELAY){
            final var msg = "The delay should be larger then the simulation minTimeBetweenEvents to ensure VMs are gracefully shutdown.";
            throw new IllegalArgumentException(msg);
        }

        setVmDestructionDelayFunction(vm -> delay);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If null is given, the default VM destruction delay function will be used.</p>
     * @param function {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public DatacenterBroker setVmDestructionDelayFunction(final Function<Vm, Double> function) {
        this.vmDestructionDelayFunction = function == null ? DEF_VM_DESTRUCTION_DELAY_FUNC : function;
        return this;
    }



    /**
     * The policy used to select the closest Datacenter to run each {@link #getVmWaitingList() waiting VM},
     * according to their timezone offset.
     * This policy is just used if {@link #isSelectClosestDatacenter() selection of the closest datacenter} is enabled.
     *
     * @param lastDatacenter the last selected Datacenter
     * @param vm the VM trying to be created
     * @return the Datacenter selected to request the creating of waiting VMs
     *         or {@link Datacenter#NULL} if no suitable Datacenter was found
     * @see #defaultDatacenterMapper(Datacenter, Vm)
     * @see #setSelectClosestDatacenter(boolean)
     */
    protected Datacenter closestDatacenterMapper(final Datacenter lastDatacenter, final Vm vm) {
        return TimeZoned.closestDatacenter(vm, getDatacenterList());
    }

    /**
     * The default policy used to select a Datacenter to run {@link #getVmWaitingList() waiting VMs}.
     * @param lastDatacenter the last selected Datacenter
     * @param vm the VM trying to be created
     * @return the Datacenter selected to request the creating of waiting VMs
     *         or {@link Datacenter#NULL} if no suitable Datacenter was found
     * @see DatacenterBroker#setDatacenterMapper(BiFunction)
     * @see #closestDatacenterMapper(Datacenter, Vm)
     */
    protected abstract Datacenter defaultDatacenterMapper(Datacenter lastDatacenter, Vm vm);

    /**
     * The default policy used to select a VM to execute a given Cloudlet.
     * The method defines the default policy used to map VMs for Cloudlets
     * that are waiting to be created.
     *
     * <p>Since this policy can be dynamically changed
     * by calling {@link #setVmMapper(Function)},
     * this method will always return the default policy
     * provided by the subclass where the method is being called.</p>
     *
     * @param cloudlet the cloudlet that needs a VM to execute
     * @return the selected Vm for the cloudlet or {@link Vm#NULL} if
     * no suitable VM was found
     *
     * @see #setVmMapper(Function)
     */
    protected abstract Vm defaultVmMapper(Cloudlet cloudlet);

    @Override
    public <T extends Vm> List<T> getVmFailedList() {
        return  (List<T>) vmFailedList;
    }

    @Override
    public boolean isRetryFailedVms() {
        return failedVmsRetryDelay > 0;
    }

    @Override
    public double getFailedVmsRetryDelay() {
        return failedVmsRetryDelay;
    }

    @Override
    public void setFailedVmsRetryDelay(final double failedVmsRetryDelay) {
        this.failedVmsRetryDelay = failedVmsRetryDelay;
    }

    @Override
    public boolean isShutdownWhenIdle() {
        return shutdownWhenIdle;
    }

    @Override
    public DatacenterBroker setShutdownWhenIdle(final boolean shutdownWhenIdle) {
        this.shutdownWhenIdle = shutdownWhenIdle;
        return this;
    }

    /***
     * Methodes for GPU Dataceter**************************************************************************************************************************************************************************************
     * GPU
     */
    /**
     * This method is used to send to the broker the list with virtual machines that must be
     * created.
     *
     * @param list the list
     * @pre list !=null
     * @post $none
     */
    public void submitgpuVmList(List<? extends Vm> list) {
        getgpuVmList().addAll(list);
    }

    /**
     * This method is used to send to the broker the list of cloudlets.
     *
     * @param list the list
     * @pre list !=null
     * @post $none
     *
     *
     *
     * The method {@link #submitVmList(java.util.List)} may have
     * be checked too.
     */
    public void submitgpuCloudletList(List<? extends GpuCloudlet> list) {
        getgpuCloudletList().addAll(list);
    }

    /**
     * Specifies that a given cloudlet must run in a specific virtual machine.
     *
     * @param cloudletId ID of the cloudlet being bount to a vm
     * @param vmId the vm id
     * @pre cloudletId > 0
     * @pre id > 0
     * @post $none
     */
    public abstract void bindgpuCloudletToVm(int cloudletId, int vmId);

    /**
     * Process the return of a request for the characteristics of a Datacenter.
     *
     * @param ev a SimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processResourceCharacteristics(SimEvent ev) {
        DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
        getDatacenterCharacteristicsList().put((int)characteristics.getId(), characteristics);

        if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
            setDatacenterRequestedIdsList(new ArrayList<Integer>());
            createVmsInDatacenter(getDatacenterIdsList().get(0));
        }
    }

    /**
     * Process a request for the characteristics of a PowerDatacenter.
     *
     * @param ev a SimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processResourceCharacteristicsRequest(SimEvent ev) {
        setDatacenterIdsList(getSimulation().getCloudInfoService().getcisList());
        setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());

        Log.printConcatLine(getSimulation().clock(), ": ", getName(), ": Cloud Resource List received with ",
                getDatacenterIdsList().size(), " resource(s)");

        for (Integer datacenterId : getDatacenterIdsList()) {
            sendNow(ev.getSource(), CloudSimTag.RESOURCE_CHARACTERISTICS, getId());
        }
    }

    /**
     * Process the ack received due to a request for VM creation.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     */
    protected void processVmCreate(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int datacenterId = data[0];
        int vmId = data[1];
        int result = data[2];

        if (result == CloudSimTags.TRUE) {
            getVmsToDatacentersMap().put(vmId, datacenterId);
            getVmCreatedList().add(VmList.getById(getgpuVmList(), vmId));
            Log.printConcatLine(getSimulation().clock(), ": ", getName(), ": VM #", vmId,
                    " has been created in Datacenter #", datacenterId, ", Host #",
                    VmList.getById(getVmCreatedList(), vmId).getHost().getId());
        } else {
            Log.printConcatLine(getSimulation().clock(), ": ", getName(), ": Creation of VM #", vmId,
                    " failed in Datacenter #", datacenterId);
        }

        incrementVmsAcks();

        // all the requested VMs have been created
        if (getVmCreatedList().size() == getgpuVmList().size() - getVmsDestroyed()) {
            submitgpuCloudlets();
        } else {
            // all the acks received, but some VMs were not created
            if (getgpuVmsRequested() == getVmsAcks()) {
                // find id of the next datacenter that has not been tried
                for (int nextDatacenterId : getDatacenterIdsList()) {
                    if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
                        createVmsInDatacenter(nextDatacenterId);
                        return;
                    }
                }

                // all datacenters already queried
                if (getVmCreatedList().size() > 0) { // if some vm were created
                    submitgpuCloudlets();
                } else { // no vms created. abort
                    Log.printLine(getSimulation().clock() + ": " + getName()
                            + ": none of the required VMs could be created. Aborting");
                    finishExecution();
                }
            }
        }
    }

    /**
     * Process a cloudlet return event.
     *
     * @param ev a SimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processgpuCloudletReturn(SimEvent ev) {
        Cloudlet cloudlet = (Cloudlet) ev.getData();
        getgpuCloudletReceivedList().add(cloudlet);
        Log.printConcatLine(getSimulation().clock(), ": ", getName(), ": Cloudlet ", cloudlet.getId(),
                " received");
        cloudletsSubmitted--;
        if (getgpuCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all cloudlets executed
            Log.printConcatLine(getSimulation().clock(), ": ", getName(), ": All Cloudlets executed. Finishing...");
            clearDatacenters();
            finishExecution();
        } else { // some cloudlets haven't finished yet
            if (getgpuCloudletList().size() > 0 && cloudletsSubmitted == 0) {
                // all the cloudlets sent finished. It means that some bount
                // cloudlet is waiting its VM be created
                clearDatacenters();
                createVmsInDatacenter(0);
            }

        }
    }

    /**
     * Process non-default received events that aren't processed by
     * This method should be overridden by subclasses in other to process
     * new defined events.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     * @todo to ensure the method will be overridden, it should be defined
     * as abstract in a super class from where new brokers have to be extended.
     */
    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printConcatLine(getName(), ".processOtherEvent(): ", "Error - an event is null.");
            return;
        }

        Log.printConcatLine(getName(), ".processOtherEvent(): Error - event unknown by this DatacenterBroker.");
    }

    /**
     * Create the submitted virtual machines in a datacenter.
     *
     * @param datacenterId Id of the chosen Datacenter
     * @pre $none
     * @post $none
     * @see #submitVmList(java.util.List)
     */
    protected void createVmsInDatacenter(int datacenterId) {
        // send as much vms as possible for this datacenter before trying the next one
        int requestedVms = 0;

        for (Vm vm : getgpuVmList()) {
            if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
                Log.printLine(getSimulation().clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
                        + " in " + getSimulation().getEntityId(getDatacenter(vm).getName()));
                sendNow(getDatacenterList().get(datacenterId), CloudSimTag.VM_CREATE_ACK, vm);
                requestedVms++;
            }
        }

        getDatacenterRequestedIdsList().add(datacenterId);

        setgpuVmsRequested(requestedVms);
        setVmsAcks(0);
    }

    /**
     * Submit cloudlets to the created VMs.
     *
     * @pre $none
     * @post $none
     * @see #submitCloudletList(java.util.List)
     */
    protected void submitgpuCloudlets() {
        int vmIndex = 0;
        List<Cloudlet> successfullySubmitted = new ArrayList<Cloudlet>();
        for (GpuCloudlet cloudlet : getgpuCloudletList()) {
            Vm vm;
            // if user didn't bind this cloudlet and it has not been executed yet
            if (cloudlet.getVm().getId() == -1) {
                vm = getgpuVmsCreatedList().get(vmIndex);
            } else { // submit to the specific vm
                vm = VmList.getById((List<Vm>)getgpuVmsCreatedList(),(int) cloudlet.getVm().getId());
                if (vm == null) { // vm was not created
                    if(!Log.isDisabled()) {
                        Log.printConcatLine(getSimulation().clock(), ": ", getName(), ": Postponing execution of cloudlet ",
                                cloudlet.getId(), ": bount VM not available");
                    }
                    continue;
                }
            }

            if (!Log.isDisabled()) {
                Log.printConcatLine(getSimulation().clock(), ": ", getName(), ": Sending cloudlet ",
                        cloudlet.getId(), " to VM #", vm.getId());
            }

            cloudlet.getVm().setId(vmIndex);
            sendNow(getVmFromCreatedList((int)vm.getId()).getSimulation().getEntityId(vm.toString()), CloudSimTag.CLOUDLET_SUBMIT, cloudlet);
            cloudletsSubmitted++;
            vmIndex = (vmIndex + 1) % getgpuVmsCreatedList().size();
            getgpuCloudletSubmittedList().add(cloudlet);
            successfullySubmitted.add(cloudlet);
        }

        // remove submitted cloudlets from waiting list
        getgpuCloudletList().removeAll(successfullySubmitted);
    }

    /**
     * Destroy all virtual machines running in datacenters.
     *
     * @pre $none
     * @post $none
     */
    protected void clearDatacenters() {
        for (Vm vm : getgpuVmsCreatedList()) {
            Log.printConcatLine(getSimulation().clock(), ": " + getName(), ": Destroying VM #", vm.getId());
            sendNow(getVmFromCreatedList((int)vm.getId()).getSimulation().getEntityId(vm.toString()), CloudSimTag.VM_DESTROY, vm);
        }

        getgpuVmsCreatedList().clear();
    }

    /**
     * Send an internal event communicating the end of the simulation.
     *
     * @pre $none
     * @post $none
     */
    protected void finishExecution() {
        sendNow(getSimulation().getEntityId(getName()), CloudSimTag.END_OF_SIMULATION);
    }

    public void shutdownEntity() {
        Log.printConcatLine(getName(), " is shutting down...");
    }


    public void startEntity() {
        Log.printConcatLine(getName(), " is starting...");
        schedule(getSimulation().getEntityId(getName()), 0, CloudSimTag.RESOURCE_CHARACTERISTICS_REQUEST);
    }

    /**
     * Gets the vm list.
     *
     * @param <T> the generic type
     * @return the vm list
     */
    @SuppressWarnings("unchecked")
    public <T extends Vm> List<T> getgpuVmList() {
        return (List<T>) vmList;
    }

    /**
     * Sets the vm list.
     *
     * @param <T> the generic type
     * @param vmList the new vm list
     */
    protected <T extends Vm> void setgpuVmList(List<T> vmList) {
        this.vmList = vmList;
    }

    /**
     * Gets the cloudlet list.
     *
     * @param <T> the generic type
     * @return the cloudlet list
     */
    @SuppressWarnings("unchecked")
    public <T extends GpuCloudlet> List<T> getgpuCloudletList() {
        return (List<T>) cloudletList;
    }

    /**
     * Sets the cloudlet list.
     *
     * @param <T> the generic type
     * @param cloudletList the new cloudlet list
     */
    protected <T extends Cloudlet> void setgpuCloudletList(List<T> cloudletList) {
        this.cloudletList = cloudletList;
    }

    /**
     * Gets the cloudlet submitted list.
     *
     *   * @return the cloudlet submitted list
     */
    @SuppressWarnings("unchecked")
    public  List<GpuCloudlet> getgpuCloudletSubmittedList() {
        return  gpucloudletSubmittedList;
    }

    /**
     * Sets the cloudlet submitted list.
     *
     * @param <T> the generic type
     */
    protected <T extends Cloudlet> void setgpuCloudletSubmittedList(List<GpuCloudlet> gpucloudletSubmittedList) {
        this.gpucloudletSubmittedList = gpucloudletSubmittedList;
    }

    /**
     * Gets the cloudlet received list.
     *
     * @param <T> the generic type
     * @return the cloudlet received list
     */
    @SuppressWarnings("unchecked")
    public <T extends Cloudlet> List<T> getgpuCloudletReceivedList() {
        return (List<T>) cloudletReceivedList;
    }

    /**
     * Sets the cloudlet received list.
     *
     * @param <T> the generic type
     * @param cloudletReceivedList the new cloudlet received list
     */
    protected <T extends Cloudlet> void setgpuCloudletReceivedList(List<T> cloudletReceivedList) {
        this.cloudletReceivedList = cloudletReceivedList;
    }

    /**
     * Gets the vm list.
     *
     * @param <T> the generic type
     * @return the vm list
     */
    @SuppressWarnings("unchecked")
    public <T extends Vm> List<T> getgpuVmsCreatedList() {
        return (List<T>) vmsCreatedList;
    }

    /**
     * Sets the vm list.
     *
     * @param <T> the generic type
     * @param vmsCreatedList the vms created list
     */
    protected <T extends Vm> void setgpuVmsCreatedList(List<T> vmsCreatedList) {
        this.vmsCreatedList = vmsCreatedList;
    }

    /**
     * Gets the vms requested.
     *
     * @return the vms requested
     */
    protected int getgpuVmsRequested() {
        return vmsRequested;
    }

    /**
     * Sets the vms requested.
     *
     * @param vmsRequested the new vms requested
     */
    protected void setgpuVmsRequested(int vmsRequested) {
        this.vmsRequested = vmsRequested;
    }

    /**
     * Gets the vms acks.
     *
     * @return the vms acks
     */
    protected int getVmsAcks() {
        return vmsAcks;
    }

    /**
     * Sets the vms acks.
     *
     * @param vmsAcks the new vms acks
     */
    protected void setVmsAcks(int vmsAcks) {
        this.vmsAcks = vmsAcks;
    }

    /**
     * Increment the number of acknowledges (ACKs) sent in response
     * to requests of VM creation.
     */
    protected void incrementVmsAcks() {
        vmsAcks++;
    }

    /**
     * Gets the vms destroyed.
     *
     * @return the vms destroyed
     */
    protected int getVmsDestroyed() {
        return vmsDestroyed;
    }

    /**
     * Sets the vms destroyed.
     *
     * @param vmsDestroyed the new vms destroyed
     */
    protected void setVmsDestroyed(int vmsDestroyed) {
        this.vmsDestroyed = vmsDestroyed;
    }

    /**
     * Gets the datacenter ids list.
     *
     * @return the datacenter ids list
     */
    protected List<Integer> getDatacenterIdsList() {
        return datacenterIdsList;
    }

    /**
     * Sets the datacenter ids list.
     *
     * @param datacenterIdsList the new datacenter ids list
     */
    protected void setDatacenterIdsList(List<Integer> datacenterIdsList) {
        this.datacenterIdsList = datacenterIdsList;
    }

    /**
     * Gets the vms to datacenters map.
     *
     * @return the vms to datacenters map
     */
    protected Map<Integer, Integer> getVmsToDatacentersMap() {
        return vmsToDatacentersMap;
    }

    /**
     * Sets the vms to datacenters map.
     *
     * @param vmsToDatacentersMap the vms to datacenters map
     */
    protected void setVmsToDatacentersMap(Map<Integer, Integer> vmsToDatacentersMap) {
        this.vmsToDatacentersMap = vmsToDatacentersMap;
    }

    /**
     * Gets the datacenter characteristics list.
     *
     * @return the datacenter characteristics list
     */
    protected Map<Integer, DatacenterCharacteristics> getDatacenterCharacteristicsList() {
        return datacenterCharacteristicsList;
    }

    /**
     * Sets the datacenter characteristics list.
     *
     * @param datacenterCharacteristicsList the datacenter characteristics list
     */
    protected void setDatacenterCharacteristicsList(
            Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList) {
        this.datacenterCharacteristicsList = datacenterCharacteristicsList;
    }

    /**
     * Gets the datacenter requested ids list.
     *
     * @return the datacenter requested ids list
     */
    protected List<Integer> getDatacenterRequestedIdsList() {
        return datacenterRequestedIdsList;
    }

    /**
     * Sets the datacenter requested ids list.
     *
     * @param datacenterRequestedIdsList the new datacenter requested ids list
     */
    protected void setDatacenterRequestedIdsList(List<Integer> datacenterRequestedIdsList) {
        this.datacenterRequestedIdsList = datacenterRequestedIdsList;
    }




}
