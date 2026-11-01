package org.zoxweb.shared.util;

public class GetNameDescriptionDisplayImpl
        implements GetNameDescriptionDisplay {
    private final String name;
    private final String description;
    private final String displayName;
    private final GetNameDescription nameDescription;

    public GetNameDescriptionDisplayImpl(String name, String description, String displayName) {
        SUS.checkIfNull( "Name must not be null.", name);
        this.name = name;
        this.description = description;
        this.displayName = displayName;
        this.nameDescription = null;
    }

    public GetNameDescriptionDisplayImpl(GetNameDescription nameDescription, String displayName) {
        SUS.checkIfNull( "NameDescription must not be null.", nameDescription);
        this.nameDescription = nameDescription;
        this.displayName = displayName;
        this.name = null;
        this.description = null;
    }

    @Override
    public String getName() {
        return nameDescription != null ? nameDescription.getName() : name;
    }

    @Override
    public String getDescription() {
        return nameDescription != null ? nameDescription.getDescription() : description;
    }

    @Override
    public String getDisplayName() {
        return displayName != null ? displayName : getName();
    }

}
