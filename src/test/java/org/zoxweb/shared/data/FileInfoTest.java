/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.data.ticket.TicketContainer;
import org.zoxweb.shared.data.ticket.TicketIssuer;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SUS;

public class FileInfoTest {
	
	public enum CFFIParams
			implements GetNVConfig {
		
		NAME(NVConfigManager.createNVConfig("name", null,"Name", true, true, String.class)),
		CHILD_NAME( NVConfigManager.createNVConfig("CHILD_NAME", null,"ChildName",false, true, String.class))

		;
	
		private final NVConfig nvc;

		CFFIParams(NVConfig nvc) {
            this.nvc = nvc;
		}
		
		public NVConfig getNVConfig() {
			return nvc;
		}
	}
	
	@SuppressWarnings("serial")
	public static class ChildFileInfo
		extends FileInfo {

		private List<NVPair> tempo = new ArrayList<NVPair>();

		public static final NVConfigEntity  NVC_CHILD_FILE_INFO = new NVConfigEntityPortable("child_file_info", null , "ChildFileInfo", true, false, false, false,ChildFileInfo.class, SUS.extractNVConfigs(CFFIParams.values()), null, false, FileInfo.NVC_FILE_INFO);//,SUS.extractNVConfigs( new Params[]{Params.REFERENCE_ID, Params.NAME, Params.LENGTH}));

		public ChildFileInfo() {
			super(NVC_CHILD_FILE_INFO);
		}

		/**
		 * Get the the filename
		 * @return child name 
		 */
		public String getChildName() {
			return lookupValue( CFFIParams.CHILD_NAME);
		}

		public void setChildName(String name)
			throws NullPointerException, IllegalArgumentException {
			setValue(  CFFIParams.CHILD_NAME, name);
		}

		public List<NVPair> getTempo() {
			return tempo;
		}

		public void setTempo(List<NVPair> tempo) {
			this.tempo = tempo;
		}
	}

	public static final NVConfigEntity  NVC = new NVConfigEntityPortable(null, null , "ChildFileInfo", true, false, false, false, FileInfo.class, SUS.extractNVConfigs(CFFIParams.values()), null, false, null);//,SUS.extractNVConfigs( new Params[]{Params.REFERENCE_ID, Params.NAME, Params.LENGTH}));
	
