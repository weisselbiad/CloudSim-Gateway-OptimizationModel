/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.core;

import gpu.GpuVm;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.network.HostPacket;
import org.cloudbus.cloudsim.power.PowerMeter;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.autoscaling.VerticalVmScaling;
import org.cloudsimplus.traces.google.GoogleTaskEventsTraceReader;

/**
 * Tags indicating a type of action that
 * needs to be undertaken by CloudSim entities when they receive or send events.
 * <b>NOTE:</b> To avoid conflicts with other tags,
 * CloudSim reserves numbers lower than 300 and the number 9600.
 *
 * @author Manzur Murshed
 * @author Rajkumar Buyya
 * @author Anthony Sulistio
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Toolkit 1.0
 */
public enum CloudSimTag implements Comparable<CloudSimTag> {
    /** An unclassified tag. */
    NONE(-1),

    /**
     * Tag used for requesting an entity to shut down.
     * That ensures a graceful shutdown, after other entity events are processed.
     */
    ENTITY_SHUTDOWN(-2),

    /**
     * Denotes the end of simulation.
     */
    SIMULATION_END,

    /**
     * Denotes a request from a Datacenter to register itself. This tag is normally used
     * between {@link CloudInformationService} and Datacenter entities.
     * When such a {@link SimEvent} is sent, the {@link SimEvent#getData()}
     * must be a {@link Datacenter} object.
     */
    DC_REGISTRATION_REQUEST,

    /**
     * Denotes a request from a broker to a {@link CloudInformationService} to get
     * the list of all Datacenters, including the ones that can support advanced reservation.
     */
    DC_LIST_REQUEST,

    /**
     * Denotes a request to register a {@link CloudInformationService} entity as a regional CIS.
     * When such a {@link SimEvent} is sent, the {@link SimEvent#getData()}
     * must be a {@link CloudInformationService} object.
     */
    REGISTER_REGIONAL_CIS,

    /**
     * Denotes a request to get a list of other regional CIS entities from the
     * system CIS entity.
     */
    REQUEST_REGIONAL_CIS,

    /**
     * This tag is used by an entity to send ping requests.
     */
    ICMP_PKT_SUBMIT,

    /**
     * This tag is used to return the ping request back to sender.
     */
    ICMP_PKT_RETURN,

    /**
     * Denotes the return of a finished Cloudlet back to the sender.
     * This tag is normally used by Datacenter entity.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Cloudlet} object.
     */
    CLOUDLET_RETURN,

    /**
     * Denotes the submission of a Cloudlet. This tag is normally used between
     * a DatacenterBroker and Datacenter entity.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Cloudlet} object.
     */
    CLOUDLET_SUBMIT,

    /**
     * Denotes the submission of a Cloudlet with an acknowledgement. This tag is
     * normally used between DatacenterBroker and Datacenter entity.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Cloudlet} object.
     *
     */
    CLOUDLET_SUBMIT_ACK,

    /**
     * Cancels a Cloudlet submitted in the Datacenter entity.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Cloudlet} object.
     */
    CLOUDLET_CANCEL,

    /**
     * Pauses a Cloudlet submitted in the Datacenter entity.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Cloudlet} object.
     */
    CLOUDLET_PAUSE,

    /**
     * Pauses a Cloudlet submitted in the Datacenter entity with an
     * acknowledgement.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Cloudlet} object.
     */
    CLOUDLET_PAUSE_ACK,

    /**
     * Resumes a Cloudlet submitted in the Datacenter entity.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Cloudlet} object.
     */
    CLOUDLET_RESUME,

    /**
     * Resumes a Cloudlet submitted in the Datacenter entity with an
     * acknowledgement.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Cloudlet} object.
     */
    CLOUDLET_RESUME_ACK,

    /**
     * Request a Cloudlet to be set as ready to start executing inside a VM.
     * This event is sent by a DatacenterBroker to itself to define the time when
     * a specific Cloudlet should start executing.
     * This tag is commonly used when Cloudlets are created from a trace file
     * such as a {@link GoogleTaskEventsTraceReader Google Cluster Trace}.
     *
     * <p>When the status of a Cloudlet is set to {@link Cloudlet.Status#READY},
     * the Cloudlet can be selected to start running as soon as possible
     * by a {@link CloudletScheduler}.</p>
     *
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Cloudlet} object.
     */
    CLOUDLET_READY,

    /**
     * Request a Cloudlet to be set as failed.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Cloudlet} object.
     */
    CLOUDLET_FAIL,

