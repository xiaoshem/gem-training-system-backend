package cn.org.alan.exam.utils;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.utils.security.SysUserDetails;
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

    @Test
    public void shouldReturnUserIdForAuthenticatedUser() {
        User user = new User();
        user.setId(168);
        SysUserDetails userDetails = new SysUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        "",
                        Collections.singletonList(new SimpleGrantedAuthority("role_teacher"))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals(Integer.valueOf(168), SecurityUtil.getUserId());
    }

    @Test(expected = ServiceRuntimeException.class)
    public void shouldRejectAnonymousPrincipalWithFriendlyBusinessException() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "anonymousUser",
                        "",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityUtil.getUserId();
    }
}
