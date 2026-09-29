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
package org.zoxweb.shared.data.ticket;

import org.zoxweb.shared.accounting.BillingItemsContainerDAO;
import org.zoxweb.shared.data.NVEntityContainerDAO;
import org.zoxweb.shared.data.ResolutionStatus;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 * The TicketContainerDAO class represents a support ticket that groups the ticket status, the ticket issuer information,
 * an optional billing items container, and the ticket resolutions added to its content.
 */
@SuppressWarnings("serial")
public class TicketContainer
        extends NVEntityContainerDAO {

    public enum Param
            implements GetNVConfig {
        STATUS(NVConfigManager.createNVConfig("status", "The status of the ticket.", "Status", false, true, ResolutionStatus.class)),
        ISSUER(NVConfigManager.createNVConfigEntity("issuer", "The ticket issuer.", "Issuer", true, true, TicketIssuer.NVC_TICKET_ISSUER, ArrayType.NOT_ARRAY)),
        BILLING_ITEMS_CONTAINER(NVConfigManager.createNVConfigEntity("billing_items_container", "The billing items container.", "BillingItemsContainer", false, true, BillingItemsContainerDAO.class, ArrayType.NOT_ARRAY)),

        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_TICKET_CONTAINER = new NVConfigEntityPortable
            (
                    "ticket_container",
                    null,
                    "Ticket",
                    true,
                    false,
                    false,
                    false,
                    TicketContainer.class,
                    SUS.extractNVConfigs(Param.values()),
                    null,
                    false,
                    NVEntityContainerDAO.NVC_NVENTITY_CONTAINER_DAO
            );

    /**
     * The default constructor.
     */
    public TicketContainer() {
        super(NVC_TICKET_CONTAINER);
    }

    /**
     * This constructor instantiates TicketContainerDAO object based on given NVConfigEntity.
     *
     * @param nvce
     */
    protected TicketContainer(NVConfigEntity nvce) {
        super(nvce);
    }

    @Override
    public String getName() {
        return getIssuerInfo().getName();
    }

    @Override
    public void setName(String name) {
        getIssuerInfo().setName(name);
    }

    public ResolutionStatus getStatus() {
        return lookupValue(Param.STATUS);
    }

    public void setStatus(ResolutionStatus status) {
        setValue(Param.STATUS, status);
    }

    public TicketIssuer getIssuerInfo() {
        return lookupValue(Param.ISSUER);
    }

    public void setIssuerInfo(TicketIssuer issuer) {
        if (issuer != null) {
            issuer.setCanonicalID(getCanonicalID());
        }

        setValue(Param.ISSUER, issuer);
    }


    public BillingItemsContainerDAO getBillingItemsContainerDAO() {
        return lookupValue(Param.BILLING_ITEMS_CONTAINER);
    }

    public void setBillingItemsContainerDAO(BillingItemsContainerDAO container) {
        if (container != null) {
            container.setCanonicalID(getCanonicalID());
        }

        setValue(Param.BILLING_ITEMS_CONTAINER, container);
    }

    /**
     * Adds TicketResolutionDAO to container.
     * A TicketContainerDAO object can contain multiple TicketResolution objects.
     *
     * @param ticketResolutionToAdd
     * @return TicketResolutionDAO
     */
    public synchronized TicketResolution addTicketResolutionDAO(TicketResolution ticketResolutionToAdd) {
        if (ticketResolutionToAdd != null && contains(ticketResolutionToAdd.getReferenceID()) == null) {
            ticketResolutionToAdd.setCanonicalID(getCanonicalID());

            return (TicketResolution) getContent().add(ticketResolutionToAdd);
        }

        return null;
    }

    /**
     * Removes TicketResolutionDAO from container.
     *
     * @param ticketResolutionToRemove
     * @return TicketResolutionDAO
     */
    public synchronized TicketResolution removeTicketResolutionDAO(TicketResolution ticketResolutionToRemove) {
        if (ticketResolutionToRemove != null) {
            return (TicketResolution) getContent().remove(ticketResolutionToRemove);
        }

        return null;
    }

}