    /**
     * Requests an indefinite-length Cloudlet (negative value) to be finished by
     * setting its length as the current number of processed MI.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Cloudlet} object.
     *
     * <p>Events with a negative tag have higher priority.
     * In this case, if a message with this tag is sent,
     * it means that the Cloudlet has to be finished by replacing
     * its negative length with an actual positive value.
     * Only after that, the processing of Cloudlets can be updated.
     * That is way this event must be processed before other events.
     * </p>
     */
    CLOUDLET_FINISH(-2),

    /**
     * Requests a Cloudlet to be cancelled.
     * The Cloudlet can be cancelled under user request or because
     * another Cloudlet on which this one was dependent died.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Cloudlet} object.
     */
    CLOUDLET_KILL,

    /**
     * Request a Cloudlet to have its attributes changed.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Runnable} that represents a no-argument and no-return function
     * that will perform the Cloudlet attribute update.
     * The Runnable most encapsulate everything needed to update
     * the Cloudlet's attributes, including the Cloudlet
     * which will be updated.
     *
     * <p>Since the logic to update the attributes of a Cloudlet
     * can be totally customized according to the researcher needs,
     * there is no standard way to perform such an operation.
     * As an example, you may want to reduce by half
     * the number of PEs required by a Cloudlet from a list at a given time.
     * This way, the Runnable function may be defined as a Lambda Expression as follows.
     * Realize the {@code cloudletList} is considered to be accessible anywhere in the surrounding scope.
     * </p>
     *
     * <pre>
     * {@code Runnable runnable = () -> cloudletList.forEach(cloudlet -> cloudlet.setNumberOfPes(cloudlet.getNumberOfPes()/2));}
     * </pre>
     *
     * <p>The {@code runnable} variable must be set as the data for the event to be sent with this tag.</p>
     */
    CLOUDLET_UPDATE_ATTRIBUTES,

    /**
     * Denotes a request to retry creating waiting VMs from a {@link DatacenterBroker}.
     */
    VM_CREATE_RETRY,

    /**
     * Denotes a request to create a new VM in a {@link Datacenter}
     * where the {@link SimEvent#getData()} of the reply event is a {@link Vm} object.
     *
     * <p>Using this tag, the Datacenter acknowledges the reception of the request.
     * To check if the VM was in fact created inside the requested Datacenter
     * one has only to call {@link Vm#isCreated()}.
     * </p>
     */
    VM_CREATE_ACK,

    /**
     * Denotes a request to destroy a VM in a {@link Datacenter}.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Vm} object.
     */
    VM_DESTROY,

    /**
     * Denotes a request to destroy a new VM in a {@link Datacenter} with
     * acknowledgement information sent by the Datacenter.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link Vm} object.
     */
    VM_DESTROY_ACK,

    /**
     * Denotes a request to finish the migration of a new VM in a {@link Datacenter}.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@code Map.Entry<Vm, Host>} representing to which {@link Host}
     * a VM must be migrated.
     *
     * <p>
     * If {@link Host#NULL} is given, the Datacenter will try to find
     * a suitable Host when the migration request message is processed.
     * </p>
     */
    VM_MIGRATE,

    /**
     * Denotes a request to finish the migration of a new VM in a {@link Datacenter} with
     * acknowledgement information sent by the Datacenter.
     * @see #VM_MIGRATE
     */
    VM_MIGRATE_ACK,

    /**
     * Denotes an internal event generated in a {@link Datacenter}
     * to notify itself to update the processing of cloudlets.
     *
     * <p>When an event of this type is sent, the {@link SimEvent#getData()}
     * can be a {@link Host} object to indicate that just the Cloudlets
     * running in VMs inside such a Host must be updated.
     * The Host is an optional parameter which if omitted,
     * means that all Hosts from the Datacenter will have
     * its cloudlets updated.</p>
     */
    VM_UPDATE_CLOUDLET_PROCESSING,

    /**
     * Denotes a request vertical scaling of VM resources
     * such as Ram, Bandwidth or Pe.
     * When an event of this type is sent, the {@link SimEvent#getData()}
     * must be a {@link VerticalVmScaling} object.
     */
    VM_VERTICAL_SCALING,

    /**
     * Denotes the transmission of packets up through the network topology.
     */
    NETWORK_EVENT_UP,

    NETWORK_EVENT_SEND,

