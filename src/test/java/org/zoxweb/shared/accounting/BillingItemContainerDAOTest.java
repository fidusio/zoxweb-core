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
package org.zoxweb.shared.accounting;

//import org.junit.Test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;

public class BillingItemContainerDAOTest
{

    @Test
    public void testBillingItemsContainerDAO()
    {
        BillingItemsContainerDAO container = new BillingItemsContainerDAO();

        for (int i = 0; i < 10; i++)
        {
            BillingItemDAO item = new BillingItemDAO();
            item.setUnitCost(new BigDecimal(10));
            item.setQuantity(new BigDecimal(1));

            container.addBillingItem(item);
        }

        // Items: 10  Total: 100
        Assertions.assertNotNull(container.getBillingItems());
        Assertions.assertEquals(10, container.getBillingItems().size());
        Assertions.assertEquals(new BigDecimal(100), container.getTotal());

        // Add item
        // Items: 11  Total: 120
        BillingItemDAO item1 = new BillingItemDAO();
        item1.setUnitCost(new BigDecimal(10));
        item1.setQuantity(new BigDecimal(2));
        container.addBillingItem(item1);
        Assertions.assertEquals(11, container.getBillingItems().size());
        Assertions.assertEquals(new BigDecimal(120), container.getTotal());

        // Add item
        // Items: 10  Total: 100
        container.removeBillingItem(item1);
        Assertions.assertEquals(10, container.getBillingItems().size());
        Assertions.assertEquals(new BigDecimal(100), container.getTotal());

        // Add item
        // Items: 11  Total: 260
        BillingItemDAO item2 = new BillingItemDAO();
        item2.setUnitCost(new BigDecimal(80));
        item2.setQuantity(new BigDecimal(2));
        container.addBillingItem(item2);
        Assertions.assertEquals(11, container.getBillingItems().size());
        Assertions.assertEquals(new BigDecimal(260), container.getTotal());

        // Add item
        // Items: 12  Total: 260
        BillingItemDAO item3 = new BillingItemDAO();
        item3.setUnitCost(new BigDecimal(20));
        item3.setQuantity(new BigDecimal(2));
        container.addBillingItem(item3);
        Assertions.assertEquals(12, container.getBillingItems().size());
        Assertions.assertEquals(new BigDecimal(300), container.getTotal());

        // Add item
        // Items: 13  Total: 500
        BillingItemDAO item4 = new BillingItemDAO();
        item4.setReferenceID("4");
        item4.setUnitCost(new BigDecimal(50));
        item4.setQuantity(new BigDecimal(4));
        container.addBillingItem(item4);
        Assertions.assertEquals(13, container.getBillingItems().size());
        Assertions.assertEquals(new BigDecimal(500), container.getTotal());

        // Add existing item
        // Items: 13  Total: 500
        container.addBillingItem(item4);
        Assertions.assertEquals(13, container.getBillingItems().size());
        Assertions.assertEquals(new BigDecimal(500), container.getTotal());

        // Add existing item
        // Items: 14  Total: 900
        BillingItemDAO item5 = new BillingItemDAO();
        item5.setUnitCost(new BigDecimal(100));
        item5.setQuantity(new BigDecimal(4));
        container.addBillingItem(item5);
        Assertions.assertEquals(14, container.getBillingItems().size());
        Assertions.assertEquals(new BigDecimal(900), container.getTotal());
    }

}