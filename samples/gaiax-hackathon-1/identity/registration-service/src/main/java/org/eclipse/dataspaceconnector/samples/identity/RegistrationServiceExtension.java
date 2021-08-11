/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.samples.identity;

import org.eclipse.dataspaceconnector.iam.ion.IonClientImpl;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.DidDocument;
import org.eclipse.dataspaceconnector.iam.ion.spi.DidStore;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.iam.ObjectStore;
import org.eclipse.dataspaceconnector.spi.iam.RegistrationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Map;
import java.util.Set;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class RegistrationServiceExtension implements ServiceExtension {
    @EdcSetting
    private static final String EDC_SETTING_CRAWLER_INTERVAL_MIN = "edc.ion.crawler.interval-minutes";
    private ServiceExtensionContext context;
    private Scheduler quartzScheduler;

    @Override
    public Set<String> provides() {
        return Set.of("edc:registration-service");
    }

    @Override
    public Set<String> requires() {
        return Set.of("edc:did-documentstore");
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
        this.context = context;
        var didDocumentStore = (DidStore) context.getService(ObjectStore.class);


        // create the registration service, which offers a REST API
        var regSrv = new RegistrationServiceImpl(context.getMonitor(), didDocumentStore);
        context.registerService(RegistrationService.class, regSrv);

        // register the service as REST controller
        var webService = context.getService(WebService.class);
        webService.registerController(regSrv);

        // create the crawler that periodically browses ION for new DIDs
        var ionClient = new IonClientImpl(context.getTypeManager());

        context.getMonitor().info("RegistrationService ready to go");
    }

    @Override
    public void start() {
        DidStore didStore = (DidStore) context.getService(ObjectStore.class);
        try {
            quartzScheduler = StdSchedulerFactory.getDefaultScheduler();
            quartzScheduler.start();

            var minutes = Integer.parseInt(context.getSetting(EDC_SETTING_CRAWLER_INTERVAL_MIN, "30"));

            scheduleCrawler(minutes, didStore, context.getMonitor());
            context.getMonitor().info("Started periodic crawling of the ION database every " + minutes + " minutes");

        } catch (SchedulerException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void shutdown() {
        try {
            context.getMonitor().info("Stopping ION crawler");
            quartzScheduler.shutdown();
            context.getMonitor().info("Stopped ION crawler");
        } catch (SchedulerException e) {
            throw new EdcException(e);
        }

    }

    private void scheduleCrawler(int intervalMinutes, ObjectStore<DidDocument> objectStore, Monitor monitor) throws SchedulerException {
        JobDetail job = newJob(CrawlerJob.class)
                .setJobData(new JobDataMap(Map.of("STORE", objectStore, "MONITOR", monitor)))
                .withIdentity("ion-crawler-job", "ion")
                .build();
        Trigger trigger = newTrigger()
                .withIdentity("ion-crawler-trigger", "ion")
                .startNow()
                .withSchedule(simpleSchedule().withIntervalInMinutes(intervalMinutes).repeatForever())
                .build();

        quartzScheduler.scheduleJob(job, trigger);
    }

}