    /**
     * Denotes the transmission of packets down through the network topology.
     */
    NETWORK_EVENT_DOWN,

    /**
     * Denotes the transmission of packets targeting a given Host.
     * The {@link SimEvent#getData()} must be a {@link HostPacket}
     * to be processed.
     */
    NETWORK_EVENT_HOST,

    /**
     * Denotes failure events such as hosts or VMs failures.
    */
    FAILURE,

    /**
     * Denotes a request to generate a host failure.
     */
    HOST_FAILURE,

    /**
     * Denotes a request to a Datacenter to add a Host or list of Hosts to a Datacenter.
     * The {@link SimEvent#getData()} must be a Host to be added
     * to the Datacenter where the message is being sent to.
     * The source of such events is the {@link CloudInformationService}.
     */
    HOST_ADD,

    /**
     * Denotes a request to a Datacenter to remove a Host or list of Hosts from a Datacenter.
     * The {@link SimEvent#getData()} must be the ID of the Host that will be removed
     * from the Datacenter they belong to.
     *
     * <p>For this event, it's used the ID instead of the Host itself because the Host instance
     * with the specified ID should be looked into the Datacenter Host list in order to remove it.
     * A Host should be removed in case of maintenance or failure,
     * but there isn't such a distinction yet, so a failure is simulated to remove the Host.
     * The source of such events is the {@link CloudInformationService}.
     * </p>
     */
    HOST_REMOVE,

    /**
     * Denotes a power measurement performed periodically by a {@link PowerMeter} on
     * entities having a {@link PowerModel}, such as {@link Datacenter}s and {@link Host}s.
     */
    POWER_MEASUREMENT,

    /**
     * Denotes a tag for starting up a {@link Host} inside a {@link Datacenter}.
     * When such a {@link SimEvent} is sent, the {@link SimEvent#getData()}
     * must be a {@link Host} object.
     */
    HOST_POWER_ON,

    /**
     * Denotes a tag for shutting down a {@link Host} inside a {@link Datacenter}.
     * When such a {@link SimEvent} is sent, the {@link SimEvent#getData()}
     * must be a {@link Host} object.
     */
    HOST_POWER_OFF,
    BASE,
    /** Starting constant value for network-related tags. **/
    NETBASE,

    /** Denotes boolean <tt>true</tt> in <tt>int</tt> value. */
     TRUE,
    /** Denotes boolean <tt>false</tt> in <tt>int</tt> value. */
    FALSE,

    /** Denotes the default baud rate for CloudSim entities. */
 DEFAULT_BAUD_RATE,

    /** Schedules an entity without any delay. */
  SCHEDULE_NOW ,

    /** Denotes the end of simulation. */
   END_OF_SIMULATION ,

    /**
     * Denotes an abrupt end of simulation. That is, one event of this type is enough for
     * {@link } to trigger the end of the simulation
     */
   ABRUPT_END_OF_SIMULATION ,

    /**
     * Denotes insignificant simulation entity or time. This tag will not be used for identification
     * purposes.
     */
    INSIGNIFICANT ,

    /** Sends an Experiment object between UserEntity and Broker entity */
    EXPERIMENT,

    /**
     * Denotes a cloud resource to be registered. This tag is normally used between
     * {@link CloudInformationService} and CloudResouce entities.
     */
    REGISTER_RESOURCE ,

    /**
     * Denotes a cloud resource to be registered, that can support advance reservation. This tag is
     * normally used between {@link CloudInformationService} and CloudResouce entity.
     */
     REGISTER_RESOURCE_AR ,

    /**
     * Denotes a list of all hostList's, including the ones that can support advance reservation. This
     * tag is normally used between {@link CloudInformationService} and CloudSim entity.
     */
    RESOURCE_LIST ,

    /**
     * Denotes a list of hostList's that only support advance reservation. This tag is normally used
     * between {@link CloudInformationService} and CloudSim entity.
     */
     RESOURCE_AR_LIST ,

    /**
     * Denotes cloud resource characteristics information. This tag is normally used between CloudSim
     * and CloudResource entity.
     */
    RESOURCE_CHARACTERISTICS,

    /**
     * Denotes cloud resource allocation policy. This tag is normally used between CloudSim and
     * CloudResource entity.
     */
    RESOURCE_DYNAMICS,

    /**
     * Denotes a request to get the total number of Processing Elements (PEs) of a resource. This
     * tag is normally used between CloudSim and CloudResource entity.
     */
    RESOURCE_NUM_PE,

