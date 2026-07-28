package com.novelkeep.novel.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class NovelSeedDataInitializer implements ApplicationRunner {

    private final NovelService novelService;

    public NovelSeedDataInitializer(NovelService novelService) {
        this.novelService = novelService;
    }

    @Override
    public void run(ApplicationArguments args) {
        novelService.createSamplesIfEmpty();
    }
}
