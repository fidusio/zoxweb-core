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
        new SubjectInfo().setEmail("email@email.com");
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
    @Test
    public void subjectInfoTest()
    {
        String email = "MaRio@zoxweb.com";
        SubjectIdentifier subjectIdentifier = new SubjectIdentifier();
        subjectIdentifier.setSubjectID(email);
        subjectIdentifier.setSubjectType(BaseSubjectID.SubjectType.USER);
        subjectIdentifier.setSubjectGUID(UUID.randomUUID().toString());
        SubjectInfo subjectInfo = new SubjectInfo();
        subjectInfo.setGUID(subjectIdentifier.getSubjectGUID());
        subjectInfo.setEmail(email);

        assert(subjectInfo.getEmail().equals("mario@zoxweb.com"));

        System.out.println(GSONUtil.toJSONDefault(subjectIdentifier));
        System.out.println(GSONUtil.toJSONDefault(subjectInfo));
    }
}