    /**
     * Denotes a request to get the total number of free Processing Elements (PEs) of a resource.
     * This tag is normally used between CloudSim and CloudResource entity.
     */
   RESOURCE_NUM_FREE_PE,

    /**
     * Denotes a request to record events for statistical purposes. This tag is normally used
     * between CloudSim and CloudStatistics entity.
     */
    RECORD_STATISTICS,

    /** Denotes a request to get a statistical list. */
    RETURN_STAT_LIST,

    /**
     * Denotes a request to send an Accumulator object based on category into an event scheduler.
     * This tag is normally used between ReportWriter and CloudStatistics entity.
     */
   RETURN_ACC_STATISTICS_BY_CATEGORY,

    /**
     * Denotes a request to register a CloudResource entity to a regional
     * {@link CloudInformationService} (CIS) entity.
     */
    REGISTER_REGIONAL_GIS,

    /**
     * Denotes a request to get a list of other regional CIS entities from the system CIS entity.
     */
    REQUEST_REGIONAL_GIS,

    /**
     * Denotes request for cloud resource characteristics information. This tag is normally used
     * between CloudSim and CloudResource entity.
     */
    RESOURCE_CHARACTERISTICS_REQUEST,

    /** This tag is used by an entity to send ping requests. */
    INFOPKT_SUBMIT,

    /** This tag is used to return the ping request back to sender. */
    INFOPKT_RETURN,

    /**
     * Denotes the return of a Cloudlet back to sender.
     * This tag is normally used by CloudResource entity.
     */
//    public static final int CLOUDLET_RETURN = BASE + 20;

    /**
     * Denotes the submission of a Cloudlet.
     * This tag is normally used between CloudSim User and CloudResource entity.
     */
    //   public static final int CLOUDLET_SUBMIT = BASE + 21;

    /**
     * Denotes the submission of a Cloudlet with an acknowledgement. This tag is normally used
     * between CloudSim User and CloudResource entity.
     */
    //   public static final int CLOUDLET_SUBMIT_ACK = BASE + 22;

    /** Cancels a Cloudlet submitted in the CloudResource entity. */
    //   public static final int CLOUDLET_CANCEL = BASE + 23;

    /** Denotes the status of a Cloudlet. */
    CLOUDLET_STATUS,

    /** Pauses a Cloudlet submitted in the CloudResource entity. */
    //  public static final int CLOUDLET_PAUSE = BASE + 25;

    /**
     * Pauses a Cloudlet submitted in the CloudResource entity with an acknowledgement.
     */
    //  public static final int CLOUDLET_PAUSE_ACK = BASE + 26;

    /** Resumes a Cloudlet submitted in the CloudResource entity. */
    //  public static final int CLOUDLET_RESUME = BASE + 27;

    /**
     * Resumes a Cloudlet submitted in the CloudResource entity with an acknowledgement.
     */
    //  public static final int CLOUDLET_RESUME_ACK = BASE + 28;

    /** Moves a Cloudlet to another CloudResource entity. */
    CLOUDLET_MOVE,

    /**
     * Moves a Cloudlet to another CloudResource entity with an acknowledgement.
     */
    CLOUDLET_MOVE_ACK,

    /**
     * Denotes a request to create a new VM in a {@link Datacenter} with acknowledgement
     * information sent by the Datacenter.
     */
    VM_CREATE,

    /**
     * Denotes a request to create a new VM in a {@link Datacenter}
     * with acknowledgement information sent by the Datacenter.
     */
    //  public static final int VM_CREATE_ACK = BASE + 32;

    /**
     * Denotes a request to destroy a new VM in a {@link Datacenter}.
     */
    //  public static final int VM_DESTROY = BASE + 33;

    /**
     * Denotes a request to destroy a new VM in a {@link Datacenter}
     * with acknowledgement information sent by the Datacener.
     */
    //   public static final int VM_DESTROY_ACK = BASE + 34;

    /**
     * Denotes a request to migrate a new VM in a {@link Datacenter}.
     */
    //   public static final int VM_MIGRATE = BASE + 35;

    /**
     * Denotes a request to migrate a new VM in a {@link Datacenter}
     * with acknowledgement information sent by the Datacener.
     */
    //   public static final int VM_MIGRATE_ACK = BASE + 36;


    /**
     * Denotes an event to send a file from a user to a {@link Datacenter}.
     */
    VM_DATA_ADD,