	public static void main(String[] args) {
		try {
            FileInfo fi = new ChildFileInfo();

            List<NVPair> props = new ArrayList<NVPair>();

            props.add( new NVPair("Marwan", "nael"));
            props.add( new NVPair("Imad", "NAEL"));
            long i = 100;
            long refID =0;
            fi.setReferenceID( ""+(++refID));
            fi.setCreationTime(i++);
            fi.setLastTimeRead(i++);
            fi.setLastTimeUpdated(i++);
            fi.setName("Child1");
            fi.setContentType("mime1");
            //fi.setResourceLocation("store1");
            ((ChildFileInfo)fi).setChildName("baby1");
            fi.setLength( i++);
            fi.setProperties( props);
            System.out.println( fi);


            fi = new ChildFileInfo();
            fi.setReferenceID( ""+(++refID));
            fi.setCreationTime(i++);
            fi.setLastTimeRead(i++);
            fi.setLastTimeUpdated(i++);
            fi.setName("Child2");
            fi.setContentType("mime2");
            //fi.setResourceLocation("store2");
            ((ChildFileInfo)fi).setChildName("baby2");
            fi.setLength( i++);
            fi.setProperties( props);
            System.out.println( fi);
            System.out.println(  ((NVConfigEntity)fi.getNVConfig()).getAttributes());



            fi = new FileInfo();
            fi.setReferenceID(  ""+(++refID));
            fi.setCreationTime(i++);
            fi.setLastTimeRead(i++);
            fi.setLastTimeUpdated(i++);
            fi.setName("parent1");
            fi.setContentType("exe");
            //fi.setResourceLocation("store3");

            fi.setLength( i++);
            fi.setProperties( props);
            System.out.println( fi);

            fi = new FileInfo();
            fi.setReferenceID(  ""+(++refID));
            fi.setCreationTime(i++);
            fi.setLastTimeRead(i++);
            fi.setLastTimeUpdated(i++);
            fi.setName("/dir/parent2");
            fi.setContentType("exe");
            //fi.setResourceLocation("store4");

            fi.setLength( i++);
            fi.setProperties( props);

            FileInfo remote = new FileInfo();
            remote.setName("RemoteFile");
            System.out.println(GSONUtil.toJSON( fi, true));

            fi.setRemoteFileInfo(remote);
            System.out.println( fi);
            System.out.println(  ((NVConfigEntity)fi.getNVConfig()).getAttributes());

            System.out.println( fi.getProperties());
            System.out.println("remote:" + fi.getRemoteFileInfo());
            String json = null;
            long ts;

            try {
                json = GSONUtil.toJSON( fi.getOriginalFileInfo(), true);
                System.out.println( json);

                System.out.println( fi);
                FileInfo fiTemp = GSONUtil.fromJSON( json, FileInfo.class);
                String temp = GSONUtil.toJSON( fiTemp, true);
                System.out.println((temp.equals(json))+"[IMADS]:" +fiTemp);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                File f = File.createTempFile("temp", "temp");
                f.deleteOnExit();
                System.out.println( f.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            };

            long total = 0;
            long totalTempo=0;
            int repeat = 10;
            fi = new ChildFileInfo();

            fi.setReferenceID(  ""+(++refID));
            fi.setCreationTime(System.nanoTime());
            fi.setLastTimeRead(System.nanoTime());
            fi.setLastTimeUpdated(System.nanoTime());
            fi.setName("Child5");
            fi.setContentType("mime1");
            //fi.setResourceLocation("store1");
            ((ChildFileInfo)fi).setChildName("baby1");
            fi.setLength( i++);
            fi.setProperties( props);

            for (i = 0; i < repeat; i++) {
                ts = System.nanoTime();
                fi.getProperties();
                ts=  System.nanoTime() - ts;
                total+= ts;
                System.out.println("it took " + ts);
                ts = System.nanoTime();
                ((ChildFileInfo)fi).getTempo();
                ts=  System.nanoTime() - ts;
                totalTempo+=ts;

                System.out.println("it took " + ts);
            }

            System.out.println( "Average:" + (total/repeat) +  " " +(totalTempo/repeat));


            try {
                System.out.println( "json:" + GSONUtil.toJSON(fi, true, false, true));
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println(((NVConfigEntity) fi.getNVConfig()).getAttributes());

            System.out.println("Subsearch:" + TicketIssuer.NVC_TICKET_ISSUER.lookup( "email"));
            TicketContainer ticket = new TicketContainer();

            NVConfigEntity nvceTicket = (NVConfigEntity) ticket.getNVConfig();
            System.out.println(nvceTicket);
            System.out.println("Subsearch:" + TicketContainer.NVC_TICKET_CONTAINER.lookup( "issuer.email"));


            //NVEntity nve = ZWDataFactory.SINGLETON.createNVEntity(nvceTicket.lookup("issuer").getName());
            NVConfigEntity nvceIssuer = (NVConfigEntity) nvceTicket.lookup("issuer");
            System.out.println("Issuer:" + ticket.getIssuerInfo());
            ticket.setValue("issuer.assignee", "mnael@zoxweb.com");


            ticket.setIssuerInfo((TicketIssuer) ZWDataFactory.SINGLETON.createNVEntity("ticket_issuer_dao"));
            ticket.setValue("issuer.assignee", "mnael@zoxweb.com");
            System.out.println("Issuer:" + ticket.getIssuerInfo() + ":" + nvceIssuer.getName());

        } catch(Throwable t) {
			t.printStackTrace();
		}
	}
}
