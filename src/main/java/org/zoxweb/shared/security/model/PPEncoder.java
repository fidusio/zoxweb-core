package org.zoxweb.shared.security.model;


import org.zoxweb.shared.security.model.SecurityModel.PermissionToken;
import org.zoxweb.shared.util.*;

/**
 * Permission pattern encoder
 * @author javaconsigliere
 *
 */
public class PPEncoder
implements DataEncoder<String[], String>
{
	public static final PPEncoder SINGLETON = new PPEncoder();
	public static final char PATTERN_SEP = ':';

	
	private PPEncoder() {}
	public String encode(String ...patterns) 
	{
		return SUS.trimOrEmpty(SharedUtil.toCanonicalID(PATTERN_SEP, (Object[])patterns)).toLowerCase();
	}
	
	
	public String encodePattern(String pattern, GetNameValue<String> gnvs)
	{
		return SharedStringUtil.embedText(pattern, gnvs.getName(), gnvs.getValue());
	}
	
	public String encodePattern(String pattern, String token, String value)
	{
		return SharedStringUtil.embedText(pattern, token, value);
	}
	
	public String encodePattern(String pattern, PermissionToken token, String value)
	{
		return SharedStringUtil.embedText(pattern, token.getValue(), value);
	}

}