    /**
     * Denotes an event to send a file from a user to a {@link Datacenter}
     * with acknowledgement information sent by the Datacener.
     */
    VM_DATA_ADD_ACK,

    /**
     * Denotes an event to remove a file from a {@link Datacenter} .
     */
    VM_DATA_DEL,

    /**
     * Denotes an event to remove a file from a {@link Datacenter}
     * with acknowledgement information sent by the Datacener.
     */
    VM_DATA_DEL_ACK,

    /**
     * Denotes an internal event generated in a {@link Datacenter}.
     */
    VM_DATACENTER_EVENT,

    /**
     * Denotes an internal event generated in a Broker.
     */
    VM_BROKER_EVENT,

    Network_Event_UP ,

    Network_Event_send,

    RESOURCE_Register ,

    Network_Event_DOWN ,

    Network_Event_Host ,

    NextCycle ,

    GPU_TASK_SUBMIT ,

    /**
     * Denotes an internal event in the GpuDatacenter. Updates the progress of
     * executions.
     */
    VGPU_DATACENTER_EVENT ,

    /**
     * Denotes an event to evaluate the power consumption of a
     * {@link gpu.power.PowerGpuDatacenter
     * PowerGpuDatacenter}.
     */
    GPU_VM_DATACENTER_POWER_EVENT ,

    /**
     * Denotes an event to perform a {@link GpuVm} placement in a
     * {@link RemoteGpuDatacenterEx}.
     */
     GPU_VM_DATACENTER_PLACEMENT ,

    /**
     * Denotes an event to update GPU memory transfers.
     */
     GPU_MEMORY_TRANSFER,

    /**
     * Denotes the return of a GpuCloudlet to the sender.
     */
    GPU_CLOUDLET_RETURN,

    /**
     * Base value used for Replica Manager tags.
     */
    RM_BASE,

    /**
     * Base value for catalogue tags.
     */
    CTLG_BASE ,

    /**
     * Default Maximum Transmission Unit (MTU) of a link in bytes.
     */
    DEFAULT_MTU ,

    /**
     * The default packet size (in byte) for sending events to other entity.
     */
    PKT_SIZE ,

    /**
     * Denotes that file addition is successful.
     */
    FILE_ADD_SUCCESSFUL ,

    /**
     * Denotes that file addition is failed because the storage is full.
     */
    FILE_ADD_ERROR_STORAGE_FULL ,

    /**
     * Denotes that file addition is failed because the file already exists in
     * the catalogue and it is read-only file.
     */
    FILE_ADD_ERROR_EXIST_READ_ONLY,
    /**
     * Denotes that file deletion is successful.
     */
    FILE_DELETE_SUCCESSFUL ,

    /**
     * Denotes that file deletion is failed due to an unknown error.
     */
    FILE_DELETE_ERROR ,

    /**
     * Denotes the request to de-register / delete a master file from the
     * Replica Catalogue.
     * <p>
     * The format of this request is Object[2] = {String lfn, Integer
     * resourceID}.
     * </p>
     *
     * The reply tag name is {@link #CTLG_DELETE_MASTER_RESULT}.
     */
    CTLG_DELETE_MASTER ,

    /**
     * Sends the result of de-registering a master file back to sender.
     * <p>
     * The format of the reply is Object[2] = {String lfn, Integer resultID}.
     * </p>NOTE: The result id is in the form of CTLG_DELETE_MASTER_XXXX where
     * XXXX means the error/success message
     */
    CTLG_DELETE_MASTER_RESULT ,

    FILE_DELETE_MASTER_RESULT ,

    FILE_ADD_ERROR_EMPTY, FILE_ADD_MASTER_RESULT;

    private final int priority;

    CloudSimTag() {
        this.priority = 0;
    }

    /**
     * Creates an event tag with a given priority.
     * Negative values give higher priority.
     * @param priority the priority to set
     */
    CloudSimTag(final int priority) {
        this.priority = priority;
    }

    /**
     * Gets the event tag priority.
     * Negative values indicates higher priority.
     */
    public int priority(){
        return priority;
    }

    /**
     * Checks if this tag is between a given range of tags,
     * according to their {@link #ordinal()} values.
     * @param startInclusive the tag starting the range to check
     * @param endInclusive the tag finishing the range to check
     * @return
     */
    public boolean between(final CloudSimTag startInclusive, final CloudSimTag endInclusive){
        return this.ordinal() >= startInclusive.ordinal() && this.ordinal() <= endInclusive.ordinal();
    }





}
