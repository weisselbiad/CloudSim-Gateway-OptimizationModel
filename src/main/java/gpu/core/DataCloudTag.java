package gpu.core;


import org.cloudbus.cloudsim.core.CloudSimTag;

/**
 * Contains additional tags for DataCloud features, such as file
 * information retrieval, file transfers, and storage info.
 *
 * @author Uros Cibej
 * @author Anthony Sulistio
 * @since CloudSim Toolkit 1.0
 */
public enum  DataCloudTag  {
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

     FILE_ADD_ERROR_EMPTY;

    /**
     * A private constructor to avoid class instantiation.
     */
    private DataCloudTag(){}
}
