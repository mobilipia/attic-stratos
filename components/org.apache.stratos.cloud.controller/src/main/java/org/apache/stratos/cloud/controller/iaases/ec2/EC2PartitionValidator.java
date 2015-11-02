/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.iaases.ec2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.Partition;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.Scope;

import java.util.Properties;

/**
 * AWS-EC2 {@link org.apache.stratos.cloud.controller.iaases.PartitionValidator} implementation.
 */
public class EC2PartitionValidator implements PartitionValidator {

    private static final Log log = LogFactory.getLog(EC2PartitionValidator.class);
    private IaasProvider iaasProvider;
    private Iaas iaas;

    @Override
    public IaasProvider validate(Partition partition, Properties properties) throws InvalidPartitionException {
        // validate the existence of the region and zone properties.
        try {
            if (properties.containsKey(Scope.REGION.toString())) {
                String region = properties.getProperty(Scope.REGION.toString());
                if (iaasProvider.getImage() != null && !iaasProvider.getImage().contains(region)) {
                    String message = String
                            .format("Invalid partition detected [partition-id] %s, [region] %s. Image ID does not "
                                            + "contain region [image] %s",
                                    partition.getId(), region, iaasProvider.getImage());
                    log.error(message);
                    throw new InvalidPartitionException(message);
                }
                iaas.isValidRegion(region);
                IaasProvider updatedIaasProvider = new IaasProvider(iaasProvider);
                if (properties.containsKey(Scope.ZONE.toString())) {
                    String zone = properties.getProperty(Scope.ZONE.toString());
                    iaas.isValidZone(region, zone);
                    updatedIaasProvider.setProperty(CloudControllerConstants.AVAILABILITY_ZONE, zone);
                }
                updateProperties(updatedIaasProvider, properties);
                Iaas updatedIaas = updatedIaasProvider.buildIaas();
                updatedIaas.setIaasProvider(updatedIaasProvider);
                return updatedIaasProvider;
            } else {
                return iaasProvider;
            }
        } catch (Exception ex) {
            String message = String.format("Invalid partition detected: [partition-id] %s", partition.getId());
            log.error(message, ex);
            throw new InvalidPartitionException(message, ex);
        }
    }

    private void updateProperties(IaasProvider updatedIaasProvider, Properties properties) {
        for (Object property : properties.keySet()) {
            if (property instanceof String) {
                String key = (String) property;
                updatedIaasProvider.setProperty(key, properties.getProperty(key));
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Added [property] %s to the IaasProvider.", key));
                }
            }
        }
    }

    @Override
    public void setIaasProvider(IaasProvider iaas) {
        this.iaasProvider = iaas;
        this.iaas = iaas.getIaas();
    }
}
