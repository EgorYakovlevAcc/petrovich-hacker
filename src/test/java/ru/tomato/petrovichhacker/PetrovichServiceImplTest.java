package ru.tomato.petrovichhacker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;
import ru.tomato.petrovichhacker.service.PetrovichService;

@SpringBootTest
class PetrovichServiceImplTest {
    @Autowired
    private PetrovichService petrovichService;

    @Test
    void auth() {
        Assert.isTrue(this.petrovichService.auth(), "Auth is failed.");
    }

    @Test
    void checkValues() {
        this.petrovichService.checkValues();
    }
}
