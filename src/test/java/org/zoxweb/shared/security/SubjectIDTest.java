package org.zoxweb.shared.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.BaseSubjectID;

import java.util.UUID;

public class SubjectIDTest
{

    @BeforeAll
    public static void init()
    {
        new SubjectIdentifier();
    }
    @Test
    public void subjectIdentifierTest()
    {
        SubjectIdentifier subjectIdentifier = new SubjectIdentifier();
        subjectIdentifier.setSubjectID(UUID.randomUUID().toString());
        subjectIdentifier.setSubjectType(BaseSubjectID.SubjectType.ENTITY);
        subjectIdentifier.setSubjectGUID(UUID.randomUUID().toString());
        System.out.println(GSONUtil.toJSONDefault(subjectIdentifier));
    }
}
