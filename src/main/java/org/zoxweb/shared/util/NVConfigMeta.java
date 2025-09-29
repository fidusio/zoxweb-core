package org.zoxweb.shared.util;

import org.zoxweb.shared.data.DataConst.DataParam;

import java.util.List;

public class NVConfigMeta {
    public enum MetaAction {
        SUBMIT,
    }

    public enum Param
            implements GetNVConfig, GetName {
        NAME(DataParam.NAME.getNVConfig()),
        DESCRIPTION(DataParam.DESCRIPTION.getNVConfig()),
        DEFAULT_VALUE(NVConfigManager.createNVConfig("default_value", "The default value of the parameter its type is defined the meta_type", "DefaulValue", false, true, String.class)),
        DISPLAY_NAME(NVConfigManager.createNVConfig("display_name", "display name", "DisplayName", false, true, String.class)),
        META_TYPE(NVConfigManager.createNVConfig(MetaToken.META_TYPE.getName(), "The class name of the object", "MetaType", false, true, String.class)),
        FILTERS(NVConfigManager.createNVConfig("filters", "List of filters", "Filter", false, true, NVStringList.class)),
        IS_HIDDEN(NVConfigManager.createNVConfig("is_hidden", "If the data is hidden", "IsHidden", false, true, Boolean.class)),
        IS_MANDATORY(NVConfigManager.createNVConfig("is_mandatory", "display name", "IsMandatory", false, true, Boolean.class)),
        IS_VISIBLE(NVConfigManager.createNVConfig("is_visible", "if true the item is visible", "IsVisible", false, true, Boolean.class)),
        ACTIONS(NVConfigManager.createNVConfig("actions", "Actions that could be appriled on the meta object", "Actions", false, true, NVStringList.class)),


        ;
        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }

        public String getName() {
            return nvc.getName();
        }
    }

    private NVGenericMap metaData;

    public NVConfigMeta() {
        metaData = new NVGenericMap();
        metaData.add(new NVPair(Param.NAME, (String) null));
        metaData.add(new NVPair(Param.DESCRIPTION, (String) null));
        metaData.add(new NVPair(Param.DISPLAY_NAME, (String) null));
        metaData.add(new NVPair(Param.META_TYPE, (String) null));
        metaData.add(new NVBoolean(Param.IS_HIDDEN.getName(), false));
        metaData.add(new NVBoolean(Param.IS_MANDATORY.getName(), false));
        metaData.add(new NVBoolean(Param.IS_VISIBLE.getName(), false));
        metaData.add(new NVStringList(Param.FILTERS.getName()));
        metaData.add(new NVStringList(Param.ACTIONS.getName()));
    }


    public NVConfigMeta(NVGenericMap nvgm) {
        this();
        for (GetNameValue<?> gnv : nvgm.values()) {
            metaData.add(gnv);
        }
    }

    public NVConfigMeta(String name, String displayName, String classType, boolean isHidden, boolean isMandatory, boolean isVisible, String... filters) {
        this();
        metaData.add(new NVPair(Param.NAME, name));
        metaData.add(new NVPair(Param.DISPLAY_NAME, displayName));
        metaData.add(new NVPair(Param.META_TYPE, classType));
        //metaData.add(new NVStringList(Param.FILTERS.getName()));
        metaData.add(new NVBoolean(Param.IS_HIDDEN.getName(), isHidden));
        metaData.add(new NVBoolean(Param.IS_MANDATORY.getName(), isMandatory));
        metaData.add(new NVBoolean(Param.IS_VISIBLE.getName(), isVisible));
        if (filters != null) {
            for (String filter : filters) {
                ((NVStringList) metaData.get((GetName) Param.FILTERS)).getValue().add(filter);
            }
        }

    }

    public String toString() {
        return "" + metaData.getValue();
    }

    public String getName() {
        return metaData.getValue((GetName) Param.NAME);
    }

    public String getDisplayName() {
        String ret = metaData.getValue((GetName) Param.DISPLAY_NAME);
        if (ret == null)
            ret = getName();

        return ret;
    }

    public String getDescription() {
        return metaData.getValue((GetName) Param.DESCRIPTION);
    }

    public String getMetaType() {
        return metaData.getValue((GetName) Param.META_TYPE);
    }

    public String[] getFilters() {
        NVStringList ret = (NVStringList) metaData.get((GetName) Param.FILTERS);
        return ret.getValue().toArray(new String[0]);
    }

    public boolean isHidden() {
        return metaData.getValue((GetName) Param.IS_HIDDEN);
    }

    public boolean isMandatory() {
        return metaData.getValue((GetName) Param.IS_MANDATORY);
    }

    public boolean isVisible() {
        return metaData.getValue((GetName) Param.IS_VISIBLE);
    }

    public MetaAction[] getActions() {
        List<String> actions = metaData.getValue(((GetName) Param.ACTIONS));
        if (actions.size() == 0) {
            return null;
        }

        MetaAction ret[] = new MetaAction[actions.size()];
        int index = 0;
        for (String str : actions) {
            ret[index++] = SharedUtil.lookupEnum(str, MetaAction.values());
        }

        return ret;
    }


    public NVConfigMeta addAction(MetaAction ma) {
        List<String> actions = metaData.getValue((GetName) Param.ACTIONS);
        actions.add(ma.name());

        return this;
    }

    public void setActions(MetaAction... setActions) {
        List<String> actions = metaData.getValue((GetName) Param.ACTIONS);
        actions.clear();
        for (MetaAction ma : setActions) {
            actions.add("" + ma);
        }
    }

    public <V> V getDefaultValue() {
        return metaData.getValue((GetName) Param.DEFAULT_VALUE);
    }

    public NVGenericMap getMetaConfigInfo() {
        return metaData;
    }
}
