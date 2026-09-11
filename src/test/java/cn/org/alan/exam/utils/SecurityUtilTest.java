package cn.org.alan.exam.utils;

import org.junit.After;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class SecurityUtilTest {

    @After
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldReturnAuditorRoleCode() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "auditor",
                        "",
                        Collections.singletonList(new SimpleGrantedAuthority("role_auditor"))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals(Integer.valueOf(4), SecurityUtil.getRoleCode());
    }
}
