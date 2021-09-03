package ru.tomato.petrovichhacker.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StopWatch;
import ru.tomato.petrovichhacker.service.GoodsAvailabilityChecker;
import ru.tomato.petrovichhacker.service.PetrovichService;

import java.util.Date;

public class GoodsAvailabilityCheckerImpl implements GoodsAvailabilityChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoodsAvailabilityCheckerImpl.class);
    private PetrovichService petrovichService;

    public GoodsAvailabilityCheckerImpl(PetrovichService petrovichService) {
        this.petrovichService = petrovichService;
    }

    @Override
    @Scheduled(fixedRateString = "${petrovichhacker.rate}000")
    public void check() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("checkValues task");
        LOGGER.info("Start execute scheduled goods amount check: [{}]", new Date());
        this.petrovichService.checkValues();
        stopWatch.stop();
        LOGGER.info("CheckValues spends: {} seconds", stopWatch.getTotalTimeSeconds());
    }
}
