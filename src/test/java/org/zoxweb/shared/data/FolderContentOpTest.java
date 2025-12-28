package org.zoxweb.shared.data;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.NVPair;

public class FolderContentOpTest 
{
	public static void main(String ...args)
	{
		
		try
		{
			
			
			AddressDAO address = new AddressDAO();
			address.setName("tizi");
			address.setStreet("123 Main St.");
			address.setCity("Los Angeles");
			address.setStateOrProvince("CA");
			address.setCountry("USA");
			address.setZIPOrPostalCode("90025");
			
			FolderContentOp fco = new FolderContentOp();
			
			
			fco.setFromFolderRef("fromFolder");
			fco.setToFolderRef("toFolder");
			fco.getNVERefs().add(new NVPair("file", "f1"));
			fco.getNVERefs().add(new NVPair("document", "d1"));
			fco.getContent().add(address);
			String json = GSONUtil.toJSON(fco, false, false, true);
			
			System.out.println(json);
			
			fco = GSONUtil.fromJSON(json);
			json = GSONUtil.toJSON(fco, false, false, true);
			
			System.out.println(json);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		
	}
}
