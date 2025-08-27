package org.zoxweb.shared.data;


import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.List;

@SuppressWarnings("serial")
public class FolderContentOp
        extends SetNameDescriptionDAO {
    public enum Param
            implements GetNVConfig {
        FROM_FOLDER_REF(NVConfigManager.createNVConfig("from_folder_ref", "From folder reference id", "FromFolderRef", true, false, String.class)),
        TO_FOLDER_REF(NVConfigManager.createNVConfig("to_folder_ref", "To folder reference id", "ToFolderRef", true, true, String.class)),
        NVES_REF(NVConfigManager.createNVConfig("nves_ref", "List of nves reference id", "NVESRef", true, true, String[].class)),
        CONTENT(NVConfigManager.createNVConfigEntity("content", "The folder content", "Content", false, true, NVEntity[].class, ArrayType.LIST)),
        CRUD_OP(NVConfigManager.createNVConfig("action", "CRUD operation", "Action", true, true, CRUD.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_FOLDER_CONTENT_OP = new NVConfigEntityPortable(
            "folder_content_op",
            "FolderContentOp",
            FolderContentOp.class.getSimpleName(),
            true, false,
            false, false,
            FolderContentOp.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );


    public FolderContentOp() {
        super(NVC_FOLDER_CONTENT_OP);
    }


    public FolderContentOp(FolderInfoDAO fromFolder, FolderInfoDAO toFolder, List<NVEntity> list) {
        this();
        setFromFolder(fromFolder);
        setToFolder(toFolder);

        for (NVEntity nve : list) {
            addNVERef(nve);
        }
    }

    public void setFromFolder(FolderInfoDAO folder) {
        setFromFolderRef(folder.getReferenceID());
    }

    public void setFromFolderRef(String fromFolderRef) {
        setValue(Param.FROM_FOLDER_REF, fromFolderRef);
    }

    public String getFromFolderRef() {
        return lookupValue(Param.FROM_FOLDER_REF);
    }

    public void setToFolder(FolderInfoDAO folder) {
        setToFolderRef(folder.getReferenceID());
    }

    public void setToFolderRef(String ToFolderRef) {
        setValue(Param.TO_FOLDER_REF, ToFolderRef);
    }

    public String getToFolderRef() {
        return lookupValue(Param.TO_FOLDER_REF);
    }


    @SuppressWarnings("unchecked")
    public ArrayValues<NVEntity> getContent() {
        return (ArrayValues<NVEntity>) lookup(Param.CONTENT);
    }

    @SuppressWarnings("unchecked")
    public ArrayValues<NVPair> getNVERefs() {
        return (ArrayValues<NVPair>) lookup(Param.NVES_REF);
    }

    public void addNVERef(NVEntity nve) {
        getNVERefs().add(new NVPair(nve.getName() != null ? nve.getName() : nve.getReferenceID(), nve.getReferenceID()));
    }

    public CRUD getOp() {
        return lookupValue(Param.CRUD_OP);
    }

    public void setOp(CRUD crud) {
        setValue(Param.CRUD_OP, crud);
    }

}
