package cn.org.alan.exam.service.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthServiceImplTest {

    @Test
    public void extractsBrowserDeviceInformation() {
        assertEquals("Windows NT 10.0",
                AuthServiceImpl.extractDeviceType("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/126.0"));
    }

    @Test
    public void fallsBackWhenUserAgentIsMissingOrUnrecognized() {
        assertEquals("未知设备", AuthServiceImpl.extractDeviceType(null));
        assertEquals("未知设备", AuthServiceImpl.extractDeviceType("Codex Phase3 Smoke Test"));
    }
}
