/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.gateway.engine.policies;

import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.beans.PolicyFailureType;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.apiman.gateway.engine.components.IPolicyFailureFactoryComponent;
import io.apiman.gateway.engine.components.IRateLimiterComponent;
import io.apiman.gateway.engine.policies.config.RateLimitingConfig;
import io.apiman.gateway.engine.policies.config.rates.RateLimitingGranularity;
import io.apiman.gateway.engine.policies.config.rates.RateLimitingPeriod;
import io.apiman.gateway.engine.policies.i18n.Messages;
import io.apiman.gateway.engine.policy.IPolicyChain;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.gateway.engine.rates.RateBucketPeriod;

/**
 * Policy that enforces rate limits.
 *
 * @author eric.wittmann@redhat.com
 */
public class RateLimitingPolicy extends AbstractMappedPolicy<RateLimitingConfig> {
    
    /**
     * Constructor.
     */
    public RateLimitingPolicy() {
    }

    /**
     * @see io.apiman.gateway.engine.policy.AbstractPolicy#getConfigurationClass()
     */
    @Override
    protected Class<RateLimitingConfig> getConfigurationClass() {
        return RateLimitingConfig.class;
    }
    
    /**
     * @see io.apiman.gateway.engine.policies.AbstractMappedPolicy#doApply(io.apiman.gateway.engine.beans.ServiceRequest, io.apiman.gateway.engine.policy.IPolicyContext, java.lang.Object, io.apiman.gateway.engine.policy.IPolicyChain)
     */
    @Override
    protected void doApply(final ServiceRequest request, final IPolicyContext context, final RateLimitingConfig config,
            final IPolicyChain<ServiceRequest> chain) {
        String bucketId = createBucketId(request, config);
        RateBucketPeriod period = getPeriod(config);

        // Couldn't get a bucket id?  It means no user was found.
        if (bucketId == null) {
            IPolicyFailureFactoryComponent failureFactory = context.getComponent(IPolicyFailureFactoryComponent.class);
            PolicyFailure failure = failureFactory.createFailure(PolicyFailureType.Other, PolicyFailureCodes.NO_USER_FOR_RATE_LIMITING, Messages.i18n.format("RateLimitingPolicy.NoUser")); //$NON-NLS-1$
            chain.doFailure(failure);
            return;
        }
        
        IRateLimiterComponent rateLimiter = context.getComponent(IRateLimiterComponent.class);
        rateLimiter.accept(bucketId, period, config.getLimit(), new IAsyncResultHandler<Boolean>() {
            @Override
            public void handle(IAsyncResult<Boolean> result) {
                if (result.isError()) {
                    chain.throwError(result.getError());
                } else {
                    boolean accepted = result.getResult();
                    if (accepted) {
                        chain.doApply(request);
                    } else {
                        IPolicyFailureFactoryComponent failureFactory = context.getComponent(IPolicyFailureFactoryComponent.class);
                        PolicyFailure failure = failureFactory.createFailure(PolicyFailureType.Other, PolicyFailureCodes.RATE_LIMIT_EXCEEDED, Messages.i18n.format("RateLimitingPolicy.RateExceeded")); //$NON-NLS-1$
                        chain.doFailure(failure);
                    }
                }
            }
        });
    }

    /**
     * Creates the ID of the rate bucket to use.  The ID is composed differently 
     * depending on the configuration of the policy.
     * 
     * @param request
     * @param config
     */
    private String createBucketId(ServiceRequest request, RateLimitingConfig config) {
        if (config.getGranularity() == RateLimitingGranularity.User) {
            String header = config.getUserHeader();
            String user = request.getHeaders().get(header);
            if (user == null) {
                // policy failure
                return null;
            } else {
                StringBuilder builder = new StringBuilder();
                builder.append(request.getApiKey());
                builder.append("||USER||"); //$NON-NLS-1$
                builder.append(request.getContract().getApplication().getOrganizationId());
                builder.append("||"); //$NON-NLS-1$
                builder.append(request.getContract().getApplication().getApplicationId());
                builder.append("||"); //$NON-NLS-1$
                builder.append(user);
                return builder.toString();
            }
        } else if (config.getGranularity() == RateLimitingGranularity.Application) {
            StringBuilder builder = new StringBuilder();
            builder.append(request.getApiKey());
            builder.append("||APP||"); //$NON-NLS-1$
            builder.append(request.getContract().getApplication().getOrganizationId());
            builder.append("||"); //$NON-NLS-1$
            builder.append(request.getContract().getApplication().getApplicationId());
            return builder.toString();
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(request.getApiKey());
            builder.append("||SERVICE||"); //$NON-NLS-1$
            builder.append(request.getContract().getService().getOrganizationId());
            builder.append("||"); //$NON-NLS-1$
            builder.append(request.getContract().getService().getServiceId());
            return builder.toString();
        }
    }

    /**
     * Gets the appropriate bucket period from the config.
     * @param config
     */
    private RateBucketPeriod getPeriod(RateLimitingConfig config) {
        RateLimitingPeriod period = config.getPeriod();
        switch (period) {
        case Second:
            return RateBucketPeriod.Second;
        case Day:
            return RateBucketPeriod.Day;
        case Hour:
            return RateBucketPeriod.Hour;
        case Minute:
            return RateBucketPeriod.Minute;
        case Month:
            return RateBucketPeriod.Month;
        case Year:
            return RateBucketPeriod.Year;
        default:
            return RateBucketPeriod.Month;
        }
    }

}
